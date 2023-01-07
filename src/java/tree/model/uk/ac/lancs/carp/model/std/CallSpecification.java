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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import uk.ac.lancs.carp.map.Accessor;
import uk.ac.lancs.carp.map.Argument;
import uk.ac.lancs.carp.map.Builder;
import uk.ac.lancs.carp.map.CallModel;
import uk.ac.lancs.carp.map.Completable;
import uk.ac.lancs.carp.map.Completer;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.Getter;
import uk.ac.lancs.carp.map.ResponseModel;
import uk.ac.lancs.carp.map.Setter;
import uk.ac.lancs.carp.map.StaticCompletable;
import uk.ac.lancs.carp.map.Tester;
import uk.ac.lancs.carp.map.Union;
import uk.ac.lancs.carp.model.DocContext;
import uk.ac.lancs.carp.model.DocRef;
import uk.ac.lancs.carp.model.DocRenderer;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.Tab;
import uk.ac.lancs.carp.model.TextFile;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.UnresolvedTypeException;
import uk.ac.lancs.carp.syntax.doc.Content;

/**
 * Models a permitted call on an interface type.
 * 
 * @author simpsons
 */
public final class CallSpecification {
    /**
     * Specifies the parameters that can be passed in this call.
     */
    public final StructureType parameters;

    /**
     * Specifies the responses that can be made to this call.
     */
    public final Map<ExternalName, ResponseSpecification> responses;

    /**
     * Submit any type names referenced by this call. This method simply
     * passes the call on to
     * {@link Type#gatherReferences(ExternalName, BiConsumer)} applied
     * to its own {@link #parameters} and the values of
     * {@link #responses}.
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
        for (ResponseSpecification rsp : responses.values())
            rsp.gatherReferences(referrent, dest);
    }

    /**
     * Specify a call on an interface.
     * 
     * @param parameters the parameters that can be passed in this call
     * 
     * @param responses an index of responses that can be made to this
     * call; a copy is retained
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public CallSpecification(StructureType parameters,
                             Map<? extends ExternalName,
                                 ? extends ResponseSpecification> responses) {
        Objects.requireNonNull(parameters, "parameters");
        Objects.requireNonNull(responses, "responses");
        this.parameters = parameters;
        this.responses = InterfaceType.orderedCopyOf(responses);
    }

    /**
     * Get a string representation of this call. This consists of the
     * parameters in braces (see {@link StructureType#toString()} if not
     * empty, followed by each of the responses (see
     * {@link ResponseSpecification#toString()}) prefixed with
     * <samp>=&gt;</samp>.
     * 
     * @return the string representation
     */
    @Override
    public String toString() {
        return (parameters.isEmpty() ? "" : (" " + parameters.toString()))
            + responses.entrySet().stream()
                .map(e -> " => " + e.getKey() + e.getValue())
                .collect(Collectors.joining());
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
        assert parameters != null;
        assert responses != null;
        result = prime * result + parameters.hashCode();
        result = prime * result + responses.hashCode();
        return result;
    }

    /**
     * Test whether this another object is identical to this call.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a
     * {@link CallSpecification} with the same parameters and responses;
     * {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (!(obj instanceof CallSpecification)) return false;
        CallSpecification other = (CallSpecification) obj;
        assert parameters != null;
        assert responses != null;
        assert other.parameters != null;
        assert other.responses != null;
        return parameters.equals(other.parameters) &&
            responses.equals(other.responses);
    }

    /**
     * Resolve local names embedded in this call into fully qualified
     * names.
     * 
     * @param ctxt a context for mapping a local name to a fully
     * qualified one, and for reporting errors
     * 
     * @return a new call specification if any of the original's
     * components changed when resolved; this object otherwise
     * 
     * @throws UnresolvedTypeException if resolution yielded no type
     */
    public CallSpecification qualify(QualificationContext ctxt) {
        StructureType altParams = parameters.qualify(null, ctxt);
        Map<ExternalName, ResponseSpecification> altResponses =
            new LinkedHashMap<>();
        boolean changed = false;
        for (var entry : responses.entrySet()) {
            ResponseSpecification rsp = entry.getValue();
            ResponseSpecification altRsp = rsp.qualify(ctxt);
            altResponses.put(entry.getKey(), altRsp);
            if (!altRsp.equals(rsp)) changed = true;
        }
        if (changed || !altParams.equals(parameters)) {
            CallSpecification alt =
                new CallSpecification(altParams, altResponses);
            ctxt.copyAssociations(alt, this);
            return alt;
        }
        return this;
    }

