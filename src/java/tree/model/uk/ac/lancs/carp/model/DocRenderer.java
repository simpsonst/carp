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

package uk.ac.lancs.carp.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.syntax.doc.ClosingMarkupTag;
import uk.ac.lancs.carp.syntax.doc.Content;
import uk.ac.lancs.carp.syntax.doc.ContentVisitor;
import uk.ac.lancs.carp.syntax.doc.Documentation;
import uk.ac.lancs.carp.syntax.doc.EntityReference;
import uk.ac.lancs.carp.syntax.doc.InlineTag;
import uk.ac.lancs.carp.syntax.doc.OpeningMarkupTag;
import uk.ac.lancs.carp.syntax.doc.TagAttr;
import uk.ac.lancs.carp.syntax.doc.TextContent;

/**
 * Outputs a documentation comment with a given context.
 *
 * @author simpsons
 */
public final class DocRenderer implements ContentVisitor<Void> {
    private final TextFile out;

    private final ExpansionContext expCtxt;

    private final DocContext ctxt;

    /**
     * Create a documentation renderer.
     * 
     * @param ctxt the context specifying which tags to relay and how to
     * qualify local references
     * 
     * @param expCtxt a context for expanding references to other types
     * 
     * @param out the destination for writing the comment, which should
     * be in documentation-comment mode
     */
    public DocRenderer(TextFile out, DocContext ctxt,
                       ExpansionContext expCtxt) {
        this.out = out;
        this.ctxt = ctxt;
        this.expCtxt = expCtxt;
    }

    private boolean inAttr = false;

