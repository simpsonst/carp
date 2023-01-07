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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiConsumer;
import uk.ac.lancs.carp.map.ExternalName;

/**
 * A representation of an interface definition
 * 
 * @author simpsons
 */
public final class ModuleDefinition {
    /**
     * An immutable mapping from locally defined names to fully
     * qualified remote names
     */
    public final Map<ExternalName, ExternalName> imports;

    /**
     * An immutable mapping from locally defined names to type
     * definitions
     */
    public final Map<ExternalName, Type> types;

    /**
     * Create a module definition from a set of imports and types. The
     * inputs are copied.
     * 
     * @param imports the set of imports
     * 
     * @param types the set of types
     * 
     * @return a new definition from copies of the provided components
     * 
     * @constructor
     */
    public static ModuleDefinition
        define(Map<? extends ExternalName, ? extends ExternalName> imports,
               Map<? extends ExternalName, ? extends Type> types) {
        return new ModuleDefinition(Map.copyOf(imports), Map.copyOf(types));
    }

    private ModuleDefinition(Map<ExternalName, ExternalName> imports,
                             Map<ExternalName, Type> types) {
        this.imports = imports;
        this.types = types;
    }

    /**
     * Gather references to other types in the type definitions of this
     * module.
     * 
     * @param dest Invoked to report references. The first argument will
     * be the referenced type's name; the second, the referring type.
     */
    public void gatherReferences(BiConsumer<? super ExternalName,
                                            ? super ExternalName> dest) {
        for (var entry : types.entrySet()) {
            ExternalName typeName = entry.getKey();
            Type model = entry.getValue();

            model.gatherReferences(typeName, dest);
        }
    }

    /**
     * Qualify all type names in this module. A name can be qualified in
     * three ways:
     * 
     * <ul>
     * 
     * <li>It could be the short form of an imported name, which
     * replaces it.
     * 
     * <li>It could be the short form of a built-in type, which replaces
     * it. The {@code qualificationWrapper} argument allows built-in
     * types to be specified.
     * 
     * <li>It could a name defined locally in the same module. The
     * module name is prefixed to it.
     * 
     * </ul>
     * 
     * @param moduleName the module name
     * 
     * @param ctxt a context to resolve unqualified names, record
     * qualified elements replacing unqualified ones in source, and
     * report names that failed to be qualified. See
     * {@link #getQualificationContext(ExternalName, BiConsumer, QualificationReporter)}.
     * 
     * @return an equivalent definition with no unqualified names
     * 
     * @constructor
     */
    public ModuleDefinition qualify(ExternalName moduleName,
                                    QualificationContext ctxt) {
        Map<ExternalName, Type> fullDefs = new HashMap<>();
        for (var entry : types.entrySet()) {
            ExternalName localTypeName = entry.getKey();
            ExternalName typeName = moduleName.resolve(localTypeName);
            Type typeSpec = entry.getValue();
            Type fullTypeSpec = typeSpec.qualify(typeName, ctxt);
            fullDefs.put(typeName, fullTypeSpec);
        }
        ModuleDefinition ref =
            new ModuleDefinition(Collections.emptyMap(), Map.copyOf(fullDefs));
        ctxt.copyAssociations(ref, this);
        return ref;
    }

    /**
     * Get a qualification context that understands the names defined in
     * this module. The result should be passed to
     * {@link #qualify(ExternalName, QualificationContext)} using the
     * same module name.
     * 
     * @param moduleName the module name
     * 
     * @param assocCopier an action to take when a fully qualified
     * element (1st argument) replaces another (2nd argument)
     * 
     * @param reporter an action to take when an unqualified name fails
     * to resolve
     * 
     * @return the requested context
     * 
     * @constructor
     */
    public QualificationContext
        getQualificationContext(ExternalName moduleName,
                                BiConsumer<Object, Object> assocCopier,
                                QualificationReporter reporter) {
        /* Expand the imports to include local names. */
        Map<ExternalName, ExternalName> localNames = new HashMap<>(imports);
        for (var localTypeName : types.keySet()) {
            ExternalName typeName = moduleName.resolve(localTypeName);
            localNames.put(localTypeName, typeName);
        }

        return new QualificationContext() {
            @Override
            public ExternalName qualify(ExternalName name) {
                return localNames.get(name);
            }

            @Override
            public void copyAssociations(Object to, Object from) {
                if (assocCopier != null) assocCopier.accept(to, from);
            }

            @Override
            public void report(ExternalName name, int line, int column) {
                if (reporter != null) reporter.report(name, line, column);
            }
        };
    }

    /**
     * Write types to a set of properties. Each type is assigned an
     * arbitrary non-negative integer in [0,<var>n</var>), and encoded
     * as properties with the prefix
     * <samp><var>prefix</var>type.<var>index</var>.</samp>. An
     * additional property <samp><var>prefix</var>type.count</samp> is
     * written to indicate the number of types <var>n</var>.
     * 
     * <p>
     * The results can be reloaded with
     * {@link #load(java.lang.String, java.util.Properties, java.lang.ClassLoader, uk.ac.lancs.carp.model.LoadContext)}.
     * 
     * @param prefix the prefix to use on property names
     * 
     * @param props the destination properties
     */
    public void describe(String prefix, Properties props) {
        props.setProperty(prefix + "type.count",
                          Integer.toString(this.types.size()));
        int code = 0;
        for (var entry : this.types.entrySet()) {
            String pfx = prefix + "type." + code++ + ".";
            ExternalName name = entry.getKey();
            Type elem = entry.getValue();
            props.setProperty(pfx + "name", name.toString());
            elem.describe(pfx, props);
        }
    }

    /**
     * Load a definition from properties. These must match those
     * generated by {@link #describe(String, Properties)}.
     * 
     * @param prefix the prefix to use on property names
     * 
     * @param props the source properties
     * 
     * @param loader the class loader to search for implementations
     * 
     * @param ctxt the context for loading nested elements
     * 
     * @return a new definition as described by the properties
     * 
     * @constructor
     * 
     * @throws NumberFormatException if the number of type definitions
     * is malformed
     * 
     * @throws NullPointerException if the number of type definitions is
     * unspecified
     */
    public static ModuleDefinition load(String prefix, Properties props,
                                        ClassLoader loader, LoadContext ctxt) {
        Map<ExternalName, Type> types = new HashMap<>();
        int count = Integer.parseInt(props.getProperty(prefix + "type.count"));
        for (int code = 0; code < count; code++) {
            String pfx = prefix + "type." + code + ".";
            ExternalName name =
                ExternalName.parse(props.getProperty(pfx + "name"));
            Type elem = Type.load(pfx, props, ctxt);
            types.put(name, elem);
        }
        return new ModuleDefinition(Collections.emptyMap(), Map.copyOf(types));
    }
}
