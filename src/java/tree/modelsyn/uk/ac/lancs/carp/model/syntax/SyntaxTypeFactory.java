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

package uk.ac.lancs.carp.model.syntax;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.DefinitionError;
import uk.ac.lancs.carp.model.ModuleDefinition;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.syntax.Node;

/**
 * Knows how to load a type specification from a syntax tree. Such a
 * factory should be annotated with {@link TokenKey} as many times as
 * necessary to recognize syntax trees that it can process. The argument
 * of this annotation is a sequence of token types, the first being the
 * root of the tree being parsed, and each subsequent being the first
 * child of the previous.
 * 
 * @author simpsons
 */
public interface SyntaxTypeFactory {
    /**
     * Parse a syntax tree as an interface definition.
     *
     * @param tree the root of the syntax tree
     *
     * @param srcAssoc an agent to associate generated elements with
     * parsed nodes
     *
     * @param impls the class loader to search for implementations
     *
     * @return a collection of type definitions and imports based on the
     * supplied tree
     *
     * @throws IllegalArgumentException if the root of the tree is not a
     * TokenTYpe#DECLS
     *
     * @throws DefinitionError if there is a problem with the syntax,
     * er, or something
     * 
     * @constructor
     */
    public static ModuleDefinition parseDefinition(Node<TokenType> tree,
                                                   SourceAssociator srcAssoc,
                                                   ClassLoader impls) {
        if (tree.type != TokenType.DECLS)
            throw new IllegalArgumentException("bad tree root type"
                + tree.type);
        final Node<TokenType> top = tree;
        Map<ExternalName, ExternalName> imports = new HashMap<>();
        Map<ExternalName, Type> types = new HashMap<>();
        while (!tree.isEmpty()) {
            switch (tree.child(0).type) {
            case KWD_IMPORT:
                ExternalName name =
                    ExternalName.parse(tree.child(1).child(0).text());
                Node<TokenType> asClause = tree.child(1).child(1);
                final ExternalName alias;
                if (asClause.isEmpty()) {
                    /* Use the leaf of the full name as the alias. */
                    alias = name.getLeaf();
                } else {
                    /* The alias is explicit. */
                    alias = ExternalName.parse(asClause.child(1).text());
                }
                tree = tree.child(2);
                // TODO: Check whether the alias has been defined.
                imports.put(alias, name);
                break;

            case KWD_TYPE:
                /* Parse the type, and add a type definition to the
                 * map. */
                ExternalName typeName =
                    ExternalName.parse(tree.child(1).child(0).text());
                Node<TokenType> typeSpec = tree.child(1).child(1);
                Type type =
                    SyntaxTypeFactory.loadType(typeSpec, impls, srcAssoc);
                if (type == null)
                    throw new DefinitionError("unknown type specification "
                        + typeSpec);
                types.put(typeName, type);
                srcAssoc.associate(type, tree);
                tree = tree.child(2);
                break;

            default:
                throw new IllegalArgumentException("unexpected first child "
                    + tree);
            }
        }
        ModuleDefinition def = ModuleDefinition.define(imports, types);
        srcAssoc.associate(def, top);
        return def;
    }

    /**
     * Load a type specification from a syntax tree.
     * 
     * @param tree the syntax tree
     * 
     * @param impls the class loader to search for implementations
     *
     * @param srcAssoc an agent to associate generated elements with
     * parsed nodes
     * 
     * @return the type specification; or {@code null} if not recognized
     */
    Type load(Node<TokenType> tree, ClassLoader impls,
              SourceAssociator srcAssoc);

    /**
     * Get a list of annotations specifying sequences of tokens that an
     * type factory should match. This method exists to deal with
     * classes annotated with {@link TokenKey} multiply or singly.
     * 
     * @param fact the factory whose annotations are sought
     * 
     * @return a list of annotations, possibly empty
     */
    private static List<TokenKey> getTokenKeys(SyntaxTypeFactory fact) {
        TokenKey got = fact.getClass().getAnnotation(TokenKey.class);
        if (got != null) return Collections.singletonList(got);
        TokenKeys gots = fact.getClass().getAnnotation(TokenKeys.class);
        if (gots == null) return Collections.emptyList();
        return Arrays.asList(gots.value());
    }

    /**
     * Load a type specification from a syntax tree.
     * {@link SyntaxTypeFactory#getFactory(Node, ClassLoader)} is used
     * to get the appropriate factory, and then
     * {@link SyntaxTypeFactory#load(Node, ClassLoader, SourceAssociator)}
     * is invoked on that.
     * 
     * @param root the root of the tree
     * 
     * @param impls the class loader to search for implementations
     * 
     * @param srcAssoc an agent to associate generated elements with
     * parsed nodes
     * 
     * @return the type specification; or {@code null} if not recognized
     * 
     * @constructor
     */
    public static Type loadType(Node<TokenType> root, ClassLoader impls,
                                SourceAssociator srcAssoc) {
        SyntaxTypeFactory fact = SyntaxTypeFactory.getFactory(root, impls);
        if (fact == null) return null;
        return fact.load(root, impls, srcAssoc);
    }

    /**
     * Get the type factory suitable for parsing a syntax tree with a
     * root whose first child is of a given type. {@link TokenType}
     * annotations on the class are checked to identify sequences of
     * tokens that must match the sequence formed from the root,
     * followed by its first child, followed by <em>its</em> first
     * child, etc.
     * 
     * @param root the root of the syntax tree to parseDefinition
     * 
     * @param loader the class loader to search for implementations
     * 
     * @return the matching factory; or {@code null} if not recognized
     */
    static SyntaxTypeFactory getFactory(final Node<TokenType> root,
                                        ClassLoader loader) {
        for (var fact : ServiceLoader.load(SyntaxTypeFactory.class, loader)) {
            List<TokenKey> keys = getTokenKeys(fact);
            next_pattern: for (TokenKey got : keys) {
                if (got == null) continue;
                Node<TokenType> cursor = root;
                for (TokenType tt : got.value()) {
                    if (cursor == null || tt != cursor.type)
                        continue next_pattern;
                    cursor = cursor.child(0);
                }
                return fact;
            }
        }
        return null;
    }
}
