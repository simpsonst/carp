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

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.json.JsonString;
import uk.ac.lancs.carp.codec.Codecs;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.TypeModel;
import uk.ac.lancs.carp.model.DocContext;
import uk.ac.lancs.carp.model.DocRef;
import uk.ac.lancs.carp.model.DocRenderer;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.Tab;
import uk.ac.lancs.carp.model.TextFile;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.UndefinableTypeException;

/**
 * Models an interface type.
 * 
 * @author simpsons
 */
public class InterfaceType implements Type {
    /**
     * Specifies the interface types that this type inherits calls from.
     */
    public final Collection<Type> ancestors;

    /**
     * Specifies the calls that can be made on this interface.
     */
    public final Map<ExternalName, CallSpecification> calls;

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns an encoder which calls
     * {@link EncodingContext#establishCallback(java.lang.Class, java.lang.Object)}
     * to encode the interface reference as a URI.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        Objects.requireNonNull(type, "type");
        return (value, dctxt) -> {
            URI location = dctxt.establishCallback(type, value);
            String loc = location.toASCIIString();
            return Codecs.asJson(loc);
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation returns a decoder which interprets
     * its argument as a URI, and passes it to
     * {@link DecodingContext#seek(java.lang.Class, java.net.URI)}.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        Objects.requireNonNull(type, "type");
        return (value, dctxt) -> {
            JsonString typed = (JsonString) value;
            URI location = URI.create(typed.getString());
            return dctxt.seek(type, location);
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation simply calls
     * {@link CallSpecification#gatherReferences(uk.ac.lancs.carp.map.ExternalName, java.util.function.BiConsumer)}
     * on each of its calls.
     */
    @Override
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        for (CallSpecification call : calls.values())
            call.gatherReferences(referrent, dest);
    }

    /**
     * Model an interface type.
     * 
     * @param calls an index of calls that can be made on this
     * interface; a copy is made
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public InterfaceType(Map<? extends ExternalName,
                             ? extends CallSpecification> calls) {
        this(calls, null);
    }

    /**
     * Create an unmodifiable copy of a map, preserving iteration order.
     * This is to be used instead of {@link Map#copyOf(Map)}, which does
     * not guarantee to preserve iteration order.
     * 
     * @param <K> the key type
     * 
     * @param <V> the value type
     * 
     * @param input the map to be copied
     * 
     * @return a map with the same contents as the input at the time of
     * copying, including iteration order
     */
    static <K, V> Map<K, V> orderedCopyOf(Map<? extends K, ? extends V> input) {
        return Collections.unmodifiableMap(new LinkedHashMap<>(input));
    }

    /**
     * Create an unmodifiable copy of a collection, preserving iteration
     * order. This is to be used instead of
     * {@link java.util.Set#copyOf(Collection)}, which does not
     * guarantee to preserve iteration order.
     * 
     * @param <E> the element type
     * 
     * @param input the collection to be copied
     * 
     * @return a collection with the same contents as the input,
     * including iteration order
     */
    static <E> Collection<E> orderedCopyOf(Collection<? extends E> input) {
        return Collections.unmodifiableCollection(new LinkedHashSet<>(input));
    }

    /**
     * Model an interface type.
     * 
     * @param calls an index of calls that can be made on this
     * interface; a copy is made
     * 
     * @param ancestors the set of ancestor types; may be {@code null}
     * as a surrogate for an empty set
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public InterfaceType(Map<? extends ExternalName,
                             ? extends CallSpecification> calls,
                         Collection<? extends Type> ancestors) {
        Objects.requireNonNull(calls, "calls");
        this.calls = InterfaceType.orderedCopyOf(calls);
        this.ancestors = ancestors == null ? Collections.emptySet() :
            orderedCopyOf(ancestors);
    }

    /**
     * Get a string representation of this interface type. This is the
     * keyword <samp>interface</samp> followed by each call (see
     * {@link CallSpecification#toString()} in square brackets.
     * 
     * <p>
     * This format is identical to the IDL format that expresses this
     * type.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return "[ "
            + ancestors.stream().map(e -> "inherit " + e + "; ")
                .collect(Collectors.joining())
            + calls.entrySet().stream()
                .map(e -> "call " + e.getKey() + e.getValue() + ";")
                .collect(Collectors.joining(" "))
            + " ]";
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
        assert calls != null;
        result = prime * result + calls.hashCode();
        return result;
    }

    /**
     * Test whether this interface is identical to another object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is an
     * {@link InterfaceType} with the same calls and ancestors types;
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof InterfaceType)) return false;
        InterfaceType other = (InterfaceType) obj;
        assert calls != null;
        assert other.calls != null;
        if (!calls.equals(other.calls)) return false;
        if (!ancestors.equals(other.ancestors)) return false;
        return true;
    }

    static final String KEY = "iface";

    static final String CALL_FIELD = "call";

    static final String CALL_COUNT_FIELD = CALL_FIELD + ".count";

    static final String ANCESTOR_FIELD = "ancestor";

    static final String ANCESTOR_COUNT_FIELD = ANCESTOR_FIELD + ".count";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}. It sets {@value #CALL_COUNT_FIELD} to the number
     * of calls, and assigns each call a number from zero, invoking
     * {@link CallSpecification#describe(String, Properties)} on it,
     * extending the prefix with {@value #CALL_FIELD}, a dot, the
     * assigned number, and another dot. Similarly, it sets
     * {@value #ANCESTOR_COUNT_FIELD} to the number of inherited types,
     * and assigns each ancestor a number from zero, invoking
     * {@link Type#describe(String, Properties)} on it, extending the
     * prefix with {@value #ANCESTOR_FIELD}, a dot, the assigned number,
     * and another dot.
     */
    @Override
    public void describe(String prefix, Properties props) {
        props.setProperty(prefix + "type", KEY);

        /* Record details on each call. */
        {
            int code = 0;
            for (var entry : calls.entrySet()) {
                final String subpfx = prefix + CALL_FIELD + "." + code + ".";
                props.setProperty(subpfx + "name", entry.getKey().toString());
                entry.getValue().describe(subpfx, props);
                code++;
            }
            props.setProperty(prefix + CALL_COUNT_FIELD,
                              Integer.toString(code));
        }

        /* Record inherited types. */
        {
            int code = 0;
            for (Type entry : ancestors) {
                final String subpfx =
                    prefix + ANCESTOR_FIELD + "." + code + ".";
                entry.describe(subpfx, props);
                code++;
            }
            props.setProperty(prefix + ANCESTOR_COUNT_FIELD,
                              Integer.toString(code));
        }
    }

    static InterfaceType load(Properties props, String prefix,
                              LoadContext ctxt) {
        assert KEY.equals(props.getProperty(prefix + "type"));

        /* Load the descriptions of calls on the interface. */
        Map<ExternalName, CallSpecification> calls = new LinkedHashMap<>();
        for (int count = Integer
            .parseInt(props.getProperty(prefix + CALL_COUNT_FIELD)), code = 0;
             code < count; code++) {
            final String subpfx = prefix + CALL_FIELD + "." + code + ".";
            ExternalName name =
                ExternalName.parse(props.getProperty(subpfx + "name"));
            CallSpecification call =
                CallSpecification.load(props, subpfx, ctxt);
            calls.put(name, call);
        }

        /* Identify inherited types. We use zero as a default count of
         * inherited types, as the feature might not be present. */
        Collection<Type> ancestors = new LinkedHashSet<>();
        for (int count = Integer.parseInt(props
            .getProperty(prefix + ANCESTOR_COUNT_FIELD, "0")), code = 0;
             code < count; code++) {
            final String subpfx = prefix + ANCESTOR_FIELD + "." + code + ".";
            Type anc = Type.load(subpfx, props, ctxt);
            ancestors.add(anc);
        }

        return new InterfaceType(calls, ancestors);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation builds an alternative mapping from
     * call names to specifications. It calls
     * {@link CallSpecification#qualify(uk.ac.lancs.carp.model.QualificationContext)}
     * on each one, and places the result in the alternative mapping. If
     * the mapping differs, it yields a new interface type with the
     * alternative mapping. Otherwise, it returns this object unchanged.
     */
    @Override
    public InterfaceType qualify(ExternalName name, QualificationContext ctxt) {
        boolean changed = false;

        /* Qualify all the ancestors. */
        Collection<Type> altAncestors = new LinkedHashSet<>();
        for (Type entry : ancestors) {
            Type altEntry = entry.qualify(null, ctxt);
            altAncestors.add(altEntry);
            if (altEntry != entry) changed = true;
        }

        /* Qualify all the types in the calls. */
        Map<ExternalName, CallSpecification> altCalls = new LinkedHashMap<>();
        for (var entry : calls.entrySet()) {
            CallSpecification call = entry.getValue();
            CallSpecification altCall = call.qualify(ctxt);
            altCalls.put(entry.getKey(), altCall);
            if (!altCall.equals(call)) changed = true;
        }

        /* If anything changed, create a new type with the qualified
         * components. */
        if (changed) {
            InterfaceType alt = new InterfaceType(altCalls, altAncestors);
            ctxt.copyAssociations(alt, this);
            return alt;
        }
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
     * @default This implementation writes a Java interface type. Each
     * call gets its own method and nested return type. Inherited types
     * are listed in the {@code extends} clause, having been translated
     * to their Java names.
     */
    @Override
    public void defineJavaType(TextFile out, ExternalName name,
                               ExpansionContext ctxt) {
        /* We can't define this type if an ancestor type is also not
         * defined. */
        for (Type anc : ancestors)
            if (!(anc instanceof ReferenceType))
                throw new UndefinableTypeException("ancestor is not reference: "
                    + anc);

        ExternalName moduleName = name.getParent();
        String pkgName = ctxt.getTarget(moduleName);
        out.format("package %s;%n%n", pkgName);

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
        out.format("public interface %s", name.getLeaf().asJavaClassName());
        String sep = " extends";
        for (Type anc : ancestors) {
            out.format("%s %s", sep, anc.declareJava(false, false, ctxt));
            sep = ",";
        }
        out.format(" {%n");
        try (Tab tab1 = out.tab("  ")) {
            try (Tab tab2 = out.comment()) {
                out.format("%d calls", calls.size());
            }
            out.format("%n");

            for (Map.Entry<ExternalName, CallSpecification> entry : calls
                .entrySet()) {
                ExternalName callName = entry.getKey();
                CallSpecification call = entry.getValue();
                call.defineJavaType(out, name, callName, ctxt);
            }

            try (Tab tab2 = out.comment()) {
                out.format("Thanks.");
            }
            out.format("%n");
        }
        out.format("}%n");
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation concatenates the package name with
     * the Java class name equivalent of the leaf of the type name. If a
     * response name is specified, it appends a dot and the member name
     * as a Java class name, then a dot and the response name as a Java
     * class name. Otherwise, if a member name is specified, a hash is
     * appended, the member name as a Java method name, then brackets
     * surrounding comma-separated arguments generated using
     * {@link Type#declareJava(boolean, boolean, ExpansionContext)}.
     */
    @Override
    public String linkJavaDoc(DocRef ref, String pkgName,
                              ExpansionContext ctxt) {
        String base = pkgName + "." + ref.typeName.getLeaf().asJavaClassName();
        if (ref.responseName != null) {
            /* Add the call class as a nested type. */
            base += "." + ref.memberName.asJavaClassName();
            /* Add the response class as a nested type. */
            base += "." + ref.responseName.asJavaClassName();
        } else if (ref.memberName != null) {
            /* We're referring to the method. */
            base += "#" + ref.memberName.asJavaMethodName() + "(";
            String sep = "";
            CallSpecification cspec = calls.get(ref.memberName);
            for (var entry : cspec.parameters.members.values()) {
                base += sep;
                sep = ", ";

                Type type = entry.type;
                base += type.declareJava(entry.required, false, ctxt);
            }
            base += ")";
        }
        return base;
    }
}
