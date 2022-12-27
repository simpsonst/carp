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

package uk.ac.lancs.carp.model;

import java.util.BitSet;
import java.util.Properties;
import java.util.function.BiConsumer;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.map.ExternalName;

/**
 * Models an IDL type.
 * 
 * @author simpsons
 */
public interface Type {
    /**
     * The property name suffix identifying the
     * {@link CompiledTypeFactory} to use to load a type
     */
    String TYPE_PROPERTY_FIELD = "type";

    /**
     * The property name suffix that a type implementation must not use,
     * and reserved for its caller
     */
    String IDENTITY_PROPERTY_FIELD = "name";

    /**
     * Describe this type as a set of Java properties.
     * 
     * @param prefix the prefix of property names to be inserted
     * 
     * @param props the destination for properties describing the type
     * 
     * @default The implementation must not use the key suffix
     * {@value #IDENTITY_PROPERTY_FIELD}. It's used by structure types
     * and module definitions to identify their members.
     */
    void describe(String prefix, Properties props);

    /**
     * Get the text of a variable declaration.
     * 
     * @param primitive whether a primitive type is acceptable
     * 
     * @param ctxt a context for resolving IDL names to native names and
     * looking up named types for their definitions
     * 
     * @param erase {@code true} if generic information is to be removed
     * 
     * @return the variable declaration as text
     * 
     * @throws UndeclarableElementException if the type cannot be
     * dynamically declared in Java
     * 
     * @throws UndefinedTypeException if a type name has no
     * corresponding definition
     * 
     * @default This method throws an
     * {@link UndeclarableElementException} by default.
     */
    default String declareJava(boolean primitive, boolean erase,
                               ExpansionContext ctxt) {
        throw new UndeclarableElementException(this.toString());
    }

    /**
     * Submit any type names referenced by this type.
     * 
     * @param referrent the name of the referring type
     * 
     * @param dest invoked with each referenced name as its first
     * argument, and the name of the referring type as its second
     * 
     * @default An implementation that does not directly reference any
     * type name should pass the call on to any types it is composed of.
     * The default behaviour is to do nothing.
     */
    default void gatherReferences(ExternalName referrent,
                                  BiConsumer<? super ExternalName,
                                             ? super ExternalName> dest) {}

    /**
     * Get the text of a JavaDoc reference. This method should not need
     * to be called directly, but by
     * {@link ExpansionContext#linkJavaDoc(DocRef)} instead, which
     * identifies the receiver of this method, and the package name, and
     * provides itself and the original reference.
     * 
     * @param ref the CARP documentation reference. The
     * {@link DocRef#typeName} component must identify the invoked
     * object.
     * 
     * @param pkgName the Java package name for this type. This must be
     * derived from the parent of {@link DocRef#typeName}.
     * 
     * @param ctxt a context for expanding component types
     * 
     * @return the string representation of the reference for embedding
     * as a JavaDoc <code>&#64;see</code> or
     * <code>&#123;&#64;link&#125;</code> tag
     * 
     * @default The default behaviour is to throw
     * {@link UnsupportedOperationException}.
     * 
     * @throws UnsupportedOperationException if no link can be made
     */
    default String linkJavaDoc(DocRef ref, String pkgName,
                               ExpansionContext ctxt) {
        throw new UnsupportedOperationException("not supported");
    }

    /**
     * Create an equivalent type specification with no unresolved type
     * references.
     * 
     * @param name the name defining this type; or {@code null} if this
     * is not a defined type
     * 
     * @param ctxt a context for mapping a local name to a fully
     * qualified one, and for reporting errors
     * 
     * @return the new type specification
     * 
     * @throws UnresolvedTypeException if resolution yielded no type
     */
    Type qualify(ExternalName name, QualificationContext ctxt);

    /**
     * Get an encoder for this type.
     * 
     * @param ctxt a context for linking referenced types
     * 
     * @param type the specially generated Java that this type maps to;
     * or {@code null} if it maps to no type, i.e.,
     * {@link #mustBeDefinedInJava()} returns {@code false}
     * 
     * @return the requested encoder
     * 
     * @throws BadRuntimeTypeException if the {@code type} argument has
     * an unexpected value
     */
    Encoder getEncoder(Class<?> type, LinkContext ctxt);

