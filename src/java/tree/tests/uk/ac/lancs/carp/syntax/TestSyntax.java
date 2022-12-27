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

package uk.ac.lancs.carp.syntax;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Properties;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.ModuleDefinition;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.TextFile;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.std.BuiltIns;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.syntax.LL1Grammar;
import uk.ac.lancs.syntax.Lexicon;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.syntax.Parser;
import uk.ac.lancs.syntax.TextPosition;
import uk.ac.lancs.syntax.Token;

/**
 * 
 * 
 * @author simpsons
 */
public class TestSyntax {
    private TestSyntax() {}

    /**
     * @param args
     */
    public static void main(String[] args) throws Exception {
        final Lexicon<TokenType> lexicon = new Lexicon<>(TokenType.class);
        final LL1Grammar<TokenType> syntax = new LL1Grammar<>(TokenType.class);

        Map<Token<TokenType>, Token<TokenType>> docComments =
            new IdentityHashMap<>();
        Parser<TokenType> parser = syntax.newParser(TokenType.DECLS);
        KeywordRecognizer keyRec =
            new KeywordRecognizer(parser, KeywordRecognizer
                .mapUpdater(docComments, true));
        CommentAssociator comAss = new CommentAssociator(keyRec, docComments);
        CommentEliminator comElim = new CommentEliminator(comAss);

        try (Reader in = new InputStreamReader(TestSyntax.class
            .getResourceAsStream("example1.rpc"), StandardCharsets.UTF_8)) {
            lexicon.tokenize(in, comElim);
        }

        System.out.printf("  Unprocessed: %s%n", indent(parser.root()));
        Node<TokenType> root = TokenType.postprocess(parser.root());
        System.out.printf("Postprocessed: %s%n", indent(root));
        Token<TokenType> fault = parser.fault();
        if (fault != null) {
            System.out.printf("  Fault: %s%n", fault);
            System.out.printf("  Expected: %s%n", parser.expected());
        } else {
            ModuleDefinition defn = SyntaxTypeFactory
                .parseDefinition(root, (obj, start) -> {}, Thread
                    .currentThread().getContextClassLoader());
            System.out.printf("imports: %s%n", defn.imports);
            System.out.printf("types: %s%n", defn.types);

            /* Simulate the IDL being in the module a.b.c. */
            ExternalName moduleName = ExternalName.parse("a.b.c");
            QualificationContext qctxt = BuiltIns
                .wrap(defn.getQualificationContext(moduleName, null, null));
            ModuleDefinition resolved = defn.qualify(moduleName, qctxt);

            /* Export type definitions as a set of properties. */
            Properties props = new Properties();
            resolved.describe("", props);
            props.store(System.err, "xxx");

            /* Read the types back in. */
            Map<ExternalName, Type> backIn = new HashMap<>();
            final int typeCount =
                Integer.parseInt(props.getProperty("type.count"));
            final ClassLoader impls =
                Thread.currentThread().getContextClassLoader();
            LoadContext loadCtxt = new LoadContext() {
                @Override
                public ClassLoader implementations() {
                    return impls;
                }
            };
            for (int code = 0; code < typeCount; code++) {
                String pfx = "type." + code + ".";
                ExternalName name =
                    ExternalName.parse(props.getProperty(pfx + "name"));
                Type elem = Type.load(pfx, props, loadCtxt);
                backIn.put(name, elem);
            }
            System.out.printf("reconstituted: %s%n", backIn);

            ExpansionContext ctxt = new ExpansionContext() {
                @Override
                public Type getModel(ExternalName idlName) {
                    return resolved.types.get(idlName);
                }

                @Override
                public String getTarget(ExternalName moduleName) {
                    switch (moduleName.toString()) {
                    case "dingle":
                        return "org.example.dingle";

                    case "a.b.c":
                        return "org.example";

                    case "slap.me":
                        return "org.example.slap";

                    default:
                        return null;
                    }
                }

                @Override
                public QualifiedDocumentation getDocs(Object element) {
                    return null;
                }

                @Override
                public void reportBadReference(TextPosition pos, String text,
                                               String detail) {
                    System.err.printf("Bad ref at %s: %s: [%s]%n", pos, detail,
                                      text);
                }
            };
            ctxt = BuiltIns.wrap(ctxt);
            try (TextFile out = new TextFile(System.out, 4, true)) {
                for (Map.Entry<ExternalName, Type> entry : resolved.types
                    .entrySet()) {
                    ExternalName name = entry.getKey();
                    Type elem = entry.getValue();
                    if (!elem.mustBeDefinedInJava()) {
                        System.out.printf("Type %s is dynamic.%n", name);
                        continue;
                    }
                    elem.defineJavaType(out, name, ctxt);
                }
            }
        }
    }

    private static String indent(Node<TokenType> root) {
        return indent("  ", "", root);
    }

    private static String indent(String tab, String pfx, Node<TokenType> root) {
        StringBuilder result = new StringBuilder();
        indent(result, tab, pfx, root);
        return result.toString();
    }

    private static void indent(StringBuilder result, String tab, String pfx,
                               Node<TokenType> root) {
        if (root == null) {
            result.append(pfx).append("null\n");
            return;
        }
        result.append(pfx).append(root.type.toString());
        if (root.isEmpty()) {
            result.append(' ').append(root.text()).append('\n');
        } else {
            result.append(" {\n");
            int c = 0;
            for (Node<TokenType> child : root.children()) {
                indent(result, tab, pfx + c + tab, child);
                c++;
            }
            result.append(pfx).append("}\n");
        }
    }
}
