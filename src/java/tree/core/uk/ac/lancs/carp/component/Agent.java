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

package uk.ac.lancs.carp.component;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generates receivers for components of a containing receiver.
 *
 * @author simpsons
 */
public abstract class Agent {
    Agent() {}

    /**
     * Match the component in the given sub-path.
     * 
     * @param container the container of the component
     * 
     * @param subpath the sub-path to match against
     * 
     * @return the component details, including any parts of the
     * sub-path not matched; or {@code null} if the sub-path is not
     * matched
     */
    abstract Match match(Object container,
                         List<? extends CharSequence> subpath);

    /**
     * Get the service type of the components handled by this agent.
     * 
     * @return the components' service type
     */
    abstract Class<?> serviceType();

    /**
     * Learns of dynamically created components.
     */
    interface Listener {
        /**
         * Record that a new component has been created.
         * 
         * @param subpath the sub-path of the component below its
         * selecting prefix
         * 
         * @param receiver the new receiver
         * 
         * @param agency the receiver's own agency
         * 
         * @return {@code true} if the listener should not be invoked
         * again; {@code false} otherwise
         */
        boolean update(List<? extends CharSequence> subpath, Object receiver,
                       Agency agency);
    }

    /**
     * Register for updates on components being created.
     * 
     * @param container the container of the components
     * 
     * @param listener how to inform about new components
     */
    abstract void register(Object container, Listener listener);

    /**
     * Define a shared component family with a destructor.
     * 
     * @param serviceType the service type implemented by all instances
     * in the family
     * 
     * @param constructor a means to create instances on demand, given
     * the container and an instance id
     * 
     * @param destructor a means to notify of the garbage collection of
     * an instance
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param discr a means to convert between an id and its string
     * representation in a URI
     * 
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param <Id> the index type
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl, Id> StaticIndexedAgent<Ctr, Impl, Id>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Discriminator<Id> discr,
           BiFunction<? super Ctr, ? super Id,
                      ? extends ManagedReceiver<? extends Impl>> constructor,
           BiConsumer<? super Ctr, ? super Id> destructor) {
        return new StaticIndexedAgent<>(containerType, serviceType, discr,
                                        constructor, destructor);
    }

    /**
     * Define a shared component family.
     * 
     * @param serviceType the service type implemented by all instances
     * in the family
     * 
     * @param constructor a means to create instances on demand, given
     * the container and an instance id
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param discr a means to convert between an id and its string
     * representation in a URI
     * 
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param <Id> the index type
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl, Id> StaticIndexedAgent<Ctr, Impl, Id>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Discriminator<Id> discr,
           BiFunction<? super Ctr, ? super Id,
                      ? extends ManagedReceiver<? extends Impl>> constructor) {
        return of(containerType, serviceType, discr, constructor, (c, i) -> {});
    }

    /**
     * Define a shared singleton component with a destructor.
     *
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param serviceType the service type implemented the component
     * 
     * @param constructor a means to create instances on demand, given
     * the container
     * 
     * @param destructor a means to notify of the garbage collection of
     * an instance
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param container the container
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl> SingletonAgent<Impl>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Ctr container,
           Function<? super Ctr,
                    ? extends ManagedReceiver<? extends Impl>> constructor,
           Consumer<? super Ctr> destructor) {
        return new SingletonAgent<>(containerType, serviceType, container,
                                    constructor, destructor);
    }

    /**
     * Define a shared singleton component.
     *
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param serviceType the service type implemented by the component
     * 
     * @param constructor a means to create instances on demand, given
     * the container
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param container the container
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl> SingletonAgent<Impl>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Ctr container,
           Function<? super Ctr,
                    ? extends ManagedReceiver<? extends Impl>> constructor) {
        return of(containerType, serviceType, container, constructor, c -> {});
    }

    /**
     * Define a single-container component family with a destructor.
     * 
     * @param serviceType the service type implemented by all instances
     * in the family
     * 
     * @param constructor a means to create instances on demand, given
     * the container and an instance id
     * 
     * @param destructor a means to notify of the garbage collection of
     * an instance
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param discr a means to convert between an id and its string
     * representation in a URI
     * 
     * @param container the container
     * 
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param <Id> the index type
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl, Id> IndexedAgent<Impl, Id>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Ctr container, Discriminator<Id> discr,
           BiFunction<? super Ctr, ? super Id,
                      ? extends ManagedReceiver<? extends Impl>> constructor,
           BiConsumer<? super Ctr, ? super Id> destructor) {
        return new IndexedAgent<>(containerType, serviceType, container, discr,
                                  constructor, destructor);
    }

    /**
     * Define a single-container component family.
     * 
     * @param serviceType the service type implemented by all instances
     * in the family
     * 
     * @param constructor a means to create instances on demand, given
     * the container and an instance id
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     * 
     * @param discr a means to convert between an id and its string
     * representation in a URI
     * 
     * @param container the container
     * 
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @param <Id> the index type
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl, Id> IndexedAgent<Impl, Id>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Ctr container, Discriminator<Id> discr,
           BiFunction<? super Ctr, ? super Id,
                      ? extends ManagedReceiver<? extends Impl>> constructor) {
        return of(containerType, serviceType, container, discr, constructor,
                  (c, i) -> {});
    }

    /**
     * Define a single-container singleton component with a destructor.
     * 
     * @param serviceType the service type implemented the component
     * 
     * @param constructor a means to create instances on demand, given
     * the container
     * 
     * @param destructor a means to notify of the garbage collection of
     * an instance
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     *
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl> StaticSingletonAgent<Ctr, Impl>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Function<? super Ctr,
                    ? extends ManagedReceiver<? extends Impl>> constructor,
           Consumer<? super Ctr> destructor) {
        return new StaticSingletonAgent<>(containerType, serviceType,
                                          constructor, destructor);
    }

    /**
     * Define a single-container singleton component.
     * 
     * @param serviceType the service type implemented the component
     * 
     * @param constructor a means to create instances on demand, given
     * the container
     * 
     * @param containerType the type of the container; the type of the
     * first arguments of the constructor and destructor
     *
     * @param <Ctr> the container class
     * 
     * @param <Impl> the component class
     * 
     * @return the new agent
     * 
     * @constructor
     */
    public static <Ctr, Impl> StaticSingletonAgent<Ctr, Impl>
        of(Class<Ctr> containerType, Class<? super Impl> serviceType,
           Function<? super Ctr,
                    ? extends ManagedReceiver<? extends Impl>> constructor) {
        return of(containerType, serviceType, constructor, c -> {});
    }
}
