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

import java.util.List;
import uk.ac.lancs.syntax.TextPosition;

/**
 * Specifies the presence of an opening/self-closing HTML tag in a
 * documentation comment.
 *
 * @author simpsons
 */
public final class OpeningMarkupTag implements Content {
    /**
     * Specifies whether the tag is open/empty or self-closing.
     * {@code true} means the tag is self-closing; {@code false} means
     * opening or empty.
     */
    public final boolean selfClosing;

    /**
     * The tag name
     */
    public final String name;

    /**
     * The immutable list of attributes
     */
    public final List<TagAttr> attributes;

    private final TextPosition start;

    private final TextPosition end;

    /**
     * Create an opening or self-closing mark-up tag.
     * 
     * @param start the start position of the tag in source
     * 
     * @param end the end position of the tag in source
     * 
     * @param selfClosing {@code true} if the tag is self-closing;
     * {@code false} if it is opening/empty
     * 
     * @param name the tag name
     * 
     * @param attributes the attributes
     */
    public OpeningMarkupTag(TextPosition start, TextPosition end,
                            boolean selfClosing, String name,
                            List<? extends TagAttr> attributes) {
        this.start = start;
        this.end = end;
        this.selfClosing = selfClosing;
        this.name = name;
        this.attributes = List.copyOf(attributes);
    }

    /**
     * {@inheritDoc} This implementation invokes
     * {@link ContentVisitor#acceptOpeningMarkup(OpeningMarkupTag)} on
     * its argument, passing itself as an argument.
     */
    @Override
    public <R> R visit(ContentVisitor<? extends R> visitor) {
        return visitor.acceptOpeningMarkup(this);
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
