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

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.std.MappedObjectDecoder;
import uk.ac.lancs.carp.codec.std.MappedObjectEncoder;
import uk.ac.lancs.carp.map.Constant;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.TypeModel;
import uk.ac.lancs.carp.model.BadRuntimeTypeException;
import uk.ac.lancs.carp.model.DocContext;
import uk.ac.lancs.carp.model.DocRef;
import uk.ac.lancs.carp.model.DocRenderer;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.Tab;
import uk.ac.lancs.carp.model.TextFile;
import uk.ac.lancs.carp.model.Type;

/**
 * Models an enumerated type with a fixed set of symbolic values.
 * 
 * <p>
 * This type uses {@value #KEY} as its identifier in properties, and
 * defines the field {@value #SIZE_FIELD} to specify the number of
 * members in the type. It then defines numeric subfields of
 * {@value #ITEM_FIELD} as holding the names of the members.
 * 
 * <p>
 * The corresponding type in Java is an {@code enum} class definition,
 * whose members are named according to
 * {@link ExternalName#asJavaConstantName()}.
 * 
 * @author simpsons
 */
public final class EnumeratedType implements Type {
    /**
     * The set of constants defined by this type
     */
    public final Collection<ExternalName> constants;

    private Map<String, Object> getMapping(Class<?> type)
        throws NoSuchMethodException,
            IllegalAccessException,
            InvocationTargetException,
            NoSuchFieldException {
        Map<String, Object> mapping = new HashMap<>();
        Method getValues = type.getMethod("values");
        getValues.setAccessible(true);
        Object[] values = (Object[]) getValues.invoke(type);
        for (Object v : values) {
            Enum<?> ev = (Enum<?>) v;
            String n = ev.name();
            Field f = type.getField(n);
            var der = f.getAnnotation(Constant.class);
            if (der == null) continue;
            String m = der.value();
            mapping.put(m, v);
        }
        return mapping;
    }

