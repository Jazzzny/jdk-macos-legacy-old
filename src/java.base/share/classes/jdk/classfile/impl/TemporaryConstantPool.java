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
package jdk.classfile.impl;

import jdk.classfile.*;
import jdk.classfile.constantpool.ClassEntry;
import jdk.classfile.constantpool.ConstantDynamicEntry;
import jdk.classfile.constantpool.ConstantPool;
import jdk.classfile.constantpool.ConstantPoolBuilder;
import jdk.classfile.constantpool.DoubleEntry;
import jdk.classfile.constantpool.FieldRefEntry;
import jdk.classfile.constantpool.FloatEntry;
import jdk.classfile.constantpool.IntegerEntry;
import jdk.classfile.constantpool.InterfaceMethodRefEntry;
import jdk.classfile.constantpool.InvokeDynamicEntry;
import jdk.classfile.constantpool.LoadableConstantEntry;
import jdk.classfile.constantpool.LongEntry;
import jdk.classfile.constantpool.MemberRefEntry;
import jdk.classfile.constantpool.MethodHandleEntry;
import jdk.classfile.constantpool.MethodRefEntry;
import jdk.classfile.constantpool.MethodTypeEntry;
import jdk.classfile.constantpool.ModuleEntry;
import jdk.classfile.constantpool.NameAndTypeEntry;
import jdk.classfile.constantpool.PackageEntry;
import jdk.classfile.constantpool.PoolEntry;
import jdk.classfile.constantpool.StringEntry;
import jdk.classfile.constantpool.Utf8Entry;

import java.lang.constant.MethodTypeDesc;
import java.util.Collections;
import java.util.List;

import static jdk.classfile.Classfile.TAG_METHODTYPE;

public final class TemporaryConstantPool implements ConstantPoolBuilder {
    private static final Options options = new Options(Collections.emptyList());

    private TemporaryConstantPool() {};

    public static final TemporaryConstantPool INSTANCE = new TemporaryConstantPool();

    @Override
    public Utf8Entry utf8Entry(String s) {
        return new ConcreteEntry.ConcreteUtf8Entry(this, -1, s);
    }

    @Override
    public IntegerEntry intEntry(int value) {
        return new ConcreteEntry.ConcreteIntegerEntry(this, -1, value);
    }

    @Override
    public FloatEntry floatEntry(float value) {
        return new ConcreteEntry.ConcreteFloatEntry(this, -1, value);
    }

    @Override
    public LongEntry longEntry(long value) {
        return new ConcreteEntry.ConcreteLongEntry(this, -1, value);
    }

    @Override
    public DoubleEntry doubleEntry(double value) {
        return new ConcreteEntry.ConcreteDoubleEntry(this, -1, value);
    }

    @Override
    public ClassEntry classEntry(Utf8Entry name) {
        return new ConcreteEntry.ConcreteClassEntry(this, -2, (ConcreteEntry.ConcreteUtf8Entry) name);
    }

    @Override
    public PackageEntry packageEntry(Utf8Entry name) {
        return new ConcreteEntry.ConcretePackageEntry(this, -2, (ConcreteEntry.ConcreteUtf8Entry) name);
    }

    @Override
    public ModuleEntry moduleEntry(Utf8Entry name) {
        return new ConcreteEntry.ConcreteModuleEntry(this, -2, (ConcreteEntry.ConcreteUtf8Entry) name);
    }

    @Override
    public NameAndTypeEntry natEntry(Utf8Entry nameEntry, Utf8Entry typeEntry) {
        return new ConcreteEntry.ConcreteNameAndTypeEntry(this, -3,
                                                          (ConcreteEntry.ConcreteUtf8Entry) nameEntry,
                                                          (ConcreteEntry.ConcreteUtf8Entry) typeEntry);
    }

    @Override
    public FieldRefEntry fieldRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        return new ConcreteEntry.ConcreteFieldRefEntry(this, -3,
                                                       (ConcreteEntry.ConcreteClassEntry) owner,
                                                       (ConcreteEntry.ConcreteNameAndTypeEntry) nameAndType);
    }

    @Override
    public MethodRefEntry methodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        return new ConcreteEntry.ConcreteMethodRefEntry(this, -3,
                                                        (ConcreteEntry.ConcreteClassEntry) owner,
                                                        (ConcreteEntry.ConcreteNameAndTypeEntry) nameAndType);
    }

    @Override
    public InterfaceMethodRefEntry interfaceMethodRefEntry(ClassEntry owner, NameAndTypeEntry nameAndType) {
        return new ConcreteEntry.ConcreteInterfaceMethodRefEntry(this, -3,
                                                                 (ConcreteEntry.ConcreteClassEntry) owner,
                                                                 (ConcreteEntry.ConcreteNameAndTypeEntry) nameAndType);
    }

    @Override
    public MethodTypeEntry methodTypeEntry(MethodTypeDesc descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodTypeEntry methodTypeEntry(Utf8Entry descriptor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public MethodHandleEntry methodHandleEntry(int refKind, MemberRefEntry reference) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InvokeDynamicEntry invokeDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry, NameAndTypeEntry nameAndType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ConstantDynamicEntry constantDynamicEntry(BootstrapMethodEntry bootstrapMethodEntry, NameAndTypeEntry nameAndType) {
        throw new UnsupportedOperationException();
    }

    @Override
    public StringEntry stringEntry(Utf8Entry utf8) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends PoolEntry> T maybeClone(T entry) {
        return entry;
    }

    @Override
    public BootstrapMethodEntry bsmEntry(MethodHandleEntry methodReference, List<LoadableConstantEntry> arguments) {
        throw new UnsupportedOperationException();
    }

    @Override
    public PoolEntry entryByIndex(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int entryCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public BootstrapMethodEntry bootstrapMethodEntry(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int bootstrapMethodCount() {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T> T optionValue(Classfile.Option.Key option) {
        return options.value(option);
    }

    @Override
    public boolean canWriteDirect(ConstantPool constantPool) {
        return false;
    }

    @Override
    public boolean writeBootstrapMethods(BufWriter buf) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void writeTo(BufWriter buf) {
        throw new UnsupportedOperationException();
    }
}
