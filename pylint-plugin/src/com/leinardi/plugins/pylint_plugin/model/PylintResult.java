package com.leinardi.plugins.pylint_plugin.model;

import java.util.ArrayList;

public class PylintResult {
    private int retCode;
    private int violationsCount;
    private String rating;
    private ArrayList<PylintViolation> violations;

    public PylintResult(int code, int violationsCount, ArrayList<PylintViolation> violations, String rating) {
        this.retCode = code;
        this.violations = violations;
        this.violationsCount = violationsCount;
        this.rating = rating;
    }

    public int getRetCode() {
        return this.retCode;
    }

    public ArrayList<PylintViolation> getViolations() {
        return violations;
    }

    public int getViolationsCount() {
        return violationsCount;
    }

    public String getRating() {
        return rating;
    }
}
