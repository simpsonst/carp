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

package uk.ac.lancs.carp.codec.std;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.IntFunction;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.CodecException;
import uk.ac.lancs.carp.codec.Codecs;
import uk.ac.lancs.carp.codec.Direction;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.EncodingContext;

/**
 * Converts from Java {@code short}s to JSON numbers.
 * 
 * @author simpsons
 */
public class ShortEncoder implements Encoder {
    private static final Map<IntFunction<? extends CharSequence>,
                             ShortEncoder> cache = Collections
                                 .synchronizedMap(new WeakHashMap<>());

    /**
     * Retrieve or create an encoder. The value is cached, weakly
     * indexed by the test function.
     * 
     * @param test Tests whether the input is in range, returning an
     * error message if not, or {@code null} if so.
     * 
     * @return the requested encoder
     */
    public static ShortEncoder get(IntFunction<? extends CharSequence> test) {
        return cache.computeIfAbsent(test, ShortEncoder::new);
    }

    private final IntFunction<? extends CharSequence> test;

    /**
     * Create an encoder.
     * 
     * @param test Tests whether the input is in range, returning an
     * error message if not, or {@code null} if so.
     */
    private ShortEncoder(IntFunction<? extends CharSequence> test) {
        this.test = test;
    }

    private void check(int value) {
        CharSequence msg = test.apply(value);
        if (msg == null) return;
        throw new CodecException(Direction.ENCODING, msg.toString());
    }

    @Override
    public JsonValue encodeJson(Object value, EncodingContext ctxt) {
        short typed = ((Number) value).shortValue();
        check(typed);
        return Codecs.asJson(typed);
    }
}
