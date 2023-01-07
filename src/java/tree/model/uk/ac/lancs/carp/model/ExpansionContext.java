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

package uk.ac.lancs.carp.model;

import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.syntax.TextPosition;

/**
 * Provides context for defining source code from the model. This is
 * used in the following methods:
 * 
 * <ul>
 * 
 * <li>{@link Type#declareJava(boolean, boolean, ExpansionContext)}
 * 
 * <li>{@link Type#defineJavaType(TextFile, ExternalName, ExpansionContext)}
 * 
 * <li>{@link Type#getJavaHashExpression(boolean, String, ExpansionContext)}
 * 
 * <li>{@link Type#getJavaInequalityExpression(boolean, String, String, ExpansionContext)}
 * 
 * </ul>
 * 
 * @author simpsons
 */
public interface ExpansionContext {
    /**
     * Report a bad reference in a <code>&#123;&#64;link&#125;</code> or
     * <code>&#64;see</code> tag.
     * 
     * @param pos the position in the source file
     * 
     * @param text the text of the reference
     * 
     * @param detail additional information on why the reference is bad
     */
    void reportBadReference(TextPosition pos, String text, String detail);

    /**
     * Get the target name of a module.
     * 
     * @param moduleName the module name
     * 
     * @return the target name, or {@code null} if not recognized
     */
    String getTarget(ExternalName moduleName);

    /**
     * Look up a modelled type by name.
     * 
     * @param idlName the name of the type
     * 
     * @return the model for the requested type, or {@code null} if not
     * recognized
     */
    Type getModel(ExternalName idlName);

    /**
     * Get the text of a JavaDoc reference. This method should not be
     * used to generate an argument type.
     * {@link Type#declareJava(boolean, boolean, ExpansionContext)}
     * should be used for that instead.
     * 
     * @default The default implementation performs the following:
     * 
     * <ol>
     * 
     * <li>The {@link DocRef#typeName} is extracted and passed to
     * {@link #getModel(ExternalName)}. This identifies the
     * {@link Type}.
     * 
     * <li>The enclosing Java package is identified by getting the
     * parent of {@link DocRef#typeName}, and passing to
     * {@link #getTarget(ExternalName)}.
     * 
     * <li>The result of
     * {@link Type#linkJavaDoc(DocRef, String, ExpansionContext)} is
     * returned, passing the reference, the deduced package name, and
     * this object.
     * 
     * </ol>
     * 
     * @param ref the qualified CARP documentation reference
     * 
     * @return the string representation of the reference for embedding
     * as a JavaDoc <code>&#64;see</code> or
     * <code>&#123;&#64;link&#125;</code> tag; or {@code null} if the
     * type cannot be found
     */
    default String linkJavaDoc(DocRef ref) {
        Type type = getModel(ref.typeName);
        if (type == null) return null;
        String pkgName = getTarget(ref.typeName.getParent());
        return type.linkJavaDoc(ref, pkgName, this);
    }

    /**
     * Get documentation about a given model element.
     * 
     * @param element the model element
     * 
     * @return tag-parsed documentation for the element, and a means to
     * resolve/report unqualified names; or {@code null} if no
     * documentation is found
     */
    QualifiedDocumentation getDocs(Object element);
}
