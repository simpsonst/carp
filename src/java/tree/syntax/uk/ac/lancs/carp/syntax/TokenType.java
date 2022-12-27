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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import uk.ac.lancs.syntax.Epsilon;
import uk.ac.lancs.syntax.Literal;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.syntax.NonTerminal;
import uk.ac.lancs.syntax.Production;
import uk.ac.lancs.syntax.Unmatched;

/**
 * Distinguishes tokens and non-terminals for the IDL syntax. An
 * example:
 * 
 * <pre>
 * // an enumerated type
 * type ship-type &lt;aircraft-carrier battleship cruiser submarine destroyer&gt;;
 *
 * // a structure/record type
 * type coords {
 *   x, y : 1..20; // optional trailing ;
 * };
 * 
 * // an interface type (to be inherited by another, below)
 * type notifiable [
 *   call notify { msg : string };
 * ];
 *
 * // an interface type
 * type board [
 *   inherit notifiable;
 *   call shoot { at : coords }
 *     =&gt; miss
 *     =&gt; hit
 *     =&gt; sink
 *       { kind : ship-type };
 * ];
 * </pre>
 *
 * <p>
 * Names of types, fields, constants, calls and responses must match the
 * grammar of {@link #IDENT}, and are encoded internal as
 * {@link uk.ac.lancs.carp.map.ExternalName}.
 * 
 * <p>
 * A type specification can be a primitive type, a reference type, an
 * enumerated type, a structure type, a map type, a sequence type, or a
 * set type.
 * 
 * <p>
 * A primitive type is an integer type or a real type. An integer type
 * is expressed with
 * <samp><var>signed-int</var>..<var>signed-int</var></samp>, such as
 * <samp>-100..+100</samp>; either bound can be omitted. A real type is
 * expressed with <samp>.<var>int</var></samp>, giving the number of
 * significant decimal digits.
 * 
 * <p>
 * A reference type is simply the name of a type defined elsewhere,
 * matching {@link #IDENT}.
 * 
 * <p>
 * An enumerated type is expressed with
 * <samp>&lt;<var>IDENT</var><var>…</var>&gt;</samp>.
 * 
 * <p>
 * A structure type is expressed with
 * <samp>{<var>param</var><var>…</var>}</samp>, identifying a set of
 * named, typed fields.
 * 
 * <p>
 * A map type is expressed with
 * <samp><var>type</var>-&gt;<var>type</var></samp>, identifying the key
 * type and the value type.
 * 
 * <p>
 * A set type is expressed with <samp>&amp;<var>type</var></samp>,
 * identifying the element type.
 * 
 * <p>
 * A sequence type is expressed with <samp>*<var>type</var></samp>,
 * identifying the element type.
 * 
 * @author simpsons
 */
public enum TokenType {
    /**
     * Matches unrecognized sequences in the IDL input, and should
     * result in a parse error at a later stage.
     */
    @Unmatched
    UNMATCHED,

    /**
     * Indicates end-of-input.
     */
    @Epsilon
    EPSILON,

    /**
     * Matches non-newline white-space characters, including space, tab
     * and backspace.
     */
    @Literal("[ \\t\\x0B]+")
    WHITESPACE,

    /**
     * Matches newline sequences, including CRLF, CR, LF and FF.
     */
    @Literal("\\r\\n|\\r|\\n|\\f")
    NEWLINE,

    /**
     * Matches the start of a document comment. This is a forward slash
     * followed by two asterisks, i.e., <samp>/**</samp>.
     */
    @Literal("/\\*\\*")
    DOC_COMMENT_OPEN,

    /**
     * Matches the start of a multi-line comment. This is a forward
     * slash followed by an asterisk.
     */
    @Literal("/\\*")
    COMMENT_OPEN,

    /**
     * Matches the end of a multi-line comment. This is an asterisk
     * followed by a forward slash.
     */
    @Literal("\\*/")
    COMMENT_CLOSE,

    /**
     * Matches the start of a single-line comment. This is two forward
     * slashes.
     */
    @Literal("//")
    COMMENT,

