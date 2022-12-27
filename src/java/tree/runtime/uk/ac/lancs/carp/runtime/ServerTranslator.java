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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import uk.ac.lancs.carp.Fingerprint;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.DecodingContext;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.codec.EncodingContext;
import uk.ac.lancs.carp.map.Accessor;
import uk.ac.lancs.carp.map.Argument;
import uk.ac.lancs.carp.map.CallModel;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.map.Getter;
import uk.ac.lancs.carp.map.ResponseModel;
import uk.ac.lancs.carp.map.Tester;
import uk.ac.lancs.carp.map.TypeModel;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.TypeInfo;
import uk.ac.lancs.carp.model.std.CallSpecification;
import uk.ac.lancs.carp.model.std.InterfaceType;
import uk.ac.lancs.carp.model.std.Member;
import uk.ac.lancs.carp.model.std.ResponseSpecification;

/**
 * Remembers how to translate an incoming JSON request into an
 * invocation on an interface, and translate the result back.
 * 
 * @resume A server-side call translator
 * 
 * @author simpsons
 */
public class ServerTranslator {
    /**
     * Locates run-time IDL type definitions.
     */
    private final LinkContext linkCtxt;

    /**
     * Invokes one-way user-defined methods (i.e., those returning
     * void).
     */
    private final Executor executor;

    /**
     * Creates encoding contexts to allow a receiver to generate
     * response messages. The provided table maps peer addresses to
     * their fingerprints, and should be populated by the returned
     * implementation. The final table state will then be added to the
     * response message as meta-data.
     */
    private final Function<? super Map<? super InetSocketAddress,
                                       ? super Fingerprint>,
                           ? extends EncodingContext> encodingContextProvider;

    /**
     * Creates decoding contexts to allow request messages to be
     * decoded. The provided table maps peer addresses to their
     * fingerprints, and must already be populated using meta-data in
     * the request.
     */
    private final Function<? super Map<? super InetSocketAddress,
                                       ? extends Fingerprint>,
                           ? extends DecodingContext> decodingContextProvider;

    /**
     * Holds a record of all call types found within the interface type.
     */
    private final Map<ExternalName, Call> calls;

