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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import uk.ac.lancs.carp.codec.CodecException;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Direction;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.map.Builder;
import uk.ac.lancs.carp.map.Completable;
import uk.ac.lancs.carp.map.Completer;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.Getter;
import uk.ac.lancs.carp.map.Setter;
import uk.ac.lancs.carp.map.StaticCompletable;
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
import uk.ac.lancs.carp.syntax.doc.Content;

/**
 * Models a structure type, or parameter list. A structure type consists
 * of a set of named fields, each of which may be optional, and each of
 * which has a type. Ordering is irrelevant.
 *
 * @author simpsons
 */
public final class StructureType implements Type {
    /**
     * Defines the members of the structure type, including name, type,
     * and whether required or optional.
     */
    public final Map<ExternalName, Member> members;

    @Override
    public void gatherReferences(ExternalName referrent,
                                 BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        for (Member memb : members.values())
            memb.type.gatherReferences(referrent, dest);
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation uses the {@code type} argument to
     * seek out methods annotated with {@link Getter}, and matches them
     * against its members. The types of these members are used to
     * generate their respective encoders. The returned encoder invokes
     * the getters on its argument to get the value of each field,
     * converts to JSON using the corresponding getter, and adds the
     * converted value to a JSON structure, under the corresponding
     * member name.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        class Row {
            Method getter;

            Encoder codec;

            boolean required;
        }
        final Map<ExternalName, Row> rows = new HashMap<>();

        /**
         * Find out how to read fields from the structure.
         */
        for (Method m : type.getDeclaredMethods()) {
            var ann = m.getAnnotation(Getter.class);
            if (ann != null) {
                ExternalName n = ExternalName.parse(ann.value());
                m.setAccessible(true);
                rows.computeIfAbsent(n, k -> new Row()).getter = m;
            }
        }

        /* Work out how to encode and decode each field, and which are
         * required. */
        for (var entry : members.entrySet()) {
            Encoder c = entry.getValue().type.getEncoder(null, ctxt);
            ExternalName key = entry.getKey();
            rows.computeIfAbsent(key, k -> new Row()).codec = c;
            if (entry.getValue().required)
                rows.computeIfAbsent(key, k -> new Row()).required = true;
        }

        return (value, dctxt) -> {
            JsonObjectBuilder builder = Json.createObjectBuilder();
            for (var entry : rows.entrySet()) {
                ExternalName key = entry.getKey();
                try {
                    Row row = entry.getValue();
                    Object v = row.getter.invoke(value);
                    JsonValue jv = row.codec.encodeJson(v, dctxt);
                    builder.add(key.toString(), jv);
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException ex) {
                    throw new CodecException(Direction.ENCODING,
                                             "reflecting key " + key, ex);
                }
            }
            return builder.build();
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @default This method uses the {@code type} argument to seek out
     * the nested builder class, annotated with {@link Builder}. It also
     * seeks out methods on the builder and the main class annotated
     * with {@link Setter} corresponding to each of its members, a
     * single method on each of the builder and main class annotated
     * with {@link Completer}. The types of the members are also used to
     * create decoders for each of them.
     * 
     * <p>
     * The returned decoder interprets its argument as a JSON structure,
     * and scans for fields with names matching its members' names. It
     * uses the corresponding member decoder to convert to Java, and the
     * corresponding setter to record that member's value. Finally, it
     * calls a completer to create the structure instance.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        class Row {
            Method setter, initialSetter;

            Decoder codec;

            boolean required;

            /**
             * Set this field.
             * 
             * @param builder the current builder object, or
             * {@code null} if it has not yet been created
             * 
             * @param value the value to set this field to
             * 
             * @return a builder for subsequent settings of other fields
             */
            Object set(Object builder, Object value)
                throws IllegalAccessException,
                    IllegalArgumentException,
                    InvocationTargetException {
                if (builder == null)
                    return initialSetter.invoke(null, value);
                else
                    return setter.invoke(builder, value);
            }
        }
        final Map<ExternalName, Row> rows = new HashMap<>();
        final Class<?> builderType = Arrays.asList(type.getDeclaredClasses())
            .stream().filter(c -> c.getAnnotation(Builder.class) != null)
            .findAny().get();
        final Method end = Arrays.asList(builderType.getDeclaredMethods())
            .stream().filter(m -> m.getAnnotation(Completer.class) != null)
            .findAny().get();
        final Method initialEnd = Arrays.asList(type.getDeclaredMethods())
            .stream().filter(c -> c.getParameterCount() == 0)
            .filter(c -> c.getAnnotation(Completer.class) != null).findAny()
            .get();

        /**
         * Find out how to set initial fields from the structure.
         */
        for (Method m : type.getDeclaredMethods()) {
            var ann = m.getAnnotation(Setter.class);
            if (ann != null) {
                ExternalName n = ExternalName.parse(ann.value());
                m.setAccessible(true);
                rows.computeIfAbsent(n, k -> new Row()).initialSetter = m;
            }
        }

        /**
         * Find out how to build a structure.
         */
        for (Method m : builderType.getDeclaredMethods()) {
            var ann = m.getAnnotation(Setter.class);
            if (ann == null) continue;
            ExternalName n = ExternalName.parse(ann.value());
            m.setAccessible(true);
            rows.computeIfAbsent(n, k -> new Row()).setter = m;
        }

        /* Work out how to decode each field, and which are required. */
        for (var entry : members.entrySet()) {
            Decoder c = entry.getValue().type.getDecoder(null, ctxt);
            ExternalName key = entry.getKey();
            rows.computeIfAbsent(key, k -> new Row()).codec = c;
            if (entry.getValue().required)
                rows.computeIfAbsent(key, k -> new Row()).required = true;
        }

        return (value, dctxt) -> {
            assert value != null;
            JsonObject typed = (JsonObject) value;
            Object builder = null;
            for (var entry : rows.entrySet()) {
                ExternalName key = entry.getKey();
                try {
                    Row row = entry.getValue();
                    JsonValue jv = typed.getOrDefault(key.toString(), null);
                    if (jv == null) continue;
                    Object v = row.codec.decodeJson(jv, dctxt);
                    builder = row.set(builder, v);
                } catch (IllegalAccessException | IllegalArgumentException |
                         InvocationTargetException ex) {
                    throw new CodecException(Direction.DECODING,
                                             "reflecting key " + key, ex);
                }
            }

            try {
                return builder == null ? initialEnd.invoke(null) :
                    end.invoke(builder);
            } catch (IllegalAccessException | IllegalArgumentException |
                     InvocationTargetException ex) {
                throw new CodecException(Direction.DECODING,
                                         "reflecting completion", ex);
            }
        };
    }

    /**
     * Model a structure type from explicit members.
     *
     * @param members the mapping from name to member
     * 
     * @throws NullPointerException if the argument is {@code null}
     */
    public StructureType(Map<? extends ExternalName,
                             ? extends Member> members) {
        Objects.requireNonNull(members, "members");
        this.members = InterfaceType.orderedCopyOf(members);
    }

    /**
     * Determine whether the type has any members.
     *
     * @return {@code false} if the type has at least one member;
     * {@code true} otherwise
     */
    public boolean isEmpty() {
        return members.isEmpty();
    }

    /**
     * Get a string representation of this structure type. This is
     * formed by listing each member name, an optional question mark, a
     * colon, and a string representation of the member's type. Each
     * item is separated by a semicolon, and braces surround the result.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return "{ "
            + members.entrySet().stream()
                .sorted((a, b) -> a.getKey().toString()
                    .compareTo(b.getKey().toString()))
                .map(e -> e.getKey().toString()
                    + (e.getValue().required ? "" : "?") + " : "
                    + e.getValue().type)
                .collect(Collectors.joining("; "))
            + " }";
    }

    /**
     * Get a hash code for this object.
     *
     * @return the hash code for this object, an aggregate of the hash
     * codes of its members
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        assert members != null;
        result = prime * result + members.hashCode();
        return result;
    }

    /**
     * Test whether this structure type is identical to another object.
     *
     * @param obj the other object
     *
     * @return {@code true} if the other object models a structure type
     * with exactly the same set of members and optional flags as this
     * structure type
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof StructureType)) return false;
        StructureType other = (StructureType) obj;
        assert members != null;
        assert other.members != null;
        return members.equals(other.members);
    }

    static final String KEY = "struct";

    static final String ELEM_FIELD = "elem";

    static final String COUNT_FIELD = ELEM_FIELD + ".count";

    static final String OPT_FIELD = "optional";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation sets the <samp>type</samp> field to
     * {@value #KEY}, and the field {@value #COUNT_FIELD} to the number
     * of members. Each member is assigned a number from zero, is stored
     * by invoking
     * {@link Type#declareJava(boolean, boolean, uk.ac.lancs.carp.model.ExpansionContext)}
     * on its type, extending the prefix with {@value #ELEM_FIELD}, a
     * dot, the assigned number, and another dot. Its name is stored
     * under {@value #ELEM_FIELD}, a dot, the assigned number, and
     * another dot and <samp>name</samp>. The sub-field
     * {@value #OPT_FIELD} is set if the member is optional.
     */
    @Override
    public void describe(String prefix, Properties into) {
        into.setProperty(prefix + "type", KEY);
        into.setProperty(prefix + COUNT_FIELD,
                         Integer.toString(members.size()));
        int idx = 0;
        for (Map.Entry<ExternalName, Member> entry : members.entrySet()) {
            final Member value = entry.getValue();
            String subpfx = prefix + ELEM_FIELD + "." + idx + ".";
            into.setProperty(subpfx + "name", entry.getKey().toString());
            value.type.describe(subpfx, into);
            if (value.required)
                into.remove(subpfx + OPT_FIELD);
            else
                into.setProperty(subpfx + OPT_FIELD, "yes");
            idx++;
        }
    }

    static StructureType load(Properties from, String prefix,
                              LoadContext ctxt) {
        assert KEY.equals(from.getProperty(prefix + "type"));
        final int count =
            Integer.parseInt(from.getProperty(prefix + COUNT_FIELD));
        Map<ExternalName, Member> members = new LinkedHashMap<>(count);
        for (int idx = 0; idx < count; idx++) {
            final String subpfx = prefix + ELEM_FIELD + "." + idx + ".";
            ExternalName key =
                ExternalName.parse(from.getProperty(subpfx + "name"));
            Type type = Type.load(subpfx, from, ctxt);
            Member value = from.getProperty(subpfx + OPT_FIELD) == null ?
                Member.required(type) : Member.optional(type);
            members.put(key, value);
        }
        return new StructureType(members);
    }

    /**
     * {@inheritDoc}
     * 
     * @default An alternative mapping from name to member is
     * constructed. This method applies itself to each member's type
     * recursively, and uses it to build an otherwise identical member.
     * If any resolved member is distinct from its original, a new
     * structure type is returned using the alternative mapping.
     * Otherwise, this object is returned unchanged.
     */
    @Override
    public StructureType qualify(ExternalName name, QualificationContext ctxt) {
        Map<ExternalName, Member> altMembers = new LinkedHashMap<>();
        boolean changed = false;
        for (var entry : members.entrySet()) {
            Member memb = entry.getValue();
            Member altMemb = memb.resolve(ctxt);
            altMembers.put(entry.getKey(), altMemb);
            if (!altMemb.equals(memb)) changed = true;
        }
        if (changed) {
            StructureType result = new StructureType(altMembers);
            ctxt.copyAssociations(result, this);
            return result;
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
     * @default This implementation writes a Java class declaration,
     * including a method to read each field. It also defines a nested
     * builder class, setter methods and a completer on it, and setter
     * methods and a completer on the main class to be used in the first
     * instance.
     */
    @Override
    public void defineJavaType(TextFile out, ExternalName name,
                               ExpansionContext ctxt) {
        ExternalName moduleName = name.getParent();
        String pkgName = ctxt.getTarget(moduleName);
        out.format("package %s;%n", pkgName);
        /* Insert documentation comment. */
        QualifiedDocumentation docs = ctxt.getDocs(this);
        Map<ExternalName, List<Content>> paramContent = new HashMap<>();
        DocRenderer rnd = null;
        if (docs != null) {
            /* Extract the @param tags, and index them in
             * paramContent. */
            List<List<Content>> paramTags = docs.comment.blockTags.get("param");
            if (paramTags != null) for (var tag : paramTags) {
                /* Identify the parameter. */
                List<Content> rem = new ArrayList<>();
                String paramName = DocRenderer.firstWord(rem, tag);
                if (paramName == null) continue;
                ExternalName extName = ExternalName.parse(paramName);
                if (paramContent.containsKey(extName)) {
                    /* TODO: Maybe include a weak diagnostic here?
                     * Regardless, the first definition takes
                     * precedence. */
                    continue;
                }
                paramContent.put(extName, rem);
            }

            try (Tab tab1 = out.documentation()) {
                /* Copy the documentation body, and @see, @author
                 * and @since tags. */
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
                rnd = new DocRenderer(out, docCtxt, ctxt);
                docs.comment.visit(rnd);
            }
        }
        out.format("@%s(\"%s\")%n", TypeModel.class.getCanonicalName(), name);
        out.format("public final class %s%n", name.getLeaf().asJavaClassName());
        out.format("    implements %s<%s> {%n",
                   StaticCompletable.class.getCanonicalName(),
                   name.getLeaf().asJavaClassName());
        try (Tab tab1 = out.tab("  ")) {
            try (Tab tab2 = out.documentation()) {
                out.format("Get the hash code for this object.%n");
                out.format("@return the hash code for this object%n");
            }
            out.format("@java.lang.Override%n");
            out.format("public int hashCode() {%n");
            try (Tab tab2 = out.tab("  ")) {
                out.format("int hash = 3;%n");
                for (Map.Entry<ExternalName, Member> entry : members
                    .entrySet()) {
                    ExternalName membName = entry.getKey();
                    Member memb = entry.getValue();
                    out.format("hash = 29 * hash + %s;%n",
                               memb.type
                                   .getJavaHashExpression(memb.required, "this."
                                       + membName.asJavaMethodName(), ctxt));
                }
                out.format("return hash;%n");
            }
            out.format("}%n%n");

            try (Tab tab2 = out.documentation()) {
                out.format("Test whether another object is equal to this one.%n");
                out.format("@param obj the object to test%n");
                out.format("@return {@code true} if the other object is"
                    + " of type {@link %s} and has equal fields;"
                    + " {@code false} otherwise%n", name.asJavaClassName());
            }
            out.format("@java.lang.Override%n");
            out.format("public boolean equals(Object obj) {%n");
            try (Tab tab2 = out.tab("  ")) {
                out.format("if (this == obj) return true;%n");
                out.format("if (null == obj) return false;%n");
                out.format("if (getClass() != obj.getClass()) return false;%n");
                out.format("final %s other = (%s) obj;%n",
                           name.getLeaf().asJavaClassName(),
                           name.getLeaf().asJavaClassName());
                for (Map.Entry<ExternalName, Member> entry : members
                    .entrySet()) {
                    ExternalName membName = entry.getKey();
                    Member memb = entry.getValue();
                    out.format("if (%s) return false;%n", memb.type
                        .getJavaInequalityExpression(memb.required,
                                                     "this." + membName
                                                         .asJavaMethodName(),
                                                     "other." + membName
                                                         .asJavaMethodName(),
                                                     ctxt));
                }
                out.format("return true;%n");
            }
            out.format("}%n%n");
            for (Map.Entry<ExternalName, Member> entry : members.entrySet()) {
                ExternalName membName = entry.getKey();
                Member memb = entry.getValue();
                out.format("private final %s %s;%n%n",
                           memb.type.declareJava(memb.required, false, ctxt),
                           membName.asJavaMethodName());

                List<Content> descr = paramContent.get(membName);

                /* Insert documentation comment if provided. */
                if (descr != null) try (Tab tab3 = out.documentation()) {
                    out.format("Get the value of the field which holds ");
                    assert rnd != null;
                    List<Content> trimmed = Content.trimWhiteSpace(descr);
                    rnd.visit(trimmed);
                    out.format(".%n");
                    out.format("@return ");
                    rnd.visit(trimmed);
                    out.format("%n");
                }
                out.format("@%s(\"%s\")%n", Getter.class.getCanonicalName(),
                           membName);
                out.format("public %s %s() { return this.%s; }%n%n",
                           memb.type.declareJava(memb.required, false, ctxt),
                           membName.asJavaMethodName(),
                           membName.asJavaMethodName());

                /* Insert documentation comment if provided. */
                if (descr != null) try (Tab tab3 = out.documentation()) {
                    out.format("Start building an object of type {@link %s} "
                        + "by setting the field which holds ",
                               name.getLeaf().asJavaClassName());
                    assert rnd != null;
                    List<Content> trimmed = Content.trimWhiteSpace(descr);
                    rnd.visit(trimmed);
                    out.format(".%n");
                    out.format("@param %s ", membName.asJavaMethodName());
                    rnd.visit(trimmed);
                    out.format("%n");
                    out.format("@return a new builder "
                        + "with just one field set%n");
                }
                out.format("@%s(\"%s\")%n", Setter.class.getCanonicalName(),
                           membName);
                out.format("public static Builder %s(%s %s) {%n",
                           membName.asJavaMethodName(),
                           memb.type.declareJava(memb.required, false, ctxt),
                           membName.asJavaMethodName());
                try (Tab tab2 = out.tab("  ")) {
                    out.format("return new Builder().%s(%s);%n",
                               membName.asJavaMethodName(),
                               membName.asJavaMethodName());
                }
                out.format("}%n%n");
            }
            {
                final String header = String
                    .format("private %s(", name.getLeaf().asJavaClassName());
                final String tab = ",\n" + header.codePoints().map(i -> ' ')
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                             StringBuilder::append);
                out.format("%s", header);
                String sep = "";
                for (Map.Entry<ExternalName, Member> entry : members
                    .entrySet()) {
                    ExternalName membName = entry.getKey();
                    Member memb = entry.getValue();
                    out.format("%s", sep);
                    if (false) out.format("%s@%s(\"%s\") ",
                                          TypeModel.class.getCanonicalName(),
                                          membName);
                    out.format("%s %s",
                               memb.type.declareJava(memb.required, false,
                                                     ctxt),
                               membName.asJavaMethodName());
                    sep = tab;
                }
                out.format(") {%n");
            }
            try (Tab tab2 = out.tab("  ")) {
                for (ExternalName membName : members.keySet()) {
                    out.format("this.%s = %s;%n", membName.asJavaMethodName(),
                               membName.asJavaMethodName());
                }
            }
            out.format("}%n%n");

            /* Insert documentation comment. */
            try (Tab tab2 = out.documentation()) {
                out.format("Creates objects of type {@link %s}.%n",
                           name.getLeaf().asJavaClassName());
            }
            out.format("@%s%n", Builder.class.getCanonicalName());
            out.format("public static class Builder%n");
            out.format("    implements %s<%s> {%n",
                       Completable.class.getCanonicalName(),
                       name.getLeaf().asJavaClassName());
            try (Tab tab2 = out.tab("  ")) {
                out.format("private Builder() { }%n%n");
                for (Map.Entry<ExternalName, Member> entry : members
                    .entrySet()) {
                    ExternalName membName = entry.getKey();
                    Member memb = entry.getValue();
                    out.format("private %s %s;%n%n",
                               memb.type.declareJava(false, false, ctxt),
                               membName.asJavaMethodName());
                    /* Insert documentation comment if provided. */
                    List<Content> descr = paramContent.get(membName);
                    if (descr != null) try (Tab tab3 = out.documentation()) {
                        out.format("Set the field which holds ");
                        assert rnd != null;
                        List<Content> trimmed = Content.trimWhiteSpace(descr);
                        rnd.visit(trimmed);
                        out.format(".%n");
                        out.format("@param %s ", membName.asJavaMethodName());
                        rnd.visit(trimmed);
                        out.format("%n");
                        out.format("@return this object%n");
                    }
                    out.format("@%s(\"%s\")%n", Setter.class.getCanonicalName(),
                               membName);
                    out.format("public Builder %s(%s %s) {%n",
                               membName.asJavaMethodName(),
                               memb.type.declareJava(memb.required, false,
                                                     ctxt),
                               membName.asJavaMethodName());
                    try (Tab tab3 = out.tab("  ")) {
                        out.format("this.%s = %s;%n",
                                   membName.asJavaMethodName(),
                                   membName.asJavaMethodName());
                        out.format("return this;%n");
                    }
                    out.format("}%n%n");
                }

                try (Tab tab3 = out.documentation()) {
                    out.format("Create an object of type {@link %s}"
                        + " with previously provided parameters.%n",
                               name.getLeaf().asJavaClassName());
                    out.format("@return the requested object%n");
                    out.format("@undocumented%n");
                    out.format("@deprecated Use {@link #%s()} instead.%n",
                               Completable.METHOD_NAME);
                }
                out.format("@%s%n", Deprecated.class.getCanonicalName());
                out.format("public %s _done() {%n",
                           name.getLeaf().asJavaClassName());
                try (Tab tab3 = out.tab("  ")) {
                    out.format("return %s();%n", Completable.METHOD_NAME);
                }
                out.format("}%n%n");

                try (Tab tab3 = out.documentation()) {
                    out.format("Create an object of type {@link %s}"
                        + " with previously provided parameters.%n",
                               name.getLeaf().asJavaClassName());
                    out.format("@return the requested object%n");
                }
                out.format("@%s%n", Completer.class.getCanonicalName());
                out.format("public %s %s() {%n",
                           name.getLeaf().asJavaClassName(),
                           Completable.METHOD_NAME);
                try (Tab tab3 = out.tab("  ")) {
                    out.format("return new %s(%s);%n",
                               name.getLeaf().asJavaClassName(),
                               members.keySet().stream()
                                   .map(ExternalName::asJavaMethodName)
                                   .collect(Collectors.joining(", ")));
                }
                out.format("}%n");
            }
            out.format("}%n%n");

            try (Tab tab2 = out.documentation()) {
                out.format("Create an empty object of type {@link %s}.%n",
                           name.getLeaf().asJavaClassName());
                out.format("@return the requested object%n");
                out.format("@constructor%n");
                out.format("@undocumented%n");
                out.format("@deprecated Use {@link #%s()} instead.%n",
                           Completable.METHOD_NAME);
            }
            out.format("@%s%n", Deprecated.class.getCanonicalName());
            out.format("public static %s _done() {%n",
                       name.getLeaf().asJavaClassName());
            try (Tab tab2 = out.tab("  ")) {
                out.format("return %s();%n", StaticCompletable.METHOD_NAME);
            }
            out.format("}%n%n");

            try (Tab tab2 = out.documentation()) {
                out.format("Create an empty object of type {@link %s}.%n",
                           name.getLeaf().asJavaClassName());
                out.format("@return the requested object%n");
                out.format("@constructor%n");
            }
            out.format("@%s%n", Completer.class.getCanonicalName());
            out.format("public static %s %s() {%n",
                       name.getLeaf().asJavaClassName(),
                       StaticCompletable.METHOD_NAME);
            try (Tab tab2 = out.tab("  ")) {
                out.format("return new Builder().%s();%n",
                           Completable.METHOD_NAME);
            }
            out.format("}%n");
        }
        out.format("}%n");
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation concatenates the package name with
     * the Java class name equivalent of the leaf of the type name. If a
     * member name is specified, it is converted to a Java method/field
     * name, and appended with a hash.
     */
    @Override
    public String linkJavaDoc(DocRef ref, String pkgName,
                              ExpansionContext ctxt) {
        String base = pkgName + "." + ref.typeName.getLeaf().asJavaClassName();
        if (ref.memberName != null)
            base += "#" + ref.memberName.asJavaMethodName();
        return base;
    }
}
