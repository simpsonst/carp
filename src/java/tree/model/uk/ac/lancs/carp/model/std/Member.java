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

import java.util.Objects;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models a member of a structure type or parameter list.
 * 
 * @author simpsons
 */
public final class Member {
    /**
     * Indicates the member's type
     */
    public final Type type;

    /**
     * Is set if the member is required.
     */
    public final boolean required;

    /**
     * Get a string representation of this structure member. This is an
     * optional question mark if the member is optional, a colon, and
     * the string representation of the member's type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return (required ? "" : "?") + " : " + type;
    }

    private Member(Type type, boolean required) {
        this.type = type;
        this.required = required;
    }

    /**
     * Resolve type references embedded in this member.
     * 
     * @param ctxt the context for resolution
     * 
     * @return a possibly new member with embedded types resolved
     */
    public Member resolve(QualificationContext ctxt) {
        Type altType = type.qualify(null, ctxt);
        if (!altType.equals(type)) return new Member(altType, required);
        return this;
    }

    /**
     * Create a required member.
     * 
     * @param type the member's type
     * 
     * @return the new member
     * 
     * @throws NullPointerException if the type is {@code null}
     */
    public static Member required(Type type) {
        Objects.requireNonNull(type, "type");
        return new Member(type, true);
    }

    /**
     * Create an optional member.
     * 
     * @param type the member's type
     * 
     * @return the new member
     * 
     * @throws NullPointerException if the type is {@code null}
     */
    public static Member optional(Type type) {
        Objects.requireNonNull(type, "type");
        return new Member(type, false);
    }

    /**
     * Get the hash code for this member.
     * 
     * @return the member's hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        assert type != null;
        result = prime * result + (required ? 1231 : 1237);
        result = prime * result + type.hashCode();
        return result;
    }

    /**
     * Test whether this member is equivalent to another object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a member, and has the
     * same type, and has the same requirement status; otherwise
     * {@code false}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Member other = (Member) obj;
        assert type != null;
        assert other.type != null;
        if (required != other.required) return false;
        return type.equals(other.type);
    }
}
