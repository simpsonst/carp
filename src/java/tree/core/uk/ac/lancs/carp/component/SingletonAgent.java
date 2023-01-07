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
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Manages a single component for a single container.
 * 
 * @param <Impl> the component class
 * 
 * @author simpsons
 */
public final class SingletonAgent<Impl> extends Agent {
    private final Reference<?> container;

    private Reference<Impl> cache;

    private Agency agency;

    private final Class<?> serviceType;

    private final Class<?> containerType;

    private final Function<Object,
                           ? extends ManagedReceiver<? extends Impl>> constructor;

    private final Consumer<Object> destructor;

    <Ctr> SingletonAgent(Class<Ctr> containerType,
                         Class<? super Impl> serviceType, Ctr container,
                         Function<? super Ctr,
                                  ? extends ManagedReceiver<? extends Impl>> constructor,
                         Consumer<? super Ctr> destructor) {
        this.serviceType = serviceType;
        this.containerType = containerType;
        this.constructor = c -> constructor.apply(containerType.cast(c));
        this.destructor = c -> destructor.accept(containerType.cast(c));
        this.container = PathMap.watch(container, r -> clearOut());
    }

    private synchronized void clearOut() {
        agency = null;
    }

    private synchronized void clean(Reference<Impl> ref) {
        Object container = this.container.get();
        if (container == null) return;
        if (cache == ref) destructor.accept(container);
    }

    private <R> R internalGet(Object container,
                              BiFunction<? super Impl, ? super Agency,
                                         ? extends R> action) {
        assert Thread.holdsLock(this);
        Impl receiver;
        if (cache == null || (receiver = cache.get()) == null) {
            if (cache != null) destructor.accept(container);
            ManagedReceiver<? extends Impl> mr = constructor.apply(container);
            receiver = mr.receiver;
            agency = mr.agency;
            assert agency != null;
            cache = PathMap.watch(receiver, r -> clean(r));
            inform(receiver, Collections.emptyList());
        }
        return action.apply(receiver, agency);
    }

    /**
     * Get the instance of a component from its creator. The constructor
     * will be called only if the instance doesn't already exist.
     * 
     * @return the instance
     */
    public synchronized Impl get() {
        Object container = this.container.get();
        if (container == null) return null;
        return internalGet(container, (receiver, manager) -> receiver);
    }

    @Override
    synchronized Match match(Object container,
                             List<? extends CharSequence> suffix) {
        if (container != this.container.get()) return null;
        if (!containerType.isInstance(container)) return null;
        return internalGet(containerType.cast(container),
                           (receiver,
                            agency) -> new Match(Collections.emptyList(),
                                                 suffix, receiver, agency));
    }

    private void inform(Impl receiver, List<? extends CharSequence> subpath) {
        assert Thread.holdsLock(this);
        for (Iterator<Listener> iter = listeners.iterator(); iter.hasNext();)
            if (iter.next().update(subpath, receiver, agency)) iter.remove();
    }

    private final Collection<Agent.Listener> listeners =
        Collections.newSetFromMap(new IdentityHashMap<>());

    @Override
    synchronized void register(Object container, Agent.Listener listener) {
        if (container != this.container.get()) return;

        /* Tell the new listener about any component already created for
         * this container. */
        Reference<Impl> ref = cache;
        if (ref == null) return;
        Impl receiver = ref.get();
        if (receiver == null) return;
        if (!listener.update(Collections.emptyList(), container, agency))
            listeners.add(listener);
    }

    @Override
    Class<?> serviceType() {
        return serviceType;
    }
}
