/*
 * Copyright 2018 Roberto Leinardi.
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
 */

package com.leinardi.pycharm.pylint.checker;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.leinardi.pycharm.pylint.util.FileTypes;
import org.jetbrains.annotations.Nullable;

final class PsiFileValidator {

    private PsiFileValidator() {
    }

    public static boolean isScannable(@Nullable final PsiFile psiFile, final Project project) {
        ProjectFileIndex projectFileIndex = ProjectFileIndex.SERVICE.getInstance(project);
        return psiFile != null
                && psiFile.isValid()
                && psiFile.isPhysical()
                && hasDocument(psiFile)
                && isInSource(psiFile, projectFileIndex)
                && isValidFileType(psiFile);
        //                && isScannableIfTest(psiFile, pluginConfig)
        //                && modulesMatch(psiFile, module)
        //                && !isGenerated(psiFile);
    }

    private static boolean hasDocument(final PsiFile psiFile) {
        return PsiDocumentManager.getInstance(psiFile.getProject()).getDocument(psiFile) != null;
    }

    private static boolean isValidFileType(final PsiFile psiFile) {
        return FileTypes.isPython(psiFile.getFileType());
    }

    //    private static boolean isScannableIfTest(final PsiFile psiFile) {
    //        return !isTestClass(psiFile);
    //    }

    //    private static boolean isGenerated(final PsiFile psiFile) {
    //        return PythonProjectRootsUtil.isInGeneratedCode(psiFile.getVirtualFile(), psiFile.getProject());
    //    }

    private static boolean isInSource(final PsiFile psiFile, final ProjectFileIndex projectFileIndex) {
        return !projectFileIndex.isExcluded(psiFile.getVirtualFile());
    }

    //    private static boolean isTestClass(final PsiElement element) {
    //        final VirtualFile elementFile = element.getContainingFile().getVirtualFile();
    //        if (elementFile == null) {
    //            return false;
    //        }
    //
    //        final Module module = ModuleUtil.findModuleForPsiElement(element);
    //        if (module == null) {
    //            return false;
    //        }
    //
    //        final ModuleRootManager moduleRootManager = ModuleRootManager.getInstance(module);
    //        return moduleRootManager != null
    //                && moduleRootManager.getFileIndex().isInTestSourceContent(elementFile);
    //    }

    //    private static boolean modulesMatch(final PsiFile psiFile,
    //                                        final Optional<Module> module) {
    //        if (!module.isPresent()) {
    //            return true;
    //        }
    //        final Module elementModule = ModuleUtil.findModuleForPsiElement(psiFile);
    //        return elementModule != null && elementModule.equals(module.get());
    //    }

}
