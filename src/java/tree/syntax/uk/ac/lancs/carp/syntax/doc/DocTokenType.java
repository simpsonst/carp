// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright (c) 2021, Lancaster University
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the
 *   distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 *  Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.carp.syntax.doc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collections;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.ac.lancs.syntax.Epsilon;
import uk.ac.lancs.syntax.LL1Grammar;
import uk.ac.lancs.syntax.Lexicon;
import uk.ac.lancs.syntax.Literal;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.syntax.NonTerminal;
import uk.ac.lancs.syntax.Parser;
import uk.ac.lancs.syntax.Production;
import uk.ac.lancs.syntax.Token;
import uk.ac.lancs.syntax.Unmatched;

/**
 * Distinguishes tokens and non-terminals for the documentation comment
 * syntax.
 * 
 * @author simpsons
 */
public enum DocTokenType {
    /**
     * Matches all otherwise unmatched content. Plain text appears here.
     */
    @Unmatched
    UNMATCHED,

    /**
     * Indicates end-of-input.
     */
    @Epsilon
    EPSILON,

    /**
     * Matches a hexadecimal HTML character entity.
     */
    @Literal("&#x[a-fA-F0-9]+;")
    HEX_CHAR,

    /**
     * Matches a decimal HTML character entity.
     */
    @Literal("&#[0-9]+;")
    DEC_CHAR,

    /**
     * Matches an HTML entity reference.
     */
    @Literal("&[a-zA-Z_0-9]+;")
    ENT_REF,

    /**
     * Matches the start of an in-line tag.
     */
    @Literal("\\{@[a-zA-Z_]+")
    INLINE_START,

    /**
     * Matches the start of a block tag.
     */
    @Literal("@[a-zA-Z_]+")
    BLOCK_START,

    /**
     * Matches the end of an in-line tag.
     */
    @Literal("\\}")
    INLINE_END,

    /**
     * Matches the start of an HTML tag.
     */
    @Literal("<")
    MARKUP_START,

    /**
     * Matches an HTML tag name. This must be defined after
     * {@link #NMTOKEN}, which matches a superset of these strings.
     */
    @Literal("[a-zA-Z][a-zA-Z0-9]*")
    ELEMENT_NAME,

    /**
     * Matches an unquoted value of an HTML attribute. This must be
     * defined after {@link #ELEMENT_NAME}, which matches a subset of
     * these strings.
     */
    @Literal("[a-zA-Z0-9]+")
    NMTOKEN,

    /**
     * Matches the end of an HTML tag.
     */
    @Literal(">")
    MARKUP_END,

    /**
     * Matches the forward slash used to indicate an empty or closing
     * tag in (X)HTML.
     */
    @Literal("/")
    MARKUP_SLASH,

    /**
     * Matches at least one white-space character. This is used to match
     * the often optional white space between components of an HTML tag.
     */
    @Literal("\\s+")
    WHITESPACE,

    /**
     * Matches the assignment character for an HTML attribute.
     */
    @Literal("=")
    EQUALS,

    /**
     * Matches the single-quote character used to delimit XML attribute
     * values.
     */
    @Literal("'")
    SQ,

    /**
     * Matches the double-quote character used to delimit XML and HTML
     * attribute values.
     */
    @Literal("\"")
    DQ,

    /**
     * Completes a single-quoted string.
     */
    @Production({ "SQCONTENT", "SQ" })
    SQS,

    /**
     * Matches the content of a single-quoted string. This excludes the
     * single quote and block tags.
     */
    @Production({ "UNMATCHED", "SQCONTENT" })
    @Production({ "WHITESPACE", "SQCONTENT" })
    @Production({ "EQUALS", "SQCONTENT" })
    @Production({ "HEX_CHAR", "SQCONTENT" })
    @Production({ "DEC_CHAR", "SQCONTENT" })
    @Production({ "ENT_REF", "SQCONTENT" })
    @Production({ "DQ", "SQCONTENT" })
    @Production({ "ELEMENT_NAME", "SQCONTENT" })
    @Production({ "NMTOKEN", "SQCONTENT" })
    @Production({ "MARKUP_START", "SQCONTENT" })
    @Production({ "MARKUP_END", "SQCONTENT" })
    @Production({ "MARKUP_SLASH", "SQCONTENT" })
    @Production({ "INLINE_START", "SQINLINE_TAG", "SQCONTENT" })
    @Production({ })
    SQCONTENT,

