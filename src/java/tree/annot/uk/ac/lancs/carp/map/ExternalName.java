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

package uk.ac.lancs.carp.map;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Holds an immutable, valid external name. An external name is designed
 * to be used on all external interfaces, and is translated to the
 * target language in a predictable language-specific way. It consists
 * of one or more module components, separated by dots (U+002E FULL STOP
 * "{@code .}"). Each component consists of one or more words separated
 * by dashes (U+002D HYPHEN-MINUS "{@code -}") or slashes (U+002F
 * SOLIDUS "{@code /}"). Dots may not appear at the start or end of a
 * name, nor may two dots be adjacent. Similarly, dashes/slashes may not
 * appear at the start or end of a component, nor may two dashes/slashes
 * be adjacent. Words may only consist of alphanumerics, and must begin
 * with an alphabetic. Characters are normalized to lower-case. An
 * external name maps to a <dfn>target name</dfn> for any given target
 * language.
 * 
 * <p>
 * A name that identifies a structure field, an enumeration constant, or
 * an interface call or response has only one component.
 * 
 * <p>
 * In Java, a module name is mapped to a Java package name explicitly
 * using the {@link uk.ac.lancs.carp.deploy.Deploy} annotation. A type
 * name is mapped by separating the parent (the module name) from the
 * leaf, mapping the module name as above, and then capitalizing each
 * word of the leaf and concatenating, before appending it with a dot to
 * the package name. An enumeration constant has no module prefix, and
 * is mapped by converting to capitals, and joining words with an
 * underscore, except where a slash is used. A structure field has no
 * module prefix, and its sole component is mapped by capitalizing each
 * word except the first, and concatenating. A call name is similarly
 * converted, though its nested support class uses an initial capital in
 * keeping with Java convention. A response name exists as a nested
 * class, and is mapped similarly to the containing call support class.
 * These transformations are mostly consistent with the Java naming
 * convention. The use of a slash to separate words permits
 * capitalization of the next word for method/field/class names, but
 * does not map to an underscore in a constant. The following methods
 * perform this mapping, and each documents examples:
 * 
 * <ul>
 * 
 * <li>{@link #asJavaPackageName()}
 * 
 * <li>{@link #asJavaClassName()}
 * 
 * <li>{@link #asJavaMethodName()}
 * 
 * <li>{@link #asJavaConstantName()}
 * 
 * </ul>
 * 
 * @author simpsons
 */
public class ExternalName {
    enum Case {
        LOWER, UPPER, LOWER_CAMEL, UPPER_CAMEL;
    }

    static class Part {
        final List<? extends String> words;

        final BitSet slashes;

        Part(List<? extends String> words, BitSet slashes) {
            assert words != null;
            assert slashes != null;
            this.words = words;
            this.slashes = slashes;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + slashes.hashCode();
            result = prime * result + words.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (!(obj instanceof Part)) return false;
            Part other = (Part) obj;
            if (!slashes.equals(other.slashes)) return false;
            if (!words.equals(other.words)) return false;
            return true;
        }

        public String toString(Case caze, String dash, String slash) {
            StringBuilder dest = new StringBuilder();
            final int lim = words.size() - 1;
            int i = 0;
            boolean lastSlash = false;
            for (String word : words) {
                switch (caze) {
                case LOWER:
                    dest.append(word);
                    break;

                case LOWER_CAMEL:
                    dest.append(i == 0 || lastSlash ? word : toCamelCase(word));
                    break;

                case UPPER:
                    dest.append(word.toUpperCase(Locale.ROOT));
                    break;

                case UPPER_CAMEL:
                    dest.append(lastSlash ? word : toCamelCase(word));
                    break;
                }
                if (i < lim)
                    dest.append((lastSlash = slashes.get(i++)) ? slash : dash);
            }
            return dest.toString();
        }
    }

    private final List<Part> parts;

    /**
     * Parse a name into its components.
     * 
     * @param src the name as a character sequence
     * 
     * @return the parsed name
     * 
     * @throws NullPointerException if {@code src} is {@code null}
     * 
     * @throws IllegalArgumentException if the name does not conform
     * 
     * @constructor
     */
    public static ExternalName parse(CharSequence src) {
        return new ExternalName(src);
    }

    /**
     * Parse a name into its components, without error on {@code null}
     * input.
     * 
     * @param src the name as a character sequence
     * 
     * @return the parsed name; or {@code null} if the input is
     * {@code null}
     * 
     * @throws IllegalArgumentException if the name does not conform
     * 
     * @constructor
     */
    public static ExternalName parseNull(CharSequence src) {
        if (src == null) return null;
        return parse(src);
    }

    /**
     * Apply a prefix to the leaf component.
     * 
     * @param pfx the prefix to insert
     * 
     * @return the prefixed name
     */
    public ExternalName prefix(CharSequence pfx) {
        ExternalName mod = this.getParent();
        ExternalName leaf = ExternalName.parse(pfx.toString() + this.getLeaf());
        if (mod == null) return leaf;
        return mod.resolve(leaf);
    }

    private ExternalName(CharSequence src) {
        if (src == null) throw new NullPointerException();
        String[] modParts = DOTS.split(src);
        if (modParts.length == 0)
            throw new IllegalArgumentException("empty name");
        List<Part> parts = new ArrayList<>(modParts.length);
        for (String modPart : modParts) {
            if (modPart.isEmpty())
                throw new IllegalArgumentException("empty component in [" + src
                    + "]");
            List<String> lowered = new ArrayList<>();
            BitSet slashes = new BitSet();

            int end = 0;
            Matcher m = DASHES.matcher(modPart);
            while (m.find(end)) {
                /* Separator is from m.start() to m.end(). */
                assert m.end() - m.start() == 1;
                if (modPart.charAt(m.start()) == '/')
                    slashes.set(lowered.size());

                /* Word is from end to m.start(). */
                final CharSequence word = modPart.subSequence(end, m.start());
                if (!WORD.matcher(word).matches())
                    throw new IllegalArgumentException("illegal word in [" + src
                        + "]: [" + word + "]");
                lowered.add(word.toString().toLowerCase(Locale.ROOT));

                /* Continue search from next character. */
                end = m.end();
            }

            /* Include the final word that reaches to the end of the
             * part. */
            final CharSequence word =
                modPart.subSequence(end, modPart.length());
            if (!WORD.matcher(word).matches())
                throw new IllegalArgumentException("illegal word in [" + src
                    + "]: [" + word + "]");
            lowered.add(word.toString().toLowerCase(Locale.ROOT));

            parts.add(new Part(List.copyOf(lowered), slashes));
        }
        this.parts = List.copyOf(parts);
    }

    private ExternalName(List<Part> parts) {
        assert parts != null;
        this.parts = parts;
    }

    private static final Pattern WORD =
        Pattern.compile("^\\p{Alpha}[\\p{Digit}\\p{Alpha}]*$");

    private static final Pattern DOTS = Pattern.compile("\\.");

    private static final Pattern DASHES = Pattern.compile("[-/]");

    /**
     * Determine whether the name is contained within a parent.
     * 
     * @return {@code true} if the name has exactly one component
     */
    public boolean isLeaf() {
        return parts.size() == 1;
    }

    /**
     * Get the parent of this name.
     * 
     * @return this name with the last element removed; or {@code null}
     * if there is only one element
     */
    public ExternalName getParent() {
        if (parts.size() == 1) return null;
        return new ExternalName(parts.subList(0, parts.size() - 1));
    }

    /**
     * Get the leaf component of this name.
     * 
     * @return a name consisting only of the leaf component of this name
     */
    public ExternalName getLeaf() {
        if (parts.size() == 1) return this;
        return new ExternalName(parts.subList(parts.size() - 1, parts.size()));
    }

    /**
     * Combine two names, as if by resolving one within the context of
     * another.
     * 
     * @param sub the name to resolve in the context of this name
     * 
     * @return a new name consisting of this name's components followed
     * by those of {@code sub}
     */
    public ExternalName resolve(ExternalName sub) {
        return new ExternalName(Stream
            .concat(this.parts.stream(), sub.parts.stream())
            .collect(Collectors.toList()));
    }

    /**
     * Get the hash code of this name.
     * 
     * @return the hash code of this name
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((parts == null) ? 0 : parts.hashCode());
        return result;
    }

    /**
     * Test whether this name equals another object.
     * 
     * @param obj the object to test against
     * 
     * @return {@code true} if the other object is a
     * {@link ExternalName} and has identical components
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ExternalName other = (ExternalName) obj;
        if (parts == null) {
            if (other.parts != null) return false;
        } else if (!parts.equals(other.parts)) return false;
        return true;
    }

    /**
     * Convert the name to its string representation. Passing this to
     * {@link #parse(CharSequence)} should yield an identical name.
     * 
     * @return the string representation of this name
     */
    @Override
    public String toString() {
        return parts.stream().map(ws -> ws.toString(Case.LOWER, "-", "/"))
            .collect(Collectors.joining("."));
    }

    /**
     * Convert the first character of a string to upper-case.
     * 
     * @param in the input string
     * 
     * @return the input string with the first character converted to
     * upper-case
     */
    private static String toCamelCase(String in) {
        return in.substring(0, 1).toUpperCase(Locale.ROOT) + in.substring(1);
    }

    /**
     * Convert a sequence of words to a camel-case name. Each word is
     * converted using {@link #toCamelCase(String)}, and the results are
     * concatenated.
     * 
     * @param words the input sequence
     * 
     * @return the sequence concatenated with upper-case characters
     * marking the start of each word
     */
    private static String toClassName(List<? extends String> words) {
        return words.stream().map(ExternalName::toCamelCase)
            .collect(Collectors.joining());
    }

    /**
     * Convert this name to a Java package name. Each word is joined
     * using an underscore, and each component is joined using a dot.
     * For example, <samp>org.example-org.victory-truly-handicaps</samp>
     * becomes <samp>org.example_org.victory_truly_handicaps</samp>.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asJavaPackageName()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>org.example_org.victory_truly_handicaps</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>slap_me.up_side.the_head.my_ietf_index</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>slap_me.up_side.the_head.ietf_index</samp></td>
     * </tr>
     * </table>
     * 
     * <p>
     * This method should not generally be used to convert a module name
     * to a Java package name (as that is an explicit mapping specified
     * by {@link uk.ac.lancs.carp.deploy.Deploy}), but only to locate
     * resources such as {@value #JAVA_PROPERTIES_LEAF_NAME} and
     * {@value #JAVA_IDL_LEAF_NAME} in calls to
     * {@link javax.annotation.processing.Filer#getResource(javax.tools.JavaFileManager.Location, CharSequence, CharSequence)}
     * or
     * {@link javax.annotation.processing.Filer#createResource(javax.tools.JavaFileManager.Location, CharSequence, CharSequence, javax.lang.model.element.Element...) }.
     * 
     * @return this name converted to a Java package name
     */
    public String asJavaPackageName() {
        return parts.stream().map(ws -> ws.toString(Case.LOWER, "_", ""))
            .collect(Collectors.joining("."));
    }

    /**
     * Convert this name to a Java class name. The parent is stripped.
     * The leaf name is converted by upper-casing each word and
     * concatenating.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asJavaClassName()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>VictoryTrulyHandicaps</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>MyIETFIndex</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>IETFIndex</samp></td>
     * </tr>
     * </table>
     * 
     * @return this name converted to a Java class name
     */
    public String asJavaClassName() {
        return toClassName(getLeafPart().words);
    }

    private Part getLeafPart() {
        return getLeaf().parts.get(0);
    }

    /**
     * Convert the leaf of this name to a Java member name. Each word of
     * the leaf, except the first, is upper-cased, and the results
     * concatenated.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asJavaMethodName()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>victoryTrulyHandicaps</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>myIetfIndex</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>ietfIndex</samp></td>
     * </tr>
     * </table>
     * 
     * @return the leaf of this name converted to a Java member name
     */
    public String asJavaMethodName() {
        return getLeafPart().toString(Case.LOWER_CAMEL, "", "");
    }

    /**
     * Convert the leaf of this name to a Java member name. Each word of
     * the leaf is upper-cased, and the results concatenated with
     * underscores.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asJavaConstantName()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>VICTORY_TRULY_HANDICAPS</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>MY_IETF_INDEX</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>IETF_INDEX</samp></td>
     * </tr>
     * </table>
     * 
     * @return the leaf of this name converted to a Java member name
     */
    public String asJavaConstantName() {
        return getLeafPart().toString(Case.UPPER, "_", "");
    }

    /**
     * Convert this name to a sequence of URI path elements. Each
     * component becomes a path element simply by removing slashes. Path
     * elements are joined by slashes.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asPathElements()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>org/example-org/victory-truly-handicaps</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>slap-me/up-side/the-head/my-ietf-index</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>slap-me/up-side/the-head/ietf-index</samp></td>
     * </tr>
     * </table>
     * 
     * @return this name converted to path elements
     */
    public String asPathElements() {
        return parts.stream().map(p -> p.toString(Case.LOWER, "-", ""))
            .collect(Collectors.joining("/"));
    }

    /**
     * The name of the IDL file within a package
     */
    public static final String JAVA_IDL_LEAF_NAME = "carp.rpc";

    /**
     * Convert this name to a Java resource path for a properties file.
     * Each word is concatenated with an underscore, and each component
     * with a slash. <samp>/carp.properties</samp> is suffixed.
     * 
     * <table summary="This table lists external names against their
     * expansions using this method.">
     * <caption>Example conversions for
     * {@link #asJavaTypeResourcePath()}</caption> <thead>
     * <tr>
     * <th>Input</th>
     * <th>Expansion</th>
     * </tr>
     * </thead>
     * <tr>
     * <td><samp>org.example-org.victory-truly-handicaps</samp></td>
     * <td><samp>org/example_org/victory_truly_handicaps/carp.properties</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.my-i/e/t/f-index</samp></td>
     * <td><samp>slap_me/up_side/the_head/my_ietf_index/carp.properties</samp></td>
     * </tr>
     * <tr>
     * <td><samp>slap-me.up-side.the-head.i/e/t/f-index</samp></td>
     * <td><samp>slap_me/up_side/the_head/ietf_index/carp.properties</samp></td>
     * </tr>
     * </table>
     * 
     * @return the resource path of the corresponding IDL
     */
    public String asJavaTypeResourcePath() {
        return parts.stream().map(ws -> ws.toString(Case.LOWER, "_", ""))
            .collect(Collectors.joining("/")) + "/" + JAVA_PROPERTIES_LEAF_NAME;
    }

    /**
     * The name of the properties file within a package
     */
    public static final String JAVA_PROPERTIES_LEAF_NAME = "carp.properties";

    /**
     * 
     * @undocumented
     * 
     * @param args
     */
    public static void main(String[] args) {
        List<ExternalName> examples = Arrays
            .asList("org.example-org.victory-truly-handicaps",
                    "slap-me.up-side.the-head.my-i/e/t/f-index",
                    "slap-me.up-side.the-head.i/e/t/f-index")
            .stream().map(ExternalName::parse).collect(Collectors.toList());
        Map<String, Function<ExternalName, String>> formats = new HashMap<>();
        formats.put("asPathElements", ExternalName::asPathElements);
        formats.put("asJavaClassName", ExternalName::asJavaClassName);
        formats.put("asJavaConstantName", ExternalName::asJavaConstantName);
        formats.put("asJavaMethodName", ExternalName::asJavaMethodName);
        formats.put("asJavaPackageName", ExternalName::asJavaPackageName);
        formats.put("asJavaTypeResourcePath",
                    ExternalName::asJavaTypeResourcePath);
        for (Map.Entry<String, Function<ExternalName, String>> entry : formats
            .entrySet()) {
            System.out.printf("<table summary=\"This table lists"
                + " external names against their"
                + " expansions using this method.\">%n");
            System.out.printf(
                              "<caption>Example conversions for"
                                  + " {@link #%s()}</caption>%n",
                              entry.getKey());
            System.out.printf("<thead>%n");
            System.out.printf("<tr><th>Input</th> <th>Expansion</th></tr>%n");
            System.out.printf("</thead>%n");
            for (ExternalName example : examples) {
                System.out.printf("<tr><td><samp>%s</samp></td> ", example);
                System.out.printf("<td><samp>%s</samp></td></tr>%n",
                                  entry.getValue().apply(example));
            }
            System.out.printf("</table>%n%n");
        }
    }
}
