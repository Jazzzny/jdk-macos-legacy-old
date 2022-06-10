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

/*
 * @test
 * @summary Testing Classfile low module attribute.
 * @run testng LowModuleTest
 */
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystems;
import java.nio.file.Files;

import java.lang.reflect.Method;
import java.nio.file.Path;

import jdk.classfile.Attribute;
import jdk.classfile.ClassModel;
import jdk.classfile.Classfile;
import jdk.classfile.Attributes;
import jdk.classfile.attribute.*;
import jdk.classfile.constantpool.ClassEntry;
import jdk.classfile.constantpool.ModuleEntry;
import jdk.classfile.constantpool.PackageEntry;
import jdk.classfile.constantpool.Utf8Entry;
import org.testng.ITest;
import org.testng.annotations.*;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

/**
 * LowModuleTest
 */
@Test
public class LowModuleTest implements ITest {

    private static final boolean VERBOSE = false;

    @DataProvider(name = "corpus")
    public static Object[] provide() throws IOException {
        return Files.walk(FileSystems.getFileSystem(URI.create("jrt:/")).getPath("modules/"))
                .filter(p -> Files.isRegularFile(p))
                .filter(p -> p.endsWith("module-info.class"))
                .toArray();
    }

    private String testMethod = "";
    private final Path path;
    private final ClassModel classLow;

    @Factory(dataProvider = "corpus")
    public LowModuleTest(Path path) throws IOException {
        this.path = path;
        this.classLow = Classfile.parse(path);
    }

    @BeforeMethod
    public void handleTestMethodName(Method method) {
        testMethod = method.getName();
    }

    @Override
    public String getTestName() {
        return testMethod + "[" + path.toString() + "]";
    }

    @Test
    public void testRead() {
        try {
            testRead0();
        } catch(Exception ex) {
            System.err.printf("%nFAIL %s - %s%n", path, ex);
            ex.printStackTrace(System.err);
            throw ex;
        }
    }

    private void testRead0() {
        for (Attribute<?> attr : classLow.attributes()) {
            printf("%nCHECK %s%n", getTestName());
            switch (attr.attributeName()) {
                case Attributes.NAME_SOURCE_FILE: {
                    SourceFileAttribute sfa = (SourceFileAttribute) attr;
                    Utf8Entry sf = sfa.sourceFile();
                    printf("SourceFile %s%n", sf);
                    break;
                }
                case Attributes.NAME_MODULE: {
                    ModuleAttribute mal = (ModuleAttribute) attr;
                    ModuleEntry mni = mal.moduleName();
                    int mf = mal.moduleFlagsMask();
                    Utf8Entry mv = mal.moduleVersion().orElse(null);
                    printf("Module %s [%d] %s%n", mni, mf, mv);
                    for (ModuleRequireInfo r : mal.requires()) {
                        ModuleEntry rm = r.requires();
                        int ri = r.requiresFlagsMask();
                        Utf8Entry rv = r.requiresVersion().orElse(null);
                        printf("  Requires %s [%d] %s%n", rm, ri, rv);
                    }
                    for (ModuleExportInfo e : mal.exports()) {
                        printf("  Export %s [%d] - ",
                               e.exportedPackage(), e.exportsFlags());
                        for (ModuleEntry mi : e.exportsTo()) {
                            printf("%s ", mi);
                        }
                        println();
                    }
                    for (ModuleOpenInfo o : mal.opens()) {
                        printf("  Open %s [%d] - ",
                               o.openedPackage(), o.opensFlags());
                        for (ModuleEntry mi : o.opensTo()) {
                            printf("%s ", mi);
                        }
                        println();
                    }
                    for (ClassEntry u : mal.uses()) {
                        printf("  Use %s%n", u);
                    }
                    for (ModuleProvideInfo provide : mal.provides()) {
                        printf("  Provide %s - ", provide.provides());
                        for (ClassEntry ci : provide.providesWith()) {
                            printf("%s ", ci);
                        }
                        println();
                    }
                    break;
                }
                case Attributes.NAME_MODULE_PACKAGES: {
                    ModulePackagesAttribute mp = (ModulePackagesAttribute) attr;
                    printf("ModulePackages%n");
                    for (PackageEntry pi : mp.packages()) {
                        printf("  %s%n", pi);
                    }
                    break;
                }
                case Attributes.NAME_MODULE_TARGET: {
                    ModuleTargetAttribute mt = (ModuleTargetAttribute) attr;
                    printf("ModuleTarget %s%n", mt.targetPlatform());
                    break;
                }
                case Attributes.NAME_MODULE_RESOLUTION: {
                    ModuleResolutionAttribute mr = (ModuleResolutionAttribute) attr;
                    printf("ModuleResolution %d%n", mr.resolutionFlags());
                    break;
                }
                case Attributes.NAME_MODULE_HASHES: {
                    ModuleHashesAttribute mh = (ModuleHashesAttribute) attr;
                    printf("ModuleHashes %s%n", mh.algorithm());
                    for (ModuleHashInfo hi : mh.hashes()) {
                        printf("  %s: %n", hi.moduleName());
                        for (byte b : hi.hash()) {
                            printf("%2x", b);
                        }
                        println();
                    }
                    break;
                }
            }
        }
    }

    private void printf(String format, Object... args) {
        if (VERBOSE) {
            System.out.printf(format, args);
        }
    }

    private void println() {
        if (VERBOSE) {
            System.out.println();
        }
    }

//    @Test
//    public void testWrite() {
//        try {
//            testWrite0();
//        } catch(Exception ex) {
//            System.err.printf("%nFAIL %s - %s%n", path, ex);
//            ex.printStackTrace(System.err);
//            throw ex;
//        }
//    }
//
//    private void testWrite0() {
//        ConstantPoolLow cp = classLow.constantPool();
//        ConstantPoolLow cp2 = cp.clonedPool();
//        int sz = cp.size();
//        // check match of constant pools
//        assertEquals(cp2.size(), cp.size(),  "Cloned size should match");
//        for (int i = 1; i < sz; ) {
//            ConstantPoolInfo info = cp.get(i);
//            ConstantPoolInfo info2 = cp2.get(i);
//            assertNotNull(info2, "Test set up failure -- Null CP entry copy of " + info.tag() + " [" + info.index() + "] @ "
//                    + i
//                    + " -- " + getTestName()
//            );
//            assertEquals(info2.index(), info.index(),
//                    "Test set up failure -- copying constant pool (index). \n"
//                            + "Orig: " + info.tag() + " [" + info.index() + "].\n"
//                            + "Copy: " + info2.tag() + " [" + info2.index() + "].");
//            assertEquals(info2.tag(), info.tag(),
//                    "Test set up failure -- copying constant pool (tag). \n"
//                            + "Orig: " + info.tag() + " [" + info.index() + "].\n"
//                            + "Copy: " + info2.tag() + " [" + info2.index() + "].");
//            i += info.tag().poolEntries;
//        }
//        for (Attribute attr : classLow.attributes()) {
//            if (attr instanceof UnknownAttribute) {
//                System.err.printf("Unknown attribute %s - in %s%n", attr.attributeName(), path);
//            } else {
//                BufWriter gbb = new BufWriter(cp);
//                BufWriter gbb2 = new BufWriter(cp);
//                attr.writeTo(gbb);
//                attr.writeTo(gbb2);
//                assertEquals(gbb2.bytes(), gbb.bytes(),
//                        "Mismatched written attributes -  " + attr);
//            }
//        }
//    }
}
