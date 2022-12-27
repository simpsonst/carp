// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2021, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.carp.model;

import java.util.Formatter;

/**
 * Provides an indented context for formatted output. The methods
 * {@link #tab(String)}, {@link #comment()} and {@link #documentation()}
 * return auto-closable objects that should be used with
 * try-with-resources constructs.
 *
 * @author simpsons
 */
public final class TextFile implements AutoCloseable {
    private final PositionTracker tracker;

    private String indent = "";

    private final Formatter out;

    /**
     * Create a context from a character-sequence consumer.
     * 
     * @param base the consumer of character sequences
     * 
     * @param tabWidth the width of a tab character
     * 
     * @param tabConversion whether tabs are to be converted to spaces
     */
    public TextFile(Appendable base, int tabWidth, boolean tabConversion) {
        this.tracker = new PositionTracker(base, tabWidth, tabConversion);
        this.out = new Formatter(this.tracker);
    }

    /**
     * Format a string and write it to this context.
     * 
     * @param fmt the string format, as defined by
     * {@link Formatter#format(String, Object...)}
     * 
     * @param args arguments referenced by format specifiers in the
     * format string
     * 
     * @return this object
     */
    public TextFile format(String fmt, Object... args) {
        out.format(fmt, args);
        return this;
    }

    /**
     * Append characters to the indent.
     * 
     * @param indent the additional indentation
     * 
     * @return an object which, when closed, returns the context to the
     * previous indent
     */
    public Tab tab(String indent) {
        final String oldIndent = this.indent;
        this.indent = oldIndent + indent;
        tracker.setIndent(this.indent);
        return new Tab(() -> {
            this.indent = oldIndent;
            tracker.setIndent(oldIndent);
        });
    }

    /**
     * Create a context for a multi-line comment.
     * 
     * @return the embedded context
     */
    public Tab comment() {
        final String oldIndent = this.indent;
        format("/* ");
        this.indent = " ".repeat(tracker.pos() - 3) + " * ";
        tracker.setIndent(this.indent);
        return new Tab(() -> {
            format(" */");
            this.indent = oldIndent;
            tracker.setIndent(oldIndent);
        });
    }

    /**
     * Create a context for a documentation comment.
     * 
     * @return the embedded context
     */
    public Tab documentation() {
        final String oldIndent = this.indent;
        format("/**");
        this.indent = " ".repeat(tracker.pos() - 3) + " * ";
        format("%n");
        tracker.setIndent(this.indent);
        return new Tab(() -> {
            this.indent = oldIndent;
            tracker.setIndent(oldIndent);
            format(" */%n");
        });
    }

    /**
     * Flush the formatter.
     */
    @Override
    public void close() {
        out.flush();
    }
}
