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

package jdk.classfile.attribute;

import jdk.classfile.Attribute;
import jdk.classfile.ClassElement;
import jdk.classfile.ClassSignature;
import jdk.classfile.FieldElement;
import jdk.classfile.MethodElement;
import jdk.classfile.constantpool.Utf8Entry;
import jdk.classfile.impl.BoundAttribute;
import jdk.classfile.MethodSignature;
import jdk.classfile.Signature;
import jdk.classfile.impl.TemporaryConstantPool;
import jdk.classfile.impl.UnboundAttribute;

/**
 * Models the {@code Signature} attribute (JVMS 4.7.9), which
 * can appear on classes, methods, or fields. Delivered as a
 * {@link jdk.classfile.ClassElement}, {@link jdk.classfile.FieldElement}, or
 * {@link jdk.classfile.MethodElement} when traversing
 * the corresponding model type.
 */
public sealed interface SignatureAttribute
        extends Attribute<SignatureAttribute>,
                ClassElement, MethodElement, FieldElement
        permits BoundAttribute.BoundSignatureAttribute, UnboundAttribute.UnboundSignatureAttribute {

    /**
     * {@return the signature for the class, method, or field}
     */
    Utf8Entry signature();

    /**
     * Parse the siganture as a class signature.
     * @return the class signature
     */
    default ClassSignature asClassSignature() {
        return ClassSignature.parseFrom(signature().stringValue());
    }

    /**
     * Parse the siganture as a method signature.
     * @return the method signature
     */
    default MethodSignature asMethodSignature() {
        return MethodSignature.parseFrom(signature().stringValue());
    }

    /**
     * Parse the siganture as a type signature.
     * @return the type signature
     */
    default Signature asTypeSignature() {
        return Signature.parseFrom(signature().stringValue());
    }

    /**
     * {@return a {@code Signature} attribute for a class}
     * @param classSignature the signature
     */
    static SignatureAttribute of(ClassSignature classSignature) {
        return of(TemporaryConstantPool.INSTANCE.utf8Entry(classSignature.signatureString()));
    }

    /**
     * {@return a {@code Signature} attribute for a method}
     * @param methodSignature the signature
     */
    static SignatureAttribute of(MethodSignature methodSignature) {
        return of(TemporaryConstantPool.INSTANCE.utf8Entry(methodSignature.signatureString()));
    }

    /**
     * {@return a {@code Signature} attribute}
     * @param signature the signature
     */
    static SignatureAttribute of(Signature signature) {
        return of(TemporaryConstantPool.INSTANCE.utf8Entry(signature.signatureString()));
    }

    /**
     * {@return a {@code Signature} attribute}
     * @param signature the signature
     */
    static SignatureAttribute of(Utf8Entry signature) {
        return new UnboundAttribute.UnboundSignatureAttribute(signature);
    }
}
