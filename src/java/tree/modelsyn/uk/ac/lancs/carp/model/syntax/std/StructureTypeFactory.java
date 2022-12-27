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

package uk.ac.lancs.carp.model.syntax.std;

import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.std.Member;
import uk.ac.lancs.carp.model.std.StructureType;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.carp.model.syntax.TokenKey;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.carp.model.syntax.SourceAssociator;

/**
 * Creates models of structure types from syntax trees.
 * 
 * @see StructureType
 *
 * @author simpsons
 */
@Service(SyntaxTypeFactory.class)
@TokenKey({ TokenType.TSPEC, TokenType.OPEN_BRACE })
public class StructureTypeFactory implements SyntaxTypeFactory {
    @Override
    public StructureType load(Node<TokenType> tree, ClassLoader impls,
                              SourceAssociator srcAssoc) {
        assert tree.type == TokenType.TSPEC;
        assert tree.child(0).type == TokenType.OPEN_BRACE;
        Node<TokenType> list = tree.child(1);
        return StructureTypeFactory.loadTail(list, impls, srcAssoc);
    }

    /**
     * Parse a {@link TokenType#PTAIL} into a structure type
     * specification.
     * 
     * @constructor
     * 
     * @param list the root of the tree to parseDefinition; a
     * {@link TokenType#PTAIL}
     * 
     * @param impls the class loader to search for implementations
     *
     * @param srcAssoc an agent to associate generated elements with
     * parsed nodes
     * 
     * @return the parsed structure
     * 
     * @throws AssertionError if an unexpected token type is encountered
     */
    static StructureType loadTail(Node<TokenType> list, ClassLoader impls,
                                  SourceAssociator srcAssoc) {
        Map<ExternalName, Member> members = new LinkedHashMap<>();
        while (list.size() >= 2) {
            assert list.type == TokenType.PTAIL;

            /* Get the parameter's type. */
            Node<TokenType> typeNode = list.child(0).child(2);
            Type type = SyntaxTypeFactory.loadType(typeNode, impls, srcAssoc);

            /* Iterate over the parameter names. */
            Node<TokenType> alist = list.child(0).child(0);
            while (true) {
                assert alist.type == TokenType.ALIST;

                /* Pick out the parameter name and optional flag. */
                ExternalName en = ExternalName.parse(alist.child(0).text());
                boolean optional = !alist.child(1).isEmpty();
                Member memb =
                    optional ? Member.optional(type) : Member.required(type);
                members.put(en, memb);

                /* Move to the next parameter name. */
                alist = alist.child(2);
                assert alist.type == TokenType.ATAIL;
                if (alist.isEmpty()) break;
                alist = alist.child(1);
            }

            /* Move to the next parameter. */
            list = list.child(1);
            if (list.size() < 2) break;
            list = list.child(1);
        }
        return new StructureType(members);
    }
}
