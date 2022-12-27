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

package uk.ac.lancs.carp.component.std;

import java.util.UUID;
import java.util.regex.Matcher;
import uk.ac.lancs.carp.component.Discriminator;

/**
 * Encodes and decodes UUIDs. The case of encoded values must be
 * specified. Decoding can be tolerant to case.
 * 
 * @author simpsons
 */
public final class UUIDDiscriminator implements Discriminator<UUID> {
    private final String pattern;

    private final String group;

    private final boolean upper;

    private UUIDDiscriminator(boolean upper, boolean tolerant) {
        String digit =
            tolerant ? "[0-9a-fA-F]" : upper ? "[0-9A-F]" : "[0-9a-f]";

        this.upper = upper;
        this.group = "id" + UUID.randomUUID().toString().replace("-", "");
        pattern = "(?<" + group + ">" + digit + "{4}-" + digit + "{2}-" + digit
            + "{2}-" + digit + "{2}-" + digit + "{6})";
    }

    /**
     * Create a discriminator which encodes in upper-case, but accepts
     * either case.
     * 
     * @return the requested discriminator
     */
    public static UUIDDiscriminator tolerantUpper() {
        return new UUIDDiscriminator(true, true);
    }

    /**
     * Create a discriminator which encodes and decodes only in
     * upper-case.
     * 
     * @return the requested discriminator
     */
    public static UUIDDiscriminator intolerantUpper() {
        return new UUIDDiscriminator(true, false);
    }

    /**
     * Create a discriminator which encodes in lower-case, but accepts
     * either case.
     * 
     * @return the requested discriminator
     */
    public static UUIDDiscriminator tolerantLower() {
        return new UUIDDiscriminator(false, true);
    }

    /**
     * Create a discriminator which encodes and decodes only in
     * lower-case.
     * 
     * @return the requested discriminator
     */
    public static UUIDDiscriminator intolerantLower() {
        return new UUIDDiscriminator(false, false);
    }

    @Override
    public UUID decode(Matcher matcher) {
        return UUID.fromString(matcher.group());
    }

    @Override
    public CharSequence encode(UUID key) {
        return upper ? key.toString().toUpperCase() : key.toString();
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
