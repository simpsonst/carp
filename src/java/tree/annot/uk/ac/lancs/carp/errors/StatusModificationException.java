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

package uk.ac.lancs.carp.errors;

import java.util.Collections;
import java.util.Map;

/**
 * Indicates that an attempt to set or update a receiver's status
 * failed. This exception includes an unmodifiable set of
 * application-defined parameters allowing the mistake that the caller
 * has made to be formally described.
 * 
 * @author simpsons
 */
public class StatusModificationException extends Exception {
    /**
     * Specifies parameters of the explanation of the caller's mistake.
     */
    public final Map<String, String> params;

    /**
     * Create an exception with a detail message.
     * 
     * @param message the detail message
     */
    public StatusModificationException(String message) {
        this(Collections.emptyMap(), message);
    }

    /**
     * Create an exception.
     */
    public StatusModificationException() {
        this.params = Collections.emptyMap();
    }

    /**
     * Create an exception with user-defined parameters.
     * 
     * @param params a user-defined set of error parameters detailing
     * the caller's mistake
     */
    public StatusModificationException(Map<? extends String,
                                           ? extends String> params) {
        this.params =
            params == null ? Collections.emptyMap() : Map.copyOf(params);
    }

    /**
     * Create an exception with user-defined parameters and a detail
     * message.
     * 
     * @param params a user-defined set of error parameters detailing
     * the caller's mistake
     * 
     * @param message the detail message
     */
    public StatusModificationException(Map<? extends String,
                                           ? extends String> params,
                                       String message) {
        super(message);
        this.params =
            params == null ? Collections.emptyMap() : Map.copyOf(params);
    }
}
