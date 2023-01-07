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

import java.lang.ref.Reference;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;

/**
 * Creates and stores proxies weakly. The user supplies a constructor
 * taking the service type and endpoint URI. Given the same arguments,
 * the {@link #getProxy(Class, URI)} method will return the same value,
 * provided that value has not been garbage-collected in the interim.
 * 
 * @author simpsons
 */
public final class ProxyCache {
    /**
     * Create a proxy cache.
     * 
     * @param constructor a means to create new proxies
     */
    public ProxyCache(BiFunction<? super Class<?>, ? super URI,
                                 ?> constructor) {
        this.constructor = constructor;
    }

    /**
     * Specifies how to create a fresh proxy given the service type and
     * the endpoint URI.
     */
    private final BiFunction<? super Class<?>, ? super URI, ?> constructor;

    /**
     * Remove a cleared reference from the maps.
     * 
     * @param location the endpoint
     * 
     * @param type the service type
     * 
     * @param value the reference being removed
     */
    private synchronized void purge(URI location, Class<?> type,
                                    Reference<Object> value) {
        /* Walk through the direct cache, and remove the reference, but
         * only if it is the current one. */
        Map<URI, Reference<Object>> inner = cache.get(type);
        if (inner != null) {
            inner.remove(location, value);
            if (inner.isEmpty()) cache.remove(type);
        }

        /* The inner map of the reverse cache should already have had
         * its entry removed. If it is now empty, the entry in the outer
         * map can be removed too. */
        reverseCache.remove(type, Collections.emptyMap());
    }

    private final Map<Class<?>, Map<URI, Reference<Object>>> cache =
        new HashMap<>();

    private final Map<Class<?>, Map<Object, URI>> reverseCache =
        new HashMap<>();

    /**
     * Get the address of a proxy.
     * 
     * @param type the service type
     * 
     * @param proxy the proxy
     * 
     * @return the location, or {@code null} if unknown
     */
    public URI getLocation(Class<?> type, Object proxy) {
        return reverseCache.getOrDefault(type, Collections.emptyMap())
            .get(proxy);
    }

    /**
     * Get a proxy, given service type and location. The result is
     * weakly cached.
     * 
     * @param type the service type
     * 
     * @param location the endpoint
     * 
     * @param <Srv> the service type
     * 
     * @return the proxy
     */
    public synchronized <Srv> Srv getProxy(Class<Srv> type, URI location) {
        /* Make sure we have a direct mapping for the service type. */
        Map<URI, Reference<Object>> inner =
            cache.computeIfAbsent(type, k -> new HashMap<>());

        /* Look up the URI. There might be no reference, or it might be
         * cleared. In either case, we need to create the new proxy. It
         * will be eligible for garbage collection, so we can't use any
         * of the Map::compute* methods. */
        Reference<Object> ref = inner.get(location);
        Object proxy;
        if (ref == null || (proxy = ref.get()) == null) {
            /* Create the proxy, and a weak reference to it that will
             * notify us when it goes. Put it in the direct inner
             * map. */
            proxy = constructor.apply(type, location);
            ref = Internals.watch(proxy, r -> purge(location, type, r));
            inner.put(location, ref);

            /* Put the proxy weakly in the reverse map too. We don't
             * need the result, as it's just the location, which we
             * already have, so we can use Map::computeIfAbsent. */
            reverseCache.computeIfAbsent(type, k -> new WeakHashMap<>())
                .put(proxy, location);
        }

        /* Whether we've just created the proxy, or recovered it from
         * the cache, we just have to present it as the right type. */
        return type.cast(proxy);
    }
}
