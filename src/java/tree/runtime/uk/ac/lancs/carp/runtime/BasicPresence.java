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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.protocol.HttpContext;
import uk.ac.lancs.carp.Carp;
import uk.ac.lancs.carp.ClientPresence;
import uk.ac.lancs.carp.Configuration;
import uk.ac.lancs.carp.Fingerprint;
import uk.ac.lancs.carp.FingerprintRepository;
import uk.ac.lancs.carp.Presence;
import uk.ac.lancs.carp.PresenceFactory;
import uk.ac.lancs.carp.ServerPresence;
import uk.ac.lancs.carp.WebPlacement;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.component.Agency;
import uk.ac.lancs.carp.component.PathMap;
import uk.ac.lancs.carp.component.PathMatch;
import uk.ac.lancs.carp.errors.StatusModificationException;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.LinkException;
import uk.ac.lancs.carp.model.TypeInfo;
import uk.ac.lancs.carp.model.std.BuiltIns;
import uk.ac.lancs.scc.jardeps.Service;

/**
 * Provides a presence both for proxies to remote objects and local
 * objects exposed to remote invocation. The user must provide the
 * following pieces of configuration:
 * 
 * <ul>
 * 
 * <li>a supply of Apache Commons {@link HttpClient}, allowing them to
 * be configured with (for example) an appropriate SSL context;
 * 
 * <li>an optional {@link FingerprintRepository} (may be {@code null}),
 * allowing (for example) an SSL context to be updated on self-signed
 * certificates;
 * 
 * <li>the presence's location in a server (a {@link WebPlacement}).
 * 
 * </ul>
 * 
 * @author simpsons
 */
public class BasicPresence implements Presence {
    private final Supplier<? extends CloseableHttpClient> clientFactory;

    private final WebPlacement placement;

    private final FingerprintRepository fingerprints;

    private final boolean shortCircuit;

    /**
     * Create a basic presence with distinct base URI and root virtual
     * path.
     * 
     * @param clientFactory Invoked to obtain fresh HTTP clients on
     * demand.
     * 
     * @param fingerprints a repository of learned fingerprints
     * 
     * @param placement the place where this presence is being served
     * from
     * 
     * @param executor an executor for asynchronous calls
     * 
     * @param shortCircuit whether to return direct local receivers
     * instead of proxies
     */
    BasicPresence(Supplier<? extends CloseableHttpClient> clientFactory,
                  WebPlacement placement, FingerprintRepository fingerprints,
                  Executor executor, boolean shortCircuit) {
        this.clientFactory = clientFactory;
        this.placement = placement;
        this.fingerprints = fingerprints;
        this.shortCircuit = shortCircuit;
        this.typeClients = new ClientTranslatorCache(linkCtxt, clientFactory,
                                                     this::getEncodingContext,
                                                     this::getDecodingContext);
        this.typeServers = new ServerTranslatorCache(linkCtxt, executor,
                                                     this::getEncodingContext,
                                                     this::getDecodingContext);
    }

    private final ClientTranslatorCache typeClients;

    private static final LinkContext linkCtxt = BuiltIns.wrap((n, cl) -> {
        TypeContext.Record rec = TypeContext.getType(n, cl);
        try {
            return new TypeInfo(rec.def, rec.getBaseClass());
        } catch (ClassNotFoundException ex) {
            throw new LinkException(n.toString(), ex);
        }
    });

    /**
     * Create a proxy for a remote receiver. The client translator for
     * the service type is created or retrieved, an invocation handler
     * is generated from it given the receiver's address, and then a new
     * proxy is created from that handler.
     * 
     * @param <Srv> the service type
     * 
     * @param type the service type
     * 
     * @param location the remote receiver
     * 
     * @return a new proxy
     */
    private <Srv> Srv createProxy(Class<Srv> type, URI location) {
        ClientTranslator z = typeClients.get(type);
        ClassLoader cl = type.getClassLoader();
        Class<?>[] ta = new Class<?>[] { type };
        InvocationHandler h = z.getHandler(location);
        return type.cast(Proxy.newProxyInstance(cl, ta, h));
    }

    @Override
    public <Srv> Srv elaborate(Class<Srv> type, URI location) {
        /* Check for a local object. We might be able to return it
         * directly. */
        URI relative = this.placement.base().relativize(location);
        if (shortCircuit && relative != location) {
            /* This should be a local object. */
            PathMatch r = pathMap
                .resolve(Carp.pathAsPartsWithEmptyTail(relative.getPath()));
            if (r.type == type) return type.cast(r.receiver);
            throw new IllegalArgumentException("not " + type + " at "
                + location);
        }

        /* It's a remote object, so ensure we have a proxy to it. */
        return proxies.getProxy(type, location);
    }

    @Override
    public <Srv> void bind(String suffix, Class<Srv> type, Srv receiver,
                           Agency agency) {
        pathMap.register(Carp.pathAsParts(suffix), type, receiver,
                         agency == null ? Agency.empty() : agency);
    }

