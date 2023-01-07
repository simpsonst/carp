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

package uk.ac.lancs.carp.component;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages a single component for each container.
 *
 * @param <Ctr> the container class
 * 
 * @param <Impl> the component class
 * 
 * @author simpsons
 */
public final class StaticSingletonAgent<Ctr, Impl> extends Agent {
    private final Map<Ctr, Reference<Impl>> cache = new WeakHashMap<>();

    private final Map<Impl, Agency> agencies = new WeakHashMap<>();

    private final Class<?> serviceType;

    private final Class<Ctr> containerType;

    private final Function<? super Ctr,
                           ? extends ManagedReceiver<? extends Impl>> constructor;

    private final Consumer<? super Ctr> destructor;

    StaticSingletonAgent(Class<Ctr> containerType,
                         Class<? super Impl> serviceType,
                         Function<? super Ctr,
                                  ? extends ManagedReceiver<? extends Impl>> constructor,
                         Consumer<? super Ctr> destructor) {
        this.serviceType = serviceType;
        this.containerType = containerType;
        this.constructor = constructor;
        this.destructor = destructor;
    }

    private synchronized void clean(Reference<Ctr> cref, Reference<Impl> ref) {
        Ctr container = cref.get();
        if (container == null) return;
        if (cache.remove(container, ref)) destructor.accept(container);
    }

    private <R> R internalGet(Ctr container,
                              BiFunction<? super Impl, ? super Agency,
                                         ? extends R> action) {
        assert Thread.holdsLock(this);
        Reference<Impl> ref = cache.get(container);
        Impl receiver;
        Agency agency;
        if (ref == null || (receiver = ref.get()) == null) {
            if (ref != null) destructor.accept(container);
            ManagedReceiver<? extends Impl> mr = constructor.apply(container);
            receiver = mr.receiver;
            agency = mr.agency;
            agencies.put(receiver, agency);
            Reference<Ctr> cref = new WeakReference<>(container);
            ref = PathMap.watch(receiver, r -> clean(cref, r));
            cache.put(container, ref);
        } else {
            agency = agencies.get(receiver);
        }
        return action.apply(receiver, agency);
    }

    /**
     * Get the instance of a component from its creator. The constructor
     * will be called only if the instance doesn't already exist.
     * 
     * @param container the container
     * 
     * @return the instance
     */
    public synchronized Impl get(Ctr container) {
        return internalGet(container, (receiver, agency) -> {
            inform(container, receiver, Collections.emptyList());
            return receiver;
        });
    }

    @Override
    synchronized Match match(Object container,
                             List<? extends CharSequence> suffix) {
        if (!containerType.isInstance(container)) return null;
        return internalGet(containerType.cast(container),
                           (receiver,
                            bindings) -> new Match(Collections.emptyList(),
                                                   suffix, receiver, bindings));
    }

    private void inform(Ctr container, Impl receiver,
                        List<? extends CharSequence> subpath) {
        assert Thread.holdsLock(this);
        Agency agency = agencies.getOrDefault(receiver, Agency.empty());
        for (Iterator<Listener> iter = listeners
            .getOrDefault(container, Collections.emptySet()).iterator();
             iter.hasNext();)
            if (iter.next().update(subpath, receiver, agency)) iter.remove();
    }

    private final Map<Ctr, Collection<Listener>> listeners =
        new WeakHashMap<>();

    @Override
    synchronized void register(Object container, Listener listener) {
        Ctr ctr = containerType.cast(container);

        /* Make sure we have the listener set for this container, and
         * add the new listener to it. */
        listeners
            .computeIfAbsent(ctr,
                             k -> Collections
                                 .newSetFromMap(new IdentityHashMap<>()))
            .add(listener);

        /* Tell the new listener about any component already created for
         * this container. */
        Reference<Impl> ref = cache.get(ctr);
        if (ref == null) return;
        Impl receiver = ref.get();
        if (receiver == null) return;
        Agency agency = agencies.getOrDefault(receiver, Agency.empty());
        listener.update(Collections.emptyList(), container, agency);
    }

    @Override
    Class<?> serviceType() {
        return serviceType;
    }
}