    /**
     * Identifies a document comment after post-processing.
     */
    DOC_COMMENT,

    /**
     * Identifies a token for the keyword <samp>import</samp>.
     */
    KWD_IMPORT,

    /**
     * Identifies a token for the keyword <samp>as</samp>.
     */
    KWD_AS,

    /**
     * Identifies a token for the keyword <samp>type</samp>.
     */
    KWD_TYPE,

    /**
     * Identifies a token for the keyword <samp>call</samp>.
     */
    KWD_CALL,

    /**
     * Identifies a token for the keyword <samp>inherit</samp>.
     */
    KWD_INHERIT,

    /**
     * Matches an identifier. This consists of an
     * alphanumeric/dash/slash sequence, forbidding a leading digit,
     * dash or slash, a trailing dash/slash, or adjacent dashes/slashes.
     */
    @Literal("\\p{Alpha}\\p{Alnum}*(?:[-/]\\p{Alpha}\\p{Alnum}*)*")
    IDENT,

    /**
     * Matches a qualified identifier, a dot-separated sequence of
     * {@link #IDENT}.
     */
    @Production({ "IDENT", "AFTER_LONG_IDENT" })
    LONG_IDENT,

    /**
     * Completes a qualified identifier, or matches an extension of it.
     */
    @Production({ "DOT", "LONG_IDENT" })
    @Production({ })
    AFTER_LONG_IDENT,

    /**
     * Matches an opening brace.
     */
    @Literal("\\{")
    OPEN_BRACE,

    /**
     * Matches a closing brace.
     */
    @Literal("\\}")
    CLOSE_BRACE,

    /**
     * Matches an opening angle bracket or less-than sign.
     */
    @Literal("<")
    OPEN_ANGLE,

    /**
     * Matches a closing angle bracket or greater-than sign.
     */
    @Literal(">")
    CLOSE_ANGLE,

    /**
     * Matches an opening square bracket.
     */
    @Literal("\\[")
    OPEN_BRACKET,

    /**
     * Matches a closing square bracket.
     */
    @Literal("\\]")
    CLOSE_BRACKET,

    /**
     * Matches an opening round bracket.
     */
    @Literal("\\(")
    OPEN_PAREN,

    /**
     * Matches a closing round bracket.
     */
    @Literal("\\)")
    CLOSE_PAREN,

    /**
     * Matches a dash followed by a greater-than sign.
     */
    @Literal("->")
    ARROW,

    /**
     * Matches an equals sign followed by a greater-than sign.
     */
    @Literal("=>")
    IMPLIES,

    /**
     * Matches an ampersand.
     */
    @Literal("&")
    AMP,

    /**
     * Matches an asterisk.
     */
    @Literal("\\*")
    STAR,

    /**
     * Matches a semicolon.
     */
    @Literal(";")
    SEMICOLON,

    /**
     * Matches a colon.
     */
    @Literal(":")
    COLON,

    /**
     * Matches the opening of an interface type.
     */
    @Literal("#\\{")
    HASH_BRACE,

    /**
     * Matches a hash.
     */
    @Literal("#")
    HASH,

    /**
     * Matches a question mark.
     */
    @Literal("\\?")
    QUERY,

    /**
     * Matches a comma.
     */
    @Literal(",")
    COMMA,

    /**
     * Matches a triple dot.
     */
    @Literal("\\.\\.\\.")
    DOTDOTDOT,

    /**
     * Matches a double dot.
     */
    @Literal("\\.\\.")
    DOTDOT,

    /**
     * Matches a dot or full stop.
     */
    @Literal("\\.")
    DOT,

    /**
     * Matches a string literal.
     */
    @Literal("\"(?:" + "[\\p{Print}&&[^\"\\\\]]" + "|" + "\\\\[\"'nftba\\\\]"
        + "|" + "\\\\u[0-9a-fA-F]{4})*\"")
    STRING_LITERAL,

    /**
     * Matches an integer literal.
     */
    @Literal("(?:0[xX][0-9a-fA-F]+|0[0-7]*|[1-9][0-9]*)")
    INT,

