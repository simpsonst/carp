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

import java.io.IOException;

/**
 *
 * @author simpsons
 */
class PositionTracker implements Appendable {
    private final Appendable base;

    private final int tabWidth;

    private final boolean tabConversion;

    public PositionTracker(Appendable base, int tabWidth,
                           boolean tabConversion) {
        this.base = base;
        this.tabWidth = tabWidth;
        this.tabConversion = tabConversion;
    }

    private int pos = 0;

    public int pos() {
        return pos;
    }

    @Override
    public Appendable append(CharSequence csq) throws IOException {
        return appendUnchecked(csq, 0, csq.length());
    }

    @Override
    public Appendable append(CharSequence csq, int start, int end)
        throws IOException {
        check(csq, start, end);
        return appendUnchecked(csq, start, end);
    }

    public Appendable appendUnchecked(CharSequence csq, int start, int end)
        throws IOException {
        int last = start;
        for (int i = start; i < end; i++) {
            char c = csq.charAt(i);
            switch (c) {
            case '\t':
            case '\n':
            case '\r':
            case '\f':
                if (i > last) {
                    indent();
                    base.append(csq, last, i);
                    pos += i - last;
                }
                append(c);
                last = i + 1;
                break;
            }
        }
        if (end - last > 0) {
            indent();
            base.append(csq, last, end);
            pos += end - last;
        }
        return this;
    }

    @Override
    public Appendable append(char c) throws IOException {
        indent();
        switch (c) {
        case '\t':
            final int step = tabWidth - pos % tabWidth;
            if (tabConversion) {
                String sp = " ".repeat(step);
                base.append(sp);
            } else {
                base.append(c);
            }
            pos += step;
            break;

        case '\n':
        case '\r':
        case '\f':
            base.append(c);
            pos = 0;
            break;

        default:
            base.append(c);
            pos++;
            break;
        }
        return this;
    }

    private String indent = "";

    private int indentPos = 0;

    private void indent() throws IOException {
        if (pos > 0) return;
        base.append(indent);
        pos = indentPos;
    }

    public void setIndent(CharSequence csq) {
        setIndentUnchecked(csq, 0, csq.length());
    }

    public void setIndent(CharSequence csq, int start, int end) {
        check(csq, start, end);
        setIndentUnchecked(csq, start, end);
    }

    private static void check(CharSequence csq, int start, int end) {
        if (start < 0)
            throw new IndexOutOfBoundsException("-ve start: " + start);
        if (end < 0) throw new IndexOutOfBoundsException("-ve end: " + end);
        if (start > end)
            throw new IndexOutOfBoundsException("start/end inverted: " + start
                + " > " + end);
        if (end > csq.length())
            throw new IndexOutOfBoundsException("end too big: " + end + " > "
                + csq.length());
    }

    private void setIndentUnchecked(CharSequence csq, int start, int end) {
        StringBuilder mod = new StringBuilder();
        int len = 0;
        for (int i = start; i < end; i++) {
            char c = csq.charAt(i);
            switch (c) {
            default:
                mod.append(c);
                len++;
                break;

            case '\t':
                final int adv = tabWidth - len % tabWidth;
                len += adv;
                if (tabConversion)
                    mod.append(" ".repeat(adv));
                else
                    mod.append('\t');
                break;

            case '\n':
            case '\r':
            case '\f':
                throw new IllegalArgumentException("indent contains"
                    + " line reset after [" + csq.subSequence(start, i) + "]");
            }
        }

        indent = mod.toString();
        indentPos = len;
    }
}
