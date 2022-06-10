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
 * @summary Testing Classfile stack maps generator.
 * @build testdata.*
 * @run testng StackMapsTest
 */

import jdk.classfile.Classfile;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import org.testng.annotations.Test;
import static helpers.TestUtil.assertEmpty;
import static jdk.classfile.Classfile.ACC_STATIC;

import java.lang.constant.ClassDesc;
import java.lang.constant.ConstantDescs;
import java.lang.constant.MethodTypeDesc;
import java.util.List;
import jdk.classfile.jdktypes.AccessFlag;

/**
 * StackMapsTest
 */
public class StackMapsTest {

    private byte[] buildDeadCode() {
        return Classfile.build(
                ClassDesc.of("DeadCodePattern"),
                List.of(Classfile.Option.generateStackmap(false), Classfile.Option.patchDeadCode(false)),
                clb -> clb.withMethodBody(
                        "twoReturns",
                        MethodTypeDesc.of(ConstantDescs.CD_void),
                        0,
                        cob -> cob.return_().return_()));
    }

    @Test
    public void testDeadCodePatternPatch() throws Exception {
        testTransformedStackMaps(buildDeadCode());
    }

    @Test(expectedExceptions = VerifyError.class)
    public void testDeadCodePatternFail() throws Exception {
        testTransformedStackMaps(buildDeadCode(), Classfile.Option.patchDeadCode(false));
    }

    @Test
    public void testUnresolvedPermission() throws Exception {
        testTransformedStackMaps("modules/java.base/java/security/UnresolvedPermission.class");
    }

    @Test
    public void testURL() throws Exception {
        testTransformedStackMaps("modules/java.base/java/net/URL.class");
    }

    @Test
    public void testPattern1() throws Exception {
        testTransformedStackMaps("/testdata/Pattern1.class");
    }

    @Test
    public void testPattern2() throws Exception {
        testTransformedStackMaps("/testdata/Pattern2.class");
    }

    @Test
    public void testPattern3() throws Exception {
        testTransformedStackMaps("/testdata/Pattern3.class");
    }

    @Test
    public void testPattern4() throws Exception {
        testTransformedStackMaps("/testdata/Pattern4.class");
    }

    @Test
    public void testPattern5() throws Exception {
        testTransformedStackMaps("/testdata/Pattern5.class");
    }

    @Test
    public void testPattern6() throws Exception {
        testTransformedStackMaps("/testdata/Pattern6.class");
    }

    @Test
    public void testPattern7() throws Exception {
        testTransformedStackMaps("/testdata/Pattern7.class");
    }

    @Test
    public void testPattern8() throws Exception {
        testTransformedStackMaps("/testdata/Pattern8.class");
    }

    @Test
    public void testPattern9() throws Exception {
        testTransformedStackMaps("/testdata/Pattern9.class");
    }

    @Test
    public void testPattern10() throws Exception {
        testTransformedStackMaps("/testdata/Pattern10.class");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMethodSwitchFromStatic() {
        Classfile.build(ClassDesc.of("TestClass"), clb ->
                clb.withMethod("testMethod", MethodTypeDesc.of(ConstantDescs.CD_Object, ConstantDescs.CD_int),
                               ACC_STATIC,
                               mb -> mb.withCode(cob -> {
                                           var t = cob.newLabel();
                                           cob.aload(0).goto_(t).labelBinding(t).areturn();
                                       })
                                       .withFlags()));
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void testMethodSwitchToStatic() {
        Classfile.build(ClassDesc.of("TestClass"), clb ->
                clb.withMethod("testMethod", MethodTypeDesc.of(ConstantDescs.CD_int, ConstantDescs.CD_int),
                               0, mb ->
                                       mb.withCode(cob -> {
                                             var t = cob.newLabel();
                                             cob.iload(0).goto_(t).labelBinding(t).ireturn();
                                         })
                                         .withFlags(AccessFlag.STATIC)));
    }

    private static final FileSystem JRT = FileSystems.getFileSystem(URI.create("jrt:/"));

    private static void testTransformedStackMaps(String classPath, Classfile.Option<?>... options) throws Exception {
        testTransformedStackMaps(
                classPath.startsWith("/")
                            ? StackMapsTest.class.getResourceAsStream(classPath).readAllBytes()
                            : Files.readAllBytes(JRT.getPath(classPath)),
                options);
    }

    private static void testTransformedStackMaps(byte[] originalBytes, Classfile.Option<?>... options) throws Exception {
        //transform the class model
        var classModel = Classfile.parse(originalBytes, options);
        var transformedBytes = Classfile.build(classModel.thisClass().asSymbol(), List.of(options),
                                               cb -> {
//                                                   classModel.superclass().ifPresent(cb::withSuperclass);
//                                                   cb.withInterfaces(classModel.interfaces());
//                                                   cb.withVersion(classModel.majorVersion(), classModel.minorVersion());
                                                   classModel.forEachElement(cb);
                                               });

        //then verify transformed bytecode
        assertEmpty(Classfile.parse(transformedBytes).verify(null));
    }
}
