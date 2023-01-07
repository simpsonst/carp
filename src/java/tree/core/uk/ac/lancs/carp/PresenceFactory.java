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
 * Provides implementations of presences.
 *
 * @author simpsons
 */
public interface PresenceFactory {
    /**
     * Specifies a factory's ability to provide an implementation of a
     * presence.
     */
    public enum Suitability {
        /**
         * No presence can be created with the provided parameters.
         */
        UNMET,

        /**
         * A reasonably optimal presence can be created with the
         * provided parameters.
         */
        OKAY,

        /**
         * A presence can be created with the provided parameters, but
         * it would be somewhat suboptimal. A factory returning this
         * code will be used only if no other factory returns
         * {@link #OKAY}.
         */
        SUBOPTIMAL,

        /**
         * A presence can be created with the provided parameters, but
         * much of it will be unused. A factory returning this code will
         * be used only if no other factory returns {@link #OKAY} or
         * {@link #SUBOPTIMAL}.
         */
        OVERKILL;
    }

    /**
     * Determine the suitability of a client presence created by this
     * factory to provided parameters.
     * 
     * @param params the parameters to consider
     * 
     * @return the suitability of this factory's implementation
     */
    Suitability considerClient(Configuration params);

    /**
     * Determine the suitability of a server presence created by this
     * factory to provided parameters.
     * 
     * @param params the parameters to consider
     * 
     * @return the suitability of this factory's implementation
     */
    Suitability considerServer(Configuration params);

    /**
     * Determine the suitability of a duplex presence created by this
     * factory to provided parameters.
     * 
     * @param params the parameters to consider
     * 
     * @return the suitability of this factory's implementation
     */
    Suitability consider(Configuration params);

    /**
     * Create a client presence with provided parameters.
     * 
     * @param params the parameters of the presence
     * 
     * @return the requested presence
     * 
     * @throws IllegalArgumentException if a presence cannot be created
     * with the provided parameters
     */
    ClientPresence buildClient(Configuration params);

    /**
     * Create a server presence with provided parameters.
     * 
     * @param params the parameters of the presence
     * 
     * @return the requested presence
     * 
     * @throws IllegalArgumentException if a presence cannot be created
     * with the provided parameters
     */
    ServerPresence buildServer(Configuration params);

    /**
     * Create a duplex presence with provided parameters.
     * 
     * @param params the parameters of the presence
     * 
     * @return the requested presence
     * 
     * @throws IllegalArgumentException if a presence cannot be created
     * with the provided parameters
     */
    Presence build(Configuration params);
}
