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

package uk.ac.lancs.carp;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.function.BiFunction;
import java.util.function.Supplier;

/**
 * Holds the configuration for creating a presence.
 *
 * @author simpsons
 */
public final class Configuration {
    /**
     * Identifies a presence parameter.
     *
     * @param <T> the parameter type
     */
    public static class Key<T> {
        Key() {}
    }

    private final Map<Key<?>, Object> settings;

    /**
     * Create a presence parameter identifier.
     *
     * @param <T> the parameter type
     *
     * @return a new parameter identifier
     *
     * @constructor
     */
    public static <T> Key<T> key() {
        return new Key<>();
    }

    /**
     * Get a presence parameter.
     *
     * @param <T> the parameter type
     *
     * @param key the key identifying the parameter
     *
     * @return the parameter value; or {@code null} if not set
     */
    @SuppressWarnings(value = "unchecked")
    public <T> T get(Key<T> key) {
        return (T) settings.get(key);
    }

    /**
     * Get a parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public <T> T get(Key<T> key, T defaultValue) {
        T v = get(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Get a Boolean parameter value, or compute a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultSupplier invoked to provide the default value if
     * the parameter is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public <T> T computeIfAbsent(Key<T> key,
                                 Supplier<? extends T> defaultSupplier) {
        Objects.requireNonNull(defaultSupplier, "defaultValue");
        T v = get(key);
        return v == null ? defaultSupplier.get() : v;
    }

    /**
     * Get an integer parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public int get(Key<? extends Number> key, int defaultValue) {
        Number v = get(key);
        return v == null ? defaultValue : v.intValue();
    }

    /**
     * Get a long-integer parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public long get(Key<? extends Number> key, long defaultValue) {
        Number v = get(key);
        return v == null ? defaultValue : v.longValue();
    }

    /**
     * Get a double-precision floating-point parameter value, or a
     * default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public double get(Key<? extends Number> key, double defaultValue) {
        Number v = get(key);
        return v == null ? defaultValue : v.doubleValue();
    }

    /**
     * Get a byte parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public byte get(Key<? extends Byte> key, byte defaultValue) {
        Byte v = get(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Get a short integer parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public short get(Key<? extends Short> key, short defaultValue) {
        Short v = get(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Get a character parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public char get(Key<? extends Character> key, char defaultValue) {
        Character v = get(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Get a single-precision floating-point parameter value, or a
     * default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public float get(Key<? extends Number> key, float defaultValue) {
        Number v = get(key);
        return v == null ? defaultValue : v.floatValue();
    }

    /**
     * Get a Boolean parameter value, or a default value.
     *
     * @param key the key identifying the parameter
     *
     * @param defaultValue the default value to return if the parameter
     * is unset
     *
     * @return the parameter value; or the default value if not set
     */
    public boolean get(Key<? extends Boolean> key, boolean defaultValue) {
        Boolean v = get(key);
        return v == null ? defaultValue : v;
    }

    /**
     * Determine whether a parameter has been specified. This is the
     * direct negation of {@link #lacks(Key)}.
     *
     * @param key the key identifying the parameter
     *
     * @return {@code true} if the parameter has been set; {@code false}
     * otherwise
     */
    public boolean has(Key<?> key) {
        return settings.containsKey(key);
    }

    /**
     * Determine whether a parameter has been omitted. This is the
     * direct negation of {@link #has(Key)}.
     *
     * @param key the key identifying the parameter
     *
     * @return {@code false} if the parameter has been set; {@code true}
     * otherwise
     */
    public boolean lacks(Key<?> key) {
        return !has(key);
    }

    /**
     * Determine whether any of several parameters have been omitted.
     *
     * @param keys the keys identifying required parameters
     *
     * @return {@code false} if all parameters have been set;
     * {@code false} otherwise
     */
    public boolean lacksAny(Key<?>... keys) {
        for (Key<?> key : keys)
            if (lacks(key)) return true;
        return false;
    }

