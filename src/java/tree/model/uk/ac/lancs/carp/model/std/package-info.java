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
 * Defines the standard set of model elements.
 * 
 * <p>
 * The following elements return {@code true} from
 * {@link uk.ac.lancs.carp.model.Type#mustBeDefinedInJava()}:
 * 
 * <ul>
 * 
 * <li>{@link EnumeratedType}
 * 
 * <li>{@link StructureType}
 * 
 * <li>{@link InterfaceType}
 * 
 * </ul>
 * 
 * <p>
 * Consequently,
 * {@link uk.ac.lancs.carp.model.Type#declareJava(boolean,boolean,uk.ac.lancs.carp.model.ExpansionContext)}
 * methods must not be called on them, and
 * {@link uk.ac.lancs.carp.model.Type#getEncoder(Class,uk.ac.lancs.carp.model.LinkContext)}
 * and
 * {@link uk.ac.lancs.carp.model.Type#getDecoder(Class,uk.ac.lancs.carp.model.LinkContext)}
 * must only be called on them with a non-{@code null} type argument.
 * 
 * <p>
 * The following elements model primitive types, i.e., they do not
 * reference other {@link uk.ac.lancs.carp.model.Type}s:
 * 
 * <ul>
 * 
 * <li>{@link EnumeratedType}
 * 
 * <li>{@link IntegerType}
 * 
 * <li>{@link RealType}
 * 
 * <li>{@link ReferenceType}
 * 
 * <li>{@link StringType}
 * 
 * <li>{@link UUIDType}
 * 
 * </ul>
 * 
 * <p>
 * The following classes do not implement
 * {@link uk.ac.lancs.carp.model.Type}, but do form part of the node
 * hierarchy in complex constructs such as structures and interfaces:
 * 
 * <ul>
 * 
 * <li>{@link Member} (models a member of {@link StructureType})
 * 
 * <li>{@link CallSpecification} (models a call in an
 * {@link InterfaceType})
 * 
 * <li>{@link ResponseSpecification} (models a response to a call)
 * 
 * </ul>
 * 
 * <p>
 * Two strategies are used by these classes to handle not implementing
 * {@link uk.ac.lancs.carp.model.Type}. In some cases, the construct is
 * simply made public, so that individual components can be accessed
 * directly in any way suitable for the context. In others, the class
 * has a method which shadows one on
 * {@link uk.ac.lancs.carp.model.Type}, perhaps with some redundant
 * parameters removed, allowing a recursive operation to be passed
 * through.
 */
package uk.ac.lancs.carp.model.std;
