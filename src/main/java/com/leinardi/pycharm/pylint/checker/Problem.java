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

import com.intellij.codeInspection.InspectionManager;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.psi.PsiElement;
import com.leinardi.pycharm.pylint.plapi.SeverityLevel;
import org.jetbrains.annotations.NotNull;

public class Problem {
    private final PsiElement target;
    private final SeverityLevel severityLevel;
    private final int line;
    private final int column;
    private final String symbol;
    private final String message;
    private final boolean afterEndOfLine;
    private final boolean suppressErrors;

    public Problem(@NotNull final PsiElement target,
                   @NotNull final String message,
                   @NotNull final SeverityLevel severityLevel,
                   final int line,
                   final int column,
                   final String symbol,
                   final boolean afterEndOfLine,
                   final boolean suppressErrors) {
        this.target = target;
        this.message = message;
        this.severityLevel = severityLevel;
        this.line = line;
        this.column = column;
        this.symbol = symbol;
        this.afterEndOfLine = afterEndOfLine;
        this.suppressErrors = suppressErrors;
    }

    @NotNull
    public ProblemDescriptor toProblemDescriptor(final InspectionManager inspectionManager) {
        return inspectionManager.createProblemDescriptor(target,
                "inspection.message, message()",
                null, problemHighlightType(), false, afterEndOfLine);
    }

    @NotNull
    public String message() {
        return message;
    }

    @NotNull
    public String symbol() {
        return symbol;
    }

    @NotNull
    public SeverityLevel severityLevel() {
        return severityLevel;
    }

    public int line() {
        return line;
    }

    public int column() {
        return column;
    }

    private ProblemHighlightType problemHighlightType() {
        if (!suppressErrors) {
            switch (severityLevel()) {
                case ERROR:
                case FATAL:
                    return ProblemHighlightType.ERROR;
                case WARNING:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
                case REFACTOR:
                case CONVENTION:
                    return ProblemHighlightType.WEAK_WARNING;
                default:
                    return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
            }
        }
        return ProblemHighlightType.GENERIC_ERROR_OR_WARNING;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Problem problem = (Problem) o;

        if (line != problem.line) {
            return false;
        }
        if (column != problem.column) {
            return false;
        }
        if (afterEndOfLine != problem.afterEndOfLine) {
            return false;
        }
        if (suppressErrors != problem.suppressErrors) {
            return false;
        }
        if (!target.equals(problem.target)) {
            return false;
        }
        if (severityLevel != problem.severityLevel) {
            return false;
        }
        return message.equals(problem.message);
    }

    @Override
    public int hashCode() {
        int result = target.hashCode();
        result = 31 * result + severityLevel.hashCode();
        result = 31 * result + line;
        result = 31 * result + column;
        result = 31 * result + message.hashCode();
        result = 31 * result + (afterEndOfLine ? 1 : 0);
        result = 31 * result + (suppressErrors ? 1 : 0);
        return result;
    }
}
