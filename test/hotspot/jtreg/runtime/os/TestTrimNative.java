/*
 * Copyright (c) 2023 SAP SE. All rights reserved.
 * Copyright (c) 2023 Red Hat, Inc. All rights reserved.
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
 *
 */

/*
 * @test id=serial
 * @requires vm.gc.Serial
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative test -XX:+UseSerialGC
 */

/*
 * @test id=parallel
 * @requires vm.gc.Serial
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative test -XX:+UseParallelGC
 */

/*
 * @test id=shenandoah
 * @requires vm.gc.Serial
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative test -XX:+UseShenandoahGC
 */

/*
 * @test id=Znongen
 * @requires vm.gc.Serial
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative test -XX:+UseZGC -XX:-ZGenerational
 */

/*
 * @test id=Zgen
 * @requires vm.gc.Serial
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative test -XX:+UseZGC -XX:+ZGenerational
 */

// Other tests

/*
 * @test id=testOffByDefault
 * @summary Test that -GCTrimNative disables the feature
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative testOffByDefault
 */

/*
 * @test id=testOffExplicit
 * @summary Test that GCTrimNative is off by default
 * @requires (os.family=="linux") & !vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative testOffExplicit
 */

/*
 * @test id=testOffOnNonCompliantPlatforms
 * @summary Test that GCTrimNative is off on unsupportive platforms
 * @requires (os.family!="linux") | vm.musl
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @run driver TestTrimNative testOffOnNonCompliantPlatforms
 */

import jdk.internal.misc.Unsafe;
import jdk.test.lib.Platform;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;

