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

import java.math.BigInteger;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.carp.model.syntax.TokenKey;
import uk.ac.lancs.carp.model.std.IntegerType;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.carp.model.syntax.SourceAssociator;

/**
 * Creates models of integer types from syntax trees.
 * 
 * @see IntegerType
 *
 * @author simpsons
 */
@Service(SyntaxTypeFactory.class)
@TokenKey({ TokenType.TSPEC, TokenType.SINT })
@TokenKey({ TokenType.TSPEC, TokenType.DOTDOT })
@TokenKey({ TokenType.TSPEC, TokenType.DOTDOTDOT })
public class IntegerTypeFactory implements SyntaxTypeFactory {
    @Override
    public IntegerType load(Node<TokenType> tree, ClassLoader impls, SourceAssociator srcAssoc) {
        assert tree.type == TokenType.TSPEC;

        final BigInteger min, max;
        switch (tree.child(0).type) {
        case SINT:
            min = new BigInteger(tree.child(0).text());
            assert tree.child(1).type == TokenType.CLOSE_INT_TYPE;
            switch (tree.child(1).child(0).type) {
            case DOTDOTDOT:
                max = null;
                break;

            case DOTDOT:
                max = new BigInteger(tree.child(1).child(1).text());
                break;

            default:
                throw new AssertionError("unreachable");
            }
            break;

        case DOTDOT:
            min = null;
            max = new BigInteger(tree.child(1).text());
            break;

        case DOTDOTDOT:
            min = null;
            max = null;
            break;

        default:
            throw new AssertionError("unreachable");
        }

        return new IntegerType(min, max);
    }
}
