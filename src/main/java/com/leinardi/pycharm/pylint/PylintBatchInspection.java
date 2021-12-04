package com.leinardi.pycharm.pylint;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ex.ExternalAnnotatorBatchInspection;
import org.jetbrains.annotations.NotNull;

/**
 * By itself, the `PylintAnnotator` class does not provide support for the explicit "Inspect code" feature.
 *
 * This class uses `ExternalAnnotatorBatchInspection` middleware to provides that functionality.
 *
 * Modeled after `com.jetbrains.python.inspections.PyPep8Inspection`
 */
public class PylintBatchInspection extends LocalInspectionTool implements ExternalAnnotatorBatchInspection {
    public static final String INSPECTION_SHORT_NAME = "Pylint";

    @Override
    public @NotNull String getShortName() {
        return INSPECTION_SHORT_NAME;
    }
}
