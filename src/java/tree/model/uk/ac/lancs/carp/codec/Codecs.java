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

package uk.ac.lancs.carp.codec;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.UUID;
import javax.json.JsonNumber;
import javax.json.JsonString;
import javax.json.JsonValue;

/**
 * Static utilities for JSON conversion
 *
 * @author simpsons
 */
public final class Codecs {
    /**
     * Create a JSON number from a {@code int}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(int value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Integer.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return true;
            }

            @Override
            public int intValue() {
                return value;
            }

            @Override
            public int intValueExact() {
                return value;
            }

            @Override
            public long longValue() {
                return value;
            }

            @Override
            public long longValueExact() {
                return value;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(value);
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(value);
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@code long}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(long value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Long.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return true;
            }

            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public int intValueExact() {
                int r = (int) value;
                if (r != value)
                    throw new ArithmeticException("too big for int: " + value);
                return r;
            }

            @Override
            public long longValue() {
                return value;
            }

            @Override
            public long longValueExact() {
                return value;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(value);
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(value);
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@code short}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(short value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Short.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return true;
            }

            @Override
            public int intValue() {
                return value;
            }

            @Override
            public int intValueExact() {
                return value;
            }

            @Override
            public long longValue() {
                return value;
            }

            @Override
            public long longValueExact() {
                return value;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(value);
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(value);
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@code float}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(float value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Float.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return false;
            }

            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public int intValueExact() {
                int r = intValue();
                if (r != value)
                    throw new ArithmeticException("not integral: " + value);
                return r;
            }

            @Override
            public long longValue() {
                return (long) value;
            }

            @Override
            public long longValueExact() {
                long r = longValue();
                if (r != value)
                    throw new ArithmeticException("not long-integral: "
                        + value);
                return r;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(longValue());
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(longValueExact());
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON Boolean value from a java {@code boolean}.
     * 
     * @param value the value to be converted
     * 
     * @return the converted value
     */
    public static JsonValue asJson(boolean value) {
        return value ? JsonValue.TRUE : JsonValue.FALSE;
    }

    /**
     * Create a JSON string from a Java {@link String}.
     * 
     * @param value the value to be converted
     * 
     * @return the converted value
     */
    public static JsonString asJson(String value) {
        return new JsonString() {
            @Override
            public String getString() {
                return value;
            }

            @Override
            public CharSequence getChars() {
                return value;
            }

            @Override
            public ValueType getValueType() {
                return ValueType.STRING;
            }
        };
    }

    /**
     * Create a JSON string from a Java {@link CharSequence}.
     * 
     * @param value the value to be converted
     * 
     * @return the converted value
     */
    public static JsonString asJson(CharSequence value) {
        return new JsonString() {
            @Override
            public String getString() {
                return value.toString();
            }

            @Override
            public CharSequence getChars() {
                return value;
            }

            @Override
            public ValueType getValueType() {
                return ValueType.STRING;
            }
        };
    }

    /**
     * Create a JSON number from a {@link BigDecimal}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(BigDecimal value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return value.toString();
            }

            @Override
            public boolean isIntegral() {
                return false;
            }

            @Override
            public int intValue() {
                return value.intValue();
            }

            @Override
            public int intValueExact() {
                return value.intValueExact();
            }

            @Override
            public long longValue() {
                return value.longValue();
            }

            @Override
            public long longValueExact() {
                return value.longValueExact();
            }

            @Override
            public BigInteger bigIntegerValue() {
                return value.toBigInteger();
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return value.toBigIntegerExact();
            }

            @Override
            public double doubleValue() {
                return value.doubleValue();
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return value;
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@code double}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(double value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Double.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return false;
            }

            @Override
            public int intValue() {
                return (int) value;
            }

            @Override
            public int intValueExact() {
                int r = intValue();
                if (r != value)
                    throw new ArithmeticException("not integral: " + value);
                return r;
            }

            @Override
            public long longValue() {
                return (long) value;
            }

            @Override
            public long longValueExact() {
                long r = longValue();
                if (r != value)
                    throw new ArithmeticException("not long-integral: "
                        + value);
                return r;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(longValue());
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(longValueExact());
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@code byte}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(byte value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return Byte.toString(value);
            }

            @Override
            public boolean isIntegral() {
                return true;
            }

            @Override
            public int intValue() {
                return value;
            }

            @Override
            public int intValueExact() {
                return value;
            }

            @Override
            public long longValue() {
                return value;
            }

            @Override
            public long longValueExact() {
                return value;
            }

            @Override
            public BigInteger bigIntegerValue() {
                return BigInteger.valueOf(value);
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return BigInteger.valueOf(value);
            }

            @Override
            public double doubleValue() {
                return value;
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return BigDecimal.valueOf(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON number from a {@link BigInteger}.
     *
     * @param value the value to be converted
     *
     * @return the converted value
     */
    public static JsonNumber asJson(BigInteger value) {
        return new JsonNumber() {
            @Override
            public String toString() {
                return value.toString();
            }

            @Override
            public boolean isIntegral() {
                return true;
            }

            @Override
            public int intValue() {
                return value.intValue();
            }

            @Override
            public int intValueExact() {
                return value.intValueExact();
            }

            @Override
            public long longValue() {
                return value.longValue();
            }

            @Override
            public long longValueExact() {
                return value.longValueExact();
            }

            @Override
            public BigInteger bigIntegerValue() {
                return value;
            }

            @Override
            public BigInteger bigIntegerValueExact() {
                return value;
            }

            @Override
            public double doubleValue() {
                return value.doubleValue();
            }

            @Override
            public BigDecimal bigDecimalValue() {
                return new BigDecimal(value);
            }

            @Override
            public ValueType getValueType() {
                return ValueType.NUMBER;
            }
        };
    }

    /**
     * Create a JSON string from a Java {@link UUID}.
     * 
     * @param value the value to be converted
     * 
     * @return the converted value
     */
    public static JsonString asJson(UUID value) {
        return asJson(value.toString());
    }

    /**
     * Create a UUID from a JSON string. The string is processed to be
     * more tolerant of variations. Non-hex characters are stripped,
     * then dashes are inserted exactly where {@link UUID#toString()}
     * specifies, so that {@link UUID#fromString(String) will accept the
     * input.
     * 
     * @param value the value to be converted
     * 
     * @return the converted value
     */
    public static UUID asUUID(JsonString value) {
        String s = value.getString();
        s = s.replaceAll("[^0-9a-fA-F]", "");
        s = s.subSequence(0, 8) + "-" + s.subSequence(8, 12) + "-"
            + s.subSequence(12, 16) + "-" + s.subSequence(16, 20) + "-"
            + s.subSequence(20, 32);
        return UUID.fromString(s);
    }

    private Codecs() {}
}
