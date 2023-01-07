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

import java.io.StringWriter;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonReader;
import javax.json.JsonReaderFactory;
import javax.json.JsonValue;
import javax.json.JsonWriter;
import javax.json.JsonWriterFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.ac.lancs.carp.Fingerprint;
import uk.ac.lancs.carp.InternalServerException;
import uk.ac.lancs.carp.MissingEndpointException;
import uk.ac.lancs.carp.ProtocolException;
import uk.ac.lancs.carp.RemoteInvocationException;
import uk.ac.lancs.carp.TransportException;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.errors.StatusModificationException;
import uk.ac.lancs.carp.map.Argument;
import uk.ac.lancs.carp.map.Builder;
import uk.ac.lancs.carp.map.CallModel;
import uk.ac.lancs.carp.map.Completer;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.ResponseModel;
import uk.ac.lancs.carp.map.Setter;
import uk.ac.lancs.carp.map.TypeModel;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.TypeInfo;
import uk.ac.lancs.carp.model.std.CallSpecification;
import uk.ac.lancs.carp.model.std.InterfaceType;
import uk.ac.lancs.carp.model.std.Member;
import uk.ac.lancs.carp.model.std.ResponseSpecification;

/**
 * Remembers how to implement a proxy for a specific IDL-generated
 * interface type. Its primary purpose is to generate an
 * {@link InvocationHandler} for any given URI endpoint, so that a proxy
 * can be constructed to make calls to that endpoint.
 * 
 * @see #getHandler(java.net.URI)
 * 
 * @resume A client-side call translator
 * 
 * @author simpsons
 */
public class ClientTranslator {
    private static final Method toStringMethod, equalsMethod, hashCodeMethod;
    static {
        /* When we create a proxy for an interface type, we have to pick
         * out these three methods (defined on java.lang.Object) for
         * special treatment. They're not going to change between
         * instances, so we load them statically. */
        try {
            toStringMethod = Object.class.getMethod("toString");
            hashCodeMethod = Object.class.getMethod("hashCode");
            equalsMethod = Object.class.getMethod("equals", Object.class);
        } catch (NoSuchMethodException ex) {
            /* There's something seriously wrong if we can't find
             * toString(), hashCode() and equals(Object) on
             * java.lang.Object. */
            throw new AssertionError("unreachable", ex);
        }
    }

    private static final JsonReaderFactory jsonReaders =
        Json.createReaderFactory(Collections.emptyMap());

    private static final JsonWriterFactory jsonWriters =
        Json.createWriterFactory(Collections.emptyMap());

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
     * Provides the implementation of a single method of an
     * {@link InvocationHandler}.
     */
    private interface MethodImplementation {
        /**
         * Invoke the method. Note that this is identical to
         * {@link InvocationHandler#invoke(Object, Method, Object[])},
         * except that the <code>method</code> argument is missing, as
         * that information has already been used by this stage to
         * select which instance of this interface is to be invoked.
         * 
         * @param proxy the proxy through which the method is being
         * invoked
         * 
         * @param args the method arguments
         * 
         * @return the return type
         * 
         * @throws Throwable if an exception is thrown
         */
        Object invoke(Object proxy, Object[] args) throws Throwable;
    }

    /**
     * Defines how to implement each method of a proxy. Given the URI
     * endpoint to invoke, an invocation handler is returned, allowing a
     * map from {@link Method} to {@link MethodImplementation} to be
     * built. This map can be given to a very simply invocation handler
     * that looks up the method in the map, and relays the call on to
     * the method-specific handler.
     */
    private final Map<Method,
                      Function<? super URI,
                               ? extends MethodImplementation>> callsByMethod =
                                   new HashMap<>();