    @Override
    public void unbind(String suffix) {
        pathMap.deregister(suffix);
    }

    @Override
    public <Srv> void unbind(Class<Srv> type, Srv receiver) {
        pathMap.deregister(type, receiver);
    }

    /**
     * Keeps a record of receivers we've exposed.
     */
    private final PathMap pathMap = new PathMap();

    /**
     * Caches proxies to remote objects.
     */
    private final ProxyCache proxies = new ProxyCache(this::createProxy);

    /**
     * Get an encoding context that also looks up the fingerprint of the
     * resultant URI, and stores it in a map, if it exists.
     * 
     * @param map the destination map
     * 
     * @return an encoding context that deposits fingerprints in the map
     */
    private EncodingContext getEncodingContext(Map<? super InetSocketAddress,
                                                   ? super Fingerprint> map) {
        return (type, receiver) -> establishCallback(map, type, receiver);
    }

    private URI
        establishCallback(Map<? super InetSocketAddress,
                              ? super Fingerprint> map,
                          Class<?> type, Object receiver) {
        /* TODO: Lose the redundant container and containerType
         * parameters. */

        /* Look up the object and type in our cache. */
        URI endpoint = proxies.getLocation(type, receiver);
        if (endpoint == null) {
            String path = Carp.partsAsPath(pathMap.recognize(type, receiver));

            /* We are exposing a local receiver, so generate a fully
             * qualified URI to it. */
            return this.placement.base().resolve(path);
        }

        /* Check to see if the URI host:port has a fingerprint. */
        InetSocketAddress peer = Carp.getPeer(endpoint);
        if (fingerprints != null) {
            Fingerprint print = fingerprints.getFingerprint(peer);
            if (print != null) map.put(peer, print);
        }
        return endpoint;
    }

    /**
     * Get a decoding context that records each supplied fingerprint if
     * a proxy is set up to a peer associated with the fingerprint.
     * 
     * @param map the set of supplied fingerprints
     * 
     * @return the requested context
     */
    private DecodingContext getDecodingContext(Map<? super InetSocketAddress,
                                                   ? extends Fingerprint> map) {
        return (type, location) -> {
            InetSocketAddress peer = Carp.getPeer(location);
            Fingerprint print = map.get(peer);
            if (print != null && fingerprints != null)
                fingerprints.recordFingerprint(peer, print);
            return elaborate(type, location);
        };
    }