    /**
     * Completes a double-quoted string.
     */
    @Production({ "DQCONTENT", "DQ" })
    DQS,

    /**
     * Matches the content of a double-quoted string. This excludes the
     * double quote and block tags.
     */
    @Production({ "UNMATCHED", "DQCONTENT" })
    @Production({ "WHITESPACE", "DQCONTENT" })
    @Production({ "EQUALS", "DQCONTENT" })
    @Production({ "HEX_CHAR", "DQCONTENT" })
    @Production({ "DEC_CHAR", "DQCONTENT" })
    @Production({ "ENT_REF", "DQCONTENT" })
    @Production({ "SQ", "DQCONTENT" })
    @Production({ "ELEMENT_NAME", "DQCONTENT" })
    @Production({ "NMTOKEN", "DQCONTENT" })
    @Production({ "MARKUP_START", "DQCONTENT" })
    @Production({ "MARKUP_END", "DQCONTENT" })
    @Production({ "MARKUP_SLASH", "DQCONTENT" })
    @Production({ "INLINE_START", "DQINLINE_TAG", "DQCONTENT" })
    @Production({ })
    DQCONTENT,

    /**
     * Matches the start of an HTML tag. This includes <samp>&lt;</samp>
     * and optional white space. The next token determines whether this
     * is an explicitly empty tag, a closing tag, or an
     * opening/implicit-empty tag.
     */
    @Production({ "OPTWS", "MARKUP_TAG_CHOICE" })
    MARKUP_TAG,

    /**
     * Determines whether an HTML tag is closing (it continues with a
     * slash) or not (with an element name).
     */
    @Production({ "MARKUP_SLASH", "OPTWS", "ELEMENT_NAME", "OPTWS",
        "MARKUP_END" })
    @Production({ "ELEMENT_NAME", "OPTWS", "MARKUP_TAIL" })
    MARKUP_TAG_CHOICE,

    /**
     * Matches HTML attributes and detects whether a tag is explicitly
     * empty.
     */
    @Production({ "MARKUP_SLASH", "OPTWS", "MARKUP_END" })
    @Production({ "MARKUP_END" })
    @Production({ "ELEMENT_NAME", "OPTWS", "ATTR", "OPTWS", "MARKUP_TAIL" })
    MARKUP_TAIL,

    /**
     * Matches the assignment of a value to an attribute, or that an
     * already parsed attribute is a flag.
     */
    @Production({ "EQUALS", "OPTWS", "ATTR_VALUE" })
    @Production({ })
    ATTR,

    /**
     * Matches an attribute's value. Single-quoted and double-quoted
     * string, and raw name tokens are recognized. Because element name
     * tokens are a subset of name tokens, they are also matched.
     */
    @Production({ "SQ", "SQS" })
    @Production({ "DQ", "DQS" })
    @Production("ELEMENT_NAME")
    @Production("NMTOKEN")
    ATTR_VALUE,

    /**
     * Matches optional white space.
     */
    @Production("WHITESPACE")
    @Production({ })
    OPTWS,

