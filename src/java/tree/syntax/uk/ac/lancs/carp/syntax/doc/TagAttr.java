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
 * Details an attribute of an HTML tag in a documentation comment.
 * 
 * @author simpsons
 */
public class TagAttr implements SourceMaterial {
    private final TextPosition start;

    private final TextPosition end;

    /**
     * The attribute name
     */
    public final String name;

    /**
     * The immutable attribute value
     */
    public final List<Content> value;

    /**
     * Create an HTML tag attribute. The value will be copied.
     * 
     * @param start the start position of the attribute in source
     * 
     * @param end the end position of the attribute in source
     * 
     * @param name the attribute name
     * 
     * @param value the attribute value
     */
    public TagAttr(TextPosition start, TextPosition end, String name,
                   List<? extends Content> value) {
        this.name = name;
        this.value = List.copyOf(value);
        this.start = start;
        this.end = end;
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
