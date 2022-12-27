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

package uk.ac.lancs.carp.runtime;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Holds arbitrary, on-demand context for each class loader.
 * 
 * @todo Don't try to support the bootstrap class loader, because there
 * are no ClassLoader.getBootstrapResource...() methods.
 *
 * @param <T> the context type
 *
 * @author simpsons
 */
public final class ClassLoaderContext<T> {
    private final Function<? super T, T> creator;

    private T bootstrapContext;

    private final Map<ClassLoader, T> contexts =
        Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Create a class-loader context hierarchy.
     *
     * @param creator mechanism to create a context from its parent.
     */
    public ClassLoaderContext(Function<? super T, T> creator) {
        this.creator = creator;
    }

    /**
     * Get the context for the given class loader. The context will be
     * created on demand, as will any of its ancestors if they do not
     * already exist. Ancestors are always created before their
     * descendants. This method is thread-safe.
     *
     * @param loader the class loader whose context is sought
     *
     * @return the context for the given class loader
     */
    public T getContext(ClassLoader loader) {
        if (loader == null || loader.getParent() == loader)
            return getBootstrapContext();
        return contexts.computeIfAbsent(loader, k -> creator
            .apply(getContext(loader.getParent())));
    }

    /**
     * Indicates a positive failure to access a resource. For example,
     * the resource is found, but failed to load; or the resource must
     * exist at a given location, but was not found. The method
     * {@link ClassLoaderContext#getResource(ClassLoader, BiFunction)}
     * uses this to short-circuit the search for a resource.
     */
    public static class ResourceException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        /**
         * Create an exception.
         */
        public ResourceException() {}

        /**
         * Create an exception with a detail message.
         * 
         * @param message the detail message
         */
        public ResourceException(String message) {
            super(message);
        }

        /**
         * Create an exception with a detail message and cause.
         * 
         * @param message the detail message
         * 
         * @param cause the cause of the exception
         */
        public ResourceException(String message, Throwable cause) {
            super(message, cause);
        }

        /**
         * Create an exception with a cause.
         * 
         * @param cause the cause of the exception
         */
        public ResourceException(Throwable cause) {
            super(cause);
        }

        /**
         * Create an exception with a detail message, a cause, with
         * suppression state and stack-trace mutability.
         * 
         * @param message the detail message
         * 
         * @param cause the cause of the exception
         * 
         * @param enableSuppression whether suppression is enabled
         * 
         * @param writableStackTrace whether the stack trace is writable
         */
        protected ResourceException(String message, Throwable cause,
                                    boolean enableSuppression,
                                    boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

    private T getBootstrapContext() {
        synchronized (contexts) {
            if (bootstrapContext == null)
                bootstrapContext = creator.apply(null);
            return bootstrapContext;
        }
    }

    /**
     * Get a resource from a context associated with a class loader or
     * one of its ancestors. Each ancestor is consulted before its
     * descendant. Contexts will be created on demand.
     *
     * @param <R> the resource type
     *
     * @param loader the class loader
     *
     * @param getter a mechanism to get a resource from a specific
     * context without checking recursively, its first argument being
     * the context, and its second being the associated class loader,
     * and returning the resource if it has it, or returning
     * {@code null} if the context does not have it but another might,
     * or throwing {@link ResourceException} if it does not have it and
     * no other can
     *
     * @return the requested resource if found; {@code null} otherwise
     *
     * @throws ResourceException if the resource is found but could not
     * be loaded
     */
    public <R> R getResource(ClassLoader loader,
                             BiFunction<? super T, ? super ClassLoader,
                                        ? extends R> getter) {
        T ctxt;
        if (isBootstrap(loader)) {
            loader = null;
            ctxt = getBootstrapContext();
        } else {
            ClassLoader parentLoader = loader.getParent();
            R result = getResource(parentLoader, getter);
            if (result != null) return result;

            Function<ClassLoader, T> op = k -> {
                T parentCtxt = isBootstrap(parentLoader) ?
                    getBootstrapContext() : contexts.get(parentLoader);
                return creator.apply(parentCtxt);
            };
            ctxt = contexts.computeIfAbsent(loader, op);
        }

        /* We can't ask the user to load from the bootstrap class
         * loader, as it has no special methods, like
         * getBootstrapResource(String), etc. */
        if (loader == null) return null;

        return getter.apply(ctxt, loader);
    }

    private static boolean isBootstrap(ClassLoader loader) {
        return loader == null || loader.getParent() == loader;
    }
}