    /**
     * Matches top-level content consisting of HTML tags, character
     * entities, entity references, in-line documentation tags, and
     * miscellaneous content. The closing brace <samp>&#125;</samp> is
     * also recognized when it is not closing an in-line tag.
     */
    @Production({ "UNMATCHED", "CONTENT" })
    @Production({ "WHITESPACE", "CONTENT" })
    @Production({ "EQUALS", "CONTENT" })
    @Production({ "HEX_CHAR", "CONTENT" })
    @Production({ "DEC_CHAR", "CONTENT" })
    @Production({ "ENT_REF", "CONTENT" })
    @Production({ "SQ", "CONTENT" })
    @Production({ "DQ", "CONTENT" })
    @Production({ "ELEMENT_NAME", "CONTENT" })
    @Production({ "NMTOKEN", "CONTENT" })
    @Production({ "MARKUP_END", "CONTENT" })
    @Production({ "MARKUP_SLASH", "CONTENT" })
    @Production({ "INLINE_END", "CONTENT" })
    @Production({ "INLINE_START", "INLINE_TAG", "CONTENT" })
    @Production({ "MARKUP_START", "MARKUP_TAG", "CONTENT" })
    @Production({ })
    CONTENT,

    /**
     * Matches content within an in-line tag consisting of HTML tags,
     * character entities, entity references, in-line documentation
     * tags, and miscellaneous content. The closing brace
     * <samp>&#125;</samp> is not recognized.
     */
    @Production({ "UNMATCHED", "INLINE_CONTENT" })
    @Production({ "WHITESPACE", "INLINE_CONTENT" })
    @Production({ "EQUALS", "INLINE_CONTENT" })
    @Production({ "HEX_CHAR", "INLINE_CONTENT" })
    @Production({ "DEC_CHAR", "INLINE_CONTENT" })
    @Production({ "ENT_REF", "INLINE_CONTENT" })
    @Production({ "SQ", "INLINE_CONTENT" })
    @Production({ "DQ", "INLINE_CONTENT" })
    @Production({ "ELEMENT_NAME", "INLINE_CONTENT" })
    @Production({ "NMTOKEN", "INLINE_CONTENT" })
    @Production({ "MARKUP_END", "INLINE_CONTENT" })
    @Production({ "MARKUP_SLASH", "INLINE_CONTENT" })
    @Production({ "INLINE_START", "INLINE_TAG", "INLINE_CONTENT" })
    @Production({ "MARKUP_START", "MARKUP_TAG", "INLINE_CONTENT" })
    @Production({ })
    INLINE_CONTENT,

    /**
     * Matches a block tag, consisting of <samp>@<var>name</var></samp>,
     * optional white space, and all following content.
     */
    @Production({ "OPTWS", "CONTENT" })
    BLOCK_TAG,

    /**
     * Matches an in-line tag not inside an HTML attribute value,
     * consisting of <samp>&#123;@<var>name</var></samp>, optional white
     * space, and all following content up to and including the closing
     * <samp>&#125;</samp>.
     */
    @Production({ "OPTWS", "INLINE_CONTENT", "INLINE_END" })
    INLINE_TAG,

    /**
     * Matches an in-line tag inside a single-quoted HTML attribute
     * value, consisting of <samp>&#123;@<var>name</var></samp>,
     * optional white space, and all following content up to and
     * including the closing <samp>&#125;</samp>.
     */
    @Production({ "OPTWS", "SQCONTENT", "INLINE_END" })
    SQINLINE_TAG,

    /**
     * Matches an in-line tag inside a double-quoted HTML attribute
     * value, consisting of <samp>&#123;@<var>name</var></samp>,
     * optional white space, and all following content up to and
     * including the closing <samp>&#125;</samp>.
     */
    @Production({ "OPTWS", "DQCONTENT", "INLINE_END" })
    DQINLINE_TAG,

    /**
     * Matches trailing block tags of a documentation comment.
     */
    @Production({ "BLOCK_START", "BLOCK_TAG", "BLOCK_TAGS" })
    @Production({ })
    BLOCK_TAGS,

    /**
     * Matches a document comment.
     */
    @Production({ "CONTENT", "BLOCK_TAGS" })
    DOCUMENT,

    /**
     * Replaces concatenated sequences of plain text.
     */
    TEXT;

