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

package uk.ac.lancs.carp.codec.std;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.LongFunction;
import javax.json.JsonNumber;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.CodecException;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Direction;
import uk.ac.lancs.carp.codec.DecodingContext;

/**
 * Converts from JSON numbers to Java {@code long}s.
 * 
 * @author simpsons
 */
public class LongDecoder implements Decoder {
    private static final Map<LongFunction<? extends CharSequence>,
                             LongDecoder> cache = Collections
                                 .synchronizedMap(new WeakHashMap<>());

    /**
     * Retrieve or create a decoder. The value is cached, weakly indexed
     * by the test function.
     * 
     * @param test Tests whether the input is in range, returning an
     * error message if not, or {@code null} if so.
     * 
     * @return the requested decoder
     */
    public static LongDecoder get(LongFunction<? extends CharSequence> test) {
        return cache.computeIfAbsent(test, LongDecoder::new);
    }

    private final LongFunction<? extends CharSequence> test;

    /**
     * Create a decoder.
     * 
     * @param test Tests whether the input is in range, returning an
     * error message if not, or {@code null} if so.
     */
    public LongDecoder(LongFunction<? extends CharSequence> test) {
        this.test = test;
    }

    private void check(long value) {
        CharSequence msg = test.apply(value);
        if (msg == null) return;
        throw new CodecException(Direction.DECODING, msg.toString());
    }

    @Override
    public Object decodeJson(JsonValue value, DecodingContext ctxt) {
        JsonNumber typed = (JsonNumber) value;
        long decoded = typed.longValueExact();
        check(decoded);
        return decoded;
    }
}
