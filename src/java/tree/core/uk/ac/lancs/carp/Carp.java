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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.http.impl.client.CloseableHttpClient;
import uk.ac.lancs.carp.Configuration.Key;
import static uk.ac.lancs.carp.Configuration.key;
import uk.ac.lancs.carp.map.Completable;
import uk.ac.lancs.carp.map.StaticCompletable;
import uk.ac.lancs.carp.map.Union;

/**
 * Holds miscellaneous static methods.
 * 
 * @author simpsons
 */
public final class Carp {
    /**
     * Identifies a location in a web server that a server presence
     * should occupy.
     */
    public static final Key<WebPlacement> PLACEMENT = key();

    /**
     * Specifies whether a duplex presence should provide to its own
     * clients direct references to its own receivers. One is likely to
     * want to turn this off when doing local diagnostic tests, as it
     * prevents {@link ClientPresence#elaborate(Class, java.net.URI)}
     * from recognizing local receivers, and returning them directly.
     */
    public static final Key<Boolean> LOCAL_SHORT_CIRCUIT = key();

    /**
     * Identifies an executor for a server presence to invoke methods
     * with no response types.
     */
    public static final Key<Executor> ASYNCHRONOUS_EXECUTOR = key();

    /**
     * Identifies a supplier of HTTP clients that a client presence can
     * use to invoke remote objects.
     */
    public static final Key<Supplier<? extends CloseableHttpClient>> CLIENTS =
        key();

    /**
     * Identifies a repository to allow a presence to keep track of
     * fingerprints of HTTPS peers.
     */
    public static final Key<FingerprintRepository> FINGERPRINTS = key();

    /**
     * Prepare to create a presence.
     * 
     * @return a builder for gathering the necessary parameters
     */
    public static Configuration.Builder start() {
        return Configuration.builder();
    }

    /**
     * Get the peer identity for a URI.
     *
     * @param endpoint an endpoint belonging to the peer
     *
     * @return the peer's identity
     *
     * @throws UnsupportedOperationException if the port is not
     * explicit, and the scheme is unrecognized
     */
    public static InetSocketAddress getPeer(URI endpoint) {
        int port = endpoint.getPort();
        if (port < 0) {
            switch (endpoint.getScheme().toLowerCase(Locale.ROOT)) {
            case "http":
                port = 80;
                break;

            case "https":
                port = 443;
                break;

            default:
                throw new UnsupportedOperationException("unknown URI scheme: "
                    + endpoint.getScheme());
            }
        }
        return InetSocketAddress.createUnresolved(endpoint.getHost(), port);
    }

    /**
     * Get a pattern matching a prefix of a virtual path. For example,
     * <code>getPrefixPattern("foo/bar/baz", "tail")</code> becomes the
     * pattern <samp>^/foo/bar/baz(?:/(?&lt;tail&gt;.*))?$</samp>. This
     * will then match the following strings:
     * 
     * <table summary="This table lists strings against whether they
     * match the pattern, and what the tail group reports as.">
     * <thead>
     * <tr>
     * <th>Input</th>
     * <th>Result</th>
     * <th><samp>tail</samp> value</th>
     * </tr>
     * </thead> <tbody>
     * <tr>
     * <td><samp>/foo/bar/baz</samp></td>
     * <td>match</td>
     * <td>not set</td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar/baz/qux</samp></td>
     * <td>match</td>
     * <td><samp>qux</samp></td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar/ba</samp></td>
     * <td colspan=2>no match</td>
     * </tr>
     * <tr>
     * <td><samp>/foo/bar/bazlord</samp></td>
     * <td colspan=2>no match</td>
     * </tr>
     * </tbody>
     * </table>
     * 
     * @param path the path, whose leading and trailing slashes are
     * removed, and whose adjacent slashes are reduced
     * 
     * @param tailGroup the name of the group matching any tail of a
     * string matched against the resultant pattern; or {@code null} to
     * access it as simply group 1
     * 
     * @return the requested pattern
     */
    public static Pattern getPrefixPattern(String path, String tailGroup) {
        String normal = normalizePrefix(path);
        String pattern = "^" + Pattern.quote(normal) + "(?:/("
            + (tailGroup == null ? "" : ("?<" + tailGroup + ">")) + ".*))?$";
        return Pattern.compile(pattern);
    }

    /**
     * Treat a URI as a URI prefix, and yield the prefix as a URI
     * suitable for relativization. For example,
     * <samp>http://example.com/foo/bar</samp> is normalized to
     * <samp>http://example.com/foo/bar/</samp>. The transformation
     * follows these steps:
     * 
     * <ul>
     * 
     * <li>Call {@link URI#getPath()} on the supplied URI to get its
     * virtual path.
     * 
     * <li>Call {@link #normalizePrefix(java.lang.String)} on the
     * virtual path, which strips duplicate and trailing slashes, and
     * ensures that it has one leading slash unless empty.
     * 
     * <li>Append a slash, so this path now has the form <samp>/</samp>,
     * <samp>/foo/</samp>, or <samp>/foo/bar/</samp>, etc.
     * 
     * <li>Resolve this new path against the original URI. Because the
     * new path always begins with a slash, this replaces the virtual
     * path entirely. Any query string is also removed, because this is
     * not part of the path retrieved from the URI.
     * 
     * </ul>
     * 
     * @param cand the URI to normalize as a prefix
     * 
     * @return the normalized URI prefix
     */
    public static URI normalizePrefix(URI cand) {
        return cand.resolve(normalizePrefix(cand.getPath()) + '/');
    }

