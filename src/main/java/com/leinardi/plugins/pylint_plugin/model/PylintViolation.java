package com.leinardi.plugins.pylint_plugin.model;

public class PylintViolation {
    public static final int DEBUG = 0;
    public static final int REFACTOR = 1;
    public static final int CONVENTION = 2;
    public static final int WARNING = 3;
    public static final int ERROR = 4;
    public static final int FATAL = 5;
    public static final int HEADER = -1; // not a real error, just a separator
    private static final String SPLIT_REG_EX = ": [RCWEF]:";

    private int level;
    private String file;
    private int line;
    private int column;
    private String message;
    private String raw;
    // used only by headers
    private int[] violationsCounts;
    private boolean collapsed;

    public PylintViolation(String raw, int level) {
        this.raw = raw;
        this.level = level;
        assert ((level == DEBUG)
                | (level == REFACTOR)
                | (level == CONVENTION)
                | (level == WARNING)
                | (level == ERROR)
                | (level == FATAL)
        );
        if (level == DEBUG) {
            return;
        }
        String loc;
        String[] pair;
        System.out.println(level);
        System.out.println(raw);
        pair = raw.split(SPLIT_REG_EX);
        loc = pair[0];
        if (pair.length == 1) {
            message = "";
        } else {
            message = pair[1];
        }
        String[] parts = loc.split(":");
        file = parts[0];
        if (parts.length == 1) {
            line = 0;
            column = 0;
        } else {
            try {
                line = Integer.parseInt(parts[1]);
            } catch (NumberFormatException e) {
                line = 0;
            }
            if (parts.length == 2) {
                column = 0;
            } else {
                try {
                    column = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    column = 0;
                }
            }
        }
    }

    public PylintViolation(String file, int level, int[] violationsCounts) {
        this.level = level;
        this.violationsCounts = violationsCounts;
        this.file = file;
        collapsed = false;
        assert (level == HEADER);
    }

    public String toString() {
        assert (level == DEBUG);
        // all other levels should be processed
        // by our custom renderer
        return raw;
    }

    public void toggle() {
        assert (level == HEADER);
        collapsed = !collapsed;
    }

    public int getLevel() {
        return level;
    }

    public String getFile() {
        return file;
    }

    public int getLine() {
        return line;
    }

    public int getColumn() {
        return column;
    }

    public String getMessage() {
        return message;
    }

    public String getRaw() {
        return raw;
    }

    public int[] getViolationsCounts() {
        return violationsCounts;
    }

    public boolean isCollapsed() {
        return collapsed;
    }

    public boolean isViolation() {
        return level == DEBUG
                || level == REFACTOR
                || level == CONVENTION
                || level == WARNING
                || level == ERROR
                || level == FATAL;
    }
}
