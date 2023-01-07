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
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Maintains typed receivers, including their components, in a string
 * hierarchy. Nodes of the hierarchy are expressed as lists of strings
 * (paths or sub-paths). A typed receiver is a receiver object and its
 * service type. The same object may be registered with a different
 * service type under a different path.
 * 
 * @author simpsons
 */
public final class PathMap {
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

    /**
     * Pack-rats recent references. References to be retained a short
     * while are added to this set. Periodically, the collection is
     * moved to {@link #oldRefs}, and replaced with a new, empty
     * collection, so whatever was in {@link #oldRefs} will be subject
     * to garbage collection.
     */
    static AtomicReference<Collection<Object>> currentRefs =
        new AtomicReference<>(new HashSet<>());

    /**
     * Pack-rats not-so-recent references. Assigned from
     * {@link #currentRefs} periodically, exposing anything in its old
     * value to garbage collection.
     */
    static Collection<Object> oldRefs;

    private static final long PACKRAT_PERIOD = 5 * 1000;

    /**
     * Records when the pack-rat collections are to be cycled next.
     */
    static long nextPurge = System.currentTimeMillis() + PACKRAT_PERIOD;

    /**
     * Pack-rat an object. The object won't be garbage-collected for at
     * least {@value #PACKRAT_PERIOD} milliseconds.
     * 
     * @param obj the object to be retained
     */
    private static void keep(Object obj) {
        currentRefs.get().add(obj);
    }

    static {
        /* Create a daemon thread to run the actions of discarded
         * references, and cycle the pack-rat collections. */
        Thread t = new Thread(PathMap.class.getCanonicalName()) {
            @Override
            public void run() {
                for (;;) {
                    try {
                        /* Check for a discarded reference, and run its
                         * clean-up action. */
                        final long now = System.currentTimeMillis();
                        ActionReference<?> ref =
                            (ActionReference<?>) queue.remove(nextPurge - now);
                        if (ref != null) ref.run();
                    } catch (RuntimeException | InterruptedException ex) {
                        /* Don't care and shouldn't be a reason for us
                         * to stop. */
                    }

                    /* Cycle the pack-rat collections if necessary. */
                    final long now = System.currentTimeMillis();
                    if (now >= nextPurge) {
                        nextPurge += PACKRAT_PERIOD;
                        oldRefs = currentRefs.getAndSet(new HashSet<>());
                    }
                }
            }
        };
        t.setDaemon(true);
        t.start();
    }

    private class Service {
        final Reference<Object> receiver;

        final Class<?> type;

        final Agency agency;

        final List<String> path;

        public Service(Class<?> type, Object receiver, List<String> path,
                       Agency agency) {
            this.path = path;
            this.agency = agency;
            this.type = type;
            this.receiver = new ActionReference<>(receiver, ref -> {
                cleanOut(path, Service.this);
            });
        }
    }

    private final Map<List<String>, Service> paths = new HashMap<>();

    private final Map<Object, Map<Class<?>, Service>> services =
        new WeakHashMap<>();

    synchronized void cleanOut(List<String> path, Object value) {
        paths.remove(path, value);
    }

    /**
     * Install a service consistently in the indices. The service is
     * identified by type and receiver. If the service is available
     * under another path, it will be removed from that path. If
     * something is already installed under the path, it is uninstalled.
     * 
     * @param path the path under which to store the service
     * 
     * @param type the service type
     * 
     * @param receiver the receiver implementing the service
     * 
     * @param agency the bindings for dynamic objects under the service
     */
    private void install(List<String> path, Class<?> type, Object receiver,
                         Agency agency) {
        assert Thread.holdsLock(this);
        /* Create a new entry, and index it by path and by service id
         * (type and receiver). */
        Service srv = new Service(type, receiver, path, agency);
        Service oldSrvByReceiver = services
            .computeIfAbsent(receiver, k -> new HashMap<>()).put(type, srv);
        Service oldSrvByPath = paths.put(path, srv);

        /* Ensure we are notified of components dynamically created. */
        agency.install(path, receiver, type, weakInstaller);

        /* Clear out stuff from one index that we've overwritten in the
         * other. */
        if (oldSrvByReceiver != null)
            paths.remove(oldSrvByReceiver.path, oldSrvByReceiver);
        if (oldSrvByPath != null)
            services.getOrDefault(receiver, Collections.emptyMap())
                .remove(oldSrvByPath.type, oldSrvByPath);

        /* Prevent the receiver from being immediately
         * garbage-collected. */
        keep(receiver);
    }

    /**
     * Find a typed receiver's position in the hierarchy.
     * 
     * @param serviceType the service type
     * 
     * @param receiver the receiver
     * 
     * @return the receiver's position as an immutable list; or
     * {@code null} if not found
     */
    public List<String> locate(Class<?> serviceType, Object receiver) {
        return processCallbacks(() -> {
            Service srv =
                services.getOrDefault(receiver, Collections.emptyMap())
                    .get(serviceType);
            if (srv != null) return List.copyOf(srv.path);
            return null;
        });
    }

    /**
     * Ensure a typed receiver's position in the hierarchy. If the
     * receiver has no position, an anonymous path will be allocated to
     * it.
     * 
     * @param serviceType the service type
     * 
     * @param receiver the receiver
     * 
     * @return the receiver's position as an immutable list
     */
    public List<String> recognize(Class<?> serviceType, Object receiver) {
        return processCallbacks(() -> {
            Service srv =
                services.getOrDefault(receiver, Collections.emptyMap())
                    .get(serviceType);
            if (srv != null) return List.copyOf(srv.path);

            /* We don't know the receiver, so create an anonymous path
             * for it. */
            List<String> path =
                Arrays.asList("anon", UUID.randomUUID().toString());
            install(path, serviceType, receiver, Agency.empty());
            return path;
        });
    }