    /**
     * Create a client-side call translator.
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
     * 
     * @param type the service type
     */
    public ClientTranslator(Class<?> type, LinkContext linkCtxt,
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

        /* To deal with inherited types, keep a map from Java class to
         * model element. */
        class Maplet {
            final InterfaceType model;

            final Map<ExternalName, Class<?>> rspTypes;

            Maplet(Class<?> t) {
                /* Check that the interface type is IDL-generated. */
                var mapping = t.getAnnotation(TypeModel.class);
                if (mapping == null)
                    throw new IllegalArgumentException("non-IDL type: " + t);

                /* Load the IDL type using TypeContext (which should
                 * yield back the same Java type). */
                ExternalName modelName = ExternalName.parse(mapping.value());
                TypeInfo info = linkCtxt.seek(modelName, t.getClassLoader());
                // TODO: Check that the type is the same; info.type ==
                // t.

                /* Iterate over the nested classes to find responses to
                 * each method. */
                Map<ExternalName, Class<?>> rspTypes = new HashMap<>();
                for (var cand : t.getDeclaredClasses()) {
                    var cder = cand.getAnnotation(CallModel.class);
                    if (cder == null) continue;
                    ExternalName cn = ExternalName.parse(cder.value());
                    rspTypes.put(cn, cand);
                }
                this.rspTypes = Map.copyOf(rspTypes);
                this.model = (InterfaceType) info.def;
            }
        }
        Map<Class<?>, Maplet> mapping = new HashMap<>();

        /* Iterate over the methods of the interface type, mapping them
         * to their IDL equivalents. For each, create a translator from
         * the Object[] args array into a JSON object, and a translator
         * from the response JSON object into a Java response type. */
        for (Method meth : type.getMethods()) {
            {
                var mder = meth.getAnnotation(CallModel.class);
                if (mder != null) {
                    Class<?> declarer = meth.getDeclaringClass();
                    Maplet maplet =
                        mapping.computeIfAbsent(declarer, Maplet::new);
                    InterfaceType declarerElem = maplet.model;

                    ExternalName mn = ExternalName.parse(mder.value());
                    CallSpecification cspec = declarerElem.calls.get(mn);
                    Call c = new Call(mn, meth, maplet.rspTypes.get(mn), cspec);
                    callsByMethod.put(meth, c);
                }
            }
        }
    }

    /**
     * Get an invocation handler to invoke a URI. A new one is created;
     * there is no caching.
     * 
     * @param location the remote location
     * 
     * @return the requested invocation handler
     */
    public InvocationHandler getHandler(URI location) {
        Map<Method, MethodImplementation> meths = new HashMap<>();
        meths.putAll(callsByMethod.entrySet().stream().collect(Collectors
            .toMap(Map.Entry::getKey, e -> e.getValue().apply(location))));
        meths.put(equalsMethod, (proxy, args) -> args[0] == proxy);
        meths.put(toStringMethod, (proxy, args) -> "carp:" + location);
        meths.put(hashCodeMethod,
                  (proxy, args) -> System.identityHashCode(proxy));
        var copy = Map.copyOf(meths);
        return (proxy, method, args) -> copy.get(method).invoke(proxy, args);
    }

    private EncodingContext getEncodingContext(Map<? super InetSocketAddress,
                                                   ? super Fingerprint> map) {
        return encodingContextProvider.apply(map);
    }

    private DecodingContext getDecodingContext(Map<? super InetSocketAddress,
                                                   ? extends Fingerprint> map) {
        return decodingContextProvider.apply(map);
    }

    private class Call implements Function<URI, MethodImplementation> {
        private final ExternalName name;

        class Response {
            /**
             * Analyse a response type to determine how to build a
             * response.
             * 
             * @param name the response-type name
             * 
             * @param type the response type
             * 
             * @param spec the response specification
             */
            Response(ExternalName name, Class<?> type,
                     ResponseSpecification spec) {
                this.name = name;

                /* Identify per-field builder initiators, as well as the
                 * empty completer. */
                Map<ExternalName, Method> inits = new HashMap<>();
                Method initDone = null;
                for (var entry : type.getDeclaredMethods()) {
                    var com = entry.getAnnotation(Completer.class);
                    if (com != null) {
                        entry.setAccessible(true);
                        initDone = entry;
                        continue;
                    }

                    var der = entry.getAnnotation(Setter.class);
                    if (der == null) continue;
                    ExternalName n = ExternalName.parse(der.value());
                    entry.setAccessible(true);
                    inits.put(n, entry);
                }
                assert initDone != null;

                /* Identify the builder class. */
                Class<?> builderType =
                    Arrays.asList(type.getDeclaredClasses()).stream()
                        .filter(t -> t.getAnnotation(Builder.class) != null)
                        .findAny().get();

                /* Identify the setters and the completer. */
                Map<ExternalName, Method> setters = new HashMap<>();
                Method done = null;
                for (var entry : builderType.getDeclaredMethods()) {
                    var com = entry.getAnnotation(Completer.class);
                    if (com != null) {
                        entry.setAccessible(true);
                        done = entry;
                        continue;
                    }

                    var der = entry.getAnnotation(Setter.class);
                    if (der == null) continue;
                    ExternalName n = ExternalName.parse(der.value());
                    entry.setAccessible(true);
                    setters.put(n, entry);
                }
                assert done != null;

                /* Ensure we have decoders for each response
                 * parameter. */
                for (var entry : spec.parameters.members.entrySet()) {
                    ExternalName pn = entry.getKey();
                    Member memb = entry.getValue();
                    Decoder codec = memb.type.getDecoder(null, linkCtxt);
                    Method setter = setters.get(pn);
                    Method initSetter = inits.get(pn);
                    InParam par = new InParam(pn, codec, setter, initSetter);
                    params.add(par);
                }

                this.done = done;
                this.initDone = initDone;
            }

