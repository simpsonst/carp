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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import uk.ac.lancs.syntax.Node;

/**
 * Provides parsed content from a documentation comment.
 * 
 * @author simpsons
 */
public interface Content extends SourceMaterial {
    /**
     * Visit this content.
     * 
     * @param <R> the return type
     * 
     * @param visitor an object to be invoked using a distinct method
     * for each content type
     * 
     * @return the value returned by the call to the visitor
     */
    <R> R visit(ContentVisitor<? extends R> visitor);

    /**
     * Parse a sequence of tokens and non-terminals as content. The
     * input must be the output of
     * {@link DocTokenType#postprocess(Node)}. The following node types
     * are recognized:
     * 
     * <ul>
     * 
     * <li>
     * <p>
     * An {@link DocTokenType#INLINE_TAG} is expected to being with a
     * token specifying the inline tag name. The remaining nodes are
     * parsed recursively. An {@link InlineTag} is generated from these.
     * 
     * <li>
     * <p>
     * A {@link DocTokenType#MARKUP_TAG} is expected to begin with an
     * {@link DocTokenType#ELEMENT_NAME} (providing the tag name) if it
     * is an opening or self-closing HTML tag, or with a
     * {@link DocTokenType#MARKUP_SLASH} if it is a closing HTML tag
     * (and the next token provides the tag name). For an opening tag,
     * the tag name is followed by multiple {@link DocTokenType#ATTR}s,
     * each defining an attribute. A self-closing tag ends with a
     * {@link DocTokenType#MARKUP_SLASH}. An {@link OpeningMarkupTag} or
     * {@link ClosingMarkupTag} is generated.
     * 
     * <li>
     * <p>
     * A {@link DocTokenType#TEXT} token specifies literal text, and a
     * {@link TextContent} is generated from it.
     * 
     * <li>
     * <p>
     * An {@link DocTokenType#ENT_REF} token specifies the name of an
     * unrecognized entity reference, and an {@link EntityReference} is
     * generated from it.
     * 
     * </ul>
     * 
     * <p>
     * Iteration stops early when an unknown node type is encountered.
     * The iterator is then rewound so that the unrecognized node will
     * be next.
     * 
     * @param iter an iterator over the tokens and non-terminals
     * 
     * @return a list of content elements generated from elements
     * consumed from the iterator
     */
    public static List<Content>
        parse(ListIterator<? extends Node<DocTokenType>> iter) {
        List<Content> result = new ArrayList<>();
        while (iter.hasNext()) {
            var n = iter.next();
            switch (n.type) {
            case INLINE_TAG: {
                String name = n.child(0).text();
                List<Content> ct = parse(n.children(1).listIterator());
                result.add(new InlineTag(n.start, n.end, name, ct));
                break;
            }

            case MARKUP_TAG:
                switch (n.child(0).type) {
                case ELEMENT_NAME: {
                    /* This is a starting, empty or self-closing tag. */
                    String name = n.child(0).text();
                    List<TagAttr> ats = new ArrayList<>();
                    boolean closing = false;
                    for (var c : n.children(1)) {
                        if (c.type == DocTokenType.MARKUP_SLASH) closing = true;
                        if (c.type != DocTokenType.ATTR) continue;
                        String atname = c.child(0).text();
                        List<Content> value =
                            parse(c.child(1).children().listIterator());
                        TagAttr at = new TagAttr(c.child(1).start,
                                                 c.child(1).end, atname, value);
                        ats.add(at);
                    }
                    result.add(new OpeningMarkupTag(n.start, n.end, closing,
                                                    name, ats));
                    break;
                }

                case MARKUP_SLASH:
                    /* This is a closing tag. */
                    result.add(new ClosingMarkupTag(n.start, n.end,
                                                    n.child(0).text()));
                    break;
                }
                break;

            case TEXT:
                result.add(new TextContent(n.start, n.end, n.text()));
                break;

            case ENT_REF:
                result.add(new EntityReference(n.start, n.end, n.text()));
                break;

            default:
                iter.previous();
                return result;
            }
        }
        return result;
    }

    /**
     * Trim leading white space from this content.
     * 
     * @return this content with leading white space removed; or
     * {@code null} if trimming results in empty content
     * 
     * @default The default behaviour is to return this object.
     */
    default Content trimLeadingWhiteSpace() {
        return this;
    }

    /**
     * Trim trailing white space from this content.
     * 
     * @return this content with trailing white space removed; or
     * {@code null} if trimming results in empty content
     * 
     * @default The default behaviour is to return this object.
     */
    default Content trimTrailingWhiteSpace() {
        return this;
    }

    /**
     * Trim leading and trailing white space from a content sequence
     * 
     * @param input the sequence of content to be trimmed
     * 
     * @return the trimmed sequence
     */
    public static List<Content>
        trimWhiteSpace(Collection<? extends Content> input) {
        /* Create a mutable copy of the input. It will be modified and
         * then returned as the result. */
        List<Content> result = new ArrayList<>(input);

        /* Trim leading space from the first element. If it becomes
         * empty, remove it, and check the new first element. */
        while (!result.isEmpty()) {
            Content alt = result.get(0).trimLeadingWhiteSpace();
            if (alt != null) {
                result.set(0, alt);
                break;
            }
            result.remove(0);
        }

        /* Reverse the sequence, and repeatedly trim the trailing space
         * from the last (now first) element as before. Re-reverse the
         * sequence. */
        Collections.reverse(result);
        while (!result.isEmpty()) {
            Content alt = result.get(0).trimTrailingWhiteSpace();
            if (alt != null) {
                result.set(0, alt);
                break;
            }
            result.remove(0);
        }
        Collections.reverse(result);

        return result;
    }
}
