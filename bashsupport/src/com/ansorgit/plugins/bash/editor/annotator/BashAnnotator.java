/*******************************************************************************
 * Copyright 2011 Joachim Ansorg, mail@ansorg-it.com
 * File: BashAnnotator.java, Class: BashAnnotator
 * Last modified: 2011-04-30 16:33
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.ansorgit.plugins.bash.editor.annotator;

import com.ansorgit.plugins.bash.editor.highlighting.BashSyntaxHighlighter;
import com.ansorgit.plugins.bash.lang.psi.api.BashBackquote;
import com.ansorgit.plugins.bash.lang.psi.api.BashString;
import com.ansorgit.plugins.bash.lang.psi.api.arithmetic.ArithmeticExpression;
import com.ansorgit.plugins.bash.lang.psi.api.arithmetic.IncrementExpression;
import com.ansorgit.plugins.bash.lang.psi.api.arithmetic.SimpleExpression;
import com.ansorgit.plugins.bash.lang.psi.api.command.BashCommand;
import com.ansorgit.plugins.bash.lang.psi.api.expression.BashSubshellCommand;
import com.ansorgit.plugins.bash.lang.psi.api.function.BashFunctionDef;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDoc;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDocEndMarker;
import com.ansorgit.plugins.bash.lang.psi.api.heredoc.BashHereDocStartMarker;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVar;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVarDef;
import com.ansorgit.plugins.bash.lang.psi.api.word.BashWord;
import com.intellij.lang.annotation.Annotation;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import org.jetbrains.annotations.NotNull;

/**
 * The annotator for the the Bash language.
 * It takes care of the advanced syntax highlighting options.
 * <p/>
 * Date: 12.04.2009
 * Time: 13:28:15
 *
 * @author Joachim Ansorg
 */
public class BashAnnotator implements Annotator {
    private final FunctionDefAnnotator functionAnnotator = new FunctionDefAnnotator();

    public void annotate(@NotNull PsiElement element, @NotNull AnnotationHolder annotationHolder) {
        if (element instanceof BashBackquote) {
            annotateBackquote(element, annotationHolder);
        } else if (element instanceof BashHereDoc) {
            annotateHereDoc((BashHereDoc) element, annotationHolder);
        } else if (element instanceof BashHereDocStartMarker) {
            annotateHereDocStart(element, annotationHolder);
        } else if (element instanceof BashHereDocEndMarker) {
            annotateHereDocEnd(element, annotationHolder);
        } else if (element instanceof BashFunctionDef) {
            functionAnnotator.annotate((BashFunctionDef) element, annotationHolder);
        } else if (element instanceof BashCommand) {
            annotateCommand((BashCommand) element, annotationHolder);
        } else if (element instanceof BashVarDef) {
            annotateVarDef((BashVarDef) element, annotationHolder);
        } else if (element instanceof BashVar) {
            BashVarAnnotator.annotateVar((BashVar) element, annotationHolder);
        } else if (element instanceof BashWord) {
            annotateWord(element, annotationHolder);
        } else if (element instanceof BashString) {
            annotateString(element, annotationHolder);
        } else if (element instanceof BashSubshellCommand) {
            annotateSubshell(element, annotationHolder);
        } else if (element instanceof IncrementExpression) {
            annotateArithmeticIncrement((IncrementExpression)element, annotationHolder);
        }
    }

    private void annotateArithmeticIncrement(IncrementExpression element, AnnotationHolder annotationHolder) {
        ArithmeticExpression first = element.subexpressions().get(0);

        if (first instanceof SimpleExpression && !(first.getFirstChild() instanceof BashVar)) {
            PsiElement operator = element.findOperatorElement();
            if (operator != null) {
                annotationHolder.createErrorAnnotation(operator, "This error only works on a variable and not on a value.");
            }
        }
    }

    private void annotateWord(PsiElement bashWord, AnnotationHolder annotationHolder) {
        //we have to mark the remaped tokens (which are words now) to have the default word formatting.
        Annotation annotation = annotationHolder.createInfoAnnotation(bashWord, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.NONE);
    }

    private void annotateString(PsiElement bashString, final AnnotationHolder holder) {
        final Annotation annotation = holder.createInfoAnnotation(TextRange.from(bashString.getTextOffset(), bashString.getTextLength()), null);
        annotation.setTextAttributes(BashSyntaxHighlighter.STRING);

        highlightVariables(bashString, holder);
    }

    private void highlightVariables(PsiElement containerElement, final AnnotationHolder holder) {
        new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(PsiElement element) {
                if (element instanceof BashVar) {
                    //containedVars.add((BashVar) containerElement);
                    Annotation infoAnnotation = holder.createInfoAnnotation(element, null);
                    infoAnnotation.setTextAttributes(BashSyntaxHighlighter.VAR_USE);
                }

                super.visitElement(element);
            }
        }.visitElement(containerElement);
    }

    private void annotateCommand(BashCommand bashCommand, AnnotationHolder annotationHolder) {
        PsiElement cmdElement = null;
        TextAttributesKey attributesKey = null;

        if (bashCommand.isFunctionCall()) {
            cmdElement = bashCommand.commandElement();
            attributesKey = BashSyntaxHighlighter.FUNCTION_CALL;
        } else if (bashCommand.isExternalCommand()) {
            cmdElement = bashCommand.commandElement();
            attributesKey = BashSyntaxHighlighter.EXTERNAL_COMMAND;
        } else if (bashCommand.isInternalCommand()) {
            cmdElement = bashCommand.commandElement();
            attributesKey = BashSyntaxHighlighter.INTERNAL_COMMAND;
        }

        if (cmdElement != null && attributesKey != null) {
            final Annotation annotation = annotationHolder.createInfoAnnotation(cmdElement, null);
            annotation.setTextAttributes(attributesKey);
        }
    }

    private void annotateBackquote(PsiElement element, AnnotationHolder holder) {
        final Annotation annotation = holder.createInfoAnnotation(element, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.BACKQUOTE);
    }

    private void annotateHereDoc(BashHereDoc element, AnnotationHolder annotationHolder) {
        final Annotation annotation = annotationHolder.createInfoAnnotation(element, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.HERE_DOC);
        annotation.setNeedsUpdateOnTyping(false);

        if (element.isEvaluatingVariables()) {
            highlightVariables(element, annotationHolder);
        }
    }

    private void annotateHereDocStart(PsiElement element, AnnotationHolder annotationHolder) {
        final Annotation annotation = annotationHolder.createInfoAnnotation(element, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.HERE_DOC_START);
    }

    private void annotateHereDocEnd(PsiElement element, AnnotationHolder annotationHolder) {
        final Annotation annotation = annotationHolder.createInfoAnnotation(element, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.HERE_DOC_END);
    }

    private void annotateVarDef(BashVarDef bashVarDef, AnnotationHolder annotationHolder) {
        final PsiElement identifier = bashVarDef.findAssignmentWord();
        if (identifier != null) {
            final Annotation annotation = annotationHolder.createInfoAnnotation(identifier, null);
            annotation.setTextAttributes(BashSyntaxHighlighter.VAR_DEF);
        }
    }

    private void annotateSubshell(PsiElement element, AnnotationHolder holder) {
        final Annotation annotation = holder.createInfoAnnotation(element, null);
        annotation.setTextAttributes(BashSyntaxHighlighter.SUBSHELL_COMMAND);
    }
}