            /**
             * Identifies which response type we're dealing with. Note
             * that this value has no function, except for potential
             * diagnostics.
             */
            final ExternalName name;

            /**
             * This method should be invoked statically to complete the
             * response when no argument has been set.
             */
            final Method initDone;

            /**
             * This method should be invoked with the current builder as
             * an argument to complete the response if at least one
             * argument has been set.
             */
            final Method done;

            final Collection<InParam> params = new HashSet<>();

            /**
             * Processes a response parameter.
             */
            class InParam {
                final ExternalName name;

                final Decoder codec;

                final Method setter, initSetter;

                InParam(ExternalName name, Decoder codec, Method setter,
                        Method initSetter) {
                    this.name = name;
                    this.codec = codec;
                    this.setter = setter;
                    this.initSetter = initSetter;
                }

                Object decode(Object builder, JsonObject params,
                              DecodingContext ctxt)
                    throws IllegalAccessException,
                        InvocationTargetException {
                    JsonValue v = params.getOrDefault(name.toString(), null);
                    if (v == null) return builder;
                    Object jv = codec.decodeJson(v, ctxt);
                    if (builder == null)
                        return initSetter.invoke(null, jv);
                    else
                        return setter.invoke(builder, jv);
                }
            }

            Object decode(DecodingContext ctxt, JsonObject params)
                throws IllegalAccessException,
                    InvocationTargetException {
                Object builder = null;
                for (InParam ip : this.params)
                    builder = ip.decode(builder, params, ctxt);
                return builder == null ? initDone.invoke(null) :
                    done.invoke(builder);
            }
        }

        Call(ExternalName name, Method meth, Class<?> rspType,
             CallSpecification spec) {
            this.name = name;

            /* Ensure that every parameter has a codec, indexed by
             * parameter number. */
            List<OutParam> params = new ArrayList<>(meth.getParameterCount());
            for (var item : meth.getParameters()) {
                var der = item.getAnnotation(Argument.class);
                ExternalName parName = ExternalName.parse(der.value());
                Member memb = spec.parameters.members.get(parName);
                Encoder codec = memb.type.getEncoder(null, linkCtxt);
                params.add(new OutParam(parName, codec));
            }
            this.params = List.copyOf(params);

            /* Scan the nested types for classes holding responses. */
            Map<ExternalName, Class<?>> rspTypes = new HashMap<>();
            if (rspType != null) {
                for (Class<?> cand : rspType.getDeclaredClasses()) {
                    var cder = cand.getAnnotation(ResponseModel.class);
                    if (cder == null) continue;
                    ExternalName rname = ExternalName.parse(cder.value());
                    rspTypes.put(rname, cand);
                }
            }

            /* Ensure we know how to process every response type. */
            Map<ExternalName, Response> responses = new HashMap<>();
            for (var entry : spec.responses.entrySet()) {
                /* Identify the response type. */
                ExternalName key = entry.getKey();
                ResponseSpecification rspec = entry.getValue();
                Class<?> rt = rspTypes.get(key);
                Response rsp = new Response(key, rt, rspec);
                responses.put(key, rsp);
            }
            this.responses = Map.copyOf(responses);
        }

        class OutParam {
            final ExternalName name;

            final Encoder codec;

            OutParam(ExternalName name, Encoder codec) {
                this.name = name;
                this.codec = codec;
            }

            void encode(JsonObjectBuilder dest, EncodingContext ctxt,
                        Object arg) {
                dest.add(name.toString(), codec.encodeJson(arg, ctxt));
            }
        }

        private final List<OutParam> params;

        private final Map<ExternalName, Response> responses;