    /**
     * Ensure that a parameter has been specified.
     *
     * @param key the key identifying the parameter
     *
     * @param msg the message to use in the exception
     *
     * @throws IllegalArgumentException if the parameter is absent,
     * using the supplied text as the detail message
     */
    public void require(Key<?> key, String msg) {
        if (lacks(key)) throw new IllegalArgumentException(msg);
    }

    private Configuration(Map<Key<?>, Object> settings) {
        super();
        this.settings = settings;
    }

    /**
     * Creates a presence configuration in stages.
     */
    public static final class Builder {
        private final Map<Key<?>, Object> settings = new IdentityHashMap<>();

        private Builder() {
            super();
        }

        /**
         * Specify a parameter.
         *
         * @param <T> the parameter type
         *
         * @param key the key identifying the parameter
         *
         * @param value the new value
         *
         * @return this object
         *
         * @throws NullPointerException if the value is {@code null}
         */
        public <T> Builder with(Key<T> key, T value) {
            Objects.requireNonNull(value);
            settings.put(key, value);
            return this;
        }

        /**
         * De-specify a parameter.
         *
         * @param key the key identifying the parameter
         *
         * @return this object
         */
        public Builder reset(Key<?> key) {
            settings.remove(key);
            return this;
        }

        /**
         * Enable a flag.
         *
         * @param key the key identifying the Boolean parameter
         *
         * @return this object
         */
        public Builder enable(Key<Boolean> key) {
            return with(key, true);
        }

        /**
         * Disable a flag.
         *
         * @param key the key identifying the Boolean parameter
         *
         * @return this object
         */
        public Builder disable(Key<Boolean> key) {
            return with(key, false);
        }

        /**
         * Complete the configuration.
         *
         * @return the completed configuration
         */
        Configuration complete() {
            return new Configuration(Map.copyOf(settings));
        }

        /**
         * Create a server presence.
         *
         * @return the new presence
         *
         * @throws IllegalArgumentException if these parameters are
         * insufficient
         */
        public ServerPresence buildServer() {
            return complete().build(PresenceFactory::considerServer,
                                    PresenceFactory::buildServer);
        }

        /**
         * Create a client presence.
         *
         * @return the new presence
         *
         * @throws IllegalArgumentException if these parameters are
         * insufficient
         */
        public ClientPresence buildClient() {
            return complete().build(PresenceFactory::considerClient,
                                    PresenceFactory::buildClient);
        }

        /**
         * Create a duplex presence.
         *
         * @return the new presence
         *
         * @throws IllegalArgumentException if these parameters are
         * insufficient
         */
        public Presence build() {
            return complete().build(PresenceFactory::consider,
                                    PresenceFactory::build);
        }
    }

    /**
     * Create a fresh builder.
     *
     * @return a builder with default settings
     */
    static Builder builder() {
        return new Builder();
    }

    private <R> R
        build(BiFunction<? super PresenceFactory, ? super Configuration,
                         ? extends PresenceFactory.Suitability> test,
              BiFunction<? super PresenceFactory, ? super Configuration,
                         ? extends R> op) {
        Map<PresenceFactory.Suitability, PresenceFactory> first =
            new EnumMap<>(PresenceFactory.Suitability.class);
        final int size = PresenceFactory.Suitability.values().length;
        for (PresenceFactory cand : ServiceLoader.load(PresenceFactory.class)) {
            PresenceFactory.Suitability suit = test.apply(cand, this);
            first.putIfAbsent(suit, cand);
            if (suit == PresenceFactory.Suitability.OKAY) break;
            if (first.size() >= size) break;
        }
        PresenceFactory chosen = first
            .compute(PresenceFactory.Suitability.OKAY, (k, v) -> v != null ? v :
                first.compute(PresenceFactory.Suitability.SUBOPTIMAL,
                              (k2, v2) -> v2 != null ? v2 : first
                                  .get(PresenceFactory.Suitability.OVERKILL)));
        if (chosen == null)
            throw new IllegalArgumentException("insufficient paramaters"
                + " for presence creation");
        return op.apply(chosen, this);
    }

}
