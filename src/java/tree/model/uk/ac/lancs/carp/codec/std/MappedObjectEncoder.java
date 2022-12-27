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

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.Codecs;
import uk.ac.lancs.carp.codec.Direction;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.MissingFieldException;
import uk.ac.lancs.carp.codec.EncodingContext;

/**
 * Maps specific Java objects to JSON strings. This class primarily
 * exists to map enumeration constants.
 * 
 * @author simpsons
 */
public class MappedObjectEncoder implements Encoder {
    private final Map<Object, String> toString;

    private MappedObjectEncoder(Map<Object, String> toString) {
        this.toString = toString;
    }

    /**
     * Create an encoder based on a mapping from string to object.
     * 
     * @param mapping the mapping from string to object
     * 
     * @return the encoder
     */
    public static MappedObjectEncoder
        byString(Map<? extends String, ?> mapping) {
        final Map<String, Object> toObject = new HashMap<>();
        final Map<Object, String> toString = new IdentityHashMap<>();
        for (var entry : mapping.entrySet()) {
            toObject.put(entry.getKey(), entry.getValue());
            toString.put(entry.getValue(), entry.getKey());
        }
        if (toObject.size() != toString.size())
            throw new IllegalArgumentException("duplicate values in "
                + mapping);
        return new MappedObjectEncoder(toString);
    }

    /**
     * Create an encoder based on a mapping from object to string.
     * 
     * @param mapping the mapping from object to string
     * 
     * @return the encoder
     */
    public static MappedObjectEncoder
        byObject(Map<?, ? extends String> mapping) {
        final Map<String, Object> toObject = new HashMap<>();
        final Map<Object, String> toString = new IdentityHashMap<>();
        for (var entry : mapping.entrySet()) {
            toString.put(entry.getKey(), entry.getValue());
            toObject.put(entry.getValue(), entry.getKey());
        }
        if (toObject.size() != toString.size())
            throw new IllegalArgumentException("duplicate values in "
                + mapping);
        return new MappedObjectEncoder(toString);
    }

    @Override
    public JsonValue encodeJson(Object value, EncodingContext ctxt) {
        String alt = toString.get(value);
        if (alt == null)
            throw new MissingFieldException(Direction.ENCODING,
                                            "unknown value: " + value);
        return Codecs.asJson(alt);
    }
}