    /**
     * Get a decoder for this type.
     * 
     * @param ctxt a context for linking referenced types
     * 
     * @param type the specially generated Java that this type maps to;
     * or {@code null} if it maps to no type, i.e.,
     * {@link #mustBeDefinedInJava()} returns {@code false}
     * 
     * @return the requested decoder
     * 
     * @throws BadRuntimeTypeException if the {@code type} argument has
     * an unexpected value
     */
    Decoder getDecoder(Class<?> type, LinkContext ctxt);

    /**
     * Get the Java hash expression suitable for contributing a value of
     * this type to a hash code.
     * 
     * @param primitive whether a primitive type is acceptable
     * 
     * @param ref a string that references the value
     * 
     * @param ctxt a context for resolving IDL names to native names and
     * looking up named types for their definitions
     * 
     * @return the Java expression
     * 
     * @default The default behaviour is to return an expression
     * invoking {@link java.util.Objects#hashCode(Object)} on the
     * referenced string.
     */
    default String getJavaHashExpression(boolean primitive, String ref,
                                         ExpansionContext ctxt) {
        return "java.util.Objects.hashCode(" + ref + ")";
    }

    /**
     * Get the Java equality expression suitable for contributing to the
     * implementation of {@link Object#equals(Object)}.
     * 
     * 
     * @param primitive whether a primitive type is acceptable
     * 
     * @param ctxt a context for resolving IDL names to native names and
     * looking up named types for their definitions
     * 
     * @param thisRef a string that references one of the values
     * 
     * @param otherRef a string that references the value being tested
     * 
     * @return the Java expression
     * 
     * @default The default behaviour is to return an expression that
     * invokes {@link Object#equals(Object)} on the first reference
     */
    default String getJavaInequalityExpression(boolean primitive,
                                               String thisRef, String otherRef,
                                               ExpansionContext ctxt) {
        return "!" + thisRef + ".equals(" + otherRef + ")";
    }

    /**
     * Load a type specification from properties. The type is identified
     * by appending <samp>type</samp> to the prefix, getting that
     * property's value as a key, loading a factory matching that key
     * with {@link CompiledTypeFactory#getFactory(String, ClassLoader)},
     * and then loading the type with
     * {@link CompiledTypeFactory#load(Properties, String, LoadContext)}.
     * 
     * @param prefix the prefix of names of properties to examine
     * 
     * @param from the properties to examine
     * 
     * @param ctxt the context for loading nested classes
     * 
     * @return the type specification; or {@code null} if not recognized
     * 
     * @constructor
     */
    public static Type load(String prefix, Properties from, LoadContext ctxt) {
        String key = from.getProperty(prefix + "type");
        if (key == null) return null;
        CompiledTypeFactory fact =
            CompiledTypeFactory.getFactory(key, ctxt.implementations());
        if (fact == null) return null;
        return fact.load(from, prefix, ctxt);
    }

    /**
     * Determine whether this is a type that should be represented in
     * Java with its own type.
     * 
     * @return {@code true} if this type should be defined in Java;
     * {@code false} otherwise
     * 
     * @default This method returns {@code false} by default.
     */
    default boolean mustBeDefinedInJava() {
        return false;
    }

    /**
     * Write out a definition of this type. This method shall not be
     * called on implementations for which
     * {@link #mustBeDefinedInJava()} returns {@code false}.
     * 
     * @param out the destination for writing characters
     * 
     * @param name the IDL name of the type being defined
     * 
     * @param ctxt a context for resolving IDL names to native names and
     * looking up named types for their definitions
     * 
     * @throws UndefinableTypeException if it is inappropriate to call
     * this method
     * 
     * @throws UndefinedTypeException if a type name has no
     * corresponding definition
     * 
     * @default This method throws an {@link UndefinableTypeException}
     * by default.
     */
    default void defineJavaType(TextFile out, ExternalName name,
                                ExpansionContext ctxt) {
        throw new UndefinableTypeException(this.toString());
    }

    /**
     * Determine whether this is a type that can be used as the index of
     * a {@link BitSet}.
     * 
     * @return {@code true} if this type is suitable as a {@link BitSet}
     * index; {@code false} otherwise
     * 
     * @default This method returns {@code false} by default.
     */
    default boolean isBitSetIndex() {
        return false;
    }

    /**
     * Determine whether this type might need brackets around it in
     * certain contexts.
     * 
     * @return {@code true} if this type might need brackets;
     * {@code false} otherwise
     * 
     * @default This method returns false by default.
     * 
     * <p>
     * The purpose of this method is to determine when to wrap the
     * string representation of this type (as returned by
     * {@link Object#toString()}) in parentheses.
     */
    default boolean ofLowPrecedence() {
        return false;
    }
}