        @Override
        public MethodImplementation apply(URI base) {
            return (proxy, args) -> {
                Map<InetSocketAddress, Fingerprint> outPrints = new HashMap<>();
                EncodingContext outCtxt = getEncodingContext(outPrints);

                /* Build up outgoing parameters. */
                JsonObjectBuilder outBuilder = Json.createObjectBuilder();
                if (args != null) for (int i = 0; i < args.length; i++)
                    params.get(i).encode(outBuilder, outCtxt, args[i]);

                /* Create the outgoing request. */
                JsonObjectBuilder reqBuilder = Json.createObjectBuilder();
                reqBuilder.add("req-type", name.toString());
                reqBuilder.add("req", outBuilder);
                reqBuilder.add("prints", Internals.encodeFromMap(outPrints));
                JsonObject req = reqBuilder.build();

                final JsonObject rsp;
                {
                    /* Convert the JSON into an entity. */
                    StringWriter out = new StringWriter();
                    try (JsonWriter writer = jsonWriters.createWriter(out)) {
                        writer.writeObject(req);
                    }
                    HttpEntity reqent = EntityBuilder.create()
                        .setContentType(ContentType.APPLICATION_JSON)
                        .setText(out.toString()).build();
                    logger.fine(() -> String.format("Request: %s%n",
                                                    out.toString()));

                    /* Create the request. */
                    HttpPost treq = new HttpPost(base);
                    treq.setEntity(reqent);

                    /* Call the server. */
                    try (CloseableHttpClient client = clientFactory.get();
                         CloseableHttpResponse trsp = client.execute(treq)) {
                        logger.fine(() -> String
                            .format("Response code: %d%n",
                                    trsp.getStatusLine().getStatusCode()));

                        /* Check for responses that don't imply a JSON
                         * response. */
                        int rcode = trsp.getStatusLine().getStatusCode();
                        switch (rcode) {
                        case HttpStatus.SC_NO_CONTENT:
                            if (responses.isEmpty()) return null;
                            throw new RemoteInvocationException("empty response");

                        case HttpStatus.SC_NOT_FOUND:
                            throw new MissingEndpointException(base.toString());

                        default:
                            break;
                        }

                        /* Check that the entity response type is
                         * JSON. */
                        ContentType rtype =
                            ContentType.getLenient(trsp.getEntity());
                        if (!ContentType.APPLICATION_JSON.getMimeType()
                            .equalsIgnoreCase(rtype.getMimeType()))
                            throw new ProtocolException("non-JSON (" + rtype
                                + ") from " + base);

                        /* Decode the response. */
                        try (JsonReader reader = jsonReaders
                            .createReader(trsp.getEntity().getContent(),
                                          StandardCharsets.UTF_8)) {
                            rsp = reader.readObject();
                        }

                        /* Check for a normal response type. */
                        switch (rcode) {
                        case HttpStatus.SC_OK:
                            break;

                        case HttpStatus.SC_UNPROCESSABLE_ENTITY:
                            throw new StatusModificationException(paramsOf(rsp
                                .getJsonObject("params")), rsp
                                    .getString("message"));

                        case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                            UUID errorId =
                                UUID.fromString(rsp.getString("error"));
                            throw new InternalServerException(errorId,
                                                              "server error "
                                                                  + errorId);

                        default:
                            throw new RemoteInvocationException("bad code "
                                + rcode + " from " + base);
                        }
                    } catch (RuntimeException | Error ex) {
                        throw ex;
                    } catch (Throwable ex) {
                        throw new TransportException(base.toString(), ex);
                    }
                }
                logger.fine(() -> String.format("Rsp: %s%n", rsp));

                /* Record the fingerprints of host:port tuples. Don't
                 * accept them yet. */
                final Map<InetSocketAddress, Fingerprint> candPrints =
                    Internals.decodeToMap(rsp.getJsonArray("prints"));

                /* Identify the response type, decode the response, and
                 * return it. */
                ExternalName rspName =
                    ExternalName.parse(rsp.getString("rsp-type"));
                Response rspObj = responses.get(rspName);
                DecodingContext inCtxt = getDecodingContext(candPrints);
                return rspObj.decode(inCtxt, rsp.getJsonObject("rsp"));
            };
        }
    }

    private static Map<String, String> paramsOf(JsonObject obj) {
        Map<String, String> params = new HashMap<>();
        for (var key : obj.keySet())
            params.put(key, obj.getString(key));
        return params;
    }

    private static final Logger logger =
        Logger.getLogger("uk.ac.lancs.carp.client");
}
