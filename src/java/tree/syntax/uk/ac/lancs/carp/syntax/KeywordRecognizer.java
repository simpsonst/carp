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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import uk.ac.lancs.syntax.Token;

/**
 * Converts identifiers into recognized keywords.
 * 
 * @author simpsons
 */
public final class KeywordRecognizer implements Consumer<Token<TokenType>> {
    private final Consumer<? super Token<TokenType>> sub;

    private final BiConsumer<? super Token<TokenType>,
                             ? super Token<TokenType>> associations;

    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("type", TokenType.KWD_TYPE);
        KEYWORDS.put("import", TokenType.KWD_IMPORT);
        KEYWORDS.put("as", TokenType.KWD_AS);
        KEYWORDS.put("call", TokenType.KWD_CALL);
        KEYWORDS.put("inherit", TokenType.KWD_INHERIT);
    }

    /**
     * Create a keyword recognizer, submitting a modified token stream
     * to a subsequent consumer.
     * 
     * @param sub the subsequent token consumer
     * 
     * @param associations notified of old and new tokens
     */
    public KeywordRecognizer(Consumer<? super Token<TokenType>> sub,
                             BiConsumer<? super Token<TokenType>,
                                        ? super Token<TokenType>> associations) {
        this.sub = sub;
        this.associations = associations;
    }

    /**
     * Accept and process a token. If the token is of type
     * {@link TokenType#IDENT}, and its {@link Token#text} field is one
     * of the following strings, the token is replaced by one of the
     * following types:
     * 
     * <table summary="This table lists keywords against their token
     * types.">
     * <thead>
     * <tr>
     * <th>Text</th>
     * <th>Token type</th>
     * </tr>
     * </thead> <tbody>
     * <tr>
     * <td><samp>call</samp></td>
     * <td>{@link TokenType#KWD_CALL}</td>
     * </tr>
     * <tr>
     * <td><samp>type</samp></td>
     * <td>{@link TokenType#KWD_TYPE}</td>
     * </tr>
     * <tr>
     * <td><samp>import</samp></td>
     * <td>{@link TokenType#KWD_IMPORT}</td>
     * </tr>
     * <tr>
     * <td><samp>as</samp></td>
     * <td>{@link TokenType#KWD_AS}</td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * <p>
     * Every call will result in exactly one call to the subsequent
     * consumer.
     * 
     * @param t the next token
     */
    @Override
    public void accept(Token<TokenType> t) {
        if (t.type == TokenType.IDENT) {
            TokenType alt = KEYWORDS.get(t.text);
            if (alt != null) {
                final var ot = t;
                t = Token.of(alt, t.start, t.end, t.text);
                associations.accept(ot, t);
            }
        }
        assert t != null;
        sub.accept(t);
    }

    /**
     * Create an updater for a map of stored associations. This is a
     * {@link BiConsumer} suitable for use with
     * {@link #KeywordRecognizer(Consumer, BiConsumer). The map should
     * normally be indexed on identity, like an {@link IdentityHashMap}
     * or {@link WeakHashMap}.
     * 
     * @param map the map to be updated
     * 
     * @param <V> the map value type
     * 
     * @param move {@code true} if the new mapping should replace the
     * old; {@code false} if the old mapping should be preserved along
     * with the new
     * 
     * @return the requested consumer
     */
    public static <V> BiConsumer<Token<TokenType>, Token<TokenType>>
        mapUpdater(Map<Token<TokenType>, V> map, boolean move) {
        if (move)
            return (ot, nt) -> {
                if (map.containsKey(ot)) map.put(nt, map.remove(ot));
            };
        else
            return (ot, nt) -> {
                if (map.containsKey(ot)) map.put(nt, map.get(ot));
            };
    }
}
