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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Manages the life cycles of direct components of a receiver.
 * 
 * @author simpsons
 */
public final class Agency {
    private final Map<List<String>, Agent> agents;

    /**
     * Create an agency.
     * 
     * @param bindings the set of bindings handled by the agency
     */
    public Agency(Collection<? extends Binding> bindings) {
        Map<List<String>, Agent> components = new HashMap<>();
        for (Binding b : bindings)
            components.put(b.subpath, b.agent);
        this.agents = Map.copyOf(components);
    }

    private static final Agency EMPTY = new Agency(Collections.emptySet());

    /**
     * Get an agency managing no components.
     * 
     * @return the empty agency
     */
    public static Agency empty() {
        return EMPTY;
    }

    static class Resolution {
        public final Object receiver;

        public final Agency agent;

        public final Class<?> type;

        public final List<String> head;

        public final List<String> tail;

        public Resolution(Object receiver, Agency manager, Class<?> type,
                          List<String> head, List<String> tail) {
            this.receiver = receiver;
            this.agent = manager;
            this.type = type;
            this.head = head;
            this.tail = tail;
        }
    }

    Resolution resolve(Object container, List<String> tail) {
        final int tailLen = tail.size();
        for (int i = tailLen; i > 0; i--) {
            List<String> prefix = tail.subList(0, i);
            Agent comp = agents.get(prefix);
            if (comp == null) continue;
            Match match = comp.match(container, tail.subList(i, tailLen));
            if (match == null) continue;
            return new Resolution(match.receiver, match.agency,
                                  comp.serviceType(),
                                  concat(prefix, match.matched),
                                  match.unmatched);
        }
        return null;
    }

    interface Installer {
        /**
         * 
         * @param path
         * @param type
         * @param receiver
         * @param agency
         * 
         * @return {@code true} if the should now be discarded;
         * {@code false} otherwise
         */
        boolean install(List<String> path, Class<?> type, Object receiver,
                        Agency agency);
    }

    void install(List<String> prefix, Object container, Class<?> containerType,
                 Installer installer) {
        for (var entry : agents.entrySet()) {
            List<String> head = concat(prefix, entry.getKey());
            Agent agent = entry.getValue();
            agent.register(container, (subpath, receiver, agency) -> installer
                .install(concat(head,
                                subpath.stream().map(Object::toString)
                                    .collect(Collectors.toList())),
                         agent.serviceType(), receiver, agency));
        }
    }

    private static <E> List<E> concat(List<? extends E> a,
                                      List<? extends E> b) {
        return Stream.concat(a.stream(), b.stream())
            .collect(Collectors.toList());
    }
}
