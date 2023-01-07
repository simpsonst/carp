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

package uk.ac.lancs.carp.codec;

/**
 * Indicates that a required field in a structure, call or response was
 * not found.
 *
 * @author simpsons
 */
public class MissingFieldException extends CodecException {
    /**
     * Create an exception.
     * 
     * @param dir the direction in which the failure occurred
     */
    public MissingFieldException(Direction dir) {
        super(dir);
    }

    /**
     * Create an exception with a detail message.
     * 
     * @param message the detail message
     * 
     * @param dir the direction in which the failure occurred
     */
    public MissingFieldException(Direction dir, String message) {
        super(dir, message);
    }

    /**
     * Create an exception with a detail message and a cause.
     * 
     * @param message the detail message
     * 
     * @param cause the cause
     * 
     * @param dir the direction in which the failure occurred
     */
    public MissingFieldException(Direction dir, String message,
                                 Throwable cause) {
        super(dir, message, cause);
    }

    /**
     * Create an exception with a cause.
     * 
     * @param cause the cause
     * 
     * @param dir the direction in which the failure occurred
     */
    public MissingFieldException(Direction dir, Throwable cause) {
        super(dir, cause);
    }

    /**
     * Create an exception with a detail message, a cause, suppression
     * and a writable stack trace.
     * 
     * @param message the detail message
     * 
     * @param cause the cause
     * 
     * @param enableSuppression whether the exception can be suppressed
     * 
     * @param writableStackTrace whether the stack trace is writable
     * 
     * @param dir the direction in which the failure occurred
     */
    protected MissingFieldException(Direction dir, String message,
                                    Throwable cause, boolean enableSuppression,
                                    boolean writableStackTrace) {
        super(dir, message, cause, enableSuppression, writableStackTrace);
    }
}
