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

package uk.ac.lancs.carp.component.std;

import java.math.BigInteger;
import java.util.UUID;
import java.util.regex.Matcher;
import uk.ac.lancs.carp.component.Discriminator;

/**
 * Encodes and decodes integers in a given base.
 * 
 * @author simpsons
 */
public final class IntegerDiscriminator implements Discriminator<BigInteger> {
    private final int base;

    private final String pattern;

    private final String padding;

    private final String group;

    private IntegerDiscriminator(int base, int pad) {
        if (base < Character.MIN_RADIX || base > Character.MAX_RADIX)
            throw new IllegalArgumentException("base out of range: " + base);

        this.base = base;
        this.group = "id" + UUID.randomUUID().toString().replace("-", "");
        if (pad < 1) {
            this.pattern = "(?<" + this.group + ">[0-9]+)";
            this.padding = null;
        } else {
            this.pattern = "(?<" + this.group + ">[0-9]{" + pad + "})";
            this.padding = "0".repeat(pad);
        }
    }

    /**
     * Create a padded discriminator.
     * 
     * @param base the base to encode with
     * 
     * @param pad the number of padding digits
     * 
     * @return the requested discriminator
     * 
     * @throws IllegalArgumentException if the padding is non-positive,
     * or the base is out of range
     */
    public static IntegerDiscriminator padded(int base, int pad) {
        if (pad < 1)
            throw new IllegalArgumentException("non-positive pad: " + pad);
        return new IntegerDiscriminator(base, pad);
    }

    /**
     * Create an unpadded discriminator.
     * 
     * @param base the base to encode with
     * 
     * @return the requested discriminator
     */
    public static IntegerDiscriminator unpadded(int base) {
        return new IntegerDiscriminator(base, 0);
    }

    @Override
    public BigInteger decode(Matcher matcher) {
        return new BigInteger(matcher.group(group), base);
    }

    @Override
    public CharSequence encode(BigInteger key) {
        String sig = key.toString(base);
        if (padding == null) return sig;
        if (sig.length() > padding.length())
            throw new IllegalArgumentException("key too long: " + sig);
        return padding.substring(0, padding.length() - sig.length()) + sig;
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
