package com.regnosys.rosetta.generator.python.util;

/**
 * A utility class to help generate Python code with strict indentation.
 * This class maintains an internal indentation level and ensures all added
 * lines are prefixed with the correct number of spaces.
 */
public class PythonCodeWriter {
    private final StringBuilder sb = new StringBuilder();
    private int level = 0;
    private static final String SPACES = "    "; // Python standard 4-space indent

    /**
     * Increases the indentation level by 1.
     */
    public void indent() {
        level++;
    }

    /**
     * Decreases the indentation level by 1, to a minimum of 0.
     */
    public void unindent() {
        if (level > 0) {
            level--;
        }
    }

    /**
     * Appends text directly to the buffer without any indentation or newline.
     * Useful for building a single line piece-by-piece.
     * 
     * @param text The text to append.
     */
    public void append(String text) {
        if (text != null) {
            sb.append(text);
        }
    }

    /**
     * Appends only the current indentation prefix to the buffer.
     */
    public void appendIndent() {
        sb.append(SPACES.repeat(level));
    }

    /**
     * Appends a single line with the current indentation level.
     * 
     * @param line The content of the line to append.
     */
    public void appendLine(String line) {
        if (line == null)
            return;
        // Skip indentation if the line is empty (to avoid trailing whitespace on truly
        // blank lines)
        // BUT if we want indented blank lines, we should pass an empty string and
        // explicitly handle it.
        // Actually, let's allow appendLine("") to produce indented whitespace for Xtend
        // compatibility.
        sb.append(SPACES.repeat(level));
        sb.append(line).append("\n");
    }

    /**
     * Appends a block of text, re-indenting each line within the block
     * to match the current indentation level.
     * 
     * @param block The multi-line block of text to append.
     */
    public void appendBlock(String block) {
        if (block == null || block.isEmpty()) {
            return;
        }
        block.lines().forEach(this::appendLine);
    }

    /**
     * Appends a simple newline character WITHOUT any indentation.
     * Use appendLine("") if you want an indented blank line.
     */
    public void newLine() {
        sb.append("\n");
    }

    /**
     * Helper method to execute a block of code within an increased indentation
     * level.
     * Automatically handles incrementing and decrementing the indentation.
     * 
     * @param runnable The block of code to execute.
     */
    public void withIndent(Runnable runnable) {
        indent();
        try {
            runnable.run();
        } finally {
            unindent();
        }
    }

    @Override
    public String toString() {
        return sb.toString();
    }
}