    /**
     * Matches a numeric sign.
     */
    @Literal("[-+]")
    SIGN,

    /**
     * Defines the non-terminal for a signed integer.
     */
    @Production({ "SIGN", "INT" })
    @Production({ "INT" })
    SINT,

    /**
     * Defines the non-terminal for a sequence of declarations.
     */
    @Production({ "KWD_TYPE", "TDECL", "DECLS" })
    @Production({ "KWD_IMPORT", "IMPORT", "DECLS" })
    @Production({ })
    DECLS,

    /**
     * Matches the optional <code>as</code> clause of an import.
     */
    @Production({ "KWD_AS", "IDENT" })
    @Production({ })
    AS_CLAUSE,

    /**
     * Completes an import clause with the identifier being imported, an
     * optional <code>as</code> clause, and a terminating semicolon.
     */
    @Production({ "LONG_IDENT", "AS_CLAUSE", "SEMICOLON" })
    IMPORT,

    /**
     * Completes a type declaration, with its name and a type
     * specification.
     */
    @Production({ "IDENT", "TSPEC", "SEMICOLON" })
    TDECL,

    /**
     * Matches a type specification.
     */
    @Production({ "OPEN_PAREN", "TSPEC", "CLOSE_PAREN", "AFTER_TSPEC" })
    @Production({ "OPEN_BRACE", "PTAIL", "AFTER_TSPEC" })
    @Production({ "OPEN_ANGLE", "ENUM_CONST_LIST", "CLOSE_ANGLE",
        "AFTER_TSPEC" })
    @Production({ "STAR", "TSPEC", "AFTER_TSPEC" })
    @Production({ "AMP", "TSPEC", "AFTER_TSPEC" })
    @Production({ "SINT", "CLOSE_INT_TYPE", "AFTER_TSPEC" })
    @Production({ "DOTDOT", "SINT", "AFTER_TSPEC" })
    @Production({ "DOTDOTDOT", "AFTER_TSPEC" })
    @Production({ "DOT", "AFTER_REAL_TYPE", "AFTER_TSPEC" })
    @Production({ "IDENT", "AFTER_LONG_IDENT", "AFTER_TSPEC" })
    @Production({ "OPEN_BRACKET", "CDECLS", "CLOSE_BRACKET", "AFTER_TSPEC" })
    TSPEC,

    /**
     * Matches optional trailing components of a map type specification.
     */
    @Production({ "ARROW", "TSPEC" })
    @Production({ })
    AFTER_TSPEC,

    /**
     * Completes an integer type specification that already has a lower
     * bound.
     */
    @Production({ "DOTDOTDOT" })
    @Production({ "DOTDOT", "SINT" })
    CLOSE_INT_TYPE,

    /**
     * Completes a real type specification with an optional precision.
     */
    @Production({ "INT" })
    @Production({ })
    AFTER_REAL_TYPE,

    /**
     * Identifies a virtual non-terminal with exactly two children of
     * type {@link #TSPEC}, formed after identifying left-associative
     * structures.
     */
    MAP_SPEC,

    /**
     * Matches a list of identifiers forming the constants of an
     * enumerated type.
     */
    @Production({ "IDENT", "ENUM_CONST_LIST" })
    @Production({ })
    ENUM_CONST_LIST,

    /**
     * Matches a sequence of call declarations or inheritances.
     */
    @Production({ "KWD_CALL", "IDENT", "CDECL", "CDECLS" })
    @Production({ "KWD_INHERIT", "TSPEC", "SEMICOLON", "CDECLS" })
    @Production({ })
    CDECLS,

    /**
     * Determines whether a call declaration has parameters.
     */
    @Production({ "OPEN_BRACE", "PTAIL", "CTAIL" })
    @Production({ "CTAIL" })
    CDECL,

    /**
     * Matches an optional response declaration.
     */
    @Production({ "IMPLIES", "IDENT", "RTAIL" })
    @Production({ "SEMICOLON" })
    CTAIL,

    /**
     * Determines whether a response declaration has parameters.
     */
    @Production({ "OPEN_BRACE", "PTAIL", "CTAIL" })
    @Production({ "CTAIL" })
    RTAIL,