    private void checkArgs(Class<?> type) {
        if (type == null)
            throw new BadRuntimeTypeException("type must be specified");
        if (!type.isEnum())
            throw new BadRuntimeTypeException("not enum: " + type);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns a
     * {@link MappedObjectEncoder} using the Java enumeration constants
     * and the string representations of their IDL names.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        checkArgs(type);
        try {
            Map<String, Object> mapping = getMapping(type);
            return MappedObjectEncoder.byString(mapping);
        } catch (NoSuchMethodException | SecurityException |
                 IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchFieldException ex) {
            throw new BadRuntimeTypeException(ex);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns a
     * {@link MappedObjectDecoder} using the Java enumeration constants
     * and the string representations of their IDL names.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        checkArgs(type);
        try {
            Map<String, Object> mapping = getMapping(type);
            return MappedObjectDecoder.byString(mapping);
        } catch (NoSuchMethodException | SecurityException |
                 IllegalAccessException | IllegalArgumentException |
                 InvocationTargetException | NoSuchFieldException ex) {
            throw new BadRuntimeTypeException(ex);
        }
    }

    /**
     * Created an enumerated type consisting of a set of distinct
     * symbolic constants.
     * 
     * @param constants the set of names of constants defined in this
     * enumerated type
     * 
     * @throws NullPointerException if the argument is {@code null}
     * 
     * @throws IllegalArgumentException if any constant has a non-leaf
     * name
     */
    public EnumeratedType(Collection<? extends ExternalName> constants) {
        Objects.requireNonNull(constants, "constants");
        Optional<? extends ExternalName> bad =
            constants.stream().filter(c -> !c.isLeaf()).findAny();
        if (bad.isPresent())
            throw new IllegalArgumentException("bad constant " + bad.get());
        this.constants = InterfaceType.orderedCopyOf(constants);
    }

    /**
     * Get a string representation of this type. This is each constant
     * in sorted order, space-separated, and surrounded by angle
     * brackets.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return "<" + constants.stream().map(Object::toString).sorted()
            .collect(Collectors.joining(" ")) + ">";
    }

    /**
     * Get this object's hash code.
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        assert constants != null;
        result = prime * result + constants.hashCode();
        return result;
    }

    /**
     * Test whether another object is identical to this enumerated type.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is an enumerated with
     * exactly the same set of constants; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof EnumeratedType)) return false;
        EnumeratedType other = (EnumeratedType) obj;
        assert constants != null;
        assert other.constants != null;
        return constants.equals(other.constants);
    }

    static final String KEY = "enum";

    static final String SIZE_FIELD = "size";

    static final String ITEM_FIELD = "item";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}. It then stores the name of each constant under the
     * field {@value #ITEM_FIELD} affixed with a dot and the constant's
     * position. It also stores the number of constants under
     * {@value #SIZE_FIELD}.
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        into.setProperty(prefix + SIZE_FIELD,
                         Integer.toString(constants.size()));
        int i = 0;
        for (ExternalName en : constants)
            into.setProperty(prefix + ITEM_FIELD + "." + i++, en.toString());
    }

    static EnumeratedType load(Properties props, String prefix) {
        assert KEY.equals(props.getProperty(prefix + "type"));
        final int sz = Integer.parseInt(props.getProperty(prefix + SIZE_FIELD));
        Collection<ExternalName> constants = new LinkedHashSet<>(sz);
        for (int i = 0; i < sz; i++) {
            String key = prefix + ITEM_FIELD + "." + i;
            ExternalName en = ExternalName.parse(props.getProperty(key));
            constants.add(en);
        }
        return new EnumeratedType(constants);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method returns this object unchanged.
     */
    @Override
    public EnumeratedType qualify(ExternalName name,
                                  QualificationContext ctxt) {
        return this;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method always returns {@code true}.
     */
    @Override
    public boolean mustBeDefinedInJava() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation writes a Java {@code enum}
     * definition.
     */
    @Override
    public void defineJavaType(TextFile out, ExternalName name,
                               ExpansionContext ctxt) {
        ExternalName moduleName = name.getParent();
        String pkgName = ctxt.getTarget(moduleName);
        out.format("package %s;%n", pkgName);

        /* Insert class-level documentation if provided. */
        QualifiedDocumentation docs = ctxt.getDocs(this);
        if (docs != null) try (Tab tab1 = out.documentation()) {
            DocContext docCtxt = new DocContext() {
                @Override
                public DocRef resolveReference(DocRef shortRef) {
                    ExternalName typeName = shortRef.typeName;
                    if (typeName == null) {
                        typeName = name;
                    } else if (typeName.isLeaf()) {
                        typeName = docs.qualifier.qualify(typeName);
                    }
                    return shortRef.qualify(typeName);
                }

                @Override
                public String includeTag(String tagName) {
                    switch (tagName) {
                    default:
                        return null;

                    case "see":
                    case "author":
                    case "since":
                        return tagName;
                    }
                }
            };
            DocRenderer rnd = new DocRenderer(out, docCtxt, ctxt);
            docs.comment.visit(rnd);
        }

        out.format("@%s(\"%s\")%n", TypeModel.class.getCanonicalName(), name);
        out.format("public enum %s {%n", name.getLeaf().asJavaClassName());
        try (Tab tab1 = out.tab("  ")) {
            for (ExternalName constant : constants) {
                /* Insert documentation comment for each constant if
                 * provided. */
                Object dkey = this.getKey(constant);
                QualifiedDocumentation cdocs = ctxt.getDocs(dkey);
                if (cdocs != null) try (Tab tab2 = out.documentation()) {
                    DocContext docCtxt = new DocContext() {
                        @Override
                        public DocRef resolveReference(DocRef shortRef) {
                            ExternalName typeName = shortRef.typeName;
                            if (typeName == null) {
                                typeName = name;
                            } else if (typeName.isLeaf()) {
                                typeName = cdocs.qualifier.qualify(typeName);
                            }
                            return shortRef.qualify(typeName);
                        }

                        @Override
                        public String includeTag(String tagName) {
                            switch (tagName) {
                            default:
                                return null;

                            case "see":
                            case "author":
                            case "since":
                                return tagName;
                            }
                        }
                    };
                    DocRenderer rnd = new DocRenderer(out, docCtxt, ctxt);
                    cdocs.comment.visit(rnd);
                }

                out.format("@%s(\"%s\")%n", Constant.class.getCanonicalName(),
                           constant);
                out.format("%s,%n", constant.asJavaConstantName());
            }
        }
        out.format("}%n");
    }

    private final Map<ExternalName, Object> keys = new ConcurrentHashMap<>();

    /**
     * Get an object uniquely identifying one of this type's constants.
     * This key is intended for the likes of {@link IdentityHashMap} and
     * {@link WeakHashMap}, which use object identity to index entries.
     * Calling this method with the same argument will yield the same
     * result.
     * 
     * @param name the name of the constant
     * 
     * @return an object unique to the constant
     */
    public Object getKey(ExternalName name) {
        if (!constants.contains(name))
            throw new IllegalArgumentException(name.toString() + " not part of "
                + constants);
        return keys.computeIfAbsent(name, n -> new Object() {
            @Override
            public String toString() {
                return n.toString();
            }
        });
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation concatenates the package name with
     * the Java class name equivalent of the leaf of the type name. If a
     * member name is specified, it is converted to a Java constant
     * name, and appended with a hash.
     */
    @Override
    public String linkJavaDoc(DocRef ref, String pkgName,
                              ExpansionContext ctxt) {
        String base = pkgName + "." + ref.typeName.getLeaf().asJavaClassName();
        if (ref.memberName != null)
            base += "#" + ref.memberName.asJavaConstantName();
        return base;
    }
}
