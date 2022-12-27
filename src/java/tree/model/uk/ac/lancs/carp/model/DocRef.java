// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2021, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.carp.model;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.carp.map.ExternalName;

/**
 * References another type, member or response. Three immutable
 * {@link ExternalName} components may be specified. More significant
 * components may be {@code null} to indicate that they are to be
 * inferred by context. Less significant components may be {@code null}
 * to reference a broader scope, e.g., a whole type rather than a
 * specific member.
 * 
 * @author simpsons
 */
public class DocRef {
    /**
     * Identifies the type by its name. May be {@code null} if not
     * specified, i.e., it is to be inferred by context.
     */
    public final ExternalName typeName;

    /**
     * Identifies the member of a type by its name. May be {@code null}
     * if not specified, i.e., it is to be inferred by context or the
     * whole type is referenced. A member may be an enumeration
     * constant, a structure field, or a method.
     */
    public final ExternalName memberName;

    /**
     * Identifies a response type by its name. May be {@code null} if
     * not specified, or is to be inferred by context, or the reference
     * is to something other than a method response.
     */
    public final ExternalName responseName;

    private static final String WORD = "\\p{Alpha}[\\p{Digit}\\p{Alpha}]*";

    private static final String WORDS = WORD + "(?:[-/]" + WORD + ")*";

    private static final Pattern ELEM_REF =
        Pattern.compile("^(?<type>" + WORDS + "(?:\\." + WORDS
            + ")*)?(?:#(?<memb>" + WORDS + "))?(?:\\?(?<rsp>" + WORDS + "))?$");

    /**
     * Create a reference from an integrated string. The full format is
     * <samp><var>type</var>#<var>member</var>?<var>response</var></samp>.
     * The <var>type</var> must conform to an {@link ExternalName}, with
     * or without a module. <var>member</var> and <var>response</var>
     * must be leaves of {@link ExternalName}.
     * 
     * <p>
     * All components are optional. However, at least one component must
     * be specified. Additionally, <var>member</var> cannot be absent
     * while the other components are present. <code>#</code> and
     * <code>?</code> must be present if their following components are
     * to be regarded as present, respectively.
     * 
     * @param text the text to parse
     */
    public DocRef(String text) {
        Matcher m = ELEM_REF.matcher(text);
        if (!m.matches())
            throw new IllegalArgumentException("bad ref: " + text);
        this.typeName = ExternalName.parseNull(m.group("type"));
        this.memberName = ExternalName.parseNull(m.group("memb"));
        this.responseName = ExternalName.parseNull(m.group("rsp"));

        /* You can't have the most and least significant components set
         * while leaving the middle unset. */
        if (this.memberName == null && typeName != null && responseName != null)
            throw new IllegalArgumentException("illegal "
                + "set-null-set combination: " + text);
    }

    private DocRef(DocRef from, ExternalName typeName,
                   ExternalName memberName) {
        this.typeName = typeName != null ? typeName : from.typeName;
        this.memberName = memberName != null ? memberName : from.memberName;
        this.responseName = from.responseName;
    }

    /**
     * Qualify the type name. The other components are preserved, while
     * the type name is replaced with the specified value.
     * 
     * @param typeName the replacement type name
     * 
     * @return the new reference
     * 
     * @constructor
     */
    public DocRef qualify(ExternalName typeName) {
        return new DocRef(this, typeName, null);
    }

    /**
     * Qualify the type name and member name. The response component is
     * preserved, while the type name and member name are replaced with
     * the specified values.
     * 
     * @param typeName the replacement type name
     * 
     * @param memberName the replacement member name
     * 
     * @return the new reference
     * 
     * @constructor
     */
    public DocRef qualify(ExternalName typeName, ExternalName memberName) {
        return new DocRef(this, typeName, memberName);
    }

    /**
     * Get a string representation of this reference. This includes the
     * type name (or an empty string if {@code null}), the member name
     * prefixed with a hash <samp>#</samp> (if not {@code null}), and
     * the response name prefixed with a query <samp>?</samp> (if not
     * {@code null}).
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return (typeName == null ? "" : typeName)
            + (memberName == null ? "" : "#" + memberName)
            + (responseName == null ? "" : "?" + responseName);
    }

    /**
     * Get the hash code for this reference.
     * 
     * @return the reference's hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + Objects.hashCode(this.typeName);
        hash = 37 * hash + Objects.hashCode(this.memberName);
        hash = 37 * hash + Objects.hashCode(this.responseName);
        return hash;
    }

    /**
     * Test whether another object equals this reference.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is also a reference and
     * has equal fields; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final DocRef other = (DocRef) obj;
        if (!Objects.equals(this.typeName, other.typeName)) return false;
        if (!Objects.equals(this.memberName, other.memberName)) return false;
        if (!Objects.equals(this.responseName, other.responseName))
            return false;
        return true;
    }
}