    /**
     * Matches a possibly empty parameter list (in a call or response)
     * or field list (in a structure type), having already matched the
     * opening brace.
     */
    @Production({ "CLOSE_BRACE" })
    @Production({ "SEMICOLON", "PTAIL" })
    PLIST,

    /**
     * Matches an optional parameter/member list.
     */
    @Production({ "CLOSE_BRACE" })
    @Production({ "PARAM", "PLIST" })
    PTAIL,

    /**
     * Matches a single parameter specification. This is a list of
     * parameters of the same type.
     */
    @Production({ "ALIST", "COLON", "TSPEC" })
    PARAM,

    /**
     * Matches a comma-separated argument list, with optionality flags.
     */
    @Production({ "IDENT", "OPT_QUERY", "ATAIL" })
    ALIST,

    /**
     * Matches an optional question mark.
     */
    @Production({ "QUERY" })
    @Production({ })
    OPT_QUERY,

    /**
     * Matches the remainder of an argument list.
     */
    @Production({ "COMMA", "ALIST" })
    @Production({ })
    ATAIL,

    /**
     * Represents a virtual non-terminal made of a sequence of
     * {@link #PLIST}, formed after identifying left-associative
     * structures.
     */
    PARAMS,

    /**
     * Represents a virtual non-terminal made of a sequence of
     * {@link #ALIST}, formed after identifying left-associative
     * structures.
     */
    ARGS,

    ;

    /**
     * Modify a syntax tree to deal with left-associative expressions
     * that can't be expressed in an LL(1) grammar. The following
     * transformations are performed:
     * 
     * <ul>
     * 
     * <li>
     * <p>
     * When a non-terminal's sub-nodes ends with {@link #AFTER_TSPEC},
     * this tail node is removed, and the non-terminal becomes the first
     * child of a {@link #MAP_SPEC}, which replaces it. The
     * left-association process is applied to the second child of the
     * removed tail node, and the result becomes the second child of the
     * replacement node.
     * 
     * </ul>
     * 
     * @param input the input syntax tree
     * 
     * @return a replacement syntax, made largely from the original
     * components
     */
    public static Node<TokenType> postprocess(Node<TokenType> input) {
        if (input == null) return null;
        if (input.isEmpty()) return input;
        Node<TokenType> last = input.child(-1);
        switch (last.type) {
        case AFTER_TSPEC: {
            Node<TokenType> keyType = postprocess(NonTerminal
                .of(input.type, input.start, input.end, input.children(0, -1)));
            if (last.isEmpty()) return keyType;
            Node<TokenType> valueType = postprocess(last.child(1));
            return NonTerminal.of(MAP_SPEC, keyType.start, valueType.end,
                                  Arrays.asList(keyType, valueType));
        }

        case PTAIL:
            if (false) {
                Node<TokenType> first = input.child(0);
                if (last.isEmpty()) return NonTerminal
                    .of(PARAMS, first.start, first.end, Arrays.asList(first));
                Node<TokenType> rem = postprocess(last.child(1));
                assert rem.type == PARAMS;
                List<Node<TokenType>> alt = new ArrayList<>(1 + rem.size());
                alt.add(first);
                alt.addAll(rem.children());
                return NonTerminal.of(PARAMS, first.start, rem.end, alt);
            }

        case ATAIL:
            if (false) {
                Node<TokenType> first = input.child(0);
                if (last.isEmpty()) return NonTerminal
                    .of(ARGS, first.start, first.end, Arrays.asList(first));
                Node<TokenType> rem = postprocess(last.child(1));
                assert rem.type == ARGS;
                List<Node<TokenType>> alt = new ArrayList<>(1 + rem.size());
                alt.add(first);
                alt.addAll(rem.children());
                return NonTerminal.of(ARGS, first.start, rem.end, alt);
            }

        default:
            return NonTerminal.of(input.type, input.start, input.end,
                                  input.children().stream()
                                      .map(TokenType::postprocess)
                                      .collect(Collectors.toList()));
        }
    }
}