import java.io.IOException;
import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestTrimNative {

    // Actual RSS increase is a lot larger than 4 MB. Depends on glibc overhead, and NMT malloc headers in debug VMs.
    // We need small-grained allocations to make sure they actually increase RSS (all touched) and to see the
    // glibc-retaining-memory effect.
    static final int szAllocations = 16;
    static final int totalAllocationsSize = 16 * 1024 * 1024; // 16 MB total
    static final int numAllocations = totalAllocationsSize / szAllocations;

    static long[] ptrs = new long[numAllocations];

    enum Unit {
        B(1), K(1024), M(1024*1024), G(1024*1024*1024);
        public final long size;
        Unit(long size) { this.size = size; }
    }

    private static OutputAnalyzer runTestWithOptions(String[] extraOptions) throws IOException {

        List<String> allOptions = new ArrayList<String>();
        allOptions.addAll(Arrays.asList(extraOptions));
        allOptions.add("-Xmx128m");
        allOptions.add("-Xms128m"); // Stabilize RSS
        allOptions.add("-XX:+AlwaysPreTouch"); // Stabilize RSS
        allOptions.add("-XX:-ExplicitGCInvokesConcurrent"); // Invoke explicit GC on System.gc
        allOptions.add("-Xlog:trim=debug");
        allOptions.add("--add-exports=java.base/jdk.internal.misc=ALL-UNNAMED");

        allOptions.add(TestTrimNative.class.getName());
        allOptions.add("RUN");
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(allOptions.toArray(new String[0]));
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldHaveExitValue(0);
        return output;
    }

    private static void checkExpectedLogMessages(OutputAnalyzer output, boolean expectEnabled,
                                                 int expectedInterval) {
        if (expectEnabled) {
            output.shouldContain("Periodic native trim enabled (interval: " + expectedInterval + " ms");
            output.shouldContain("NativeTrimmer start");
            output.shouldContain("NativeTrimmer stop");
        } else {
            output.shouldNotContain("Periodic native trim enabled");
        }
    }

    /**
     * Given JVM output, look for one or more log lines that describes a successful negative trim. The total amount
     * of trims should be matching about what the test program allocated.
     * @param output
     * @param minTrimsExpected min number of periodic trim lines expected in UL log
     * @param maxTrimsExpected min number of periodic trim lines expected in UL log
     */
    private static void parseOutputAndLookForNegativeTrim(OutputAnalyzer output, int minTrimsExpected,
                                                          int maxTrimsExpected) {
        output.reportDiagnosticSummary();
        List<String> lines = output.asLines();
        Pattern pat = Pattern.compile(".*\\[trim\\] Trim native heap: RSS\\+Swap: (\\d+)([BKMG])->(\\d+)([BKMG]).*");
        int numTrimsFound = 0;
        long rssReductionTotal = 0;
        for (String line : lines) {
            Matcher mat = pat.matcher(line);
            if (mat.matches()) {
                long rss1 = Long.parseLong(mat.group(1)) * Unit.valueOf(mat.group(2)).size;
                long rss2 = Long.parseLong(mat.group(3)) * Unit.valueOf(mat.group(4)).size;
                System.out.println("Parsed Trim Line. rss1: " + rss1 + " rss2: " + rss2);
                if (rss1 > rss2) {
                    rssReductionTotal += (rss1 - rss2);
                }
                numTrimsFound ++;
            }
            if (numTrimsFound > maxTrimsExpected) {
                throw new RuntimeException("Abnormal high number of periodic trim attempts found (more than " + maxTrimsExpected +
                        "). Does the interval setting not work?");
            }
        }
        if (numTrimsFound < minTrimsExpected) {
            throw new RuntimeException("We found fewer (periodic) trim lines in UL log than expected (expected at least " + minTrimsExpected +
                    ", found " + numTrimsFound + ").");
        }
        // This is very fuzzy. Test program malloced X bytes, then freed them again and trimmed. But the log line prints change in RSS.
        // Which, of course, is influenced by a lot of other factors. But we expect to see *some* reasonable reduction in RSS
        // due to trimming.
        float fudge = 0.5f;
        // On ppc, we see a vastly diminished return (~3M reduction instead of ~200), I suspect because of the underlying
        // 64k pages lead to a different geometry. Manual tests with larger reclaim sizes show that autotrim works. For
        // this test, we just reduce the fudge factor.
        if (Platform.isPPC()) { // le and be both
            fudge = 0.01f;
        }
        long expectedMinimalReduction = (long) (totalAllocationsSize * fudge);
        if (rssReductionTotal < expectedMinimalReduction) {
            throw new RuntimeException("We did not see the expected RSS reduction in the UL log. Expected (with fudge)" +
                    " to see at least a combined reduction of " + expectedMinimalReduction + ".");
        }
    }

    static private final void runTest(String[] VMargs) throws IOException {
        long trimInterval = 500; // twice per second
        long ms1 = System.currentTimeMillis();
        OutputAnalyzer output = runTestWithOptions (
                new String[] { "-XX:+UnlockExperimentalVMOptions",
                               "-XX:+TrimNativeHeap",
                               "-XX:TrimNativeHeapInterval=" + trimInterval
                }
        );
        long ms2 = System.currentTimeMillis();
        long runtime_ms = ms2 - ms1;

        checkExpectedLogMessages(output, true, 500);

        // We expect to see at least one GC-related trimming pause
        output.shouldMatch("NativeTrimmer pause.*(gc)");
        output.shouldMatch("NativeTrimmer unpause.*(gc)");

        long maxTrimsExpected = runtime_ms / trimInterval;
        long minTrimsExpected = maxTrimsExpected / 2;
        parseOutputAndLookForNegativeTrim(output, (int)minTrimsExpected, (int)maxTrimsExpected);
    }

    // Test that a high trim interval effectively disables trimming
    static private final void testHighTrimInterval() throws IOException {
        OutputAnalyzer output = runTestWithOptions (
                new String[] { "-XX:+UnlockExperimentalVMOptions",
                               "-XX:+TrimNativeHeap",
                               "-XX:TrimNativeHeapInterval=" + Integer.MAX_VALUE
                });
        checkExpectedLogMessages(output, true, Integer.MAX_VALUE);
        parseOutputAndLookForNegativeTrim(output,0, /*  minTrimsExpected */ 0  /*  maxTrimsExpected */);
    }

    // Test that trim-native gets disabled on platforms that don't support it.
    static private final void testOffOnNonCompliantPlatforms() throws IOException {
        OutputAnalyzer output = runTestWithOptions (
                new String[] { "-XX:+UnlockExperimentalVMOptions",
                               "-XX:+TrimNativeHeap"
                });
        checkExpectedLogMessages(output, false, 0);
        output.shouldContain("Native trim not supported on this platform");
    }

    // Test trim native is disabled if explicitly switched off
    static private final void testOffExplicit() throws IOException {
        OutputAnalyzer output = runTestWithOptions (
                new String[] { "-XX:+UnlockExperimentalVMOptions",
                               "-XX:-TrimNativeHeap"
                });
        checkExpectedLogMessages(output, false, 0);
    }

    // Test trim native is disabled if explicitly switched off
    static private final void testOffByDefault() throws IOException {
        OutputAnalyzer output = runTestWithOptions (new String[] { });
        checkExpectedLogMessages(output, false, 0);
    }

    public static void main(String[] args) throws Exception {

        if (args.length == 0) {
            throw new RuntimeException("Argument error");
        }

        if (args[0].equals("RUN")) {

            System.out.println("Will spike now...");
            for (int i = 0; i < numAllocations; i++) {
                ptrs[i] = Unsafe.getUnsafe().allocateMemory(szAllocations);
                Unsafe.getUnsafe().putByte(ptrs[i], (byte)0);
                Unsafe.getUnsafe().putByte(ptrs[i] + szAllocations / 2, (byte)0);
            }
            for (int i = 0; i < numAllocations; i++) {
                Unsafe.getUnsafe().freeMemory(ptrs[i]);
            }
            System.out.println("Done spiking.");

            // Do a system GC. Native trimming should be paused in that time.
            System.out.println("GC...");
            System.gc();

            // give GC time to react
            System.out.println("Sleeping...");
            Thread.sleep(3000);
            System.out.println("Done.");

            return;

        } else if (args[0].equals("test")) {
            runTest(Arrays.copyOfRange(args, 1, args.length));
        } else if (args[0].equals("testOffOnNonCompliantPlatforms")) {
            testOffOnNonCompliantPlatforms();
        } else if (args[0].equals("testOffExplicit")) {
            testOffExplicit();
        } else if (args[0].equals("testOffByDefault")) {
            testOffByDefault();
        } else {
            throw new RuntimeException("Invalid test " + args[0]);
        }
    }
}