    /**
     * Create a server-side call translator.
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
     * @param type the service type
     * 
     * @param executor a means to execute asynchronous calls
     */
    public ServerTranslator(Class<?> type, LinkContext linkCtxt,
                            Executor executor,
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

        InterfaceType modelElem =
            mapping.computeIfAbsent(type, Maplet::new).model;

        /**
         * Iterate over the methods of the interface type, mapping them
         * to their IDL equivalents. For each, create an object that
         * will translate a JSON message into an argument list, and
         * reflectively invoke the message, then translate the response
         * back into JSON, and return.
         */
        Map<ExternalName, Call> calls = new HashMap<>();
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
                    Call c = new Call(meth, maplet.rspTypes.get(mn), cspec);
                    calls.put(mn, c);
                }
            }
        }
        this.calls = Map.copyOf(calls);
    }

    /**
     * Invoke a regular method on a receiver.
     * 
     * @param receiver an implementation of the service type specified
     * during construction
     * 
     * @param req a JSON object consisting of the fields
     * <samp>prints</samp> (holding an array of fingerprints),
     * <samp>req-type</samp> (a JSON string identifying the method to
     * invoke) and <samp>req</samp> (a JSON object specifying the
     * request arguments)
     * 
     * @return a JSON object consisting of the fields
     * <samp>prints</samp> (holding an array of fingerprints),
     * <samp>rsp-type</samp> (a JSON string identifying the response
     * type) and <samp>rsp</samp> (holding a JSON object providing the
     * response arguments)
     * 
     * @throws InvocationTargetException a checked exception is thrown
     * by the receiver
     * 
     * @throws IllegalAccessException if the receiver's method is
     * inaccessible
     */
    public JsonObject invoke(Object receiver, JsonObject req)
        throws InvocationTargetException,
            IllegalAccessException {
        /* Make a note of any client-supplied fingerprints. */
        Map<InetSocketAddress, Fingerprint> foreignPrints =
            Internals.decodeToMap(req.getJsonArray("prints"));
        Map<InetSocketAddress, Fingerprint> nativePrints = new HashMap<>();
        DecodingContext decCtxt = getDecodingContext(foreignPrints);
        EncodingContext encCtxt = getEncodingContext(nativePrints);

        /* Identify the method. */
        ExternalName methName = ExternalName.parse(req.getString("req-type"));
        Call call = calls.get(methName);

        /* Invoke the receiver. */
        JsonObject reqBody = req.getJsonObject("req");
        JsonObjectBuilder rspBuilder = Json.createObjectBuilder();
        boolean data =
            call.invoke(receiver, decCtxt, encCtxt, reqBody, rspBuilder);
        if (data) {
            /* Tack on the fingerprints of any peers we've mentioned in
             * the response. */
            rspBuilder.add("prints", Internals.encodeFromMap(nativePrints));
            return rspBuilder.build();
        } else {
            /* This is an asynchronous call; the result is empty. */
            return null;
        }
    }

    private class Call {
        private class InParam {
            final ExternalName name;

            final Decoder codec;

            Object decode(JsonObject req, DecodingContext ctxt) {
                System.err.printf("getting in %s%n", name);
                return codec.decodeJson(req.get(name.toString()), ctxt);
            }

            InParam(ExternalName name, Decoder codec) {
                this.name = name;
                this.codec = codec;
            }
        }

        private class Response {
            class OutParam {
                final Method access;

                final Encoder codec;

                final ExternalName name;

                OutParam(ExternalName name, Encoder codec, Method access) {
                    this.access = access;
                    this.codec = codec;
                    this.name = name;
                }

                /**
                 * Encode a single response field.
                 * 
                 * @param inner the value to encode
                 * 
                 * @param ctxt a context for encoding the value into
                 * JSON
                 * 
                 * @param rspBuilder the place to add the encoded field
                 * 
                 * @return {@code true} if a non-{@code null} value was
                 * encoded; {@code false} otherwise
                 * 
                 * @throws IllegalAccessException if the field could not
                 * be accessed reflectively
                 * 
                 * @throws InvocationTargetException if the way to
                 * access the field was broken in some awful way
                 */
                boolean encode(Object inner, EncodingContext ctxt,
                               JsonObjectBuilder rspBuilder)
                    throws IllegalAccessException,
                        InvocationTargetException {
                    Object value = access.invoke(inner);
                    if (value == null) return false;
                    JsonValue jv = codec.encodeJson(value, ctxt);
                    rspBuilder.add(name.toString(), jv);
                    return true;
                }
            }

            final ExternalName name;

            final Method test;

            final Method access;

            final Collection<OutParam> params;

            /**
             * 
             * @param name
             * @param test the method on the generic response class that
             * determines which specific response type has been returned
             * 
             * @param access the method on the generic response class
             * that yields an instance of the specific response class
             * 
             * @param type the specific response class
             * 
             * @param spec
             */
            Response(ExternalName name, Method test, Method access,
                     Class<?> type, ResponseSpecification spec) {
                this.name = name;
                this.test = test;
                this.access = access;

                /* Create an index of getters on the specific response
                 * type. */
                Map<ExternalName, Method> getters = new HashMap<>();
                for (var entry : type.getDeclaredMethods()) {
                    var der = entry.getAnnotation(Getter.class);
                    if (der == null) continue;
                    ExternalName fn = ExternalName.parse(der.value());
                    getters.put(fn, entry);
                }

                /* Create encoders for each response parameter. */
                Collection<OutParam> params = new ArrayList<>();
                for (var entry : spec.parameters.members.entrySet()) {
                    ExternalName pn = entry.getKey();
                    Member memb = entry.getValue();
                    Encoder codec = memb.type.getEncoder(null, linkCtxt);
                    Method getter = getters.get(pn);
                    OutParam par = new OutParam(pn, codec, getter);
                    params.add(par);
                }
                this.params = List.copyOf(params);
            }

            /**
             * Attempt to recognize the response object, and encode a
             * response.
             * 
             * @param raw the value to be encoded
             * 
             * @param ctxt a context for encoding the response object
             * into JSON
             * 
             * @param rspBuilder the destination for encoded fields
             * 
             * @return {@code true} if the object was recognized;
             * {@code false} otherwise
             */
            boolean encode(Object raw, EncodingContext ctxt,
                           JsonObjectBuilder rspBuilder)
                throws IllegalAccessException,
                    InvocationTargetException {
                if (!((boolean) test.invoke(raw))) return false;
                Object inner = access.invoke(raw);

                for (OutParam p : params)
                    p.encode(inner, ctxt, rspBuilder);
                return true;
            }
        }

        private final Method meth;

        private final List<InParam> params;

        private final Collection<Response> responseTypes;

        /**
         * 
         * @param meth the method on the interface type to be invoked
         * 
         * @param type the generic response type
         * 
         * @param spec
         */
        Call(Method meth, Class<?> type, CallSpecification spec) {
            this.meth = meth;

            /* Create translators for each call parameter. */
            this.params = new ArrayList<>(meth.getParameterCount());
            for (var item : meth.getParameters()) {
                var der = item.getAnnotation(Argument.class);
                ExternalName pn = ExternalName.parse(der.value());
                Member memb = spec.parameters.members.get(pn);
                Decoder codec = memb.type.getDecoder(null, linkCtxt);
                this.params.add(new InParam(pn, codec));
            }

            /* Create an index of type-testing and accessor methods. */
            Map<ExternalName, Method> testers = new HashMap<>();
            Map<ExternalName, Method> accessors = new HashMap<>();
            Map<ExternalName, Class<?>> rspTypes = new HashMap<>();
            if (type != null) {
                for (var entry : type.getDeclaredMethods()) {
                    var der = entry.getAnnotation(Accessor.class);
                    if (der != null) {
                        ExternalName rtn = ExternalName.parse(der.value());
                        accessors.put(rtn, entry);
                    }

                    var tes = entry.getAnnotation(Tester.class);
                    if (tes != null) {
                        ExternalName rtn = ExternalName.parse(tes.value());
                        testers.put(rtn, entry);
                    }
                }

                /* Create an index of response types. */
                for (var entry : type.getDeclaredClasses()) {
                    var der = entry.getAnnotation(ResponseModel.class);
                    if (der != null) {
                        ExternalName rtn = ExternalName.parse(der.value());
                        rspTypes.put(rtn, entry);
                    }
                }
            }

            /* Set up the handlers for each response type. */
            Collection<Response> responseTypes = new ArrayList<>();
            for (var entry : spec.responses.entrySet()) {
                ExternalName rn = entry.getKey();
                ResponseSpecification rspec = entry.getValue();
                Method access = accessors.get(rn);
                Method test = testers.get(rn);
                Class<?> rtype = rspTypes.get(rn);
                Response rsp = new Response(rn, test, access, rtype, rspec);
                responseTypes.add(rsp);
            }
            this.responseTypes = List.copyOf(responseTypes);
        }

        /**
         * Decode a request into a parameter list, invoke a method on an
         * object with them, and encode the response into JSON.
         * 
         * @param receiver the object to invoke
         * 
         * @param encCtxt a context for encoding the response
         * 
         * @param decCtxt a context for decoding the request
         * 
         * @param req the request body (the value of the
         * <samp>req</samp> field)
         * 
         * @param rspBuilder a builder to which <samp>rsp-type</samp>
         * and <samp>rsp</samp> fields should be added
         * 
         * @return {@code true} if a response was added; {@code false}
         * if the method generates no response
         */
        boolean invoke(Object receiver, DecodingContext decCtxt,
                       EncodingContext encCtxt, JsonObject req,
                       JsonObjectBuilder rspBuilder)
            throws IllegalAccessException,
                InvocationTargetException {
            System.err.printf("Incoming request: %s%n", req);
            /* Build up the argument list. */
            Object[] params = new Object[this.params.size()];
            for (int i = 0; i < params.length; i++) {
                InParam spec = this.params.get(i);
                params[i] = spec.decode(req, decCtxt);
            }

            if (responseTypes.isEmpty()) {
                /* Invoke the native object on an executor. */
                executor.execute(() -> {
                    try {
                        try {
                            meth.invoke(receiver, params);
                        } catch (InvocationTargetException ex) {
                            throw ex.getCause();
                        }
                    } catch (Throwable t) {
                        logger.log(Level.SEVERE,
                                   "error on asynchronous invocation", t);
                    }
                });
                return false;
            }

            /* Invoke the native object. */
            Object result = meth.invoke(receiver, params);

            /* Match one of the response types to encode it. */
            JsonObjectBuilder outs = Json.createObjectBuilder();
            for (Response rt : responseTypes)
                if (rt.encode(result, encCtxt, outs)) {
                    rspBuilder.add("rsp", outs);
                    rspBuilder.add("rsp-type", rt.name.toString());
                    return true;
                }

            /* This should not be reached. */
            throw new AssertionError("unreachable");
        }
    }

    private EncodingContext getEncodingContext(Map<? super InetSocketAddress,
                                                   ? super Fingerprint> map) {
        return encodingContextProvider.apply(map);
    }

    private DecodingContext getDecodingContext(Map<? super InetSocketAddress,
                                                   ? extends Fingerprint> map) {
        return decodingContextProvider.apply(map);
    }

    private static final Logger logger =
        Logger.getLogger("uk.ac.lancs.carp.server");
}
