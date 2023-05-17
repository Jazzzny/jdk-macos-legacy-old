/*
 * Copyright (c) 2023, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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
package org.openjdk.bench.java.lang.invoke;

import jdk.internal.classfile.Classfile;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.lang.constant.ConstantDescs.*;
import static jdk.internal.classfile.Classfile.ACC_STATIC;

/**
 * A benchmark ensuring that var and method handle lazy initialization are not
 * too slow compared to eager initialization.
 */
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 10, warmups = 5, jvmArgsAppend = {
        "--add-exports", "java.base/jdk.internal.classfile=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.classfile.attribute=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.classfile.constantpool=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.classfile.instruction=ALL-UNNAMED",
        "--add-exports", "java.base/jdk.internal.classfile.components=ALL-UNNAMED"
})
public class LazyStaticColdStart {
    private static final MethodHandles.Lookup LOOKUP = MethodHandles.lookup();

    private Class<?> targetClass;

    /**
     * Ensures non-initialized targetClass is used and initializes the lazy/non-lazy handles
     * to prevent further creation costs.
     */
    @Setup(Level.Iteration)
    public void setup() throws Throwable {
        class Holder {
            static final ClassDesc describedClass = LazyStaticColdStart.class.describeConstable().orElseThrow().nested("Data");
            static final ClassDesc CD_ThreadLocalRandom = ThreadLocalRandom.class.describeConstable().orElseThrow();
            static final ClassDesc CD_Blackhole = Blackhole.class.describeConstable().orElseThrow();
            static final MethodTypeDesc MTD_void_long = MethodTypeDesc.of(CD_void, CD_long);
            static final MethodTypeDesc MTD_ThreadLocalRandom = MethodTypeDesc.of(CD_ThreadLocalRandom);
            static final MethodTypeDesc MTD_long = MethodTypeDesc.of(CD_long);
            static final byte[] classBytes = Classfile.build(describedClass, clb -> {
                clb.withField("v", CD_long, ACC_STATIC);
                clb.withMethodBody(CLASS_INIT_NAME, MTD_void, ACC_STATIC, cob -> {
                    cob.constantInstruction(100L);
                    cob.invokestatic(CD_Blackhole, "consumeCPU", MTD_void_long);
                    cob.invokestatic(CD_ThreadLocalRandom, "current", MTD_ThreadLocalRandom);
                    cob.invokevirtual(CD_ThreadLocalRandom, "nextLong", MTD_long);
                    cob.putstatic(describedClass, "v", CD_long);
                    cob.return_();
                });
            });

            static {
                class AnotherLazy {
                    static long f;
                }
                try {
                    LOOKUP.findStaticVarHandle(AnotherLazy.class, "f", long.class);
                    LOOKUP.findStaticGetter(AnotherLazy.class, "f", long.class);
                    AnotherLazy.f = 5L; // initialize class
                    LOOKUP.findStaticVarHandle(AnotherLazy.class, "f", long.class);
                    LOOKUP.findStaticGetter(AnotherLazy.class, "f", long.class);
                } catch (RuntimeException | Error e) {
                    throw e;
                } catch (Throwable ex) {
                    throw new ExceptionInInitializerError(ex);
                }
            }
        }
        targetClass = LOOKUP.defineHiddenClass(Holder.classBytes, false).lookupClass();
    }

    @Benchmark
    public VarHandle varHandleCreateLazy() throws Throwable {
        return LOOKUP.findStaticVarHandle(targetClass, "v", long.class);
    }

    @Benchmark
    public VarHandle varHandleCreateEager() throws Throwable {
        LOOKUP.ensureInitialized(targetClass);
        return LOOKUP.findStaticVarHandle(targetClass, "v", long.class);
    }

    @Benchmark
    public long varHandleInitializeCallLazy() throws Throwable {
        return (long) LOOKUP.findStaticVarHandle(targetClass, "v", long.class).get();
    }

    @Benchmark
    public long varHandleInitializeCallEager() throws Throwable {
        LOOKUP.ensureInitialized(targetClass);
        return (long) LOOKUP.findStaticVarHandle(targetClass, "v", long.class).get();
    }

    @Benchmark
    public MethodHandle methodHandleCreateLazy() throws Throwable {
        return LOOKUP.findStaticGetter(targetClass, "v", long.class);
    }

    @Benchmark
    public MethodHandle methodHandleCreateEager() throws Throwable {
        LOOKUP.ensureInitialized(targetClass);
        return LOOKUP.findStaticGetter(targetClass, "v", long.class);
    }

    @Benchmark
    public long methodHandleInitializeCallLazy() throws Throwable {
        return (long) LOOKUP.findStaticGetter(targetClass, "v", long.class).invokeExact();
    }

    @Benchmark
    public long methodHandleInitializeCallEager() throws Throwable {
        LOOKUP.ensureInitialized(targetClass);
        return (long) LOOKUP.findStaticGetter(targetClass, "v", long.class).invokeExact();
    }
}
