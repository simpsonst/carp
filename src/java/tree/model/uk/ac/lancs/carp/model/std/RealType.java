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

import java.math.BigDecimal;
import java.util.Properties;
import java.util.function.IntFunction;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.std.BigDecimalDecoder;
import uk.ac.lancs.carp.codec.std.BigDecimalEncoder;
import uk.ac.lancs.carp.codec.std.DoubleDecoder;
import uk.ac.lancs.carp.codec.std.DoubleEncoder;
import uk.ac.lancs.carp.codec.std.FloatDecoder;
import uk.ac.lancs.carp.codec.std.FloatEncoder;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models a real type with a specific or infinite precision.
 * 
 * <p>
 * This type uses {@value #KEY} as its identifier in properties, and
 * defines one optional field {@value #PREC_FIELD} specifying the
 * precision.
 * 
 * <p>
 * The corresponding type in Java may be {@code float}, {@code double},
 * {@link Float}, {@link Double} or {@link BigDecimal}. Reference types
 * are used when optional or in a primitive-forbidding context.
 * 
 * @author simpsons
 */
public final class RealType implements Type {
    private final int precision;

    private RealType(int precision) {
        if (precision < 1)
            this.precision = -1;
        else
            this.precision = precision;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns a {@link FloatEncoder} if
     * the precision is no greater than {@value #MAX_FLOAT_PRECISION}, a
     * {@link DoubleEncoder} if the precision is no greater than
     * {@value #MAX_DOUBLE_PRECISION}, or a {@link BigDecimalEncoder}
     * otherwise.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        return getCodec(BigDecimalEncoder::new, DoubleEncoder::new,
                        FloatEncoder::new);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns a {@link FloatDecoder} if
     * the precision is no greater than {@value #MAX_FLOAT_PRECISION}, a
     * {@link DoubleDecoder} if the precision is no greater than
     * {@value #MAX_DOUBLE_PRECISION}, or a {@link BigDecimalDecoder}
     * otherwise.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        return getCodec(BigDecimalDecoder::new, DoubleDecoder::new,
                        FloatDecoder::new);
    }

    private <T> T getCodec(IntFunction<T> bigDecFunc, IntFunction<T> doubleFunc,
                           IntFunction<T> floatFunc) {
        if (precision <= 1 || precision >= MAX_DOUBLE_PRECISION) {
            return bigDecFunc.apply(precision);
        } else if (precision <= MAX_FLOAT_PRECISION) {
            return floatFunc.apply(precision);
        } else {
            return doubleFunc.apply(precision);
        }
    }

    /**
     * Model a real type of infinite precision.
     * 
     * @return a model of a real type with infinite precision
     */
    public static RealType ofInfinitePrecision() {
        return new RealType(-1);
    }

    /**
     * Model a real type of a specific precision.
     *
     * @throws IllegalArgumentException if the precision is not positive
     * 
     * @param precision the precision in decimal digits
     * 
     * @return a model of a real type with the specified precision
     */
    public static RealType ofPrecision(int precision) {
        if (precision < 1)
            throw new IllegalArgumentException("non-positive precision "
                + precision);
        return new RealType(precision);
    }

    /**
     * Get a string representation of this type. For infinite precision,
     * this is simply a dot &lsquo;<samp>.</samp>&rsquo;. Otherwise, it
     * is <samp>.<var>digits</var></samp>, giving the number of digits
     * of precision.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        if (precision < 0) return ".";
        return "." + precision;
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
        result = prime * result + precision;
        return result;
    }

    /**
     * Test whether this type is identical to another object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a {@link RealType}
     * with the same precision; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof RealType)) return false;
        RealType other = (RealType) obj;
        return precision == other.precision;
    }

    static final String KEY = "real";

    static final String PREC_FIELD = "prec";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and sets the field {@value #PREC_FIELD} to hold
     * the precession (if not infinite).
     */
    @Override
    public void describe(String prefix, Properties props) {
        props.setProperty(prefix + "type", KEY);
        if (precision >= 1)
            props.setProperty(prefix + PREC_FIELD, Integer.toString(precision));
    }

    static RealType load(Properties props, String prefix) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        String precTxt = props.getProperty(prefix + PREC_FIELD);
        if (precTxt == null) return ofInfinitePrecision();
        return ofPrecision(Integer.parseInt(precTxt));
    }

    private static final int MAX_DOUBLE_PRECISION = 16;

    private static final int MAX_FLOAT_PRECISION = 7;

    /**
     * {@inheritDoc}
     * 
     * @default This implementation generates code that detects
     * inequality of {@link Float#floatToIntBits(float)} on the two
     * expressions if the precision is no more than
     * {@value #MAX_FLOAT_PRECISION} and the expressions are primitive,
     * detects inequality of {@link Double#doubleToLongBits(double)} on
     * the two expressions if the precision is no more than
     * {@value #MAX_DOUBLE_PRECISION} and the two expressions are
     * primitive, or uses the default behaviour of this method
     * otherwise.
     */
    @Override
    public String getJavaInequalityExpression(boolean primitive, String thisRef,
                                              String otherRef,
                                              ExpansionContext ctxt) {
        if (!primitive || precision < 1 || precision > MAX_DOUBLE_PRECISION)
            return Type.super.getJavaInequalityExpression(primitive, thisRef,
                                                          otherRef, ctxt);
        if (precision <= MAX_FLOAT_PRECISION)
            return "java.lang.Float.floatToIntBits(" + thisRef
                + ") != java.lang.Float.floatToIntBits(" + otherRef + ")";
        return "java.lang.Double.doubleToLongBits(" + thisRef
            + ") != java.lang.Double.doubleToLongBits(" + otherRef + ")";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation generates code that uses
     * {@link Float#floatToIntBits(float)} to generate a hash code if
     * the expression is primitive and the precision is no more than
     * {@value #MAX_FLOAT_PRECISION},
     * {@link Double#doubleToLongBits(double)} folded into 32 bits if
     * the expression is primitive and the precision is no more than
     * {@value #MAX_DOUBLE_PRECISION}, or the default behaviour of this
     * method otherwise.
     */
    @Override
    public String getJavaHashExpression(boolean primitive, String ref,
                                        ExpansionContext ctxt) {
        if (!primitive || precision < 1 || precision > MAX_DOUBLE_PRECISION)
            return Type.super.getJavaHashExpression(primitive, ref, ctxt);
        if (precision <= MAX_FLOAT_PRECISION)
            return "java.lang.Float.floatToIntBits(" + ref + ")";
        return "(int) (java.lang.Double.doubleToLongBits(" + ref
            + ") ^ (java.lang.Double.doubleToLongBits(" + ref + ") >>> 32))";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation yields <samp>float</samp> if the
     * expression is primitive and the precision is no more than
     * {@value #MAX_FLOAT_PRECISION}, <samp>java.lang.Float</samp> if
     * the precision is no more than {@value #MAX_FLOAT_PRECISION},
     * <samp>double</samp> if the expression is primitive and the
     * precision is no more than {@value #MAX_DOUBLE_PRECISION},
     * <samp>java.lang.Double</samp> if the precision is no more than
     * {@value #MAX_DOUBLE_PRECISION}, or
     * <samp>java.math.BigDecimal</samp> otherwise.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase, ExpansionContext ctxt) {
        if (precision < 1 || precision > MAX_DOUBLE_PRECISION)
            return "java.math.BigDecimal";
        if (precision <= MAX_FLOAT_PRECISION)
            return primitive ? "float" : "java.lang.Float";
        return primitive ? "double" : "java.lang.Double";
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method returns this object.
     */
    @Override
    public RealType qualify(ExternalName name, QualificationContext ctxt) {
        return this;
    }
}
