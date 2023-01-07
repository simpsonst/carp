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

/**
 * Indicates that an endpoint is not at the remote peer.
 * 
 * @author simpsons
 */
public class MissingEndpointException extends RemoteInvocationException {
    /**
     * Create an exception.
     */
    public MissingEndpointException() {}

    /**
     * Create an exception with a detail message.
     * 
     * @param message the detail message
     */
    public MissingEndpointException(String message) {
        super(message);
    }

    /**
     * Create an exception with a detail message and a cause.
     * 
     * @param message the detail message
     * 
     * @param cause the cause
     */
    public MissingEndpointException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create an exception with a cause.
     * 
     * @param cause the cause
     */
    public MissingEndpointException(Throwable cause) {
        super(cause);
    }

    /**
     * Create an exception with a detail message, a cause, suppression
     * state and stack-trace mutability.
     * 
     * @param message the detail message
     * 
     * @param cause the cause of the exception
     * 
     * @param enableSuppression whether suppression is enabled
     * 
     * @param writableStackTrace whether the stack trace is writable
     */
    protected MissingEndpointException(String message, Throwable cause,
                                       boolean enableSuppression,
                                       boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
