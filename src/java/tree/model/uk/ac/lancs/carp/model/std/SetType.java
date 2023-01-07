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

package uk.ac.lancs.carp.model.std;

import java.math.BigInteger;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.Codecs;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.Direction;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.TypeMismatchException;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models a set of elements of the same type.
 * 
 * @author simpsons
 */
public final class SetType implements Type {
    private final Type elementType;

    /**
     * Model a set of elements as a type.
     * 
     * @param elementType the element type
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public SetType(Type elementType) {
        Objects.requireNonNull(elementType, "elementType");
        this.elementType = elementType;
    }

    private static void reverse(byte[] raw) {
        for (int i = 0; i < raw.length / 2; i++) {
            byte tmp = raw[i];
            raw[i] = raw[raw.length - i - 1];
            raw[raw.length - i - 1] = tmp;
        }
    }

    private static BitSet decodeToBitset(JsonNumber num) {
        BigInteger zark = num.bigIntegerValue();
        byte[] raw = zark.toByteArray();
        reverse(raw);
        BitSet typed = BitSet.valueOf(raw);
        return typed;
    }

    private static BitSet decodeToBitset(JsonArray arr, Decoder sub,
                                         DecodingContext ctxt) {
        BitSet result = new BitSet();
        for (JsonValue v : arr) {
            Number o = (Number) sub.decodeJson(v, ctxt);
            result.set(o.intValue());
        }
        return result;
    }

    private static Collection<?> decodeToCollection(JsonNumber num) {
        BitSet result = decodeToBitset(num);
        return result.stream().boxed().collect(Collectors.toSet());
    }

    private static Collection<?> decodeToCollection(JsonArray arr, Decoder sub,
                                                    DecodingContext ctxt) {
        Collection<Object> result = new HashSet<>();
        for (JsonValue v : arr) {
            Object o = sub.decodeJson(v, ctxt);
            result.add(o);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @default If the element type's {@link Type#isBitSetIndex()} is
     * {@code true}, an encoder is returned which accepts a
     * {@link BitSet}, converts it to a byte array, then to a
     * {@link BigInteger}, and then to a JSON number. Otherwise, an
     * encoder is created for the element type, and the returned encoder
     * interprets its argument as a {@link Collection}, iterates over
     * its elements, converts each one using the element-type encoder,
     * and appends them to a JSON array, which is ultimately returned.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        if (elementType.isBitSetIndex()) {
            return (value, ectxt) -> {
                BitSet typed = (BitSet) value;

                /* Get a big-endian byte representation of the bitset,
                 * so we can convert it to a big integer. */
                byte[] raw = typed.toByteArray();
                reverse(raw);
                BigInteger zark = new BigInteger(raw);
                return Codecs.asJson(zark);
            };
        } else {
            final Encoder elemCodec = elementType.getEncoder(null, ctxt);
            return (value, ectxt) -> {
                Collection<?> typed = (Collection<?>) value;
                JsonArrayBuilder builder = Json.createArrayBuilder();
                for (Object elem : typed)
                    builder.add(elemCodec.encodeJson(elem, ectxt));
                return builder.build();
            };
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation first creates a decoder for its
     * element type. It then determines whether its element type's
     * {@link Type#isBitSetIndex()} is {@code true}.
     * 
     * <p>
     * If the element type is suitable as a {@link BitSet} index, a
     * decoder is returned that checks whether its argument is a JSON
     * number or a JSON array. If it is a JSON number, it is interpreted
     * as a {@link BigInteger}, converted to bytes, then to a
     * {@link BitSet}, which is returned. If the argument is a JSON
     * array, its elements are converted using the element-type decoder,
     * and treated as {@code int}s to be added to a returned
     * {@link BitSet}.
     * 
     * <p>
     * If the element type is not suitable as a {@link BitSet} index,a
     * decoder is returned that checks whether its argument is a JSON
     * number or a JSON array. If it is a JSON number, it is interpreted
     * as a {@link BigInteger}, converted to bytes, then to a
     * {@link BitSet}, which is converted to a {@link Collection} and
     * returned. If the argument is a JSON array, its elements are
     * converted using the element-type decoder, and added to a
     * {@link HashSet}, which is returned.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        final Decoder elemCodec = elementType.getDecoder(null, ctxt);
        if (elementType.isBitSetIndex()) {
            return (value, dctxt) -> {
                assert value != null;
                if (value instanceof JsonNumber)
                    return decodeToBitset((JsonNumber) value);
                else if (value instanceof JsonArray)
                    return decodeToBitset((JsonArray) value, elemCodec, dctxt);
                throw new TypeMismatchException(Direction.DECODING,
                                                "expected JSON array"
                                                    + " or number; got "
                                                    + value.getValueType());
            };
        } else {
            return (value, dctxt) -> {
                assert value != null;
                if (value instanceof JsonNumber)
                    return decodeToCollection((JsonNumber) value);
                else if (value instanceof JsonArray)
                    return decodeToCollection((JsonArray) value, elemCodec,
                                              dctxt);
                throw new TypeMismatchException(Direction.DECODING,
                                                "expected JSON array"
                                                    + " or number; got "
                                                    + value.getValueType());
            };
        }
    }

    /**
     * Get a string representation for this type. This is an ampersand
     * followed by the representation of the element type.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return "&" + elementType;
    }

    /**
     * Get the hash code for this object.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        assert elementType != null;
        result = prime * result + elementType.hashCode();
        return result;
    }

    /**
     * Test whether another object models the same type.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a {@link SetType}
     * with an identical element type; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof SetType)) return false;
        SetType other = (SetType) obj;
        assert elementType != null;
        assert other.elementType != null;
        return elementType.equals(other.elementType);
    }

    static final String KEY = "set";

    static final String ELEM_FIELD = "elem";

    @Override
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        elementType.gatherReferences(referrent, dest);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and invokes itself on its element type, extending
     * the prefix with {@value #ELEM_FIELD} and a dot.
     */
    @Override
    public void describe(String prefix, Properties props) {
        props.setProperty(prefix + "type", KEY);
        elementType.describe(prefix + ELEM_FIELD + ".", props);
    }

    static SetType load(String prefix, Properties props, LoadContext ctxt) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        Type elementType = Type.load(prefix + ELEM_FIELD + ".", props, ctxt);
        return new SetType(elementType);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation yields the string
     * <samp>java.util.Collection&lt;<var>elemType</var>;&gt;</samp>,
     * calling itself on the element type to obtain the expansion of
     * <var>elemType</var>.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase,
                              ExpansionContext ctxt) {
        if (elementType.isBitSetIndex()) return "java.util.BitSet";
        if (erase) return "java.util.Collection";
        return "java.util.Collection<"
            + elementType.declareJava(false, false, ctxt) + ">";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes itself on its element type,
     * passing {@code null} as the name. The result is used as the
     * element type of a new set type, which is returned.
     */
    @Override
    public SetType qualify(ExternalName name, QualificationContext ctxt) {
        return new SetType(elementType.qualify(null, ctxt));
    }
}
