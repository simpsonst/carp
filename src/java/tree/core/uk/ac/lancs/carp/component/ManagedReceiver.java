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

import java.util.Objects;

/**
 * Associates a receiver with its component agency.
 * 
 * @param <Impl> the receiver type
 *
 * @author simpsons
 */
public class ManagedReceiver<Impl> {
    /**
     * The receiver
     */
    public final Impl receiver;

    /**
     * The receiver's component agency
     */
    public final Agency agency;

    private ManagedReceiver(Impl receiver, Agency agency1) {
        Objects.requireNonNull(receiver, "receiver");
        Objects.requireNonNull(agency1, "agency");
        this.receiver = receiver;
        this.agency = agency1;
    }

    /**
     * Specify a receiver with its agency.
     * 
     * @param <Impl> the receiver type
     * 
     * @param receiver the receiver
     * 
     * @param agency the receiver's agency
     * 
     * @return the receiver with the specified agency
     * 
     * @throws NullPointerException if either argument is {@code null}
     */
    public static <Impl> ManagedReceiver<Impl> of(Impl receiver,
                                                  Agency agency) {
        return new ManagedReceiver<>(receiver, agency);
    }

    /**
     * Specify a receiver with an empty component agency.
     * 
     * @param <Impl> the receiver type
     * 
     * @param receiver the receiver
     * 
     * @return the receiver with an empty component agency
     * 
     * @throws NullPointerException if the receiver is {@code null}
     */
    public static <Impl> ManagedReceiver<Impl> of(Impl receiver) {
        return new ManagedReceiver<>(receiver, Agency.empty());
    }
}
