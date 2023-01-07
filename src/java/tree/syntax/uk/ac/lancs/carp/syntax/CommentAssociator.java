// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright 2021,2022, Lancaster University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 * 
 *  * Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.carp.syntax;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Consumer;
import uk.ac.lancs.syntax.Token;

/**
 * Associates the most recent document comment with a subsequent token.
 * All tokens except {@link TokenType#DOC_COMMENT} are passed through
 * unchanged. The most recent {@link TokenType#DOC_COMMENT} is
 * associated with the next {@link TokenType#IDENT} token.
 * 
 * <p>
 * This class should normally be used subsequent to a
 * {@link CommentEliminator} or similar, which coalesces comment tokens
 * into documentation comments, and eliminates white space and other
 * tokens.
 * 
 * @author simpsons
 */
public final class CommentAssociator implements Consumer<Token<TokenType>> {
    private final Consumer<? super Token<TokenType>> sub;

    private final Map<? super Token<TokenType>, ? super Token<TokenType>> ref;

    private Token<TokenType> latest = null;

    /**
     * Create a comment associator.
     * 
     * @param sub the subsequent token consumer
     * 
     * @param ref a map to populate by associating the latest
     * documentation comment with subsequent tokens
     * 
     * <p>
     * A {@link WeakHashMap} or {@link IdentityHashMap} is recommended.
     */
    public CommentAssociator(Consumer<? super Token<TokenType>> sub,
                             Map<? super Token<TokenType>,
                                 ? super Token<TokenType>> ref) {
        this.sub = sub;
        this.ref = ref;
    }

    /**
     * Accept and process a token. If the token is of type
     * {@link TokenType#DOC_COMMENT}, it becomes the 'latest' comment.
     * If the token is of type {@link TokenType#IDENT}, the latest
     * comment is associated with the token, and then reset.
     * 
     * <p>
     * {@link TokenType#DOC_COMMENT} tokens are not passed on to the
     * subsequent consumer.
     * 
     * @param t the next token
     */
    @Override
    public void accept(Token<TokenType> t) {
        Token<TokenType> cur = latest;
        switch (t.type) {
        case DOC_COMMENT:
            latest = t;
            return;

        case IDENT:
            latest = null;
            if (cur != null) {
                ref.put(t, cur);
            }
            break;
        }
        sub.accept(t);
    }

    /**
     * Extract documentation text from a {@link TokenType#DOC_COMMENT}
     * token. The raw text of the token is first extracted; this
     * excludes the leading comment opener, <samp>/**</samp>. The first
     * newline is located, and everything up to the start column minus 1
     * is taken as a per-line prefix. Each newline followed by such a
     * prefix is then replaced with a newline.
     * 
     * @param docToken the token to extract the text from
     * 
     * @return the extracted text
     * 
     * @throws IllegalArgumentException if the token is not a
     * {@link TokenType#DOC_COMMENT}
     */
    public static String extractDocComment(Token<TokenType> docToken) {
        if (docToken.type != TokenType.DOC_COMMENT)
            throw new IllegalArgumentException("not doc comment: " + docToken);
        final int col = docToken.start.column;
        final int nl1 = docToken.text.indexOf('\n');
        if (nl1 < 0) return docToken.text;
        final int lim = Math.min(nl1 + col, docToken.text.length());
        assert lim >= nl1;
        final CharSequence leader = docToken.text.subSequence(nl1, lim);
        String alt = docToken.text.replace(leader, "\n");
        return alt.replace("\n ", "\n");
    }
}
