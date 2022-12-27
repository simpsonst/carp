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

package uk.ac.lancs.carp.model.std;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.Decoder;
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
 * Models a map type, mapping from one type to another.
 * 
 * <p>
 * This type uses {@value #KEY} as its identifier in properties, and
 * defines two fields, {@value #KEY_FIELD} specifying the key type, and
 * {@value #VALUE_FIELD} specifying the value type.
 * 
 * @author simpsons
 */
public final class MapType implements Type {
    /**
     * {@inheritDoc}
     * 
     * @default This method always returns {@code true}.
     */
    @Override
    public boolean ofLowPrecedence() {
        return true;
    }

    private final Type keyType;

    private final Type valueType;

    /**
     * {@inheritDoc}
     * 
     * @default This implementation creates encoders for its key and
     * value types. The encoder it then returns iterates over its
     * provided map argument's entries, and builds a JSON array from
     * them. Each element of the array is itself an array of two
     * elements, namely the key and value, encoded with their respective
     * encoders.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        Encoder keyCodec = keyType.getEncoder(null, ctxt);
        Encoder valueCodec = valueType.getEncoder(null, ctxt);
        return (value, dctxt) -> {
            try {
                Map<?, ?> typed = (Map<?, ?>) value;
                JsonArrayBuilder builder = Json.createArrayBuilder();
                for (var entry : typed.entrySet()) {
                    JsonValue encKey =
                        keyCodec.encodeJson(entry.getKey(), dctxt);
                    JsonValue encValue =
                        valueCodec.encodeJson(entry.getValue(), dctxt);
                    JsonArray elem = Json.createArrayBuilder().add(encKey)
                        .add(encValue).build();
                    builder.add(elem);
                }
                return builder.build();
            } catch (ClassCastException ex) {
                throw new TypeMismatchException(Direction.DECODING,
                                                "unexpected Java type"
                                                    + " for map: "
                                                    + value.getClass(),
                                                ex);
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation creates decoders for its key and
     * value types. The decoder it then returns creates a map, and
     * interprets its argument as a JSON array, and iterates over each
     * element, which itself is taken as an array of two elements. The
     * key decoder is applied to the first element, and the value
     * decoder to the second. The decoded key and value are then placed
     * in the map. Finally, the map is returned.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        Decoder keyCodec = keyType.getDecoder(null, ctxt);
        Decoder valueCodec = valueType.getDecoder(null, ctxt);
        return (value, dctxt) -> {
            try {
                JsonArray typed = (JsonArray) value;
                Map<Object, Object> result = new HashMap<>();
                for (var entry : typed.getValuesAs(JsonArray.class)) {
                    Object decKey = keyCodec.decodeJson(entry.get(0), dctxt);
                    Object decVal = valueCodec.decodeJson(entry.get(1), dctxt);
                    result.put(decKey, decVal);
                }
                return result;
            } catch (ClassCastException ex) {
                throw new TypeMismatchException(Direction.DECODING,
                                                "expected JSON array;" + " got "
                                                    + value.getValueType(),
                                                ex);
            }
        };
    }

    /**
     * Model a map type.
     * 
     * @param keyType the key type
     * 
     * @param valueType the value type
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public MapType(Type keyType, Type valueType) {
        Objects.requireNonNull(keyType, "keyType");
        Objects.requireNonNull(valueType, "valueType");
        this.keyType = keyType;
        this.valueType = valueType;
    }

    /**
     * Get a string representation of this type. This consists of the
     * string representation of the key type, {@code "->"}, and the
     * string representation of the value type. If the key type's
     * {@link Type#ofLowPrecedence()} is {@code true}, its
     * representation is placed in a parenthesis.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return (keyType.ofLowPrecedence() ? "(" + keyType + ")" : keyType)
            + "->" + valueType;
    }

    /**
     * Get the hash code of this type.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        assert keyType != null;
        assert valueType != null;
        result = prime * result + keyType.hashCode();
        result = prime * result + valueType.hashCode();
        return result;
    }

    /**
     * Test whether this type is identical to another object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a {@link MapType}
     * with identical key and value types; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof MapType)) return false;
        MapType other = (MapType) obj;
        assert keyType != null;
        assert valueType != null;
        assert other.keyType != null;
        assert other.valueType != null;
        return keyType.equals(other.keyType) &&
            valueType.equals(other.valueType);
    }

    static final String KEY = "map";

    static final String KEY_FIELD = "key";

    static final String VALUE_FIELD = "value";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and invokes itself on its key and value types,
     * extending the prefix with {@value #KEY_FIELD} or
     * {@value #VALUE_FIELD} respectively, and a dot.
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        keyType.describe(prefix + KEY_FIELD + ".", into);
        valueType.describe(prefix + VALUE_FIELD + ".", into);
    }

    static MapType load(Properties props, String prefix, LoadContext ctxt) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        Type keyType = Type.load(prefix + KEY_FIELD + ".", props, ctxt);
        Type valueType = Type.load(prefix + VALUE_FIELD + ".", props, ctxt);
        return new MapType(keyType, valueType);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation yields the string
     * <samp>java.util.Map&lt;<var>keyType</var>,<var>valueType</var>&gt;</samp>,
     * calling itself on the key and value types to obtain the
     * expansions of <var>keyType</var> and <var>valueType</var>
     * respectively.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase,
                              ExpansionContext ctxt) {
        if (erase) return "java.util.Map";
        return "java.util.Map<" + keyType.declareJava(false, false, ctxt) + ","
            + valueType.declareJava(false, false, ctxt) + ">";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation invokes itself on its key and value
     * types, passing {@code null} as the name.
     */
    @Override
    public Type qualify(ExternalName name, QualificationContext ctxt) {
        return new MapType(keyType.qualify(null, ctxt),
                           valueType.qualify(null, ctxt));
    }
}