    /**
     * Find a receiver in the hierarchy. If no receiver is found at the
     * specified location, each ancestor is consulted until a receiver
     * is found. The remainder of the path is then submitted to its
     * component agency, to obtain components identified by the path
     * remainder. If this yields a receiver, part of the path is
     * consumed, and the remainder is submitted to <em>its</em> agency,
     * etc, until all of the path is consumed, or no more receivers are
     * found.
     * 
     * <p>
     * The part of the path that was consumed, the part that remained,
     * the receiver and its service type comprise the result.
     * 
     * @param path the deepest node in the hierarchy to test
     * 
     * @return details of the deepest matching receiver, its type and
     * position; or {@code null} if no receiver was found
     */
    public PathMatch resolve(List<String> path) {
        return processCallbacks(() -> {
            final int pathLen = path.size();
            for (int i = pathLen; i > 0; i--) {
                /* Gradually shorten the path until we get a match. */
                List<String> head = path.subList(0, i);
                Service rec = paths.get(head);
                if (rec == null) continue;

                /* Get the receiver, and make sure we hang on to it for
                 * a while. */
                Object receiver = rec.receiver.get();
                if (receiver == null) continue;
                keep(receiver);

                /* Keep resolving down until the tail is consumed, */
                List<String> tail = path.subList(i, pathLen);
                Agency agency = services.get(receiver).get(rec.type).agency;
                Class<?> type = rec.type;
                Agency.Resolution resol;
                while (!tail.isEmpty() &&
                    (resol = agency.resolve(receiver, tail)) != null) {
                    /* Record the latest results as the new current
                     * position. */
                    receiver = resol.receiver;
                    agency = resol.agent;
                    type = resol.type;
                    head = concat(head, resol.head);
                    tail = resol.tail;
                }
                return new PathMatch(type, receiver, head, tail);
            }
            return null;
        });
    }

    private static class WeakInstaller implements Agency.Installer {
        private final Reference<PathMap> ref;

        public WeakInstaller(PathMap base) {
            this.ref = new WeakReference<>(base);
        }

        @Override
        public boolean install(List<String> path, Class<?> type,
                               Object receiver, Agency agency) {
            PathMap base = ref.get();
            if (base == null) return true;
            /* We cannot synchronize on ourselves here, as it risks
             * deadlock. Instead, we add a job safely to separate
             * collection. We will process these jobs next time we
             * synchronize on ourselves. */
            base.addCallback(() -> base.install(path, type, receiver, agency));
            return false;
        }
    }

    private final Agency.Installer weakInstaller = new WeakInstaller(this);

    /**
     * Register a typed receiver with a component agency at a path.
     * 
     * @param path the path to register under
     * 
     * @param type the service type
     * 
     * @param receiver the receiver
     * 
     * @param agency the component agency
     */
    public void register(List<String> path, Class<?> type, Object receiver,
                         Agency agency) {
        processCallbacks(() -> {
            install(path, type, receiver, agency);
            return null;
        });
    }

    /**
     * De-register any receiver at a path location.
     * 
     * @param path the path to de-register
     */
    public void deregister(List<String> path) {
        processCallbacks(() -> {
            Service srv = paths.remove(path);
            if (srv == null) return null;
            Object receiver = srv.receiver.get();
            if (receiver == null) return null;
            var m1 = services.get(receiver);
            if (m1 == null) return null;
            m1.remove(srv.type);
            if (m1.isEmpty()) services.remove(receiver);
            return null;
        });
    }

    /**
     * De-register a typed receiver.
     * 
     * @param serviceType the service type
     * 
     * @param receiver the receiver
     */
    public void deregister(Class<?> serviceType, Object receiver) {
        processCallbacks(() -> {
            var m1 = services.get(receiver);
            if (m1 == null) return null;
            Service srv = m1.remove(serviceType);
            if (m1.isEmpty()) services.remove(receiver);
            paths.remove(srv.path);
            return null;
        });
    }

    /**
     * De-register all types of a receiver.
     * 
     * @param receiver the receiver
     */
    public void deregister(Object receiver) {
        processCallbacks(() -> {
            var m1 = services.remove(receiver);
            if (m1 == null) return null;
            for (var v : m1.values())
                paths.remove(v.path);
            return null;
        });
    }

    private static <E> List<E> concat(List<? extends E> a,
                                      List<? extends E> b) {
        return Stream.concat(a.stream(), b.stream())
            .collect(Collectors.toList());
    }

    private final List<Runnable> callbacks = new ArrayList<>();

    private void addCallback(Runnable action) {
        synchronized (callbacks) {
            callbacks.add(action);
        }
    }

    private void clearCallbacks() {
        assert Thread.holdsLock(this);

        /* Lock on the callbacks to extract them, then release. Only
         * then do we invoke the callbacks, so other callbacks may be
         * added meanwhile. */
        final Collection<Runnable> copy;
        synchronized (callbacks) {
            copy = List.copyOf(callbacks);
            callbacks.clear();
        }
        copy.forEach(Runnable::run);
    }

    private synchronized <R> R processCallbacks(Supplier<R> action) {
        try {
            /* Perform all the callbacks, then run the supplied action,
             * and return its value. */
            clearCallbacks();
            return action.get();
        } finally {
            /* Just before we go, check for more callbacks. */
            clearCallbacks();
        }
    }
}
