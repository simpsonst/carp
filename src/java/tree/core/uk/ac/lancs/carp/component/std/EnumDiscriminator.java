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

import java.lang.reflect.Field;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.carp.component.Discriminator;
import uk.ac.lancs.carp.map.Constant;
import uk.ac.lancs.carp.map.ExternalName;

/**
 * Encodes and decodes enumeration constants.
 * 
 * @param <E> the enumeration type
 * 
 * @author simpsons
 */
public class EnumDiscriminator<E extends Enum<E>> implements Discriminator<E> {
    private final String groupName =
        "id" + UUID.randomUUID().toString().replace("-", "");

    private final String pattern;

    private final Map<E, String> mapToString;

    private final Map<String, E> mapFromString;

    /**
     * Create a discriminator for an enumeration type.
     * 
     * @param type the enumeration type
     */
    public EnumDiscriminator(Class<E> type) {
        assert type.isEnum();
        this.mapToString = new EnumMap<>(type);
        this.mapFromString = new HashMap<>();
        E[] consts = type.getEnumConstants();
        StringBuilder pbuf = new StringBuilder();
        pbuf.append("(?<").append(groupName).append('>');
        String sep = "";
        for (E c : consts) {
            try {
                Field f = type.getField(c.name());
                var der = f.getAnnotation(Constant.class);
                ExternalName en = ExternalName.parse(der.value());
                String m = en.asPathElements();
                mapFromString.put(m, c);
                mapToString.put(c, m);
                pbuf.append(sep).append(Pattern.quote(m));
                sep = "|";
            } catch (NoSuchFieldException ex) {
                throw new AssertionError("unreachable", ex);
            }
        }
        pbuf.append(')');
        this.pattern = pbuf.toString();
    }

    @Override
    public E decode(Matcher matcher) {
        return mapFromString.get(matcher.group(groupName));
    }

    @Override
    public CharSequence encode(E key) {
        return mapToString.get(key);
    }

    @Override
    public String pattern() {
        return pattern;
    }
}
