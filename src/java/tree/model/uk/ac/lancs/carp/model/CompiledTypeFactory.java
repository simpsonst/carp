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

package uk.ac.lancs.carp.model;

import java.util.Objects;
import java.util.Properties;
import java.util.ServiceLoader;

/**
 * Knows how to load a type specification from various representations.
 * Such a factory should be annotated with {@link TypeKey}, so that
 * {@link #getFactory(java.lang.String, java.lang.ClassLoader)} will use
 * it when called with its {@code typeKey} argument set to that value.
 * 
 * @author simpsons
 */
public interface CompiledTypeFactory {
    /**
     * Load a type specification from its properties.
     * 
     * @param props the structure containing the type specification
     * 
     * @param prefix the prefix of property names defining the type
     * 
     * @param ctxt the context for loading nested types
     * 
     * @return the type specification
     */
    Type load(Properties props, String prefix, LoadContext ctxt);

    /**
     * Get the type factory suitable for loading a type definition from
     * a properties file.
     * 
     * @param typeKey the definition kind
     * 
     * @param loader the class loader to search for implementations
     * 
     * @return the matching factory; or {@code null} if not recognized
     */
    static CompiledTypeFactory getFactory(String typeKey, ClassLoader loader) {
        Objects.requireNonNull(typeKey, "typeKey");
        for (CompiledTypeFactory fact : ServiceLoader
            .load(CompiledTypeFactory.class, loader)) {
            TypeKey got = fact.getClass().getAnnotation(TypeKey.class);
            if (got == null) continue;
            assert got != null;
            if (!typeKey.equals(got.value())) continue;
            return fact;
        }
        return null;
    }
}
