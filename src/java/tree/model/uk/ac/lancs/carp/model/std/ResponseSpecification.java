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

import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.UnresolvedTypeException;

/**
 * Models a response type for a call.
 * 
 * @author simpsons
 */
public final class ResponseSpecification {
    /**
     * Specifies the parameters that can be passed in this response.
     */
    public final StructureType parameters;

    /**
     * Submit any type names referenced by this response type. This
     * simply passes the call on to
     * {@link Type#gatherReferences(ExternalName, BiConsumer)} applied
     * to {@link #parameters}.
     * 
     * @param referrent the name of the referring type
     * 
     * @param dest invoked with each referenced name as its first
     * argument, and the name of the referring type as its second
     */
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        parameters.gatherReferences(referrent, dest);
    }

    /**
     * Model a response type.
     * 
     * @param parameters a specification of the parameters that can be
     * passed
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public ResponseSpecification(StructureType parameters) {
        Objects.requireNonNull(parameters, "parameters");
        this.parameters = parameters;
    }

    /**
     * Get a string representation of this object. This is simply the
     * string representation of its parameters as a
     * {@link StructureType}.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return parameters.isEmpty() ? "" : (" " + parameters.toString());
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
        result = prime * result + parameters.hashCode();
        return result;
    }

    /**
     * Test whether another object models the same response as this
     * object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a
     * {@link ResponseSpecification} with identical parameters;
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ResponseSpecification)) return false;
        ResponseSpecification other = (ResponseSpecification) obj;
        if (parameters == null) {
            if (other.parameters != null) return false;
        } else if (!parameters.equals(other.parameters)) return false;
        return true;
    }

    /**
     * Create an equivalent response specification with no unresolved
     * type references.
     * 
     * @param ctxt a context for mapping a local name to a fully
     * qualified one, and for reporting errors
     * 
     * @return a new response specification if any of the original's
     * components changed when resolved; this object otherwise
     * 
     * @throws UnresolvedTypeException if resolution yielded no type
     */
    public ResponseSpecification qualify(QualificationContext ctxt) {
        StructureType altParams = parameters.qualify(null, ctxt);
        if (!altParams.equals(parameters)) {
            ResponseSpecification alt = new ResponseSpecification(altParams);
            ctxt.copyAssociations(alt, this);
            return alt;
        }
        return this;
    }

    /**
     * Describe this response type as a set of Java properties.
     * 
     * @param prefix the prefix of property names to be inserted
     * 
     * @param props the destination for properties describing the
     * response type
     */
    void describe(String prefix, Properties props) {
        parameters.describe(prefix + "params.", props);
    }

    static ResponseSpecification load(Properties from, String prefix,
                                      LoadContext ctxt) {
        StructureType params =
            StructureType.load(from, prefix + "params.", ctxt);
        return new ResponseSpecification(params);
    }
}
