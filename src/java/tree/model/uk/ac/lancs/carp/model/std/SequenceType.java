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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models a sequence of elements of the same type.
 * 
 * @author simpsons
 */
public final class SequenceType implements Type {
    private final Type elementType;

    @Override
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        elementType.gatherReferences(referrent, dest);
    }

    /**
     * {@inheritDoc}
     * 
     * @default An encoder is created for the element type. The returned
     * encoder treats its argument as a {@link Collection}, iterates
     * over it, and encodes each element using the element-type encoder,
     * before appending it to a JSON array, which is ultimately
     * returned.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        Encoder elemCodec = elementType.getEncoder(null, ctxt);
        return (value, c) -> {
            Collection<?> typed = (Collection<?>) value;
            JsonArrayBuilder builder = Json.createArrayBuilder();
            for (Object v : typed)
                builder.add(elemCodec.encodeJson(v, c));
            return builder.build();
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @default A decoder is created for the element type. The returned
     * decoder interprets its argument as a JSON array. It iterates over
     * the array, decoding each element with the element-type decoder,
     * and appends the result to a {@link List}, which is ultimately
     * returned.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        Decoder elemCodec = elementType.getDecoder(null, ctxt);
        return (value, c) -> {
            JsonArray arr = (JsonArray) value;
            List<Object> result = new ArrayList<>(arr.size());
            for (JsonValue v : arr)
                result.add(elemCodec.decodeJson(v, c));
            return result;
        };
    }

    /**
     * Model a type as a sequence of elements of another type.
     * 
     * @param elementType the element type
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public SequenceType(Type elementType) {
        Objects.requireNonNull(elementType, "elementType");
        this.elementType = elementType;
    }

    /**
     * Get a string representation for this type. This is an asterisk
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
        return "*" + elementType;
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
     * Test whether another object models the same type as this object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a
     * {@link SequenceType} of the same element type; {@code false}
     * otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof SequenceType)) return false;
        SequenceType other = (SequenceType) obj;
        assert elementType != null;
        assert other.elementType != null;
        return elementType.equals(other.elementType);
    }

    static final String KEY = "seq";

    static final String ELEM_FIELD = "elem";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and invokes itself on its element type, extending
     * the prefix with {@value #ELEM_FIELD} and a dot.
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        elementType.describe(prefix + ELEM_FIELD + ".", into);
    }

    static SequenceType load(Properties props, String prefix,
                             LoadContext ctxt) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        Type elementType = Type.load(prefix + ELEM_FIELD + ".", props, ctxt);
        return new SequenceType(elementType);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation yields the string
     * <samp>java.util.List&lt;<var>elemType</var>;&gt;</samp>, calling
     * itself on the element type to obtain the expansion of
     * <var>elemType</var>.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase,
                              ExpansionContext ctxt) {
        if (erase) return "java.util.List";
        return "java.util.List<" + elementType.declareJava(false, false, ctxt)
            + ">";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes itself on its element type,
     * passing {@code null} as the name. The result is used as the
     * element type of a new sequence type, which is returned.
     */
    @Override
    public SequenceType qualify(ExternalName name, QualificationContext ctxt) {
        return new SequenceType(elementType.qualify(null, ctxt));
    }
}
