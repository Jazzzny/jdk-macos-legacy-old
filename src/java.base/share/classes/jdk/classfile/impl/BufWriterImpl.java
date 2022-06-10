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


import java.util.Arrays;
import java.util.List;

import jdk.classfile.BufWriter;
import jdk.classfile.constantpool.ClassEntry;
import jdk.classfile.constantpool.ConstantPool;
import jdk.classfile.constantpool.ConstantPoolBuilder;
import jdk.classfile.WritableElement;

import java.nio.ByteBuffer;

import jdk.classfile.constantpool.PoolEntry;
import jdk.classfile.constantpool.Utf8Entry;

public final class BufWriterImpl implements BufWriter {

    private final ConstantPoolBuilder constantPool;
    private LabelResolver labelResolver;
    private ClassEntry thisClass;
    byte[] elems;
    int offset = 0;

    public BufWriterImpl(ConstantPoolBuilder constantPool) {
        this(constantPool, 64);
    }

    public BufWriterImpl(ConstantPoolBuilder constantPool, int initialSize) {
        this.constantPool = constantPool;
        elems = new byte[initialSize];
    }

    @Override
    public ConstantPoolBuilder constantPool() {
        return constantPool;
    }

    public LabelResolver labelResolver() {
        return labelResolver;
    }

    public void setLabelResolver(LabelResolver labelResolver) {
        this.labelResolver = labelResolver;
    }
    @Override
    public boolean canWriteDirect(ConstantPool other) {
        return constantPool.canWriteDirect(other);
    }

    public ClassEntry thisClass() {
        return thisClass;
    }

    public void setThisClass(ClassEntry thisClass) {
        this.thisClass = thisClass;
    }

    @Override
    public void writeU1(int x) {
        writeIntBytes(1, x);
    }

    @Override
    public void writeU2(int x) {
        writeIntBytes(2, x);
    }

    @Override
    public void writeInt(int x) {
        writeIntBytes(4, x);
    }

    @Override
    public void writeFloat(float x) {
        writeInt(Float.floatToIntBits(x));
    }

    @Override
    public void writeLong(long x) {
        writeIntBytes(8, x);
    }

    @Override
    public void writeDouble(double x) {
        writeLong(Double.doubleToLongBits(x));
    }

    @Override
    public void writeBytes(byte[] arr) {
        writeBytes(arr, 0, arr.length);
    }

    @Override
    public void writeBytes(BufWriter other) {
        BufWriterImpl o = (BufWriterImpl) other;
        writeBytes(o.elems, 0, o.offset);
    }

    @Override
    public void writeBytes(byte[] arr, int start, int length) {
        reserveSpace(length);
        System.arraycopy(arr, start, elems, offset, length);
        offset += length;
    }

    @Override
    public void patchInt(int offset, int size, int value) {
        int prevOffset = this.offset;
        this.offset = offset;
        writeIntBytes(size, value);
        this.offset = prevOffset;
    }

    @Override
    public void writeIntBytes(int intSize, long intValue) {
        reserveSpace(intSize);
        for (int i = 0; i < intSize; i++) {
            elems[offset++] = (byte) ((intValue >> 8 * (intSize - i - 1)) & 0xFF);
        }
    }

    @Override
    public void reserveSpace(int freeBytes) {
        if (offset + freeBytes > elems.length) {
            int newsize = elems.length * 2;
            while (offset + freeBytes > newsize) {
                newsize *= 2;
            }
            elems = Arrays.copyOf(elems, newsize);
        }
    }

    @Override
    public int size() {
        return offset;
    }

    @Override
    public ByteBuffer asByteBuffer() {
        return ByteBuffer.wrap(elems, 0, offset);
    }

    @Override
    public void copyTo(byte[] array, int bufferOffset) {
        System.arraycopy(elems, 0, array, bufferOffset, size());
    }

    // writeIndex methods ensure that any CP info written
    // is relative to the correct constant pool

    @Override
    public void writeIndex(PoolEntry entry) {
        int idx = constantPool.maybeClone(entry).index();
        if (idx < 1 || idx > Character.MAX_VALUE)
            throw new IllegalArgumentException(idx + " is not a valid index. Entry: " + entry);
        writeU2(idx);
    }

    @Override
    public void writeIndexOrZero(PoolEntry entry) {
        if (entry == null || entry.index() == 0)
            writeU2(0);
        else
            writeIndex(entry);
    }

    @Override
    public<T extends WritableElement<?>> void writeList(List<T> list) {
        writeU2(list.size());
        for (T t : list) {
            t.writeTo(this);
        }
    }

    @Override
    public void writeListIndices(List<? extends PoolEntry> list) {
        writeU2(list.size());
        for (PoolEntry info : list) {
            writeIndex(info);
        }
    }
}
