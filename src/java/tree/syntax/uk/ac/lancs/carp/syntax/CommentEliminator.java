// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright 2021, Lancaster University
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

import java.util.function.Consumer;
import uk.ac.lancs.syntax.TextPosition;
import uk.ac.lancs.syntax.Token;

/**
 * Eliminates comments and white space from a token stream. Long
 * document comments are preserved as a single token of type
 * {@link TokenType#DOC_COMMENT}. This processor operates in four modes:
 * 
 * <dl>
 * 
 * <dt>normal (the default)</dt>
 * 
 * <dd>White space ({@link TokenType#WHITESPACE} and
 * {@link TokenType#NEWLINE}) are removed. {@link TokenType#INT}
 * followed by {@link TokenType#IDENT} are concatenated into
 * {@link TokenType#UNMATCHED}. Adjacent sequences of
 * {@link TokenType#INT} are concatenated into
 * {@link TokenType#UNMATCHED}. {@link TokenType#DOC_COMMENT_OPEN}
 * places the processor into doc-comment mode.
 * {@link TokenType#COMMENT_OPEN} places the processor into long-comment
 * mode. {@link TokenType#COMMENT} places the processor into
 * short-comment mode.
 * 
 * <dt>doc-comment</dt>
 * 
 * <dd>Token text is accumulated until {@link TokenType#COMMENT_CLOSE}
 * is encountered. At that point, the accumulated text is submitted as a
 * {@link TokenType#DOC_COMMENT} without the surrounding <samp>/**
 * *&#x2f;</samp>, and mode returns to normal. The new token's start and
 * end positions match the enclosed text.
 * 
 * <dt>long-comment</dt>
 * 
 * <dd>Tokens are ignored until {@link TokenType#COMMENT_CLOSE} is
 * encountered.
 * 
 * <dt>short-comment</dt>
 * 
 * <dd>Tokens are ignored until {@link TokenType#NEWLINE} is
 * encountered.
 * 
 * </dl>
 * 
 * @author simpsons
 */
public final class CommentEliminator implements Consumer<Token<TokenType>> {
    private enum Mode {
        NORMAL, SHORT_COMMENT, LONG_COMMENT, DOC_COMMENT;
    }

    private final Consumer<? super Token<TokenType>> sub;

    private Token<TokenType> prior;

    private Mode mode = Mode.NORMAL;

    private final StringBuilder docComment = new StringBuilder();

    private TextPosition docStart;

    /**
     * Create a comment eliminator, submitting a modified token stream
     * to a subsequent consumer.
     * 
     * @param sub the subsequent token consumer
     */
    public CommentEliminator(Consumer<? super Token<TokenType>> sub) {
        this.sub = sub;
    }

    /**
     * Accept and process a token. Several tokens may be consumed before
     * a token is submitted to the subsequent consumer.
     * 
     * @param token the next token
     */
    @Override
    public void accept(Token<TokenType> token) {
        /* An INT must not be followed by an IDENTIFIER or another
         * INT. */
        if (prior != null) {
            switch (token.type) {
            case IDENT:
                /* The errant sequence is terminated, so append the
                 * identifier, and report as unmatched. */
                sub.accept(Token.of(TokenType.UNMATCHED, prior.start, token.end,
                                    prior.text + token.text));
                prior = null;
                return;

            case INT:
                /* The errant sequence is extended, so append the
                 * number, and store as unmatched. */
                prior = Token.of(TokenType.UNMATCHED, prior.start, token.end,
                                 prior.text + token.text);
                return;

            default:
                /* The sequence is terminated, so report what we have,
                 * whether it's an error or not, and prepare for a fresh
                 * sequence. */
                sub.accept(prior);
                prior = null;
                break;
            }
        }

        switch (mode) {
        case NORMAL:
            switch (token.type) {
            case INT:
                /* This could be the start of an errant sequence such as
                 * a NUMBER followed by an IDENTIFIER or another NUMBER.
                 * Don't report it yet. */
                prior = token;
                break;

            case UNMATCHED:
                sub.accept(token);
                break;

            case DOC_COMMENT_OPEN:
                docComment.delete(0, docComment.length());
                docStart = token.end;
                mode = Mode.DOC_COMMENT;
                break;

            case COMMENT_OPEN:
                mode = Mode.LONG_COMMENT;
                break;

            case COMMENT:
                mode = Mode.SHORT_COMMENT;
                break;

            case WHITESPACE:
            case NEWLINE:
                break;

            default:
                sub.accept(token);
                break;
            }
            break;

        case DOC_COMMENT:
            switch (token.type) {
            case COMMENT_CLOSE:
                mode = Mode.NORMAL;
                sub.accept(Token.of(TokenType.DOC_COMMENT, docStart,
                                    token.start, docComment.toString()));
                break;

            default:
                docComment.append(token.text);
                break;
            }
            break;

        case LONG_COMMENT:
            switch (token.type) {
            case COMMENT_CLOSE:
                mode = Mode.NORMAL;
                break;

            case UNMATCHED:
                break;

            default:
                break;
            }
            break;

        case SHORT_COMMENT:
            switch (token.type) {
            case NEWLINE:
                mode = Mode.NORMAL;
                break;

            case UNMATCHED:
                break;

            default:
                break;
            }
            break;
        }
    }
}
