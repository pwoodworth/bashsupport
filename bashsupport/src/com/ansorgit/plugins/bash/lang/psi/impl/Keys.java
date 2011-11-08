/*******************************************************************************
 * Copyright 2011 Joachim Ansorg, mail@ansorg-it.com
 * File: Keys.java, Class: Keys
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

package com.ansorgit.plugins.bash.lang.psi.impl;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.KeyWithDefaultValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;

import java.util.Collection;
import java.util.List;

/**
 * User: jansorg
 * Date: 06.02.11
 * Time: 18:27
 */
public interface Keys {
    final Key<Multimap<VirtualFile, PsiElement>> visitedIncludeFiles = new KeyWithDefaultValue<Multimap<VirtualFile, PsiElement>>("visitedIncludeFiles") {
        @Override
        public Multimap<VirtualFile, PsiElement> getDefaultValue() {
            return Multimaps.newListMultimap(Maps.<VirtualFile, Collection<PsiElement>>newHashMap(), new Supplier<List<PsiElement>>() {
                public List<PsiElement> get() {
                    return Lists.newLinkedList();
                }
            });
        }
    };
}
