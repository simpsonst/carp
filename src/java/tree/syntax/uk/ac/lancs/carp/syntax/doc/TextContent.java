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

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.syntax.TextPosition;

/**
 * Specifies literal text within a documentation comment.
 *
 * @author simpsons
 */
public final class TextContent implements Content {
    /**
     * The literal content
     */
    public final String text;

    private final TextPosition start;

    private final TextPosition end;

    /**
     * Create literal text content.
     * 
     * @param start the start position of the text in source
     * 
     * @param end the end position of the text in source
     * 
     * @param text the literal content
     */
    public TextContent(TextPosition start, TextPosition end, String text) {
        this.text = text;
        this.start = start;
        this.end = end;
    }

    private static final Pattern FIRST_WORD =
        Pattern.compile("^\\s*(?:([^\\s]+)(?:\\s*(.*))?)$", Pattern.DOTALL);

    /**
     * Split this content into the first word and remainder.
     * 
     * @param rem the consumer of the remaining text. Empty text content
     * is not added. White space terminating the first word is not
     * added.
     * 
     * @return the first word
     */
    public String firstWord(Collection<? super TextContent> rem) {
        Matcher m = FIRST_WORD.matcher(text);
        if (m.matches()) {
            String result = m.group(1);
            TextPosition newStart =
                TextPosition.of(start().column + result.length(), start().line);
            String remText = m.group(2);
            if (!remText.isEmpty())
                rem.add(new TextContent(newStart, end(), remText));
            return result;
        } else {
            rem.add(this);
            return null;
        }
    }

    /**
     * {@inheritDoc} This implementation invokes
     * {@link ContentVisitor#acceptText(TextContent)} on its argument,
     * passing itself as an argument.
     */
    @Override
    public <R> R visit(ContentVisitor<? extends R> visitor) {
        return visitor.acceptText(this);
    }

    @Override
    public TextPosition start() {
        return start;
    }

    @Override
    public TextPosition end() {
        return end;
    }

    private static final Pattern LEAD_TRIM = Pattern.compile("^\\s+(.*)$");

    private static final Pattern TAIL_TRIM = Pattern.compile("^(.*)\\s+$");

    @Override
    public Content trimLeadingWhiteSpace() {
        Matcher m = LEAD_TRIM.matcher(text);
        if (!m.matches()) return this;
        String alt = m.group(1);
        return new TextContent(start, end, alt);
    }

    @Override
    public Content trimTrailingWhiteSpace() {
        Matcher m = TAIL_TRIM.matcher(text);
        if (!m.matches()) return this;
        String alt = m.group(1);
        return new TextContent(start, end, alt);
    }

}