    /**
     * Normalize a syntax tree. The following transformations take
     * place:
     * 
     * <ul>
     * 
     * <li>
     * <p>
     * Every non-terminal is first processed by applying this function
     * recursively to its components.
     * 
     * <li>
     * <p>
     * {@link #HEX_CHAR} and {@link #DEC_CHAR} are replaced by their
     * literal character equivalents. {@link #ENT_REF} is similarly
     * replaced, if the entity name is recognized as one of the standard
     * HTML character entities. Otherwise, the surrounding
     * <samp>&amp;<var>name</var>;</samp> is stripped to leave only
     * <samp><var>name</var></samp>.
     * 
     * <li>
     * <p>
     * {@link #INLINE_START} always matches a string beginning
     * <samp>&#123;&#64;</samp>. These are stripped.
     * 
     * <li>
     * <p>
     * If a {@link #BLOCK_TAGS} is not empty, it consists of the token
     * used to identify a new block tag, a token for the content, and
     * its recursive successors.
     * 
     * <li>
     * <p>
     * If a {@link #DOCUMENT}'s first child is {@link #CONTENT}, its
     * contents are made the {@link #DOCUMENT}'s first children. The
     * second child is always {@link #BLOCK_TAGS}, and its contents are
     * drawn out and follow the content child(ren).
     * 
     * </ul>
     * 
     * @param input the tree to be processed
     * 
     * @return a new tree
     */
    public static Node<DocTokenType> postprocess(Node<DocTokenType> input) {
        /* This recursive function stops under these conditions. */
        if (input == null) return null;

        /* The children of non-terminals should be recursively
         * post-processed first. Create a new node which replaces the
         * input. */
        if (input instanceof NonTerminal) input = NonTerminal
            .of(input.type, input.start, input.end, input.children().stream()
                .map(DocTokenType::postprocess).collect(Collectors.toList()));

        switch (input.type) {
        case DOCUMENT: {
            /* The first child is CONTENT; the second BLOCK_TAGS.
             * Extract and concatenate their content. */
            assert input.size() == 2;
            var c0 = input.child(0);
            var c1 = input.child(1);
            assert input.child(1).type == BLOCK_TAGS;
            if (c0.type == CONTENT) {
                var nc = Stream
                    .concat(c0.children().stream(), c1.children().stream())
                    .collect(Collectors.toList());
                return NonTerminal.of(input.type, input.start, input.end, nc);
            }
            return NonTerminal.of(input.type, input.start, input.end, c0,
                                  c1.children());
        }

        case BLOCK_TAGS: {
            if (input.isEmpty()) return input;

            /* The first child is the literal required by LL(1) to
             * select the production. The second completes what was
             * matched. Fold the two together. */
            var pfx = input.child(0);
            var str = input.child(1);
            var foo = NonTerminal.of(str.type, pfx, str.children());
            var sub = input.child(2);
            return NonTerminal.of(BLOCK_TAGS, foo, sub.children());
        }

        case BLOCK_START:
            /* Strip the @ sign from an in-line tag. */
            return Token.of(BLOCK_START, input.start, input.end,
                            input.text().substring(1));

        case INLINE_START:
            /* Strip the opening brace and @ sign from an in-line
             * tag. */
            return Token.of(INLINE_START, input.start, input.end,
                            input.text().substring(2));

        case MARKUP_TAG_CHOICE: {
            if (input.size() == 3) return NonTerminal
                .of(input.type, input.child(0), input.child(2).children());
            assert input.size() == 5;
            return NonTerminal.of(input.type, input.child(0), input.child(2));
        }

        case INLINE_TAG:
        case SQINLINE_TAG:
        case DQINLINE_TAG:
            /* Swallow the white space terminating a tag start.
             * Normalize to an in-line tag. */
            assert input.child(0).type == OPTWS;
            if (input.child(1).type == CONTENT) {
                if (input.child(1).isEmpty())
                    return NonTerminal.of(INLINE_TAG, input.child(1).start,
                                          input.child(1).end,
                                          input.child(1).children());
                return NonTerminal.of(INLINE_TAG, input.child(1).children());
            }
            return NonTerminal.of(INLINE_TAG, input.children(1, 2));

        case BLOCK_TAG:
            /* Swallow the white space terminating a tag start. */
            assert input.child(0).type == OPTWS;
            if (input.child(1).type == CONTENT)
                return NonTerminal.of(BLOCK_TAG, input.child(1).children());
            return NonTerminal.of(input.type, input.children(1));

        case HEX_CHAR: {
            /* Convert the &#xXXX; code into its corresponding
             * character. */
            String text = input.text();
            text = text.substring(3, text.length() - 1);
            int cp = Integer.parseInt(text, 16);
            String ch = Character.toString(cp);
            return Token.of(TEXT, input.start, input.end, ch);
        }

        case DEC_CHAR: {
            /* Convert the &#XXX; code into its corresponding
             * character. */
            String text = input.text();
            text = text.substring(2, text.length() - 1);
            int cp = Integer.parseInt(text, 10);
            String ch = Character.toString(cp);
            return Token.of(TEXT, input.start, input.end, ch);
        }

        case ENT_REF: {
            /* Convert the &NAME; code into its corresponding character,
             * if known. Otherwise, replace the content by stripping off
             * the & and ;. */
            String text = input.text();
            text = text.substring(1, text.length() - 1);
            String cpText = ENTITIES.getProperty(text);
            if (cpText == null)
                return Token.of(input.type, input.start, input.end, text);
            int cp = Integer.parseInt(cpText, 10);
            String ch = Character.toString(cp);
            return Token.of(TEXT, input.start, input.end, ch);
        }

        case MARKUP_TAG:
            return NonTerminal.of(input.type, input.start, input.end,
                                  input.child(1).children());

        case MARKUP_TAIL: {
            if (input.size() == 5) {
                /* Another attribute has been specified. Unpack its
                 * nested value to make it into an integrated ATTR,
                 * including name and value. */
                var attr = input.child(2);
                var rest = input.child(4);
                final var ar = attr.isEmpty() ?
                    NonTerminal.of(ATTR, input.start, input.end,
                                   input.child(0)) :
                    NonTerminal.of(ATTR, input.start, input.end, input.child(0),
                                   attr.child(2));

                /* Unpack the continuation, and follow the attribute
                 * with it. */
                return NonTerminal.of(input.type, input.start, input.end, ar,
                                      rest.children());
            }

            /* This is the end of a self-closing HTML tag. We only need
             * to keep the slash to show this. */
            if (input.size() == 3) return NonTerminal
                .of(input.type, input.start, input.end, input.child(0));

            /* This is just the end of a start tag. We don't need to
             * keep it, but we still need to provide a mark-up tail. */
            assert input.size() == 1;
            return NonTerminal.of(input.type, input.start, input.end,
                                  input.children(1));
        }

        case ATTR_VALUE: {
            if (input.size() == 2) {
                var ch = input.child(1).child(0);
                if (ch.type == CONTENT)
                    return NonTerminal.of(input.type, ch.children());
                return NonTerminal.of(input.type, ch);
            }
            var ch = input.child(0);
            return NonTerminal.of(input.type,
                                  Token.of(TEXT, ch.start, ch.end, ch.text()));
        }

        case CONTENT:
        case INLINE_CONTENT:
        case SQCONTENT:
        case DQCONTENT:
            /* Where possible, convert *CONTENT into TEXT. Otherwise,
             * convert *CONTENT into plain CONTENT, unpacking the
             * continuation. */

            /* For an empty node, just normalize the type. */
            if (input.isEmpty()) return NonTerminal
                .of(CONTENT, input.start, input.end, input.children());

            /* Most productions consist of two children, the first of
             * which is either an unknown entity reference, or some
             * literal text mistaken for some other token. The second is
             * always the continuation. Make sure that the first is
             * converted to TEXT if possible, the children of the
             * continuation are unpacked, adjacent TEXT nodes are
             * concatenated, and the result is either TEXT or (where
             * necessary) CONTENT. */
            if (input.size() == 2) {
                /* With one exception (ENT_REF), the first child is to
                 * be interpreted as literal text. */
                var pfx = input.child(0);
                if (pfx.type != ENT_REF && pfx.type != TEXT)
                    pfx = Token.of(TEXT, pfx.start, pfx.end, pfx.text());

                var sub = input.child(1);
                if (sub.type == CONTENT && !sub.isEmpty()) {
                    var subc = sub.children();
                    assert !subc.isEmpty();
                    if (pfx.type == TEXT && subc.get(0).type == TEXT) {
                        /* The first child is now TEXT, and the first of
                         * the second is also text. Extract the latter,
                         * and combine it with the former. Then append
                         * the remaining children. */
                        pfx = Token.of(TEXT, pfx.start, sub.child(0).end,
                                       pfx.text() + sub.child(0).text());
                        return NonTerminal.of(CONTENT, pfx,
                                              subc.subList(1, subc.size()));
                    }

                    /* Unpack the second child, and catenate it to our
                     * converted first child. */
                    return NonTerminal.of(CONTENT, pfx, subc);
                }
                if (pfx.type == TEXT) {
                    /* The first child is now TEXT, and the second
                     * already is, so combine them into a single TEXT
                     * node. */
                    return Token.of(TEXT, pfx.start, sub.end,
                                    pfx.text() + sub.text());
                }
                return NonTerminal.of(CONTENT, pfx, sub);
            }

            /* The first child is the literal required by LL(1) to
             * select the production. The second completes what was
             * matched. Fold the two together. */
            var pfx = input.child(0);
            var str = input.child(1);
            var sub = input.child(2);
            var subc = sub.type == CONTENT ? sub.children() :
                Collections.singletonList(sub);
            if (pfx.type == INLINE_START) {
                var foo = NonTerminal.of(str.type, pfx, str.children());
                return NonTerminal.of(CONTENT, foo, subc);
            }
            assert pfx.type == MARKUP_START;
            return NonTerminal.of(CONTENT, str, subc);
        }
        return input;

    }

