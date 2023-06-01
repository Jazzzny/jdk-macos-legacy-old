/*
 * Copyright (c) 2018, 2023, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test id=OCSP
 * @bug 8195774 8243321
 * @summary Interoperability tests with Entrust CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath EntrustCA OCSP
 */

/*
 * @test id=CRL
 * @bug 8195774 8243321
 * @summary Interoperability tests with Entrust CAs
 * @library /test/lib
 * @build jtreg.SkippedException ValidatePathWithURL CAInterop
 * @run main/othervm -Djava.security.debug=certpath EntrustCA CRL
 */

public class EntrustCA {

    public static void main(String[] args) throws Exception {

        CAInterop caInterop = new CAInterop(args[0]);

        // CN=Entrust Root Certification Authority - EC1,
        // OU="(c) 2012 Entrust, Inc. - for authorized use only",
        // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
        caInterop.validate("entrustrootcaec1 [jdk]",
                "https://validec.entrust.net",
                "https://revokedec.entrust.net");

        // CN=Entrust Root Certification Authority - G4,
        // OU="(c) 2015 Entrust, Inc. - for authorized use only",
        // OU=See www.entrust.net/legal-terms, O="Entrust, Inc.", C=US
        caInterop.validate("entrustrootcag4 [jdk]",
                "https://validg4.entrust.net",
                "https://revokedg4.entrust.net");
    }
}
