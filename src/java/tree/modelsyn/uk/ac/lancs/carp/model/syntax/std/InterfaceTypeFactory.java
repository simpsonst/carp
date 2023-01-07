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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.std.CallSpecification;
import uk.ac.lancs.carp.model.std.InterfaceType;
import uk.ac.lancs.carp.model.std.ResponseSpecification;
import uk.ac.lancs.carp.model.std.StructureType;
import uk.ac.lancs.carp.model.syntax.SourceAssociator;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.carp.model.syntax.TokenKey;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.Node;

/**
 * Generates models of interface types from syntax trees.
 * 
 * @see InterfaceType
 *
 * @author simpsons
 */
@Service(SyntaxTypeFactory.class)
@TokenKey({ TokenType.TSPEC, TokenType.OPEN_BRACKET })
public class InterfaceTypeFactory implements SyntaxTypeFactory {
    @Override
    public InterfaceType load(Node<TokenType> tree, ClassLoader impls,
                              SourceAssociator srcAssoc) {
        assert tree.type == TokenType.TSPEC;
        assert tree.child(0).type == TokenType.OPEN_BRACKET;
        tree = tree.child(1);
        Map<ExternalName, CallSpecification> calls = new LinkedHashMap<>();
        Collection<Type> ancestors = new LinkedHashSet<>();
        assert tree.type == TokenType.CDECLS;
        while (!tree.isEmpty()) {
            Node<TokenType> source = tree.child(0);
            switch (source.type) {
            case KWD_CALL:
                /* Extract the call name and tree, and get ready for the
                 * next call. */
                ExternalName callName =
                    ExternalName.parse(tree.child(1).text());
                Node<TokenType> call = tree.child(2);
                tree = tree.child(3);

                assert call.type == TokenType.CDECL;
                Node<TokenType> resps;
                final StructureType parameters;
                if (call.size() > 1) {
                    /* We have an input parameter list. */
                    resps = call.child(2);
                    parameters = StructureTypeFactory.loadTail(call.child(1),
                                                               impls, srcAssoc);
                } else {
                    /* There are no inputs. */
                    resps = call.child(0);
                    parameters = new StructureType(Collections.emptyMap());
                }

                /* Parse the responses. */
                assert resps.type == TokenType.CTAIL;
                Map<ExternalName, ResponseSpecification> responses =
                    new LinkedHashMap<>();
                while (resps.size() > 1) {
                    Node<TokenType> rsrc = resps.child(1);
                    ExternalName rn = ExternalName.parse(rsrc.text());
                    resps = resps.child(2);
                    assert resps.type == TokenType.RTAIL;
                    final StructureType rparms;
                    if (resps.size() > 1) {
                        /* We have a response parameter list. */
                        rparms = StructureTypeFactory.loadTail(resps.child(1),
                                                               impls, srcAssoc);
                        resps = resps.child(2);
                    } else {
                        /* No response parameters are specified. */
                        rparms = new StructureType(Collections.emptyMap());
                        resps = resps.child(0);
                    }
                    ResponseSpecification rspec =
                        new ResponseSpecification(rparms);
                    responses.put(rn, rspec);
                    srcAssoc.associate(rspec, rsrc);
                }

                /* Build the call specification with the gathered
                 * components. */
                CallSpecification callSpec =
                    new CallSpecification(parameters, responses);
                calls.put(callName, callSpec);
                srcAssoc.associate(callSpec, source);
                break;

            case KWD_INHERIT:
                /* Extract the inherited type tree, and get ready for
                 * the next call. */
                Node<TokenType> ref = tree.child(1);
                assert tree.child(2).type == TokenType.SEMICOLON;
                tree = tree.child(3);

                /* Parse the ancestor type, and store it. */
                Type refType = SyntaxTypeFactory.loadType(ref, impls, srcAssoc);
                ancestors.add(refType);
                break;

            default:
                throw new AssertionError("unreachable; type="
                    + tree.child(0).type);
            }
        }

        return new InterfaceType(calls, ancestors);
    }
}
