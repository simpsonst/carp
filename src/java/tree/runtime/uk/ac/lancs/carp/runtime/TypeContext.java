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

package uk.ac.lancs.carp.runtime;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.CompiledTypeFactory;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.MissingTypeException;
import uk.ac.lancs.carp.model.ModuleDefinition;
import uk.ac.lancs.carp.model.Type;

/**
 * Locates and caches definitions available through the class loader
 * hierarchy.
 *
 * @author simpsons
 */
public final class TypeContext {
    /**
     * Holds the result of searching for a run-time type.
     *
     * @author simpsons
     */
    public static final class Record {
        /**
         * Holds the type model.
         */
        public final Type def;

        /**
         * Specifies the fully qualified name of the equivalent Java
         * type, assuming it exists.
         */
        private final String className;

        /**
         * Identifies the loader that defines the class, assuming it
         * exists.
         */
        public final ClassLoader loader;

        /**
         * Get the hash code of this object.
         *
         * @return the hash code
         */
        @Override
        public int hashCode() {
            int hash = 3;
            hash = 29 * hash + Objects.hashCode(def);
            hash = 29 * hash + Objects.hashCode(loader);
            hash = 29 * hash + Objects.hashCode(className);
            return hash;
        }

        /**
         * Test whether this object is equal to another object.
         *
         * @param obj the other object
         *
         * @return {@code true} if the object is a {@link Record}, and
         * its fields are identical; {@code false} otherwise
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            final Record other = (Record) obj;
            if (!Objects.equals(this.className, other.className)) return false;
            if (!Objects.equals(this.def, other.def)) return false;
            if (!Objects.equals(this.loader, other.loader)) return false;
            return true;
        }

        /**
         * Create a record of type information.
         *
         * @param def the type's definition
         *
         * @param className the name of the equivalent Java class
         *
         * @param loader the loader that defines the class and the type
         */
        public Record(Type def, String className, ClassLoader loader) {
            this.def = def;
            this.className = className;
            this.loader = loader;
        }

        /**
         * Get the class representing this type. The class will be
         * initialized, as if by
         * <code>{@linkplain Class#forName(String, boolean, ClassLoader)
         * Class.forName}({@linkplain #className className}, true,
         * {@linkplain #loader loader})</code>
         *
         * @return the requested class
         *
         * @throws ClassNotFoundException if the class cannot be located
         * by the class loader that defined the IDL type. This may
         * happen if
         * <code>{@linkplain #def def}.{@linkplain Type#mustBeDefinedInJava()
         * mustBeDefinedInJava}()</code> returns {@code null}.
         *
         * @throws LinkageError if the linkage fails
         *
         * @throws ExceptionInInitializerError if the initialization
         * provoked by this method fails
         */
        public Class<?> getBaseClass() throws ClassNotFoundException {
            if (className == null) return null;
            return Class.forName(className, true, loader);
        }
    }

    private final class Application {
        final ModuleDefinition defs;

        final ClassLoader source;

        final String pkgName;

        final Map<ExternalName,
                  Map<ExternalName, MissingTypeException>> unresolved =
                      new HashMap<>();

        Application(ModuleDefinition defs, ClassLoader source, String pkgName) {
            this.defs = defs;
            this.source = source;
            this.pkgName = pkgName;
            unresolved.putAll(defs.types.keySet().stream()
                .collect(Collectors.toMap(n -> n, n -> new HashMap<>())));
        }

        synchronized Type getDefinition(ExternalName typeName) {
            Type typeDef = defs.types.get(typeName);
            if (typeDef == null)
                throw new MissingTypeException(typeName.toString());
            return typeDef;
        }
    }

    @SuppressWarnings("unused")
    private final TypeContext parent;

    private TypeContext(TypeContext parent) {
        this.parent = parent;
    }

    private static final ClassLoaderContext<TypeContext> contexts =
        new ClassLoaderContext<>(TypeContext::new);

    /**
     * Maps a module name to its set of definitions and the
     * corresponding Java package it has been applied to.
     */
    private final Map<ExternalName, Application> apps =
        Collections.synchronizedMap(new HashMap<>());

    /**
     * Ensure that a module is loaded. The map {@link #apps} is
     * atomically updated if the entry is {@code null}.
     * 
     * @param moduleName the index key of the entry to ensure exists
     * 
     * @param source the place to load the module definition from
     * 
     * @return the entry, either cached or newly created
     */
    private Application getApplicationInternal(ExternalName moduleName,
                                               ClassLoader source) {
        Function<ExternalName, Application> loader = k -> {
            /* See if we have the module definition. If not, delegate to
             * the ancestor. */
            String resPath = moduleName.asJavaTypeResourcePath();
            InputStream res = source.getResourceAsStream(resPath);
            if (res == null) return null;

            /* Load the module definition. */
            Properties props = new Properties();
            try {
                try (InputStream in = res) {
                    props.loadFromXML(in);
                } catch (InvalidPropertiesFormatException ex) {
                    try (InputStream in = source.getResourceAsStream(resPath)) {
                        props.load(in);
                    }
                }
            } catch (IOException ex) {
                throw new ClassLoaderContext.ResourceException(moduleName
                    .toString(), ex);
            }
            LoadContext loadCtxt = new LoadContext() {
                @Override
                public ClassLoader implementations() {
                    return CompiledTypeFactory.class.getClassLoader();
                }

                @Override
                public ClassLoader source() {
                    return source;
                }
            };
            ModuleDefinition moduleDef = ModuleDefinition
                .load("module.", props,
                      CompiledTypeFactory.class.getClassLoader(), loadCtxt);
            String pkgName = props.getProperty("module.java-package");
            return new Application(moduleDef, source, pkgName);
        };
        return apps.computeIfAbsent(moduleName, loader);
    }

    /**
     * Ensure that a given module has been loaded with the correct class
     * loader.
     * 
     * @param moduleName the module to ensure is loaded
     * 
     * @param source the leaf class loader to start the search from
     * 
     * @return the appropriate module details
     */
    private static Application getApplication(ExternalName moduleName,
                                              ClassLoader source) {
        return contexts
            .getResource(source,
                         (c, l) -> c.getApplicationInternal(moduleName, l));
    }

    /**
     * Get information on an IDL type.
     *
     * @param typeName the type's name
     *
     * @param source the class loader to load from
     *
     * @return the type information, including its definition and Java
     * name
     *
     * @throws MissingTypeException if the type's module definition was
     * found, but it did not contain the type
     */
    public static Record getType(ExternalName typeName, ClassLoader source) {
        ExternalName moduleName = typeName.getParent();

        try {
            /* Ensure that the containing module has been loaded. */
            Application app = getApplication(moduleName, source);
            if (app == null)
                throw new MissingTypeException("no module for " + typeName);

            /* Get the type definition. */
            Type typeDef = app.getDefinition(typeName);

            /* Work out the Java name, and report back. */
            String className = typeDef.mustBeDefinedInJava() ?
                app.pkgName + '.' + typeName.asJavaClassName() : null;
            return new Record(typeDef, className, app.source);
        } catch (ClassLoaderContext.ResourceException ex) {
            throw new MissingTypeException("failed to load module for "
                + typeName.toString(), ex);
        }
    }
}
