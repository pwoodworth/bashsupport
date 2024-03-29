/*
 * Copyright 2010 Joachim Ansorg, mail@ansorg-it.com
 * File: BashVarDefImpl.java, Class: BashVarDefImpl
 * Last modified: 2010-07-13
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.psi.impl.vars;

import com.ansorgit.plugins.bash.lang.LanguageBuiltins;
import com.ansorgit.plugins.bash.lang.lexer.BashTokenTypes;
import com.ansorgit.plugins.bash.lang.psi.BashVisitor;
import com.ansorgit.plugins.bash.lang.psi.api.ResolveProcessor;
import com.ansorgit.plugins.bash.lang.psi.api.command.BashCommand;
import com.ansorgit.plugins.bash.lang.psi.api.function.BashFunctionDef;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVar;
import com.ansorgit.plugins.bash.lang.psi.api.vars.BashVarDef;
import com.ansorgit.plugins.bash.lang.psi.impl.BashPsiElementImpl;
import com.ansorgit.plugins.bash.lang.psi.util.BashChangeUtil;
import com.ansorgit.plugins.bash.lang.psi.util.BashIdentifierUtil;
import com.ansorgit.plugins.bash.lang.psi.util.BashPsiUtils;
import com.ansorgit.plugins.bash.settings.BashProjectSettings;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import static com.ansorgit.plugins.bash.lang.LanguageBuiltins.*;

/**
 * Date: 14.04.2009
 * Time: 17:02:37
 *
 * @author Joachim Ansorg
 */
public class BashVarDefImpl extends BashPsiElementImpl implements BashVarDef, BashVar {
    private static final Logger log = Logger.getInstance("#Bash.BashVarDef");

    private static final TokenSet accepted = TokenSet.create(
            BashTokenTypes.WORD, BashTokenTypes.ASSIGNMENT_WORD, BashTokenTypes.ARRAY_ASSIGNMENT_WORD);
    private static final Object[] EMPTY_VARIANTS = new Object[0];


    public BashVarDefImpl(ASTNode astNode) {
        super(astNode, "Bash var def");
    }

    public String getName() {
        return findAssignmentWord().getText();
    }

    public PsiElement setName(@NotNull @NonNls String newName) throws IncorrectOperationException {
        if (!BashIdentifierUtil.isValidIdentifier(newName)) {
            throw new IncorrectOperationException("can't have an empty name");
        }

        PsiElement original = findAssignmentWord();
        PsiElement replacement = BashChangeUtil.createAssignmentWord(getProject(), newName);
        return BashPsiUtils.replaceElement(original, replacement);
    }

    public boolean isArray() {
        return findAssignmentWord() == BashTokenTypes.ARRAY_ASSIGNMENT_WORD;
    }

    /**
     * Returns either a assignment word element or a array assignment word.
     *
     * @return The element if available. Otherwise null.
     */
    @NotNull
    public PsiElement findAssignmentWord() {
        PsiElement element = findChildByType(accepted);
        if (element != null) {
            return element;
        }

        //if null we probably represent a single var without assignment, i.e. the var node is nested inside of
        //as parsed var
        PsiElement firstChild = getFirstChild();
        ASTNode childNode = firstChild != null ? firstChild.getNode() : null;

        ASTNode node = childNode != null ? childNode.findChildByType(accepted) : null;
        return node != null ? node.getPsi() : firstChild;
    }


    public boolean isFunctionScopeLocal() {
        //var defs on global level can not be local
        PsiElement enclosingBlock = BashPsiUtils.findEnclosingBlock(this);
        if (enclosingBlock instanceof PsiFile) {
            return false;
        }

        //check if the command is a local-var defining command, e.g. local
        final PsiElement context = getContext();
        if (context instanceof BashCommand) {
            final BashCommand parentCmd = (BashCommand) context;
            if (parentCmd.isVarDefCommand() && localVarDefCommands.contains(parentCmd.getReferencedName())) {
                return true;
            }
        }

        //although this variable has no direct local command,
        //it's still possible that an earlier usage of the local command declared this
        //variable as function local

        //we HAVE to disable the calls to isFunctionLocal() in the var processor. Otherwise
        //we would get an infinite recursion
        final ResolveProcessor processor = new BashVarProcessor(this, false);
        BashFunctionDef functionLocalScope = BashPsiUtils.findBroadestVarDefFunctionDefScope(this);
        if (functionLocalScope == null) {
            return false;
        }

        boolean walkOn = PsiTreeUtil.treeWalkUp(processor, this, functionLocalScope, ResolveState.initial());
        PsiElement element = !walkOn ? processor.getBestResult(false, this) : null;

        if (log.isDebugEnabled()) {
            log.debug("isFunctionLocal: resolve result: " + this + " resolved to " + element);
        }

        return element instanceof BashVarDef
                && !this.equals(element)
                && ((BashVarDef) element).isFunctionScopeLocal();
    }

