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

package uk.ac.lancs.carp.syntax.doc;

import uk.ac.lancs.syntax.TextPosition;

/**
 * Specifies the presence of a closing HTML tag in a documentation
 * comment.
 * 
 * @author simpsons
 */
public final class ClosingMarkupTag implements Content {
    /**
     * Specifies the name of the HTML tag.
     */
    public final String name;

    private final TextPosition start;

    private final TextPosition end;

    /**
     * Create a closing HTML tag.
     * 
     * @param start the start position of the tag in source
     * 
     * @param end the end position of the tag in source
     * 
     * @param name the tag name
     */
    public ClosingMarkupTag(TextPosition start, TextPosition end, String name) {
        this.name = name;
        this.start = start;
        this.end = end;
    }

    /**
     * {@inheritDoc} This implementation invokes
     * {@link ContentVisitor#acceptClosingMarkup(ClosingMarkupTag)} on
     * its argument, passing itself as an argument.
     */
    @Override
    public <R> R visit(ContentVisitor<? extends R> visitor) {
        return visitor.acceptClosingMarkup(this);
    }

    @Override
    public TextPosition start() {
        return start;
    }

    @Override
    public TextPosition end() {
        return end;
    }
}