    /**
     * 
     * @undocumented
     */
    public static void main(String[] args) throws Exception {
        Lexicon<DocTokenType> lexicon = new Lexicon<>(DocTokenType.class);
        LL1Grammar<DocTokenType> syntax = new LL1Grammar<>(DocTokenType.class);

        String text = "The traffic flows into the network. @slam dunk <hr> yes";
        String text1 =
            "Expresses a &lt;change&#64; in {@friendly terminal &xx; capacity}."
                + " Either an offset &unk; or an\n"
                + "absolute<br/> value should be specified."
                + " If both <samp lang=en title=\"yes&wxxx;!\">are</samp>"
                + " specified, the\n" + "absolute value applies.\n" + "\n"
                + "@param offset a relative change to the capacity\n" + "\n"
                + "@param value the &shaft; new capacity\n" + "";
        String text2 = "trunk.\n" + "\n"
            + "@param status whether the trunk should be placed into commission\n"
            + "({@true}) or decommissioned ({@false})\n" + "  ";

        Parser<DocTokenType> parser = syntax.newParser(DocTokenType.DOCUMENT);
        Consumer<Token<DocTokenType>> show = (t) -> {
            // System.out.println(t);
            parser.accept(t);
        };
        try (Reader in = new StringReader(text2)) {
            lexicon.tokenize(in, show);
        }
        Token<DocTokenType> fault = parser.fault();
        if (fault == null) {
            Node<DocTokenType> root = parser.root();
            System.out.println(root);
            var pp = DocTokenType.postprocess(root);
            System.out.printf("%nRestructured: %s%n", pp);
        } else {
            System.out.printf("Expected: %s; got %s%n", parser.expected(),
                              fault);
        }
    }

    private static final Properties ENTITIES = new Properties();

    static {
        try (InputStream in =
            DocTokenType.class.getResourceAsStream("ents.properties")) {
            ENTITIES.load(in);
        } catch (IOException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }
}
