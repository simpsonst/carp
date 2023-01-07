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

import java.net.URI;
import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.apache.http.protocol.HttpRequestHandler;

/**
 * Identifies a locally served location on the Web, from internal and
 * external perspectives, and allows a behaviour to be defined for it.
 *
 * @author simpsons
 */
public interface WebPlacement {
    /**
     * Get the URI prefix of this location. This must end in a slash,
     * and a presence may use it to determine the full addresses of its
     * receivers.
     * 
     * @return the URI prefix
     */
    URI base();

    /**
     * Get the portion of the virtual path that goes beyond the base
     * path.
     * 
     * @param path the virtual path of a request
     * 
     * @return the sub-path with respect to the location
     * 
     * @throws IllegalArgumentException if the virtual path is not under
     * this location
     */
    String subpath(CharSequence path);

    /**
     * Get from the request line the portion of the virtual path that
     * goes beyond the base path.
     * 
     * @param line the request line
     * 
     * @return the sub-path with respect to the location
     * 
     * @throws IllegalArgumentException if the virtual path is not under
     * this location
     * 
     * @default The virtual path is extracted from the request line, and
     * passed to {@link #subpath(java.lang.CharSequence)}.
     */
    default String subpath(RequestLine line) {
        return subpath(line.getUri());
    }

    /**
     * Get from the request the portion of the virtual path that goes
     * beyond the base path.
     * 
     * @param req the request
     * 
     * @return the sub-path with respect to the location
     * 
     * @throws IllegalArgumentException if the virtual path is not under
     * this location
     * 
     * @default The virtual path is extracted from the request, and
     * passed to {@link #subpath(org.apache.http.RequestLine)}.
     */
    default String subpath(HttpRequest req) {
        return subpath(req.getRequestLine());
    }

    /**
     * Send matching requests to a handler.
     * 
     * @param handler the request handler
     */
    void register(HttpRequestHandler handler);

    /**
     * Stop sending requests to the handler.
     */
    void deregister();
}
