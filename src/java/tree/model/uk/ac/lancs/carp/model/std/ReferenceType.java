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
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.TypeInfo;
import uk.ac.lancs.carp.model.UndefinedTypeException;

/**
 * Models a reference to another type by name.
 * 
 * @author simpsons
 */
public final class ReferenceType implements Type {
    /**
     * The referenced type's name
     */
    public final ExternalName name;

    /**
     * The line number of the start position of the name in source, or 0
     * if not defined from source
     */
    public final int line;

    /**
     * The column number of the start position of the name in source, or
     * 0 if not defined from source
     */
    public final int column;

    /**
     * Specifies the source that defined this reference. It may be
     * {@code null} if the element was defined from compile-time source
     * path or class path.
     */
    public final ClassLoader source;

    /**
     * {@inheritDoc}
     * 
     * @default This method simply invokes
     * <code>dest.accept(name, referrent)</code>, as the point of the
     * {@link Type#gatherReferences(ExternalName, BiConsumer)} framework
     * is to identify types referenced by name.
     */
    @Override
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        dest.accept(name, referrent);
    }

    /**
     * Model a reference to a named type from the class path.
     * 
     * @param name the name of the referenced type
     * 
     * @param source the source which defined this element; or
     * {@code null} if not required
     * 
     * @throws NullPointerException if the name is {@code null}
     */
    public ReferenceType(ExternalName name, ClassLoader source) {
        this(name, source, 0, 0);
    }

    /**
     * Model a reference to a named type.
     * 
     * @param name the name of the referenced type
     * 
     * @param source the source which defined this element; or
     * {@code null} if not required
     * 
     * @param line the line number of the start position of the name in
     * source
     * 
     * @param column the column number of the start position of the name
     * in source
     * 
     * @throws NullPointerException if the name is {@code null}
     */
    private ReferenceType(ExternalName name, ClassLoader source, int line,
                          int column) {
        Objects.requireNonNull(name, "name");
        this.name = name;
        this.source = source;
        this.column = column;
        this.line = line;
    }

    /**
     * Model a reference to a named type from source.
     * 
     * @param name the name of the referenced type
     * 
     * @param line the line number of the start position of the name in
     * source
     * 
     * @param column the column number of the start position of the name
     * in source
     * 
     * @throws NullPointerException if the name is {@code null}
     */
    public ReferenceType(ExternalName name, int line, int column) {
        this(name, null, line, column);
    }

    /**
     * Get a string representation of this type. This is simply the
     * string representation of the referenced type's name.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return name.toString();
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
        assert name != null;
        result = prime * result + name.hashCode();
        result = prime * result + (source == null ? 0 : source.hashCode());
        return result;
    }

    /**
     * Test whether another object models an identical type to this one.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a
     * {@link ReferenceType} with the same name; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof ReferenceType)) return false;
        ReferenceType other = (ReferenceType) obj;
        assert name != null;
        assert other.name != null;
        if (!name.equals(other.name)) return false;
        if (source == null) {
            if (other.source != null) return false;
        } else if (!source.equals(other.source)) {
            return false;
        }
        return true;
    }

    static final String KEY = "ref";

    static final String REF_FIELD = "ref";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and sets the {@value #REF_FIELD} to the IDL name
     * of the referenced type.
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        into.setProperty(prefix + REF_FIELD, name.toString());
    }

    static ReferenceType load(Properties props, String prefix,
                              ClassLoader source) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        ExternalName name =
            ExternalName.parse(props.getProperty(prefix + REF_FIELD));
        return new ReferenceType(name, source);
    }

    /**
     * {@inheritDoc}
     * 
     * @default The referenced name is looked up using
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)},
     * and throws {@link UndefinedTypeException} if {@code null} is
     * returned.
     * 
     * <p>
     * Otherwise, if {@link Type#mustBeDefinedInJava()} on the returned
     * type is {@code true}, the default behaviour is invoked.
     * 
     * <p>
     * Otherwise, this method invokes itself on the type returned by
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)}.
     */
    @Override
    public String getJavaInequalityExpression(boolean primitive, String thisRef,
                                              String otherRef,
                                              ExpansionContext ctxt) {
        Type decl = ctxt.getModel(name);
        if (decl == null) throw new UndefinedTypeException(name.toString());
        if (decl.mustBeDefinedInJava())
            return Type.super.getJavaInequalityExpression(primitive, thisRef,
                                                          otherRef, ctxt);
        return decl.getJavaInequalityExpression(primitive, thisRef, otherRef,
                                                ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @default The referenced name is looked up using
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)},
     * and throws {@link UndefinedTypeException} if {@code null} is
     * returned.
     * 
     * <p>
     * Otherwise, if {@link Type#mustBeDefinedInJava()} on the returned
     * type is {@code true}, the default behaviour is invoked.
     * 
     * <p>
     * Otherwise, this method invokes itself on the type returned by
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)}.
     */
    @Override
    public String getJavaHashExpression(boolean primitive, String ref,
                                        ExpansionContext ctxt) {
        Type decl = ctxt.getModel(name);
        if (decl == null) throw new UndefinedTypeException(name.toString());
        if (decl.mustBeDefinedInJava())
            return Type.super.getJavaHashExpression(primitive, ref, ctxt);
        return decl.getJavaHashExpression(primitive, ref, ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @default The referenced name is looked up using
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)},
     * and throws {@link UndefinedTypeException} if {@code null} is
     * returned.
     * 
     * <p>
     * Otherwise, if {@link Type#mustBeDefinedInJava()} on the returned
     * type is {@code true},
     * {@link ExpansionContext#getTarget(uk.ac.lancs.carp.map.ExternalName)}
     * is applied to the name of the module of the referenced type, to
     * convert it to a Java package name. The leaf of the referenced
     * typed is converted using {@link ExternalName#asJavaClassName()},
     * and they are joined with a dot to form a fully qualified Java
     * class name, which is returned.
     * 
     * <p>
     * Otherwise, this method invokes itself on the type returned by
     * {@link ExpansionContext#getModel(uk.ac.lancs.carp.map.ExternalName)}.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase,
                              ExpansionContext ctxt) {
        /* If an IDL-named type (as this class identifies) maps to a
         * defined type in Java, we can simply reference that type. */
        Type decl = ctxt.getModel(name);
        if (decl == null) throw new UndefinedTypeException(name.toString());
        if (decl.mustBeDefinedInJava()) {
            ExternalName moduleName = name.getParent();
            String pkgName = ctxt.getTarget(moduleName);
            return pkgName + '.' + name.getLeaf().asJavaClassName();
        }

        /* The type we reference only exists by name in IDL, not in
         * Java, so we must use its expansion directly. For example, if
         * we have [type array *...], i.e., [array] is defined as a
         * sequence of unbounded integers, no Java type corresponding to
         * [array] will be generated. Wherever the IDL type needs to be
         * generated in source, its expansion should be used instead. */
        return decl.declareJava(primitive, erase, ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @param name ignored
     * 
     * @default This implementation returns this object if the
     * referenced type's name is fully qualified. Otherwise, it calls
     * {@link QualificationContext#qualify(ExternalName)} on the
     * referenced type's name, reporting details via
     * {@link QualificationContext#report(ExternalName, int, int)} if
     * the result is {@code null}, and returning this object. Otherwise,
     * it returns a new {@link ReferenceType} with the fully qualified
     * name and the same class loader.
     */
    @Override
    public ReferenceType qualify(ExternalName name, QualificationContext ctxt) {
        if (!this.name.isLeaf()) return this;
        ExternalName altName = ctxt.qualify(this.name);
        if (altName == null) {
            ctxt.report(this.name, this.line, this.column);
            return this;
        }
        ReferenceType replacement = new ReferenceType(altName, source);
        ctxt.copyAssociations(replacement, this);
        return replacement;
    }

    /**
     * {@inheritDoc}
     * 
     * @default The referenced type's name and class loader are passed
     * to {@link LinkContext#seek(ExternalName, ClassLoader)}, and
     * {@link Type#getEncoder(Class, LinkContext)} is applied to the
     * resultant type definition.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        TypeInfo typeInfo = ctxt.seek(this.name, source);
        return typeInfo.def.getEncoder(typeInfo.type, ctxt);
    }

    /**
     * {@inheritDoc}
     * 
     * @default The referenced type's name and class loader are passed
     * to {@link LinkContext#seek(ExternalName, ClassLoader)}, and
     * {@link Type#getDecoder(Class, LinkContext)} is applied to the
     * resultant type definition.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        TypeInfo typeInfo = ctxt.seek(this.name, source);
        return typeInfo.def.getDecoder(typeInfo.type, ctxt);
    }
}
