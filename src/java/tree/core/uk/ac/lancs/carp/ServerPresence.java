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
import uk.ac.lancs.carp.component.Agency;

/**
 * Defines a point of presence to expose objects to remote invocation.
 * 
 * @author simpsons
 */
public interface ServerPresence {
    /**
     * Expose an object's interface, with sub-components.
     * 
     * @param type the service type
     * 
     * @param receiver the object to invoke
     * 
     * @param <Srv> the service type
     * 
     * @param agency a mapping between sub-paths of the suffix and
     * components of the receiver
     * 
     * @param suffix the suffix beneath the presence to bind the
     * object's interface to
     */
    <Srv> void bind(String suffix, Class<Srv> type, Srv receiver,
                    Agency agency);

    /**
     * Expose an object's interface.
     * 
     * @param type the service type
     * 
     * @param receiver the object to invoke
     * 
     * @param <Srv> the service type
     * 
     * @param suffix the suffix beneath the presence to bind the
     * object's interface to
     * 
     * @default This method calls
     * {@link #bind(String, Class, Object, Agency)} with {@code null}
     * for the {@link Agency}.
     */
    default <Srv> void bind(String suffix, Class<Srv> type, Srv receiver) {
        this.bind(suffix, type, receiver, null);
    }

    /**
     * Withdraw an object's interface, identified by suffix.
     * 
     * @param suffix the suffix beneath the presence which the object's
     * interface is bound to
     */
    void unbind(String suffix);

    /**
     * Withdraw an object's interface.
     * 
     * @param type the service type
     * 
     * @param <Srv> the service type
     * 
     * @param receiver the object to invoke
     */
    <Srv> void unbind(Class<Srv> type, Srv receiver);

    /**
     * Get the public address of an exposed object.
     * 
     * @param <Srv> the service type
     * 
     * @param type the service type
     * 
     * @param receiver the object to invoke
     * 
     * @return the external address of the object; or {@code null} if
     * not found
     */
    <Srv> URI expose(Class<Srv> type, Srv receiver);
}
