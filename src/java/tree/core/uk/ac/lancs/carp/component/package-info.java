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

/**
 * Allows for exposed objects to return components automatically exposed
 * to remote invocation under predictable names.
 * 
 * <p>
 * On the server side, given a receiver registered under the name
 * <samp>foo/bar</samp>, the receiver may yield references to receivers
 * logically related to it as its components, and it might be desirable
 * for an implementation for these objects' names to reflect that
 * relationship. For example, a control interface for the main receiver
 * might be placed under <samp>foo/bar/ctrl</samp>. A whole family of
 * receivers might exist as components, each having a unique identifier
 * to appear under (say) <samp>foo/bar/service/<var>srv-id</var></samp>.
 * These components may have additional sub-components of their own, and
 * so on. Furthermore, the implementation might wish to generate these
 * receivers on-the-fly and on-demand.
 * 
 * <p>
 * This package supports the management of the mapping between
 * components and sub-paths of a receiver as a cross-cutting concern.
 * That is, the mapping can be handled out-of-band on the server side,
 * not appearing in the IDL definition. The defined interfaces can then
 * be used both locally and remotely without consideration to the
 * component mapping logic.
 * 
 * <p>
 * Every receiver object that has components to be specially named as
 * sub-paths of the receiver must have an {@link Agency} associated with
 * it. The association is through a weak reference to the main object,
 * so if that object is transient, ephemeral or created on-demand, the
 * association will not prevent its garbage collection.
 * 
 * <p>
 * An {@link Agency} must be pre-loaded with {@link Agent}s, which can
 * be defined statically for the container type. A
 * {@link SingletonAgent} manages a singleton under a specific sub-path,
 * and takes constructor/destructor functions to build an instance on
 * demand, or to notify the application that the instance has been
 * garbage collected. An {@link IndexedAgent} manages multiple
 * components distinguishable by an identifying type, and additionally
 * needs a {@link Discriminator} to convert between that type and its
 * string representation as it appears in a URI. For example, using an
 * integer discriminator, attempting to access
 * <samp>foo/bar/service/17</samp> will invoke the agent's constructor
 * function with 17 as an argument, if the instance doesn't already
 * exist. If the application wants to create the instance itself, it
 * must not invoke the constructor directly, but
 * {@link IndexedAgent#get(Object)} instead. This will automatically
 * place the new instance under its computed path, or return the cached
 * instance.
 * 
 * <p>
 * The classes {@link StaticSingletonAgent} and
 * {@link StaticIndexedAgent} can be shared by multiple containers.
 * However, their use is limited by the fact that inner classes and
 * local classes can't have static members, so agents for multiple
 * levels of nesting must normally be in the top-level class.
 * 
 * <p>
 * The {@link PathMap} class maintains a hierarchy of nodes indexed by
 * string. Receivers may be explicitly placed in the hierarchy at
 * specific nodes, along with their agencies. Creation of any of their
 * components then automatically results in them being added (with their
 * agencies) under the appropriate sub-paths. Receivers are held weakly,
 * unless recently used. This allows an application to implement a
 * method that (say) creates one of its components and returns it in the
 * result, causing that component to be automatically registered,
 * translated to a URI, passed to the client, formed into a proxy there,
 * invoked, and that call to be relayed to the receiver, which is
 * retained for a short time after it was created, and after each use.
 * 
 * @resume Automatic naming of sub-components
 * 
 * @author simpsons
 */
package uk.ac.lancs.carp.component;
