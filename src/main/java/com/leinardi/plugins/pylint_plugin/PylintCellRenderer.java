package com.leinardi.plugins.pylint_plugin;

import com.leinardi.plugins.pylint_plugin.model.PylintViolation;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

class PylintCellRenderer extends ColoredListCellRenderer {

    public PylintCellRenderer() {
        setOpaque(true);
    }

    @Override
    protected void customizeCellRenderer(@NotNull JList list, Object value, int index, boolean selected, boolean hasFocus) {
        int LINE_WIDTH = 5;
        int GAP = 10;
        String TAB = "     ";
        PylintViolation error = (PylintViolation) value;
        setFont(list.getFont());
        setToolTipText(null);
        boolean isViolation = error.isViolation() && error.getLevel() != PylintViolation.DEBUG;
        boolean collapsed = error.isCollapsed();
        setPreferredSize(new Dimension(-1, 5));
        if (error.getLevel() == PylintViolation.HEADER) {
            if (collapsed) {
                setIcon(UIManager.getIcon("Tree.collapsedIcon"));
                setIconTextGap(GAP);
            } else {
                setIcon(UIManager.getIcon("Tree.expandedIcon"));
                setIconTextGap(GAP);
            }
        } else {
            setIcon(null);
        }
        if (error.getLevel() == PylintViolation.HEADER) {
            String file = error.getFile();
            String counts = concatViolations(error);
            append(file + " ");
            append(counts, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                    new Color(255, 100, 100)));
        } else if (isViolation) {
            String line;
            if (error.getLine() > 0) {
                line = String.format("%-4d", error.getLine());
            } else {
                line = "";
            }
            append(TAB + String.format("%1$-" + LINE_WIDTH + "s", line),
                    new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                            new JBColor(new Color(PylintTerminal.GRAY), new Color(PylintTerminal.DARK_GRAY))));
            String[] chunks = error.getMessage().split("\"");
            boolean italic = false;
            for (String chunk : chunks) {
                if (italic) {
                    append(chunk, SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES);
                } else {
                    append(chunk);
                }
                italic = !italic;
            }
        } else {
            // something ill-formatted
            append(error.getRaw());
        }
    }

    public static String concatViolations(PylintViolation error) {
        StringBuilder violations = new StringBuilder();
        if (error.getViolationsCounts()[PylintViolation.FATAL] > 0) {
            violations.append(String.format("%d fatal,", error.getViolationsCounts()[PylintViolation.FATAL]));
        }
        if (error.getViolationsCounts()[PylintViolation.ERROR] > 0) {
            violations.append(String.format("%d error,", error.getViolationsCounts()[PylintViolation.ERROR]));
        }
        if (error.getViolationsCounts()[PylintViolation.WARNING] > 0) {
            violations.append(String.format("%d warning,", error.getViolationsCounts()[PylintViolation.WARNING]));
        }
        if (error.getViolationsCounts()[PylintViolation.CONVENTION] > 0) {
            violations.append(String.format("%d convention,", error.getViolationsCounts()[PylintViolation.CONVENTION]));
        }
        if (error.getViolationsCounts()[PylintViolation.REFACTOR] > 0) {
            violations.append(String.format("%d refactor,", error.getViolationsCounts()[PylintViolation.REFACTOR]));
        }
        return new String(violations.deleteCharAt(violations.length() - 1));
    }
}