    /**
     * Describe this call type as a set of Java properties.
     * 
     * @param prefix the prefix of property names to be inserted
     * 
     * @param props the destination for properties describing the
     * element
     */
    void describe(String prefix, Properties props) {
        parameters.describe(prefix + "params.", props);
        int code = 0;
        for (Map.Entry<ExternalName, ResponseSpecification> entry : responses
            .entrySet()) {
            final String subpfx = prefix + "rsp." + code + ".";
            props.setProperty(subpfx + "name", entry.getKey().toString());
            entry.getValue().describe(subpfx, props);
            code++;
        }
        props.setProperty(prefix + "rsp.count", Integer.toString(code));
    }

    static CallSpecification load(Properties from, String prefix,
                                  LoadContext ctxt) {
        StructureType params =
            StructureType.load(from, prefix + "params.", ctxt);
        Map<ExternalName, ResponseSpecification> responses =
            new LinkedHashMap<>();
        int count = Integer.parseInt(from.getProperty(prefix + "rsp.count"));
        for (int code = 0; code < count; code++) {
            final String subpfx = prefix + "rsp." + code + ".";
            ResponseSpecification spec =
                ResponseSpecification.load(from, subpfx, ctxt);
            ExternalName en =
                ExternalName.parse(from.getProperty(subpfx + "name"));
            responses.put(en, spec);
        }
        return new CallSpecification(params, responses);
    }

    private static String toTab(CharSequence in) {
        return in.codePoints().map(i -> ' ')
            .collect(StringBuilder::new, StringBuilder::appendCodePoint,
                     StringBuilder::append)
            .toString();
    }

    void defineJavaType(TextFile out, ExternalName typeName, ExternalName name,
                        ExpansionContext ctxt) {
        {
            /* Insert class-level documentation if provided. */
            QualifiedDocumentation docs = ctxt.getDocs(this);
            Map<ExternalName, List<Content>> paramContent = new HashMap<>();
            if (docs != null) {
                /* Extract the @param tags, and index them in
                 * paramContent. */
                List<List<Content>> paramTags =
                    docs.comment.blockTags.get("param");
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
                    DocContext docCtxt = new DocContext() {
                        @Override
                        public DocRef resolveReference(DocRef shortRef) {
                            ExternalName typeNamex = shortRef.typeName;
                            if (typeNamex == null) {
                                typeNamex = typeName;
                            } else if (typeNamex.isLeaf()) {
                                typeNamex = docs.qualifier.qualify(typeNamex);
                            }
                            return shortRef.qualify(typeNamex);
                        }

                        @Override
                        public String includeTag(String tagName) {
                            switch (tagName) {
                            default:
                                return null;

                            case "see":
                            case "author":
                            case "since":
                            case "return":
                                return tagName;
                            }
                        }
                    };
                    DocRenderer rnd = new DocRenderer(out, docCtxt, ctxt);
                    docs.comment.visit(rnd);

                    /* Write out @param tags. */
                    for (ExternalName parName : parameters.members.keySet()) {
                        List<Content> descr = paramContent.get(parName);
                        if (descr == null) continue;
                        List<Content> trimmed = Content.trimWhiteSpace(descr);
                        out.format("@param %s ", parName.asJavaMethodName());
                        rnd.visit(trimmed);
                        out.format("%n");
                    }
                    if (docs.comment.blockTags.get("return") == null) {
                        out.format("@return the response to the call%n");
                    }
                }
            }

            /* Write the method signature. */
            out.format("@%s(\"%s\")%n", CallModel.class.getCanonicalName(),
                       name);
            String tab = String.format("%s %s(",
                                       responses.isEmpty() ? "void" :
                                           name.asJavaClassName(),
                                       name.asJavaMethodName());
            out.format("%s", tab);
            tab = "\n" + toTab(tab);

            String sep = "";
            for (Map.Entry<ExternalName, Member> entry : parameters.members
                .entrySet()) {
                ExternalName parName = entry.getKey();
                Member memb = entry.getValue();

                out.format("%s@%s(\"%s\")%s%s %s", sep,
                           Argument.class.getCanonicalName(), parName, tab,
                           memb.type.declareJava(memb.required, false, ctxt),
                           parName.asJavaMethodName());
                sep = "," + tab;
            }
            out.format(");%n%n");
        }

