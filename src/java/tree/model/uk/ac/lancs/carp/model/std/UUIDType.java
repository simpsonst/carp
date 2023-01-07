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

package uk.ac.lancs.carp.model.std;

import java.util.Properties;
import java.util.UUID;
import javax.json.JsonString;
import uk.ac.lancs.carp.codec.Codecs;
import uk.ac.lancs.carp.codec.Decoder;
import uk.ac.lancs.carp.codec.Encoder;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LinkContext;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.Type;

/**
 * Models a string type.
 * 
 * @author simpsons
 */
public final class UUIDType implements Type {
    private UUIDType() {}

    /**
     * The sole instance of this class
     */
    public static final UUIDType INSTANCE = new UUIDType();

    /**
     * {@inheritDoc}
     * 
     * @default This implementation throws
     * {@link UnsupportedOperationException}.
     */
    @Override
    public void describe(String prefix, Properties props) {
        throw new UnsupportedOperationException("unreachable");
    }

    static final String CLASS_NAME = "java.util.UUID";

    /**
     * {@inheritDoc}
     * 
     * @default This implementation always returns {@value #CLASS_NAME}.
     */
    @Override
    public String declareJava(boolean primitive, boolean erase, ExpansionContext ctxt) {
        return CLASS_NAME;
    }

    /**
     * {@inheritDoc}
     * 
     * @default This implementation always returns this object.
     */
    @Override
    public Type qualify(ExternalName name, QualificationContext ctxt) {
        return this;
    }

    private static final Encoder encoder =
        (value, ctxt) -> Codecs.asJson((UUID) value);

    /**
     * {@inheritDoc}
     * 
     * @default This implementation always returns an encoder based on
     * {@link Codecs#asJson(java.lang.String)}.
     */
    @Override
    public Encoder getEncoder(Class<?> type, LinkContext ctxt) {
        return encoder;
    }

    private static final Decoder decoder =
        (value, ctxt) -> Codecs.asUUID((JsonString) value);

    /**
     * {@inheritDoc}
     * 
     * @default This implementation always returns a decoder based on
     * {@link JsonString#getString()}.
     */
    @Override
    public Decoder getDecoder(Class<?> type, LinkContext ctxt) {
        return decoder;
    }
}
