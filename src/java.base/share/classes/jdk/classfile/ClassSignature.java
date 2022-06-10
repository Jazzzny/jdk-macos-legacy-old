/*
 * Copyright (c) 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package jdk.classfile;

import java.util.List;
import jdk.classfile.impl.SignaturesImpl;
import static java.util.Objects.requireNonNull;
import static jdk.classfile.impl.SignaturesImpl.null2Empty;

/**
 * Models the generic signature of a class, as defined by JVMS 4.7.9.
 */
public sealed interface ClassSignature
        permits SignaturesImpl.ClassSignatureImpl {

    /** {@return the type parameters of this class} */
    List<Signature.TypeParam> typeParameters();

    /** {@return the instantiation of the superclass in this signature} */
    Signature.RefTypeSig superclassSignature();

    /** {@return the instantiation of the interfaces in this signature} */
    List<Signature.RefTypeSig> superinterfaceSignatures();

    /** {@return the raw signature string} */
    String signatureString();

    /**
     * {@return a signature}
     * @param superclassSignature the superclass
     * @param superinterfaceSignatures the interfaces
     */
    public static ClassSignature of(Signature.RefTypeSig superclassSignature,
                                    Signature.RefTypeSig... superinterfaceSignatures) {
        return of(null, superclassSignature, superinterfaceSignatures);
    }

    /**
     * {@return a signature}
     * @param typeParameters the type parameters
     * @param superclassSignature the superclass
     * @param superinterfaceSignatures the interfaces
     */
    public static ClassSignature of(List<Signature.TypeParam> typeParameters,
                                    Signature.RefTypeSig superclassSignature,
                                    Signature.RefTypeSig... superinterfaceSignatures) {
        requireNonNull(superclassSignature);
        return new SignaturesImpl.ClassSignatureImpl(null2Empty(typeParameters),
                                                     superclassSignature, List.of(superinterfaceSignatures));
    }

    /**
     * Parses a raw class signature string into a {@linkplain Signature}
     * @param signature the raw signature string
     * @return the signature
     */
    public static ClassSignature parseFrom(String signature) {
        requireNonNull(signature);
        return new SignaturesImpl().parseClassSignature(signature);
    }
}
