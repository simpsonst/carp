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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonNumber;
import javax.json.JsonObject;
import uk.ac.lancs.carp.Fingerprint;

/**
 * Holds static methods that don't need to be exposed to a regular user.
 * 
 * <p>
 * This class runs a single daemon thread to check for expired
 * references created by {@link #watch(Object, Consumer)}. The thread is
 * named after this class.
 * 
 * @author simpsons
 */
class Internals {
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();

    private static class ActionReference<T> extends WeakReference<T> {
        final Consumer<? super Reference<T>> action;

        public ActionReference(T referent,
                               Consumer<? super Reference<T>> action) {
            super(referent, queue);
            this.action = action;
        }

        void run() {
            action.accept(this);
        }
    }

    /**
     * Get a weak reference to an object, ensuring that an action is
     * invoked when it is garbage-collected.
     * 
     * @param <T> the object type
     * 
     * @param referent the object to watch
     * 
     * @param action an action to be invoked with the returned reference
     * when the object is garbage-collected
     * 
     * @return a weak reference to the object
     */
    static <T> Reference<T> watch(T referent,
                                  Consumer<? super Reference<T>> action) {
        return new ActionReference<>(referent, action);
    }

    static {
        /* Create a daemon thread to run the actions of discarded
         * references, and cycle the pack-rat collections. */
        Thread t = new Thread(Internals.class.getCanonicalName()) {
            @Override
            public void run() {
                for (;;) {
                    try {
                        /* Check for a discarded reference, and run its
                         * clean-up action. */
                        ActionReference<?> ref =
                            (ActionReference<?>) queue.remove();
                        ref.run();
                    } catch (RuntimeException | InterruptedException ex) {
                        /* Don't care and shouldn't be a reason for us
                         * to stop. */
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    /**
     * Create a fingerprint from its JSON representation.
     *
     * @param obj the JSON representation
     *
     * @return the constructed hash-code object
     */
    public static Fingerprint of(JsonObject obj) {
        String algo = obj.getString("algo");
        IntStream bytes = obj.getJsonArray("val").getValuesAs(JsonNumber.class)
            .stream().mapToInt(JsonNumber::intValue).map(v -> (byte) v);
        return Fingerprint.of(algo, bytes);
    }

    /**
     * Create a JSON representation of a fingerprint.
     * 
     * @return the JSON representation
     */
    public static JsonObject toJson(Fingerprint print) {
        JsonArrayBuilder val = Json.createArrayBuilder();
        for (int b : print.getBytes().toArray())
            val.add(b & 0xff);
        return Json.createObjectBuilder().add("algo", print.getAlgorithm())
            .add("val", val).build();
    }

    /**
     * Encode a mapping from peer identity to fingerprint into a JSON
     * array.
     *
     * @param in the mapping to encode
     *
     * @return a JSON array representing the mapping
     */
    public static JsonArray encodeFromMap(Map<? extends InetSocketAddress,
                                              ? extends Fingerprint> in) {
        JsonArrayBuilder b = Json.createArrayBuilder();
        for (Map.Entry<? extends InetSocketAddress,
                       ? extends Fingerprint> e : in.entrySet())
            b.add(Json.createObjectBuilder()
                .add("host", e.getKey().getHostString())
                .add("port", e.getKey().getPort())
                .add("print", Internals.toJson(e.getValue())));
        return b.build();
    }

    /**
     * Decode a JSON array to a mapping from peer identity to
     * fingerprint.
     *
     * @param in the array to decode
     *
     * @return an immutable map represented by the JSON
     */
    public static Map<InetSocketAddress, Fingerprint>
        decodeToMap(JsonArray in) {
        return in.getValuesAs(JsonObject.class).stream()
            .collect(Collectors.toMap(e -> {
                String host = e.getString("host");
                int port = e.getInt("port");
                return InetSocketAddress.createUnresolved(host, port);
            }, e -> Internals.of(e.getJsonObject("print"))));
    }

    private Internals() {}
}
