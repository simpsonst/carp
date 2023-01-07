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
import java.net.InetSocketAddress;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.ac.lancs.carp.Fingerprint;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.model.LinkContext;

/**
 * Creates and weakly caches client translators. Translators are indexed
 * by service type.
 * 
 * @author simpsons
 */
public final class ClientTranslatorCache {
    /**
     * Locates run-time IDL type definitions.
     */
    private final LinkContext linkCtxt;

    /**
     * Provides fresh HTTP clients. This is invoked once per CARP call.
     * It's up to the client factory to persist any context between
     * calls.
     */
    private final Supplier<? extends CloseableHttpClient> clientFactory;

    /**
     * Creates encoding contexts to allow a proxy to generate request
     * messages. The provided table maps peer addresses to their
     * fingerprints, and should be populated by the returned
     * implementation. The final table state will then be added to the
     * request message as meta-data.
     */
    private final Function<? super Map<? super InetSocketAddress,
                                       ? super Fingerprint>,
                           ? extends EncodingContext> encodingContextProvider;

    /**
     * Creates decoding contexts to allow response messages to be
     * decoded. The provided table maps peer addresses to their
     * fingerprints, and must already be populated using meta-data in
     * the response.
     */
    private final Function<? super Map<? super InetSocketAddress,
                                       ? extends Fingerprint>,
                           ? extends DecodingContext> decodingContextProvider;

    /**
     * Create a cache of client translators. The arguments provided here
     * are those to be passed to each call to
     * {@link ClientTranslator#ClientTranslator(Class, LinkContext, Supplier, Function, Function)}.
     * (Its first argument is not known at this stage.)
     * 
     * @param linkCtxt a source for IDL type definitions and their
     * native classes
     * 
     * @param clientFactory a source of HTTP clients
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
     */
    public ClientTranslatorCache(LinkContext linkCtxt,
                                 Supplier<? extends CloseableHttpClient> clientFactory,
                                 Function<? super Map<? super InetSocketAddress,
                                                      ? super Fingerprint>,
                                          ? extends EncodingContext> encodingContextProvider,
                                 Function<? super Map<? super InetSocketAddress,
                                                      ? extends Fingerprint>,
                                          ? extends DecodingContext> decodingContextProvider) {
        this.linkCtxt = linkCtxt;
        this.clientFactory = clientFactory;
        this.encodingContextProvider = encodingContextProvider;
        this.decodingContextProvider = decodingContextProvider;

    }

    private final Map<Class<?>, Reference<ClientTranslator>> cache =
        new IdentityHashMap<>();

    private synchronized void purge(Class<?> type,
                                    Reference<ClientTranslator> ref) {
        cache.remove(type, ref);
    }

    /**
     * Get the translator for a given service type. This call returns
     * the same value, given the same argument, provided the value has
     * not been garbage-collected. Otherwise, it creates a new
     * translator.
     * 
     * @param type the service type
     * 
     * @return the translator for the type
     */
    public synchronized ClientTranslator get(Class<?> type) {
        Reference<ClientTranslator> ref = cache.get(type);
        ClientTranslator result;
        if (ref == null || (result = ref.get()) == null) {
            result = new ClientTranslator(type, linkCtxt, clientFactory,
                                          encodingContextProvider,
                                          decodingContextProvider);
            ref = Internals.watch(result, r -> purge(type, r));
            cache.put(type, ref);
        }
        return result;
    }
}
