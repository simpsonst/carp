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

package uk.ac.lancs.carp.model.syntax.std;

import java.util.LinkedHashMap;
import java.util.Map;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.std.EnumeratedType;
import uk.ac.lancs.carp.model.syntax.SourceAssociator;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.carp.model.syntax.TokenKey;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.Node;

/**
 * Creates models of enumerated types from syntax trees.
 * 
 * @see EnumeratedType
 *
 * @author simpsons
 */
@Service(SyntaxTypeFactory.class)
@TokenKey({ TokenType.TSPEC, TokenType.OPEN_ANGLE })
public class EnumeratedTypeFactory implements SyntaxTypeFactory {
    @Override
    public EnumeratedType load(Node<TokenType> tree, ClassLoader impls,
                               SourceAssociator srcAssoc) {
        /* Move to the sequence of constant names. */
        tree = tree.child(1);

        /* Collect the constant names to build the type, remembering the
         * tree node which generated each constant, so we can associate
         * them. */
        Map<ExternalName, Node<TokenType>> constants = new LinkedHashMap<>();
        while (tree.type == TokenType.ENUM_CONST_LIST && tree.size() > 0) {
            ExternalName en = ExternalName.parse(tree.child(0).text());
            constants.put(en, tree);
            tree = tree.child(1);
        }
        EnumeratedType result = new EnumeratedType(constants.keySet());

        /* If an associator has been specified, associate a unique key
         * for each constant with its source node. */
        for (var entry : constants.entrySet()) {
            Object id = result.getKey(entry.getKey());
            Node<TokenType> node = entry.getValue();
            srcAssoc.associate(id, node);
        }

        return result;
    }
}