        if (!responses.isEmpty()) {
            /* Write out definitions for the responses. */
            try (Tab tab1 = out.documentation()) {
                out.format("Defines responses for {@link %s#%s(",
                           typeName.asJavaClassName(), name.asJavaMethodName());
                String sep = "";
                for (var entry : parameters.members.entrySet()) {
                    var at = entry.getValue();
                    out.format("%s%s", sep,
                               at.type.declareJava(at.required, true, ctxt));
                    sep = ", ";
                }
                out.format(")}.%n");
            }
            out.format("@%s(\"%s\")%n", CallModel.class.getCanonicalName(),
                       name);
            out.format("abstract class %s%n", name.asJavaClassName());
            out.format("    implements %s<%s.Type> {%n",
                       Union.class.getCanonicalName(), name.asJavaClassName());
            try (Tab tab1 = out.tab("  ")) {
                try (Tab tab2 = out.documentation()) {
                    out.format("Identifies the type of response "
                        + "to {@link %s#%s(", typeName.asJavaClassName(),
                               name.asJavaMethodName());
                    String sep = "";
                    for (var entry : parameters.members.entrySet()) {
                        var at = entry.getValue();
                        out.format("%s%s", sep, at.type
                            .declareJava(at.required, true, ctxt));
                        sep = ", ";
                    }
                    out.format(")}.%n");
                }
                out.format("public enum Type {%n");
                for (ExternalName rspName : responses.keySet()) {
                    try (Tab tab2 = out.tab("  ")) {
                        try (Tab tab3 = out.documentation()) {
                            /* TODO: Look for a tag @id in the
                             * response's documentation, and use that
                             * instead of this default expression. */
                            out.format("Identifies a response of type "
                                + "<samp>%s</samp> to {@link %s#%s(", rspName,
                                       typeName.asJavaClassName(),
                                       name.asJavaMethodName());
                            String sep = "";
                            for (var entry : parameters.members.entrySet()) {
                                var at = entry.getValue();
                                out.format("%s%s", sep, at.type
                                    .declareJava(at.required, true, ctxt));
                                sep = ", ";
                            }
                            out.format(")}.%n");
                        }
                        out.format("%s,%n", rspName.asJavaConstantName());
                    }
                }
                out.format("}%n%n");

                try (Tab tab2 = out.documentation()) {
                    out.format("Get the type of the response.%n");
                    out.format("@return the type of the response%n");
                }
                out.format("public abstract Type %s();%n%n", Union.METHOD_NAME);

                out.format("private final java.lang.Object carp$ref;%n");
                out.format("private %s(java.lang.Object carp$ref) {%n",
                           name.asJavaClassName());
                out.format("  this.carp$ref = carp$ref;%n");
                out.format("}%n");

                for (var entry : responses.entrySet()) {
                    ExternalName rspName = entry.getKey();
                    ResponseSpecification rsp = entry.getValue();

                    out.format("%n");
                    try (Tab tab2 = out.documentation()) {
                        out.format("@undocumented%n");
                        out.format("@deprecated Use {@link #is$%s()} instead.%n",
                                   rspName.asJavaMethodName());
                    }
                    out.format("@%s%n", Deprecated.class.getCanonicalName());
                    out.format("public final boolean _%s() {%n",
                               rspName.prefix("is-").asJavaMethodName());
                    out.format("  return is$%s();%n",
                               rspName.asJavaMethodName());
                    out.format("}%n");

                    out.format("%n");
                    try (Tab tab2 = out.documentation()) {
                        out.format("Determine whether the response is "
                            + "of type <samp>%s</samp>.%n", rspName);
                        out.format("@return {@code true} if the response"
                            + " is of the expected type;"
                            + " {@code false} otherwise%n");
                    }
                    out.format("@%s(\"%s\")%n", Tester.class.getCanonicalName(),
                               rspName);
                    out.format("public boolean is$%s() {%n",
                               rspName.asJavaMethodName());
                    out.format("  return false;%n");
                    out.format("}%n");

                    out.format("%n");
                    try (Tab tab2 = out.documentation()) {
                        out.format("Access the fields of a response"
                            + " of type <samp>%s</samp>.%n", rspName);
                        out.format("@return the fields of the response%n");
                        out.format("@throws %s if the response is of "
                            + "the wrong type%n",
                                   ClassCastException.class.getCanonicalName());
                    }
                    out.format("@%s(\"%s\")%n",
                               Accessor.class.getCanonicalName(), rspName);
                    out.format("public %s %s() {%n", rspName.asJavaClassName(),
                               rspName.asJavaMethodName());
                    out.format("  return (%s) this.carp$ref;%n",
                               rspName.asJavaClassName());
                    out.format("}%n");
                    out.format("%n");

                    QualifiedDocumentation docs = ctxt.getDocs(rsp);
                    Map<ExternalName, List<Content>> paramContent =
                        new HashMap<>();
                    DocRenderer rnd = null;
                    if (docs != null) {
                        /* Extract the @param tags, and index them in
                         * paramContent. */
                        List<List<Content>> paramTags =
                            docs.comment.blockTags.get("param");
                        if (paramTags != null) for (var tag : paramTags) {
                            /* Identify the parameter. */
                            List<Content> rem = new ArrayList<>();
                            String paramName = DocRenderer.firstWord(rem, tag);
                            if (paramName == null) continue;
                            ExternalName extName =
                                ExternalName.parse(paramName);
                            if (paramContent.containsKey(extName)) {
                                /* TODO: Maybe include a weak diagnostic
                                 * here? Regardless, the first
                                 * definition takes precedence. */
                                continue;
                            }
                            paramContent.put(extName, rem);
                        }

                        try (Tab tab2 = out.documentation()) {
                            DocContext docCtxt = new DocContext() {
                                @Override
                                public DocRef
                                    resolveReference(DocRef shortRef) {
                                    ExternalName typeNamex = shortRef.typeName;
                                    if (typeNamex == null) {
                                        typeNamex = typeName;
                                    } else if (typeNamex.isLeaf()) {
                                        typeNamex =
                                            docs.qualifier.qualify(typeNamex);
                                    }
                                    return shortRef.qualify(typeNamex);
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

                    out.format("@%s(\"%s\")%n",
                               ResponseModel.class.getCanonicalName(), rspName);
                    out.format("public static final class %s%n",
                               rspName.asJavaClassName());
                    out.format("    implements %s<%s> {%n",
                               StaticCompletable.class.getCanonicalName(),
                               name.asJavaClassName());
                    try (Tab tab2 = out.tab("  ")) {
                        /* Define final fields to carry the response
                         * parameters. */
                        for (var parEntry : rsp.parameters.members.entrySet()) {
                            ExternalName parName = parEntry.getKey();
                            Member memb = parEntry.getValue();
                            out.format("private final %s %s;%n%n",
                                       memb.type.declareJava(memb.required,
                                                             false, ctxt),
                                       parName.asJavaMethodName());

                            /* Insert documentation comment if
                             * provided. */
                            List<Content> descr = paramContent.get(parName);
                            if (descr != null)
                                try (Tab tab3 = out.documentation()) {
                                    out.format("Get the value of the field "
                                        + "which holds ");
                                    assert rnd != null;
                                    List<Content> trimmed =
                                        Content.trimWhiteSpace(descr);
                                    rnd.visit(trimmed);
                                    out.format(".%n");
                                    out.format("@return ");
                                    rnd.visit(trimmed);
                                    out.format("%n");
                                }
                            out.format("@%s(\"%s\")%n",
                                       Getter.class.getCanonicalName(),
                                       parName);
                            out.format("public final %s %s() {%n",
                                       memb.type.declareJava(memb.required,
                                                             false, ctxt),
                                       parName.asJavaMethodName());
                            try (Tab tab3 = out.tab("  ")) {
                                out.format("return this.%s;%n",
                                           parName.asJavaMethodName());
                            }
                            out.format("}%n%n");
                        }

                        /* Define a constructor to be used by the
                         * builder class. */
                        // TODO: Insert documentation comment.
                        {
                            String tab =
                                String.format("private %s(",
                                              rspName.asJavaClassName());
                            out.format("%s", tab);
                            tab = "\n" + toTab(tab);
                            String sep = "";
                            for (var parEntry : rsp.parameters.members
                                .entrySet()) {
                                ExternalName parName = parEntry.getKey();
                                Member memb = parEntry.getValue();
                                out.format("%s", sep);
                                out.format("%s %s",
                                           memb.type.declareJava(memb.required,
                                                                 false, ctxt),
                                           parName.asJavaMethodName());
                                sep = "," + tab;
                            }
                        }
                        out.format(") {%n");
                        try (Tab tab3 = out.tab("  ")) {
                            for (ExternalName parName : rsp.parameters.members
                                .keySet())
                                out.format("this.%s = %s;%n",
                                           parName.asJavaMethodName(),
                                           parName.asJavaMethodName());
                        }
                        out.format("}%n%n");

                        /* Define a builder class to gather arguments
                         * and pass them to the constructor. */
                        try (Tab tab3 = out.documentation()) {
                            /* TODO: Look for a tag @id in the
                             * response's documentation, and use that
                             * instead of this default expression. */
                            out.format("Builds a response of type "
                                + "<samp>%s</samp> to {@link %s#%s(", rspName,
                                       typeName.asJavaClassName(),
                                       name.asJavaMethodName());
                            String sep = "";
                            for (var entryx : parameters.members.entrySet()) {
                                var at = entryx.getValue();
                                out.format("%s%s", sep, at.type
                                    .declareJava(at.required, true, ctxt));
                                sep = ", ";
                            }
                            out.format(")}.%n");
                        }
                        out.format("@%s%n", Builder.class.getCanonicalName());
                        out.format("public static final class Builder%n");
                        out.format("    implements %s<%s> {%n",
                                   Completable.class.getCanonicalName(),
                                   name.asJavaClassName());
                        try (Tab tab3 = out.tab("  ")) {
                            /* Define mutable fields. */
                            for (var parEntry : rsp.parameters.members
                                .entrySet()) {
                                ExternalName parName = parEntry.getKey();
                                Member memb = parEntry.getValue();
                                out.format("private %s %s;%n%n",
                                           memb.type.declareJava(false, false,
                                                                 ctxt),
                                           parName.asJavaMethodName());
                            }

                            /* Hide the constructor. */
                            out.format("private Builder() { }%n%n");

                            /* Define a method to set each field. */
                            for (var parEntry : rsp.parameters.members
                                .entrySet()) {
                                ExternalName parName = parEntry.getKey();
                                Member memb = parEntry.getValue();
                                /* Insert documentation comment if
                                 * provided. */
                                List<Content> descr = paramContent.get(parName);
                                if (descr != null)
                                    try (Tab tab4 = out.documentation()) {
                                        out.format("Set the field which holds ");
                                        assert rnd != null;
                                        List<Content> trimmed =
                                            Content.trimWhiteSpace(descr);
                                        rnd.visit(trimmed);
                                        out.format(".%n");
                                        out.format("@param %s ",
                                                   parName.asJavaMethodName());
                                        rnd.visit(trimmed);
                                        out.format("%n");
                                        out.format("@return this object%n");
                                    }
                                out.format("@%s(\"%s\")%n",
                                           Setter.class.getCanonicalName(),
                                           parName);
                                out.format("public Builder %s(%s %s) {%n",
                                           parName.asJavaMethodName(),
                                           memb.type.declareJava(true, false,
                                                                 ctxt),
                                           parName.asJavaMethodName());
                                try (Tab tab4 = out.tab("  ")) {
                                    out.format("this.%s = %s;%n",
                                               parName.asJavaMethodName(),
                                               parName.asJavaMethodName());
                                    out.format("return this;%n");
                                }
                                out.format("}%n%n");
                            }

                            try (Tab tab4 = out.documentation()) {
                                out.format("@undocumented%n");
                                out.format("@constructor%n");
                                out.format("@deprecated Use "
                                    + "{@link #%s()} instead.%n",
                                           Completable.METHOD_NAME);
                            }
                            out.format("@%s%n",
                                       Deprecated.class.getCanonicalName());
                            out.format("public %s _done() {%n",
                                       name.asJavaClassName());
                            try (Tab tab4 = out.tab("  ")) {
                                out.format("return %s();%n",
                                           Completable.METHOD_NAME);
                            }
                            out.format("}%n%n");

                            /* Define a method to complete the build. */
                            try (Tab tab4 = out.documentation()) {
                                out.format("Complete the response.%n");
                                out.format("@constructor%n");
                                out.format("@return the completed response%n");
                            }
                            out.format("@%s%n",
                                       Completer.class.getCanonicalName());
                            out.format("public %s %s() {%n",
                                       name.asJavaClassName(),
                                       Completable.METHOD_NAME);
                            try (Tab tab4 = out.tab("  ")) {
                                out.format("return new %s(",
                                           name.asJavaClassName());
                                {
                                    out.format("new %s(",
                                               rspName.asJavaClassName());
                                    String sep = "";
                                    for (ExternalName parName : rsp.parameters.members
                                        .keySet()) {
                                        out.format("%s%s", sep,
                                                   parName.asJavaMethodName());
                                        sep = ", ";
                                    }
                                    out.format(")");
                                }
                                out.format(") {%n");
                                try (Tab tab5 = out.tab("  ")) {
                                    if (false) {
                                        /* This code is not needed
                                         * because the base class
                                         * already delegates to the
                                         * preferred method. */
                                        out.format("@%s%n", Override.class
                                            .getCanonicalName());
                                        out.format("public boolean _%s() {%n",
                                                   rspName.prefix("is-")
                                                       .asJavaMethodName());
                                        try (Tab tab6 = out.tab("  ")) {
                                            out.format("return true;%n");
                                        }
                                        out.format("}%n%n");
                                    }

                                    out.format("@%s%n", Override.class
                                        .getCanonicalName());
                                    out.format("public boolean is$%s() {%n",
                                               rspName.asJavaMethodName());
                                    try (Tab tab6 = out.tab("  ")) {
                                        out.format("return true;%n");
                                    }
                                    out.format("}%n%n");

                                    out.format("@%s%n", Override.class
                                        .getCanonicalName());
                                    out.format("public Type %s() {%n",
                                               Union.METHOD_NAME);
                                    try (Tab tab6 = out.tab("  ")) {
                                        out.format("return Type.%s;%n", rspName
                                            .asJavaConstantName());
                                    }
                                    out.format("}%n");
                                }
                                out.format("};%n");
                            }
                            out.format("}%n");
                        }
                        out.format("}%n%n");

                        /* Define static builder factories. */
                        for (var parEntry : rsp.parameters.members.entrySet()) {
                            ExternalName parName = parEntry.getKey();
                            Member memb = parEntry.getValue();
                            /* Insert documentation comment if
                             * provided. */
                            List<Content> descr = paramContent.get(parName);
                            if (descr != null)
                                try (Tab tab3 = out.documentation()) {
                                    out.format("Create a builder for a "
                                        + "<samp>%s</samp> response, and set the "
                                        + "field which holds ", rspName);
                                    assert rnd != null;
                                    List<Content> trimmed =
                                        Content.trimWhiteSpace(descr);
                                    rnd.visit(trimmed);
                                    out.format(".%n");
                                    out.format("@param %s ",
                                               parName.asJavaMethodName());
                                    rnd.visit(trimmed);
                                    out.format("%n");
                                    out.format("@return this new builder%n");
                                }
                            out.format("@%s(\"%s\")%n",
                                       Setter.class.getCanonicalName(),
                                       parName);
                            out.format("public static Builder %s(%s %s) {%n",
                                       parName.asJavaMethodName(),
                                       memb.type.declareJava(true, false, ctxt),
                                       parName.asJavaMethodName());
                            try (Tab tab3 = out.tab("  ")) {
                                out.format("return new Builder().%s(%s);%n",
                                           parName.asJavaMethodName(),
                                           parName.asJavaMethodName());
                            }
                            out.format("}%n%n");
                        }

                        try (Tab tab3 = out.documentation()) {
                            out.format("Create an empty response.%n");
                            out.format("@return the empty response%n");
                            out.format("@undocumented%n");
                            out.format("@constructor%n");
                            out.format("@deprecated Use "
                                + "{@link #%s()} instead.%n",
                                       Completable.METHOD_NAME);
                        }
                        out.format("@%s%n",
                                   Deprecated.class.getCanonicalName());
                        out.format("public static %s _done() {%n",
                                   name.asJavaClassName());
                        try (Tab tab3 = out.tab("  ")) {
                            out.format("return %s();%n",
                                       StaticCompletable.METHOD_NAME);
                        }
                        out.format("}%n%n");

                        try (Tab tab3 = out.documentation()) {
                            out.format("Create an empty response.%n");
                            out.format("@return the empty response%n");
                            out.format("@constructor%n");
                        }
                        out.format("@%s%n", Completer.class.getCanonicalName());
                        out.format("public static %s %s() {%n",
                                   name.asJavaClassName(),
                                   StaticCompletable.METHOD_NAME);
                        try (Tab tab3 = out.tab("  ")) {
                            out.format("return new Builder().%s();%n",
                                       Completable.METHOD_NAME);
                        }
                        out.format("}%n");
                    }
                    out.format("}%n");
                    /* Builder definition complete. */
                }
            }
            out.format("}%n%n");
        }
    }
}
