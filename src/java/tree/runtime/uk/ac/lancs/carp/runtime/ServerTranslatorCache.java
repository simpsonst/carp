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

import java.lang.ref.Reference;
import java.net.InetSocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.function.Function;
import uk.ac.lancs.carp.Fingerprint;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.model.LinkContext;

/**
 * Creates and weakly caches server translators. Translators are indexed
 * by receiver and service type. Receivers are held weakly. Translators
 * are held weakly if no receivers use them.
 * 
 * @author simpsons
 */
public final class ServerTranslatorCache {
    private synchronized void purge(Class<?> type,
                                    Reference<ServerTranslator> ref) {
        translators.remove(type, ref);
    }

    private final LinkContext linkCtxt;

    private final Executor executor;

    private final Function<? super Map<? super InetSocketAddress,
                                       ? super Fingerprint>,
                           ? extends EncodingContext> encodingContextProvider;

    private final Function<? super Map<? super InetSocketAddress,
                                       ? extends Fingerprint>,
                           ? extends DecodingContext> decodingContextProvider;

    /**
     * Create a cache of server translators. The arguments provided here
     * are those to be passed to each call to
     * {@link ServerTranslator#ServerTranslator(Class, LinkContext, Executor, Function, Function)}.
     * (Its first argument is not known at this stage.)
     * 
     * @param linkCtxt a source for IDL type definitions and their
     * native classes
     * 
     * @param encodingContextProvider a means of creating encoding
     * contexts that take account of certificate fingerprints, given a
     * table to populate with peer-fingerprint tuples as receiver
     * endpoints are passed for encoding
     * 
     * @param decodingContextProvider a means of creating decoding
     * contexts that take account of certificate fingerprints, given a
     * table mapping peer addresses to their fingerprints, to be
     * consulted when endpoints are decoded into proxies
     * 
     * @param executor a means to execute asynchronous calls
     */
    public ServerTranslatorCache(LinkContext linkCtxt, Executor executor,
                                 Function<? super Map<? super InetSocketAddress,
                                                      ? super Fingerprint>,
                                          ? extends EncodingContext> encodingContextProvider,
                                 Function<? super Map<? super InetSocketAddress,
                                                      ? extends Fingerprint>,
                                          ? extends DecodingContext> decodingContextProvider) {
        this.linkCtxt = linkCtxt;
        this.executor = executor;
        this.encodingContextProvider = encodingContextProvider;
        this.decodingContextProvider = decodingContextProvider;
    }

    private final Map<Class<?>, Reference<ServerTranslator>> translators =
        new IdentityHashMap<>();

    private final Map<Object, Map<Class<?>, ServerTranslator>> perReceiver =
        new WeakHashMap<>();

    private ServerTranslator get(Class<?> type) {
        assert Thread.holdsLock(this);
        Reference<ServerTranslator> ref = translators.get(type);
        ServerTranslator result;
        if (ref == null || (result = ref.get()) == null) {
            result = new ServerTranslator(type, linkCtxt, executor,
                                          encodingContextProvider,
                                          decodingContextProvider);
            ref = Internals.watch(result, r -> purge(type, r));
            translators.put(type, ref);
        }
        return result;
    }

    /**
     * Get the translator for a receiver and service type. The same
     * translator will be returned given the same service type, provided
     * it has not been garbage-collected. Otherwise, a new translator is
     * created. A translator will not be collected if currently
     * associated with a receiver.
     * 
     * @param type the service type
     * 
     * @param receiver the receiver
     * 
     * @return the translator for the type
     */
    public synchronized ServerTranslator get(Class<?> type, Object receiver) {
        return perReceiver
            .computeIfAbsent(receiver, k -> new IdentityHashMap<>())
            .computeIfAbsent(type, k -> get(k));
    }
}