    /**
     * {@inheritDoc}
     * 
     * @default Special sequences are sought in the text. Text between
     * them is written literally, while these sequences are
     * HTML-escaped. The special characters/sequences and their
     * replacements include the following:
     * 
     * <ul>
     * 
     * <li><samp>&#42;/</samp> is replaced with <samp>&amp;#42;/</samp>
     * to prevent the enclosing comment from being inadvertently closed.
     * 
     * <li><samp>&#64;</samp> is replaced with <samp>&amp;#64;</samp> to
     * prevent it from being misinterpreted as a block tag.
     * 
     * <li><samp>&#123;</samp> is replaced with <samp>&amp;#123;</samp>
     * to avoid it being misinterpreted as the start of an in-line tag.
     * 
     * <li><samp>&#125;</samp> is replaced with <samp>&amp;#125;</samp>
     * to avoid it being misinterpreted as the end of an in-line tag.
     * 
     * <li><samp>&lt;</samp> is replaced with <samp>&amp;lt;</samp>.
     * 
     * <li><samp>&gt;</samp> is replaced with <samp>&amp;gt;</samp>.
     * 
     * <li><samp>"</samp> inside an HTML attribute is replaced with
     * <samp>&amp;quot;</samp>.
     * 
     * <li><samp>&amp;</samp> is replaced with <samp>&amp;amp;</samp>.
     * 
     * </ul>
     */
    @Override
    public Void acceptText(TextContent content) {
        Pattern specials = Pattern.compile("\\*/|\\{|\\}|@|\"|<|>|\\&");
        int last = 0;
        String text = content.text;
        Matcher m = specials.matcher(text);
        while (m.find(last)) {
            out.format("%s", text.substring(last, m.start()));
            switch (m.group()) {
            case "*/":
                out.format("&#42;/");
                break;

            case "{":
                out.format("&#123;");
                break;

            case "\"":
                if (inAttr)
                    out.format("&quot;");
                else
                    out.format("\"");
                break;

            case "}":
                out.format("&#125;");
                break;

            case "<":
                out.format("&lt;");
                break;

            case ">":
                out.format("&gt;");
                break;

            case "&":
                out.format("&amp;");
                break;

            case "@":
                out.format("&#64;");
                break;

            default:
                throw new AssertionError("unreachable");
            }
            last = m.end();
        }
        out.format("%s", text.substring(last));
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default An opening or self-closing HTML tag is written,
     * including its attributes.
     */
    @Override
    public Void acceptOpeningMarkup(OpeningMarkupTag content) {
        assert !inAttr : "markup in attribute";
        out.format("<%s", content.name);
        inAttr = true;
        for (TagAttr attr : content.attributes) {
            out.format("%s", attr.name);
            if (attr.value != null) {
                out.format("=\"");
                for (Content ct : attr.value)
                    ct.visit(this);
                out.format("\"");
            }
        }
        inAttr = false;
        if (content.selfClosing) out.format(" /");
        out.format(">");
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default <samp>&lt;/<var>name</var>&gt;</samp> is written.
     */
    @Override
    public Void acceptClosingMarkup(ClosingMarkupTag content) {
        out.format("</%s>", content.name);
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation writes
     * <samp>&#123;&#64;<var>name</var> <var>content</var>&#125</samp>
     * for most in-line tags. However, if the tag name is
     * <samp>link</samp> or <samp>linkplain</samp>, the first word of
     * the content is separated, parsed as a {@link DocRef}, qualified
     * using the documentation context, and converted to a JavaDoc link
     * using {@link ExpansionContext#linkJavaDoc(DocRef)}.
     */
    @Override
    public Void acceptInlineTag(InlineTag content) {
        switch (content.name) {
        case "true":
        case "false":
        case "null":
            out.format("{@code %s}", content.name);
            return null;

        case "arg": {
            final List<Content> rem = new ArrayList<>(content.content.size());
            String firstWord = firstWord(rem, content.content);
            ExternalName en = ExternalName.parse(firstWord);
            out.format("{@code %s}", en.asJavaMethodName());
            return null;
        }

        }

        out.format("{@%s", content.name);
        final List<Content> rem;
        switch (content.name) {
        case "link":
        case "linkplain": {
            rem = new ArrayList<>(content.content.size());
            String firstWord = firstWord(rem, content.content);
            DocRef ref = new DocRef(firstWord);
            ref = ctxt.resolveReference(ref);
            String link = expCtxt.linkJavaDoc(ref);
            if (link == null) {
                out.format(" error:%s", firstWord);
                expCtxt.reportBadReference(content.start(), firstWord,
                                           "type not found");
            } else {
                out.format(" %s", link);
            }
            break;
        }

        default:
            rem = content.content;
            break;
        }
        if (!rem.isEmpty()) out.format(" ");
        for (Content cnt : rem)
            cnt.visit(this);
        out.format("}");
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation simply writes
     * <samp>&amp;<var>name</var>;</samp>, giving the name of the
     * referenced entity.
     */
    @Override
    public Void acceptEntityReference(EntityReference content) {
        out.format("&%s;", content.name);
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation first visits all content in the body
     * of the documentation. Then, for each block tag, use the
     * documentation context to determine whether the tag should be
     * rendered, and what as. When a <samp>&#64;see</samp> tag is
     * rendered, the first word of the content is separated out, parsed
     * as a {@link DocRef}, qualified using the documentation context,
     * and converted to a JavaDoc reference using
     * {@link ExpansionContext#linkJavaDoc(DocRef)}.
     */
    @Override
    public Void acceptDocumentation(Documentation content) {
        /* Render the body. */
        for (var cnt : content.body)
            cnt.visit(this);

        /* Render each block tag if acceptable. */
        for (var entry : content.blockTags.entrySet()) {
            final String inTagName = entry.getKey();
            for (List<Content> tag : entry.getValue()) {
                final String tagName = ctxt.includeTag(inTagName);
                if (tagName == null) continue;

                out.format("%n@%s", tagName);
                final List<Content> rem;
                if (inTagName.equals("see")) {
                    rem = new ArrayList<>(tag.size());
                    String firstWord = firstWord(rem, tag);
                    DocRef ref = new DocRef(firstWord);
                    ref = ctxt.resolveReference(ref);
                    String link = expCtxt.linkJavaDoc(ref);
                    if (link == null) {
                        out.format(" error:%s", firstWord);
                        expCtxt.reportBadReference(content.start(), firstWord,
                                                   "type not found");
                    } else {
                        out.format(" %s", link);
                    }
                } else {
                    rem = tag;
                }
                if (!rem.isEmpty()) out.format(" ");
                for (Content cnt : rem)
                    cnt.visit(this);
                out.format("%n");
            }
        }
        out.format("%n");
        return null;
    }

    /**
     * Visit content items in a sequence, with a prefix if non-empty.
     * 
     * @param seq the sequence of items to visit
     * 
     * @param fmt a string to prefix the visitations if the sequence is
     * non-empty
     * 
     * @param args arguments corresponding to format specifiers in the
     * prefix string
     */
    public void visit(Collection<? extends Content> seq, String fmt,
                      Object... args) {
        if (!seq.isEmpty()) out.format(fmt, args);
        for (Content cnt : seq)
            cnt.visit(this);
    }

    /**
     * Visit content items in a sequence.
     * 
     * @param seq the sequence of items to visit
     */
    public void visit(Collection<? extends Content> seq) {
        for (Content cnt : seq)
            cnt.visit(this);
    }

    /**
     * Split content into the first space-delimited word followed by
     * remaining content.
     * 
     * @param rem Remaining content is to be added to this list.
     * 
     * @param input the content to be parsed
     * 
     * @return the first word; or {@code null} if the first piece of
     * content is not plain text
     */
    public static String firstWord(List<? super Content> rem,
                                   List<? extends Content> input) {
        if (input.isEmpty()) return null;
        Content first = input.get(0);
        return first.visit(new ContentVisitor<String>() {
            @Override
            public String acceptText(TextContent content) {
                String result = content.firstWord(rem);
                rem.addAll(input.subList(1, input.size()));
                return result;
            }

            @Override
            public String acceptOpeningMarkup(OpeningMarkupTag content) {
                rem.addAll(input);
                return null;
            }

            @Override
            public String acceptClosingMarkup(ClosingMarkupTag content) {
                rem.addAll(input);
                return null;
            }

            @Override
            public String acceptInlineTag(InlineTag content) {
                rem.addAll(input);
                return null;
            }

            @Override
            public String acceptEntityReference(EntityReference content) {
                rem.addAll(input);
                return null;
            }

            @Override
            public String acceptDocumentation(Documentation content) {
                rem.addAll(input);
                return null;
            }
        });
    }
}
