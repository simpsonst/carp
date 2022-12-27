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

package uk.ac.lancs.carp.model.std;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.TypeInfo;
import uk.ac.lancs.syntax.TextPosition;

/**
 * Defines built-in types. This class defines a virtual IDL module
 * called {@value #MODULE_NAME}.
 * 
 * <p>
 * Having set up a {@link QualificationContext} of your own, and before
 * calling {@link Type#qualify(ExternalName, QualificationContext)},
 * wrap the context using {@link #wrap(QualificationContext)}. When
 * invoked, the wrapper will first delegate to your context to qualify
 * names, and only if it fails will it look for leaf names in the
 * built-in module. This priority allows the user to define their own
 * types that co-incidentally have the same names as built-ins, without
 * them being permanently hidden by the built-ins.
 * 
 * <p>
 * Having set up an {@link ExpansionContext} of your own, and before
 * calling {@link Type#declareJava(boolean, boolean, ExpansionContext)},
 * wrap the context using {@link #wrap(ExpansionContext)}. When invoked,
 * the wrapper will first recognize fully qualified built-in type names,
 * and yield their corresponding type models, before delegating other
 * names to your context.
 * 
 * <p>
 * Having set up a {@link LinkContext} of your own, and before calling
 * {@link Type#getEncoder(Class, LinkContext)} or
 * {@link Type#getDecoder(Class, LinkContext)}, wrap the context using
 * {@link #wrap(LinkContext)}. When invoked, the wrapper will recognize
 * fully qualified built-in type names, and yield their type models,
 * before delegating other names to your context.
 * 
 * <p>
 * The types defined are:
 * 
 * <dl>
 * 
 * <dt><code>boolean</code>
 * 
 * <dd>A type mapping to {@code boolean} or {@link Boolean}
 * 
 * <dt><code>string</code>
 * 
 * <dd>A type mapping to {@link String}
 * 
 * <dt><code>uuid</code>
 * 
 * <dd>A type mapping to {@link java.util.UUID}
 * 
 * <dt><code>int8</code>
 * <dt><code>int16</code>
 * <dt><code>int32</code>
 * <dt><code>int64</code>
 * 
 * <dd>Two's-complement integer types of the given widths, mapping to
 * {@code byte}/{@link Byte}, {@code short}/{@link Short},
 * {@code int}/{@link Integer} and {@code long}/{@link Long}
 * respectively
 * 
 * <dt><code>uint8</code>
 * <dt><code>uint16</code>
 * <dt><code>uint32</code>
 * <dt><code>uint64</code>
 * 
 * <dd>Unsigned integer types of the given widths
 * 
 * </dl>
 *
 * @author simpsons
 */
public final class BuiltIns {
    private BuiltIns() {}

    /**
     * Get a resolution context that fills in missing names with fully
     * qualified built-ins.
     * 
     * @param base a context to be consulted first, to see if the name
     * is known to mean something else
     * 
     * @return the requested context
     */
    public static QualificationContext wrap(QualificationContext base) {
        return new QualificationContext() {
            @Override
            public ExternalName qualify(ExternalName name) {
                /* Only during resolution do we ask the base first. */
                ExternalName baseResult = base.qualify(name);
                if (baseResult != null) return baseResult;
                assert name.isLeaf();
                ExternalName fullName = builtInModule.resolve(name);
                if (builtIns.containsKey(fullName))
                    return fullName;
                else
                    return null;
            }

            @Override
            public void copyAssociations(Object to, Object from) {
                base.copyAssociations(to, from);
            }

            @Override
            public void report(ExternalName name, int line, int column) {
                base.report(name, line, column);
            }
        };
    }

    /**
     * Get an expansion context that first checks for fully qualified
     * names in the built-in module. Note that only
     * {@link ExpansionContext#getModel(ExternalName)} is handled
     * specially. Other calls are passed on to the base.
     * 
     * @param base a context to delegate to if the name is not in
     * built-in module
     * 
     * @return the requested context
     */
    public static ExpansionContext wrap(ExpansionContext base) {
        return new ExpansionContext() {
            @Override
            public String getTarget(ExternalName moduleName) {
                /* This will never be invoked with built-in types, so we
                 * always pass on to the base. */
                return base.getTarget(moduleName);
            }

            @Override
            public Type getModel(ExternalName typeName) {
                Type result = builtIns.get(typeName);
                if (result != null) return result;
                return base.getModel(typeName);
            }

            @Override
            public QualifiedDocumentation getDocs(Object element) {
                return base.getDocs(element);
            }

            @Override
            public void reportBadReference(TextPosition pos, String text,
                                           String detail) {
                base.reportBadReference(pos, text, detail);
            }
        };
    }

    /**
     * Get a link context that first checks for fully qualified names in
     * the built-in module.
     * 
     * @param base a context to delegate to if the name is not in
     * built-in module
     * 
     * @return the requested context
     */
    public static LinkContext wrap(LinkContext base) {
        return (typeName, source) -> {
            Type result = builtIns.get(typeName);
            if (result == null) return base.seek(typeName, source);
            return new TypeInfo(result, null);
        };
    }

    private static final String MODULE_NAME = "built-in";

    private static final ExternalName builtInModule =
        ExternalName.parse(MODULE_NAME);

    private static final Map<ExternalName, Type> builtIns;

    static {
        final Map<String, Type> source = new HashMap<>();
        source.put("string", StringType.INSTANCE);
        source.put("boolean", BooleanType.INSTANCE);
        source.put("uuid", UUIDType.INSTANCE);
        source.put("uint8", new IntegerType(0, 255));
        source.put("uint16", new IntegerType(0, 65535));
        source.put("uint32", new IntegerType(0, 0xffffffffl));
        source.put("uint64",
                   new IntegerType(BigInteger.ZERO,
                                   new BigInteger("ffffffffffffffff", 16)));
        source.put("int8", new IntegerType(Byte.MIN_VALUE, Byte.MAX_VALUE));
        source.put("int16", new IntegerType(Short.MIN_VALUE, Short.MAX_VALUE));
        source.put("int32",
                   new IntegerType(Integer.MIN_VALUE, Integer.MAX_VALUE));
        source.put("int64", new IntegerType(Long.MIN_VALUE, Long.MAX_VALUE));

        builtIns = source.entrySet().stream().collect(Collectors
            .toMap(e -> builtInModule.resolve(ExternalName.parse(e.getKey())),
                   Map.Entry::getValue));
    }
}
