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

/**
 * Accepts content of different types through distinct methods.
 * 
 * @param <R> the return type
 * 
 * @author simpsons
 */
public interface ContentVisitor<R> {
    /**
     * Accept text content.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptText(TextContent content);

    /**
     * Accept an opening or self-closing HTML tag.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptOpeningMarkup(OpeningMarkupTag content);

    /**
     * Accept a closing HTML tag.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptClosingMarkup(ClosingMarkupTag content);

    /**
     * Accept an in-line documentation tag.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptInlineTag(InlineTag content);

    /**
     * Accept an unrecognized entity reference.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptEntityReference(EntityReference content);

    /**
     * Accept root documentation.
     * 
     * @param content the content
     * 
     * @return an arbitrary response
     */
    R acceptDocumentation(Documentation content);
}
