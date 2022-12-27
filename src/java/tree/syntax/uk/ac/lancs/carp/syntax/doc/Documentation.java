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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.syntax.TextPosition;

/**
 * Holds the parsed content of a documentation comment.
 * 
 * @author simpsons
 */
public final class Documentation implements Content {
    private final TextPosition start;

    private final TextPosition end;

    /**
     * Holds the immutable content of the body of this documentation.
     */
    public final List<Content> body;

    /**
     * Holds the immutable set of block tags indexed by tag name.
     */
    public final Map<String, List<List<Content>>> blockTags;

    /**
     * Create a parsed document comment. The supplied body and tag
     * content will be copied into an immutable form.
     * 
     * @param start the start position of the comment in source
     * 
     * @param end the end position of the comment in source
     * 
     * @param body the body of the comment
     * 
     * @param blockTags the set of block tags, grouped and indexed by
     * tag name
     */
    public Documentation(TextPosition start, TextPosition end,
                         List<? extends Content> body,
                         Map<? extends String,
                             ? extends List<? extends List<? extends Content>>> blockTags) {
        this.start = start;
        this.end = end;
        this.body = List.copyOf(body);
        this.blockTags = blockTags.entrySet().stream()
            .collect(Collectors
                .toMap(Map.Entry::getKey,
                       e -> e.getValue().stream()
                           .<List<Content>>map(i -> List.copyOf(i))
                           .collect(Collectors.toList())));
    }

    /**
     * Create documentation from a syntax tree. The root is expected to
     * be a {@link DocTokenType#DOCUMENT} processed with
     * {@link DocTokenType#postprocess(Node)}, with initial content
     * interpretable by {@link Content#parse(java.util.ListIterator)},
     * followed by one {@link DocTokenType#BLOCK_TAG} per block tag.
     * 
     * @param root the syntax tree
     * 
     * @return the structured documentation
     */
    public static Documentation parse(Node<DocTokenType> root) {
        List<? extends Node<DocTokenType>> input = root.children();
        var iter = input.listIterator();
        List<Content> body = Content.parse(iter);
        Map<String, List<List<Content>>> content = new HashMap<>();
        while (iter.hasNext()) {
            var n = iter.next();
            if (n.type != DocTokenType.BLOCK_TAG) continue;
            String name = n.child(0).text();
            List<Content> value = Content.parse(n.children(1).listIterator());
            content.computeIfAbsent(name, k -> new ArrayList<>()).add(value);
        }
        return new Documentation(input.get(0).start,
                                 input.get(input.size() - 1).end, body,
                                 content);
    }

    /**
     * {@inheritDoc} This implementation invokes
     * {@link ContentVisitor#acceptDocumentation(Documentation)} on the
     * argument, passing itself as an argument.
     */
    @Override
    public <R> R visit(ContentVisitor<? extends R> visitor) {
        return visitor.acceptDocumentation(this);
    }

    @Override
    public TextPosition start() {
        return start;
    }

    @Override
    public TextPosition end() {
        return end;
    }
}

