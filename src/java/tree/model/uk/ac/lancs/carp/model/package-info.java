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

/**
 * Defines the generic part of the type model for CARP. This package is
 * intended for CARP developers; regular users of CARP should not need
 * an understanding of this package.
 * 
 * <p>
 * This package centres around {@link Type}, which models a node in a
 * type hierarchy. Its methods allow a node to store itself in a
 * {@link java.util.Properties}, to be expressed as in-line Java code or
 * as a class definition, to resolve local IDL names into global ones
 * (as defined by imports), and to generate codecs between Java types
 * and JSON representations. Nodes are generated either by a
 * {@link CompiledTypeFactory} from previously stored
 * {@link java.util.Properties}, or by a
 * {@link uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory} from a syntax
 * tree.
 * 
 * <p>
 * The first use of {@link Type} is to generate a node hierarchy from a
 * parsed IDL file during compilation (specifically, annotation
 * processing), using
 * {@link uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory#loadType(uk.ac.lancs.syntax.Node, ClassLoader, uk.ac.lancs.carp.model.syntax.SourceAssociator)}.
 * Additional referenced types defined during antecedent compilations
 * may be loaded from the compilation class path using
 * {@link CompiledTypeFactory#load(java.util.Properties,String,LoadContext)}.
 * After some recursive transformations are applied, the node hierarchy
 * is used to generate source for the regular user (Java interface
 * types, for example, that the user wishes to implement or invoke),
 * using
 * {@link Type#defineJavaType(TextFile,uk.ac.lancs.carp.map.ExternalName,ExpansionContext)},
 * and {@link Type#declareJava(boolean,boolean,ExpansionContext)}. It is
 * simultaneously used to generate {@link java.util.Properties} files
 * among the output of the compiled sources, using
 * {@link Type#describe(String,java.util.Properties)}.
 * 
 * <p>
 * At execution time, generated Java properties are available through
 * various {@link java.lang.ClassLoader}s, and are used to re-constitute
 * node hierarchies, using
 * {@link CompiledTypeFactory#load(java.util.Properties,String,LoadContext)}.
 * Run-time node hierarchies are used to generate
 * {@link uk.ac.lancs.carp.codec.Encoder}s and
 * {@link uk.ac.lancs.carp.codec.Decoder}s, which are in turn used to
 * convert between Java argument lists and JSON structures.
 * 
 * <p>
 * This package only defines the framework for node hierarchies. Actual
 * node implementations are in {@link uk.ac.lancs.carp.model.std}. They
 * are hooked in as implementations of {@link CompiledTypeFactory}, and
 * found using {@link java.util.ServiceLoader}. Additionally, annotation
 * {@link TypeKey} identifies parts of {@link java.util.Properties} that
 * should be passed to
 * {@link CompiledTypeFactory#load(java.util.Properties,String,LoadContext)}.
 * 
 * @author simpsons
 */
package uk.ac.lancs.carp.model;