    private void handleHttpRequest(HttpRequest req, HttpResponse rsp,
                                   HttpContext ctxt)
        throws HttpException,
            IOException {
        String path = placement.subpath(req);

        try {
            /* Identify the receiver and its interface type. Get the
             * translator for that type. */
            PathMatch res =
                pathMap.resolve(Carp.pathAsPartsWithEmptyTail(path));
            if (!res.tail.isEmpty()) {
                rsp.setStatusCode(HttpStatus.SC_NOT_FOUND);
                return;
            }
            ServerTranslator trans = typeServers.get(res.type, res.receiver);

            final String transMethod = req.getRequestLine().getMethod();
            switch (transMethod) {
            case "POST":
                /* The request body is a JSON object, indicating which
                 * method to invoke, and also possibly containing
                 * fingerprints of any receivers referenced in the
                 * request. Extract the method-specific part, and
                 * deliver it to the right receiver. */
                try {
                    /* Interpret the request body as JSON. TODO:
                     * Decoding errors should be reported as a 400 Bad
                     * Request. */
                    final JsonObject jsonReq = jsonOf(req);

                    /* Invoke the user-defined behaviour, translating
                     * the supplied JSON into an argument list, and
                     * translating the result into JSON. */
                    JsonObject jsonRsp = trans.invoke(res.receiver, jsonReq);

                    /* Put the resultant JSON into the HTTP response. */
                    if (jsonRsp != null) {
                        rsp.setEntity(entityOf(jsonRsp));

                        /* Complete and return the response. */
                        rsp.setStatusCode(HttpStatus.SC_OK);
                    } else {
                        /* There is no response. */
                        rsp.setStatusCode(HttpStatus.SC_NO_CONTENT);
                    }
                    return;
                } catch (JsonException ex) {
                    rsp.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    // TODO: Set response body.
                    return;
                } catch (InvocationTargetException ex) {
                    throw ex.getCause();
                }

            default:
                /* The HTTP method is unknown to us. TODO: Check whether
                 * returning without setting the response will result in
                 * a 501 Not Implemented. */
                break;
            }
        } catch (StatusModificationException ex) {
            JsonObject jsonRsp =
                Json.createObjectBuilder().add("app-error", "bad-status-mod")
                    .add("params", jsonOf(ex.params))
                    .add("message", ex.getMessage()).build();
            rsp.setEntity(entityOf(jsonRsp));
            rsp.setStatusCode(HttpStatus.SC_UNPROCESSABLE_ENTITY);
        } catch (Throwable t) {
            /* Create an error id, and log the error detail locally.
             * Meanwhile, report the error id back to the client. */
            UUID errorId = UUID.randomUUID();
            logger.log(Level.SEVERE, "server error " + errorId, t);
            JsonObject jsonRsp = Json.createObjectBuilder()
                .add("error", errorId.toString()).build();
            rsp.setEntity(entityOf(jsonRsp));
            rsp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private static JsonObject
        jsonOf(Map<? extends String, ? extends String> params) {
        JsonObjectBuilder builder = Json.createObjectBuilder();
        for (var entry : params.entrySet())
            builder.add(entry.getKey(), entry.getValue());
        return builder.build();
    }

    private static JsonObject jsonOf(HttpRequest req) throws IOException {
        HttpEntityEnclosingRequest ereq = (HttpEntityEnclosingRequest) req;
        HttpEntity ent = ereq.getEntity();
        try (InputStream in = ent.getContent();
             JsonReader reader =
                 jsonReaders.createReader(in, StandardCharsets.UTF_8)) {
            return reader.readObject();
        }
    }

    private static HttpEntity entityOf(JsonObject jsonRsp) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (JsonWriter writer =
            jsonWriters.createWriter(buffer, StandardCharsets.UTF_8)) {
            writer.write(jsonRsp);
        }
        byte[] buf = buffer.toByteArray();
        return new ByteArrayEntity(buf, ContentType.APPLICATION_JSON);
    }

    private static final Logger logger = Logger.getLogger("uk.ac.lancs.carp");

    private static final JsonReaderFactory jsonReaders =
        Json.createReaderFactory(Collections.emptyMap());

    private static final JsonWriterFactory jsonWriters =
        Json.createWriterFactory(Collections.emptyMap());

    private final ServerTranslatorCache typeServers;

    @Override
    public <Srv> URI expose(Class<Srv> type, Srv receiver) {
        List<String> path = pathMap.locate(type, receiver);
        if (path == null) return null;
        return this.placement.base().resolve(Carp.partsAsPath(path));
    }

    /**
     * Creates basic presences, and determines suitability of the
     * implementation to configuration parameters.
     */
    @Service(PresenceFactory.class)
    public static class Factory implements PresenceFactory {
        @Override
        public PresenceFactory.Suitability
            considerClient(Configuration params) {
            if (params.lacks(Carp.CLIENTS)) return Suitability.UNMET;
            if (params.lacks(Carp.PLACEMENT)) return Suitability.SUBOPTIMAL;
            return Suitability.OKAY;
        }

        @Override
        public PresenceFactory.Suitability
            considerServer(Configuration params) {
            if (params.lacks(Carp.PLACEMENT)) return Suitability.UNMET;
            if (params.lacks(Carp.CLIENTS)) return Suitability.SUBOPTIMAL;
            return Suitability.OKAY;
        }

        @Override
        public PresenceFactory.Suitability consider(Configuration params) {
            if (params.lacksAny(Carp.CLIENTS, Carp.PLACEMENT))
                return Suitability.UNMET;
            return Suitability.OKAY;
        }

        @Override
        public ClientPresence buildClient(Configuration params) {
            params.require(Carp.CLIENTS, "no HTTP clients");
            var clients = params.get(Carp.CLIENTS);
            var fingerprints = params.get(Carp.FINGERPRINTS);
            return new BasicPresence(clients, null, fingerprints, null, false);
        }

        @Override
        public ServerPresence buildServer(Configuration params) {
            params.require(Carp.PLACEMENT, "no base");
            var fingerprints = params.get(Carp.FINGERPRINTS);
            var location = params.get(Carp.PLACEMENT);
            var executor =
                params.computeIfAbsent(Carp.ASYNCHRONOUS_EXECUTOR,
                                       () -> Executors.newFixedThreadPool(3));
            BasicPresence result =
                new BasicPresence(null, location, fingerprints, executor,
                                  false);
            result.register();
            return result;
        }

        @Override
        public Presence build(Configuration params) {
            params.require(Carp.CLIENTS, "no HTTP clients");
            params.require(Carp.PLACEMENT, "no base");
            var clients = params.get(Carp.CLIENTS);
            var fingerprints = params.get(Carp.FINGERPRINTS);
            var location = params.get(Carp.PLACEMENT);
            var executor =
                params.computeIfAbsent(Carp.ASYNCHRONOUS_EXECUTOR,
                                       () -> Executors.newFixedThreadPool(3));
            var shortCircuit = params.get(Carp.LOCAL_SHORT_CIRCUIT, true);
            BasicPresence result =
                new BasicPresence(clients, location, fingerprints, executor,
                                  shortCircuit);
            result.register();
            return result;
        }
    }

    void register() {
        placement.register(this::handleHttpRequest);
    }
}
