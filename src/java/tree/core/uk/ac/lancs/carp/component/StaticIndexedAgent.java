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
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Manages a family of components of the same type, distinguished by an
 * index type, for each container.
 * 
 * @param <Ctr> the container class
 * 
 * @param <Impl> the component class
 * 
 * @param <Id> the index type
 * 
 * @author simpsons
 */
public class StaticIndexedAgent<Ctr, Impl, Id> extends Agent {
    private final Map<Ctr, Map<Id, Reference<Impl>>> cache =
        new WeakHashMap<>();

    private final Map<Impl, Agency> agencies = new WeakHashMap<>();

    private final Class<?> serviceType;

    private final Class<Ctr> containerType;

    private final BiFunction<? super Ctr, ? super Id,
                             ? extends ManagedReceiver<? extends Impl>> constructor;

    private final BiConsumer<? super Ctr, ? super Id> destructor;

    private final Discriminator<Id> discr;

    private final Pattern pattern;

    StaticIndexedAgent(Class<Ctr> containerType,
                       Class<? super Impl> serviceType, Discriminator<Id> discr,
                       BiFunction<? super Ctr, ? super Id,
                                  ? extends ManagedReceiver<? extends Impl>> constructor,
                       BiConsumer<? super Ctr, ? super Id> destructor) {
        this.serviceType = serviceType;
        this.containerType = containerType;
        this.constructor = constructor;
        this.destructor = destructor;
        this.discr = discr;
        this.pattern = Pattern.compile("^" + discr.pattern() + "$");
    }

    private synchronized void clean(Reference<Ctr> cref, Id id,
                                    Reference<Impl> ref) {
        Ctr container = cref.get();
        if (container == null) return;
        if (cache.getOrDefault(container, Collections.emptyMap())
            .remove(id, ref)) destructor.accept(container, id);
    }

    private <R> R internalGet(Ctr container, Id id,
                              BiFunction<? super Impl, ? super Agency,
                                         ? extends R> action) {
        assert Thread.holdsLock(this);
        var map = cache.computeIfAbsent(container, k -> new HashMap<>());
        Reference<Impl> ref = map.get(id);
        Impl receiver;
        Agency agency;
        if (ref == null || (receiver = ref.get()) == null) {
            if (ref != null) destructor.accept(container, id);
            ManagedReceiver<? extends Impl> mr =
                constructor.apply(container, id);
            receiver = mr.receiver;
            agency = mr.agency;
            agencies.put(receiver, agency);
            Reference<Ctr> cref = new WeakReference<>(container);
            ref = PathMap.watch(receiver, r -> clean(cref, id, r));
            map.put(id, ref);
            inform(container, receiver,
                   Collections.singletonList(discr.encode(id)), agency);
        } else {
            agency = agencies.get(receiver);
        }
        return action.apply(receiver, agency);
    }

    /**
     * Get an instance of a component from its container. The
     * constructor will be called only if the instance doesn't already
     * exist.
     * 
     * @param container the container
     * 
     * @param id the id uniquely identifying the instance within the
     * container
     * 
     * @return the requested instance
     */
    public synchronized Impl get(Ctr container, Id id) {
        return internalGet(container, id, (receiver, agency) -> receiver);
    }

    @Override
    Match match(Object container, List<? extends CharSequence> subpath) {
        if (!containerType.isInstance(container)) return null;
        if (subpath.isEmpty()) return null;
        Matcher matcher = pattern.matcher(subpath.get(0));
        if (!matcher.matches()) return null;
        Id id = discr.decode(matcher);
        BiFunction<Impl, Agency, Match> action =
            (receiver, agency) -> new Match(subpath.subList(0, 1),
                                            subpath.subList(1, subpath.size()),
                                            receiver, agency);
        return internalGet(containerType.cast(container), id, action);
    }

    private void inform(Ctr container, Object receiver,
                        List<? extends CharSequence> subpath, Agency agency) {
        assert Thread.holdsLock(this);
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
        Collection<Listener> listeners =
            this.listeners.computeIfAbsent(ctr, k -> Collections
                .newSetFromMap(new IdentityHashMap<>()));

        /* Tell the new listener about any components already created
         * for this container. */
        var index = cache.get(ctr);
        if (index == null) return;
        for (var entry : index.entrySet()) {
            Id id = entry.getKey();
            Impl impl = entry.getValue().get();
            if (impl == null) continue;
            Agency agency = agencies.getOrDefault(impl, Agency.empty());
            if (listener.update(Collections.singletonList(discr.encode(id)),
                                impl, agency))
                return;
        }
        listeners.add(listener);
    }

    @Override
    Class<?> serviceType() {
        return serviceType;
    }
}
