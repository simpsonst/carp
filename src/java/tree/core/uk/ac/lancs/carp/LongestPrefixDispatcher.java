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

package uk.ac.lancs.carp;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.HttpRequest;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.protocol.HttpRequestHandlerMapper;

/**
 * Handles requests by matching the longest specific sub-path, and
 * presenting internal and external contextual information to the user.
 * 
 * @author simpsons
 */
public class LongestPrefixDispatcher implements HttpRequestHandlerMapper {
    private final Map<List<String>, Foo> handlers = new ConcurrentHashMap<>();

    private final HttpRequestHandler defaultAction;

    private final URI base;

    private final List<String> prefix;

    /**
     * Create a dispatcher to act as the primary for a server.
     * 
     * @param base the public URI of the server
     */
    public LongestPrefixDispatcher(URI base) {
        this(base, (HttpRequestHandler) null);
    }

    /**
     * Create a dispatcher to act on a sub-path of a server.
     * 
     * @param base the public URI of the intended sub-path of the server
     * 
     * @param prefix the sub-path of the server
     */
    public LongestPrefixDispatcher(URI base, String prefix) {
        this(base, prefix, null);
    }

    /**
     * Create a dispatcher to act as the primary for a server, with a
     * default action.
     * 
     * @param base the public URI of the server
     * 
     * @param defaultAction the action to take if no behaviour has been
     * defined; may be {@code null}
     */
    public LongestPrefixDispatcher(URI base, HttpRequestHandler defaultAction) {
        this(base, "", defaultAction);
    }

    /**
     * Create a dispatcher to act on a sub-path of a server, with a
     * default action.
     * 
     * @param base the public URI of the intended sub-path of the server
     * 
     * @param prefix the sub-path of the server
     * 
     * @param defaultAction the action to take if no behaviour has been
     * defined; may be {@code null}
     */
    public LongestPrefixDispatcher(URI base, String prefix,
                                   HttpRequestHandler defaultAction) {
        this.defaultAction = defaultAction;
        this.base = base.resolve(Carp.normalizePrefix(base.getPath()));
        this.prefix = Carp.pathAsParts(prefix);
    }

    /**
     * Look up a handler by matching the virtual path of the request
     * against registered prefixes.
     * 
     * @param req the request whose virtual path is to be matched
     * 
     * @return the handler registered to the longest prefix matching the
     * virtual path; or the default handler if the virtual path matches
     * no registered prefix
     */
    @Override
    public HttpRequestHandler lookup(HttpRequest req) {
        List<String> parts = Carp.pathAsParts(req.getRequestLine().getUri());

        do {
            Foo foo = handlers.get(parts);
            HttpRequestHandler h;
            if (foo == null || (h = foo.handler()) == null) {
                if (parts.isEmpty()) return defaultAction;

                /* Remove a trailing path element, and try again. */
                parts = parts.subList(0, parts.size() - 1);
                continue;
            }

            /* The prefix matched, and the handler is available. */
            return h;
        } while (true);
    }

    private class Foo implements WebPlacement {
        private HttpRequestHandler delegate;

        private final List<String> key;

        private final URI base;

        private final Pattern pattern;

        Foo(List<String> key, List<String> parts) {
            assert key.subList(prefix.size(), key.size()).equals(parts);
            this.key = key;

            /* Resolve the parts against the base URI. This is where the
             * user should regard their presence to be. */
            this.base = LongestPrefixDispatcher.this.base.resolve(parts.stream()
                .map(s -> s + "/").collect(Collectors.joining()));

            /* Form a pattern to match the full path and yield the
             * remainder. */
            this.pattern =
                Pattern
                    .compile("^"
                        + Pattern.quote(key.stream().map(s -> "/" + s)
                            .collect(Collectors.joining()))
                        + "(?:/(?<tail>.*))?$");
        }

        @Override
        public URI base() {
            return base;
        }

        @Override
        public String subpath(CharSequence path) {
            Matcher m = pattern.matcher(path);
            if (!m.matches()) throw new IllegalArgumentException("no match: "
                + pattern + " vs " + path);
            return m.group("tail");
        }

        @Override
        public synchronized void register(HttpRequestHandler handler) {
            this.delegate = handler;
        }

        @Override
        public synchronized void deregister() {
            this.delegate = null;
            LongestPrefixDispatcher.this.handlers.remove(key, this);
        }

        synchronized HttpRequestHandler handler() {
            return delegate;
        }
    }

    /**
     * Define a location within the server to be handled.
     * 
     * @param path the sub-path within the server, relative to this
     * dispatcher's sub-path
     * 
     * @return the corresponding location; or {@code null} if already in
     * use
     */
    public WebPlacement register(String path) {
        List<String> subparts = Carp.pathAsParts(path);
        List<String> key = Stream.concat(prefix.stream(), subparts.stream())
            .collect(Collectors.toList());
        Foo result = new Foo(key, subparts);
        if (handlers.putIfAbsent(key, result) == null)
            return result;
        else
            return null;
    }
}
