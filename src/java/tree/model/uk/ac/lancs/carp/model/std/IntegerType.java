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
import java.util.Properties;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.std.BigIntegerDecoder;
import uk.ac.lancs.carp.codec.std.BigIntegerEncoder;
import uk.ac.lancs.carp.codec.std.ByteDecoder;
import uk.ac.lancs.carp.codec.std.ByteEncoder;
import uk.ac.lancs.carp.codec.std.IntegerDecoder;
import uk.ac.lancs.carp.codec.std.IntegerEncoder;
import uk.ac.lancs.carp.codec.std.LongDecoder;
import uk.ac.lancs.carp.codec.std.LongEncoder;
import uk.ac.lancs.carp.codec.std.ShortDecoder;
import uk.ac.lancs.carp.codec.std.ShortEncoder;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models an integer type with minimum and maximum values.
 * 
 * <p>
 * This type uses {@value #KEY} as its identifier in properties, and
 * defines two optional fields, {@value #MIN_FIELD} and
 * {@value #MAX_FIELD} specifying the range.
 * 
 * <p>
 * The corresponding type in Java may be {@code byte}, {@code short},
 * {@code int}, {@code long}, {@link Byte}, {@link Short},
 * {@link Integer}, {@link Long} or {@link BigInteger}. Reference types
 * are used when optional or in a primitive-forbidding context.
 * 
 * @author simpsons
 */
public final class IntegerType implements Type {
    private static enum Requirement {
        BYTE, SHORT, INT, LONG, BIG;
    };

    private static final BigInteger BYTE_MAX =
        BigInteger.valueOf(Byte.MAX_VALUE);

    private static final BigInteger BYTE_MIN =
        BigInteger.valueOf(Byte.MIN_VALUE);

    private static final BigInteger SHORT_MAX =
        BigInteger.valueOf(Short.MAX_VALUE);

    private static final BigInteger SHORT_MIN =
        BigInteger.valueOf(Short.MIN_VALUE);

    private static final BigInteger INT_MAX =
        BigInteger.valueOf(Integer.MAX_VALUE);

    private static final BigInteger INT_MIN =
        BigInteger.valueOf(Integer.MIN_VALUE);

    private static final BigInteger LONG_MAX =
        BigInteger.valueOf(Long.MAX_VALUE);

    private static final BigInteger LONG_MIN =
        BigInteger.valueOf(Long.MIN_VALUE);

    private final BigInteger min, max;

    /**
     * {@inheritDoc}
     * 
     * @default Depending on the range of this type, an encoder is
     * obtained using {@link ByteEncoder#get(IntFunction)},
     * {@link ShortEncoder#get(IntFunction)},
     * {@link IntegerEncoder#get(IntFunction)},
     * {@link LongEncoder#get(LongFunction)} or
     * {@link BigIntegerEncoder#get(Function)}.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        return getCodec(BigIntegerEncoder::get, LongEncoder::get,
                        IntegerEncoder::get, ShortEncoder::get,
                        ByteEncoder::get);
    }

    /**
     * {@inheritDoc}
     * 
     * @default Depending on the range of this type, a decoder is
     * obtained using {@link ByteDecoder#get(IntFunction)},
     * {@link ShortDecoder#get(IntFunction)},
     * {@link IntegerDecoder#get(IntFunction)},
     * {@link LongDecoder#get(LongFunction)} or
     * {@link BigIntegerDecoder#get(Function)}.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        return getCodec(BigIntegerDecoder::get, LongDecoder::get,
                        IntegerDecoder::get, ShortDecoder::get,
                        ByteDecoder::get);
    }

    private <T> T getCodec(Function<Function<BigInteger, String>, T> bigIntFunc,
                           Function<LongFunction<String>, T> longFunc,
                           Function<IntFunction<String>, T> intFunc,
                           Function<IntFunction<String>, T> shortFunc,
                           Function<IntFunction<String>, T> byteFunc) {
        if (min == null) {
            if (max == null) {
                assert req == Requirement.BIG;
                return bigIntFunc.apply(v -> null);
            } else {
                assert req == Requirement.BIG;
                return bigIntFunc.apply(v -> {
                    if (v.compareTo(max) > 0)
                        return v + " out of range [\u221e," + max + "]";
                    return null;
                });
            }
        } else if (max == null) {
            assert req == Requirement.BIG;
            return bigIntFunc.apply(v -> {
                if (v.compareTo(min) < 0)
                    return v + " out of range [" + min + ",\u221e]";
                return null;
            });
        } else {
            switch (req) {
            case BIG:
                return bigIntFunc.apply(v -> {
                    if (v.compareTo(min) < 0 || v.compareTo(max) > 0)
                        return v + " out of range [" + min + "," + max + "]";
                    return null;
                });

            case LONG:
                final long longMin = min.longValue();
                final long longMax = max.longValue();
                return longFunc.apply(v -> {
                    if (v < longMin || v > longMax)
                        return v + " out of range [" + min + "," + max + "]";
                    return null;
                });

            case SHORT:
            case BYTE:
            case INT:
                final int intMin = min.intValue();
                final int intMax = max.intValue();
                IntFunction<String> msgMaker = v -> {
                    if (v < intMin || v > intMax)
                        return v + " out of range [" + min + "," + max + "]";
                    return null;
                };
                switch (req) {
                case INT:
                    return intFunc.apply(msgMaker);

                case SHORT:
                    return shortFunc.apply(msgMaker);

                case BYTE:
                    return byteFunc.apply(msgMaker);

                default:
                    throw new AssertionError("unreachable");
                }

            default:
                throw new AssertionError("unreachable");
            }
        }
    }

    private final Requirement req;

    /**
     * Get a string representation for this type. This can be
     * <samp>...</samp> for an unbounded type,
     * <samp><var>min</var>...</samp> for a lower-bounded type,
     * <samp>..<var>max</var></samp> for an upper-bounded type, and
     * <samp><var>min</var>..<var>max</var></samp> for a fully bounded
     * type.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        if (min == null) {
            if (max == null) return "...";
            return ".." + max;
        } else if (max == null) {
            return min + "...";
        } else {
            return min + ".." + max;
        }
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
        result = prime * result + ((max == null) ? 0 : max.hashCode());
        result = prime * result + ((min == null) ? 0 : min.hashCode());
        return result;
    }

    /**
     * Test whether another object is identical to this type.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is an
     * {@link IntegerType} with the same bounds; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        IntegerType other = (IntegerType) obj;
        if (max == null) {
            if (other.max != null) return false;
        } else if (!max.equals(other.max)) return false;
        if (min == null) {
            if (other.min != null) return false;
        } else if (!min.equals(other.min)) return false;
        return true;
    }

    /**
     * Specify a new bounded integer type.
     * 
     * @param min the minimum value
     * 
     * @param max the maximum value
     */
    public IntegerType(long min, long max) {
        this(BigInteger.valueOf(min), BigInteger.valueOf(max));
    }

    /**
     * Specify a new integer type.
     * 
     * @param min the minimum value, or {@code null} if there should be
     * no minimum
     * 
     * @param max the maximum value, or {@code null} if there should be
     * no maximum
     * 
     * @throws IllegalArgumentException if the minimum value is greater
     * than the maximum
     */
    public IntegerType(BigInteger min, BigInteger max) {
        if (min != null && max != null && max.compareTo(min) < 0)
            throw new IllegalArgumentException("minimum " + min
                + " above maximum " + max);

        /* Determine the minimum representation based on the maximum
         * value. */
        final Requirement maxReq;
        if (max != null) {
            if (max.compareTo(INT_MAX) > 0) {
                if (max.compareTo(LONG_MAX) > 0) {
                    maxReq = Requirement.BIG;
                } else {
                    maxReq = Requirement.LONG;
                }
            } else if (max.compareTo(SHORT_MAX) > 0) {
                maxReq = Requirement.INT;
            } else if (max.compareTo(BYTE_MAX) > 0) {
                maxReq = Requirement.SHORT;
            } else {
                maxReq = Requirement.BYTE;
            }
        } else {
            maxReq = Requirement.BIG;
        }

        /* Determine the minimum representation based on the minimum
         * value. */
        final Requirement minReq;
        if (min != null) {
            if (min.compareTo(INT_MIN) < 0) {
                if (min.compareTo(LONG_MIN) < 0) {
                    minReq = Requirement.BIG;
                } else {
                    minReq = Requirement.LONG;
                }
            } else if (min.compareTo(SHORT_MIN) < 0) {
                minReq = Requirement.INT;
            } else if (min.compareTo(BYTE_MIN) < 0) {
                minReq = Requirement.SHORT;
            } else {
                minReq = Requirement.BYTE;
            }
        } else {
            minReq = Requirement.BIG;
        }

        /* Choose the minimum representation. */
        if (minReq.compareTo(maxReq) > 0)
            req = minReq;
        else
            req = maxReq;

        this.min = min;
        this.max = max;
    }

    static final String KEY = "int";

    static final String MIN_FIELD = "min";

    static final String MAX_FIELD = "max";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and sets the field {@value #MIN_FIELD} to the
     * minimum value (if set), and sets the field {@value #MAX_FIELD} to
     * the maximum value (if set).
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        if (min != null) into.setProperty(prefix + MIN_FIELD, min.toString());
        if (max != null) into.setProperty(prefix + MAX_FIELD, max.toString());
    }

    static IntegerType load(Properties props, String prefix) {
        assert KEY.equals(props.getProperty(prefix + "type"));

        final String maxText = props.getProperty(prefix + MAX_FIELD);
        final BigInteger max = maxText == null ? null : new BigInteger(maxText);

        final String minText = props.getProperty(prefix + MIN_FIELD);
        final BigInteger min = minText == null ? null : new BigInteger(minText);

        return new IntegerType(min, max);
    }

    /**
     * {@inheritDoc}
     * 
     * @return {@code true} if the bounds are within zero to
     * {@value Integer#MAX_VALUE}; {@code false} otherwise
     */
    @Override
    public boolean isBitSetIndex() {
        return min != null && min.compareTo(BigInteger.ZERO) >= 0 &&
            max != null &&
            max.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation generates <samp><var>thisRef</var>
     * != <var>otherRef</var></samp> if they are primitive and the
     * bounds are within those of {@code long}. Otherwise, it defers to
     * the default behaviour.
     */
    @Override
    public String getJavaInequalityExpression(boolean primitive, String thisRef,
                                              String otherRef,
                                              ExpansionContext ctxt) {
        switch (req) {
        default:
            if (primitive) return thisRef + " != " + otherRef;
            break;

        case BIG:
            break;
        }
        return Type.super.getJavaInequalityExpression(primitive, thisRef,
                                                      otherRef, ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation generates
     * <samp><var>ref</var></samp> if the argument is primitive and the
     * bounds are within those of {@code int}, <samp>(int) (<var>ref
     * </var>^ (<var>ref</var> >>> 32))</samp> if the argument is
     * primitive and the bounds are within those of {@code long}.
     * Otherwise, it defers to the default behaviour.
     */
    @Override
    public String getJavaHashExpression(boolean primitive, String ref,
                                        ExpansionContext ctxt) {
        switch (req) {
        default:
            if (primitive) return ref;
            break;

        case LONG:
            if (primitive) return "(int) (" + ref + " ^ (" + ref + " >>> 32))";
            break;

        case BIG:
            break;
        }
        return Type.super.getJavaHashExpression(primitive, ref, ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @default If the argument is primitive, <samp>int</samp> is
     * generated if the bounds are within those of {@code int},
     * <samp>long</samp> if the bounds a within those of {@code long},
     * or <samp>java.math.BigInteger</samp> otherwise.
     * 
     * <p>
     * If the argument is not primitive, <samp>java.lang.Integer</samp>
     * is generated if the bounds are within those of {@code int},
     * <samp>java.lang.Long</samp> if the bounds a within those of
     * {@code long}, or <samp>java.math.BigInteger</samp> otherwise.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase, ExpansionContext ctxt) {
        if (primitive) {
            switch (req) {
            default:
                return "int";

            case LONG:
                return "long";

            case BIG:
                return "java.math.BigInteger";
            }
        } else {
            switch (req) {
            default:
                return "java.lang.Integer";

            case LONG:
                return "java.lang.Long";

            case BIG:
                return "java.math.BigInteger";
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method always returns this object unchanged.
     */
    @Override
    public IntegerType qualify(ExternalName name, QualificationContext ctxt) {
        return this;
    }
}