    public boolean hasAssignmentValue() {
        PsiElement element = findAssignmentWord();
        return element != null && BashPsiUtils.nodeType(element) == BashTokenTypes.ASSIGNMENT_WORD;
    }

    public boolean isCommandLocal() {
        final PsiElement context = getContext();
        if (context instanceof BashCommand) {
            final BashCommand parentCmd = (BashCommand) context;
            return !parentCmd.isPureAssignment() && !parentCmd.isVarDefCommand();
        }

        return false;
    }

    @Override
    public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                       @NotNull ResolveState resolveState,
                                       PsiElement lastParent,
                                       @NotNull PsiElement place) {
        return processor.execute(this, resolveState);
    }

    public PsiElement getNameIdentifier() {
        return findAssignmentWord();
    }

    public String getReferencedName() {
        return getName();
    }

    public PsiElement getElement() {
        return this;
    }

    @Override
    public PsiReference getReference() {
        return this;
    }

    public TextRange getRangeInElement() {
        return TextRange.from(0, getReferencedName().length());
    }

    public PsiElement resolve() {
        if (isCommandLocal()) {
            return null;
        }

        final String varName = getName();
        if (varName == null) {
            return null;
        }

        PsiElement resolveScope = isFunctionScopeLocal() ? findFunctionScope() : getContainingFile();

        BashVarProcessor processor = new BashVarProcessor(this, true);
        if (!BashPsiUtils.varResolveTreeWalkUp(processor, this, resolveScope, ResolveState.initial())) {
            return processor.getBestResult(false, this);
        }

        return null;
    }

    public PsiElement findFunctionScope() {
        return PsiTreeUtil.getContextOfType(this, BashFunctionDef.class, true);
    }

    public String getCanonicalText() {
        return null;
    }

    public PsiElement handleElementRename(String newName) throws IncorrectOperationException {
        return setName(newName);
    }

    public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        throw new IncorrectOperationException("unsupported");
    }

    public boolean isReferenceTo(PsiElement element) {
        //local vars can't be resolved in the script
        if (isCommandLocal()) {
            return false;
        }

        //if it's a var def and appears earlier in the same context or above it's a valid ref
        if (!(element instanceof BashVarDef)) {
            return false;
        }

        BashVarDef def = (BashVarDef) element;

        String myName = getName();
        return myName != null && myName.equals(def.getName()) && BashVarUtils.isInDefinedScope(this, def);
    }

    @NotNull
    public Object[] getVariants() {
        return EMPTY_VARIANTS;
    }

    public boolean isSoft() {
        return false;
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof BashVisitor) {
            ((BashVisitor) visitor).visitVarDef(this);
        } else {
            visitor.visitElement(this);
        }
    }

    public boolean isBuiltinVar() {
        boolean isBash_v4 = BashProjectSettings.storedSettings(getProject()).isSupportBash4();

        String name = getReferencedName();
        boolean v3_var = bashShellVars.contains(name) || bourneShellVars.contains(name);

        return isBash_v4 ? v3_var || bashShellVars_v4.contains(name) : v3_var;
    }

    public boolean isComposedVar() {
        return false;
    }

    public boolean isReadonly() {
        //fixme support "declare -r" style readonly variables

        PsiElement context = getParent();
        if (context instanceof BashCommand) {
            BashCommand command = (BashCommand) context;

            return command.isInternalCommand() && LanguageBuiltins.readonlyVarDefCommands.contains(command.getReferencedName());
        }

        return false;
    }
}
