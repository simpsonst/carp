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

import java.io.ByteArrayOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.IntStream;

/**
 * Stores a hash code with its algorithm type.
 *
 * @author simpsons
 */
public final class Fingerprint {
    private final String algo;

    private final byte[] buf;

    private Fingerprint(String algo, byte[] buf) {
        this.algo = algo;
        this.buf = buf;
    }

    /**
     * Get the digest algorithm name.
     * 
     * @return the algorithm name
     */
    public String getAlgorithm() {
        return algo;
    }

    /**
     * Create a fingerprint from an integer stream.
     * 
     * @param algo the digest algorithm
     * 
     * @param bytes the fingerprint bytes, as an integer stream
     * 
     * @return the represented fingerprint
     */
    public static Fingerprint of(String algo, IntStream bytes) {
        byte[] buf = bytes
            .collect(ByteArrayOutputStream::new,
                     (baos, i) -> baos.write((byte) i), (baos1, baos2) -> baos1
                         .write(baos2.toByteArray(), 0, baos2.size()))
            .toByteArray();
        return new Fingerprint(algo, buf);
    }

    /**
     * Get the bytes of this fingerprint as an integer stream.
     * 
     * @return the bytes of this fingerprint
     */
    public IntStream getBytes() {
        return IntStream.range(0, buf.length).map(i -> buf[i] & 0xff);
    }

    /**
     * Get the fingerprint of a certificate.
     * 
     * @param algo the digest algorithm
     * 
     * @param cert the certificate
     * 
     * @return the certificate's fingerprint
     * 
     * @throws CertificateEncodingException if an encoding error occurs
     * 
     * @throws NoSuchAlgorithmException if the algorithm name is not
     * recognized
     */
    public static Fingerprint of(String algo, X509Certificate cert)
        throws CertificateEncodingException,
            NoSuchAlgorithmException {
        byte[] der = cert.getEncoded();
        MessageDigest md = MessageDigest.getInstance(algo);
        byte[] buf = md.digest(der);
        return new Fingerprint(algo, buf);
    }

    /**
     * Get the hash code for this hash code!
     * 
     * @return the hash code
     */
    @Override
    public int hashCode() {
        int hash = 7;
        hash = 71 * hash + Objects.hashCode(this.algo);
        hash = 71 * hash + Arrays.hashCode(this.buf);
        return hash;
    }

    /**
     * Test whether this hash code equals another object.
     * 
     * @param obj the other object
     * 
     * @return {@code true} if the other object is a hash code of the
     * same algorithm and the same bytes; {@code false} otherwise
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        final Fingerprint other = (Fingerprint) obj;
        if (!Objects.equals(this.algo, other.algo)) return false;
        if (!Arrays.equals(this.buf, other.buf)) return false;
        return true;
    }
}