    /**
     * Normalize a URI path prefix. An empty string is returned if the
     * supplied path is {@code null}.
     * 
     * @param path the path to be normalized as a prefix
     * 
     * @return the normalized prefix
     */
    public static String normalizePrefix(String path) {
        if (path == null) return "";
        String norm = normalizePath(path, true);
        return norm.isEmpty() ? "" : ("/" + norm);
    }

    /**
     * Normalize a path. Multiple consecutive slashes are collapsed into
     * one. Leading slashes are removed. Trailing slashes are optionally
     * removed.
     *
     * @param path the path to be normalized
     *
     * @param checkTail whether to strip trailing slashes
     *
     * <p>
     * If trailing slashes are not stripped, they will still be
     * collapsed.
     *
     * @return the normalized path
     */
    public static String normalizePath(String path, boolean checkTail) {
        StringBuilder buffer = new StringBuilder(path);
        Matcher m = SLASHES.matcher(buffer);
        int start = 0;
        while (m.find(start)) {
            if (m.start() == 0) {
                /* Strip leading slashes. */
                buffer.delete(0, m.end());
                continue;
            }
            if (checkTail && m.end() == buffer.length()) {
                /* Strip trailing slashes. */
                buffer.delete(m.start(), m.end());
                break;
            }
            if (m.end() - m.start() > 1) {
                start = m.end() - 1;
                buffer.delete(m.start() + 1, m.end());
                continue;
            }
            start = m.end();
        }
        return buffer.toString();
    }

    private static final Pattern SLASHES = Pattern.compile("/+");

    /**
     * Split a path into its slash-separated parts, and eliminate empty
     * elements.
     * 
     * @param path the path to split
     * 
     * @return a list of the path's parts
     */
    public static List<String> pathAsParts(CharSequence path) {
        return Arrays.asList(SLASHES.split(path)).stream()
            .filter(e -> !e.isEmpty()).collect(Collectors.toList());
    }

    /**
     * Join path elements into a slash-separated string. If {@code null}
     * is supplied, {@code null} will be returned.
     * 
     * @param parts the elements to join
     * 
     * @return the joined elements
     */
    public static String partsAsPath(List<? extends CharSequence> parts) {
        return parts == null ? null : parts.stream().map(Object::toString)
            .collect(Collectors.joining("/"));
    }

    /**
     * Split a path into its slash-separated parts, and eliminate empty
     * elements, except for the last.
     * 
     * @param path the path to split
     * 
     * @return a list of the path's parts
     */
    public static List<String> pathAsPartsWithEmptyTail(String path) {
        List<String> unfiltered = Arrays.asList(SLASHES.split(path));
        if (unfiltered.isEmpty()) return unfiltered;
        int last = unfiltered.size() - 1;
        List<String> head = unfiltered.subList(0, last);
        List<String> tail = Collections.singletonList(unfiltered.get(last));
        return Stream
            .concat(head.stream().filter(e -> !e.isEmpty()), tail.stream())
            .collect(Collectors.toList());
    }

    /**
     * Complete a builder for a type.
     * 
     * @param <T> the type to be completed
     * 
     * @param builder the builder to be completed
     * 
     * @return a completed instance of the type, with contents set by
     * the builder
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> T complete(Completable<T> builder) {
        Class<?> btype = builder.getClass();
        try {
            Method meth = btype.getMethod(Completable.METHOD_NAME);
            meth.setAccessible(true);
            return (T) meth.invoke(builder);
        } catch (NoSuchMethodException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Complete an empty structure or response type.
     * 
     * @param <T> the type to be completed
     * 
     * @param type the type to be completed
     * 
     * @return a completed, empty instance of the type
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <T> T complete(Class<? extends StaticCompletable<T>> type) {
        try {
            Method meth = type.getMethod(StaticCompletable.METHOD_NAME);
            meth.setAccessible(true);
            return (T) meth.invoke(null);
        } catch (NoSuchMethodException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Get the type of a union as an enumeration constant.
     * 
     * @param <E> the union's discriminating type
     * 
     * @param union the union whose type is to be identified
     * 
     * @return the union's type
     */
    @SuppressWarnings("unchecked")
    @Deprecated
    public static <E extends Enum<E>> E type(Union<E> union) {
        Class<?> utype = union.getClass();
        try {
            Method meth = utype.getMethod(Union.METHOD_NAME);
            meth.setAccessible(true);
            return (E) meth.invoke(union);
        } catch (NoSuchMethodException | IllegalAccessException |
                 IllegalArgumentException | InvocationTargetException ex) {
            throw new AssertionError("unreachable", ex);
        }
    }

    /**
     * Test whether the union is of a specific type.
     * 
     * @param <E> the union's discriminating type
     * 
     * @param union the union whose type is to be identified
     * 
     * @param type the expected type
     * 
     * @return {@code true} if the union's type is as expected;
     * {@code false} otherwise
     */
    @Deprecated
    public static <E extends Enum<E>> boolean test(Union<E> union, E type) {
        return type(union) == type;
    }

    private Carp() {}
}
