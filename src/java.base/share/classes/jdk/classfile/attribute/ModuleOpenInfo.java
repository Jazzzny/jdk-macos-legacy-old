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

import java.util.Collection;
import java.util.List;
import java.util.Set;

import jdk.classfile.constantpool.ModuleEntry;
import jdk.classfile.constantpool.PackageEntry;
import jdk.classfile.jdktypes.AccessFlag;

import jdk.classfile.impl.UnboundAttribute;
import jdk.classfile.impl.Util;

/**
 * Models a single "opens" declaration in the {@link jdk.classfile.attribute.ModuleAttribute}.
 */
public sealed interface ModuleOpenInfo
        permits UnboundAttribute.UnboundModuleOpenInfo {

    /**
     * {@return the package being opened}
     */
    PackageEntry openedPackage();

    /**
     * @@@ opens_flags
     */
    int opensFlagsMask();

    default Set<AccessFlag> opensFlags() {
        return AccessFlag.maskToAccessFlags(opensFlagsMask(), AccessFlag.Location.MODULE_OPENS);
    }

    /**
     * {@return whether the specified access flag is set}
     * @param flag the access flag
     */
    default boolean has(AccessFlag flag) {
        return Util.has(AccessFlag.Location.MODULE_OPENS, opensFlagsMask(), flag);
    }

    /**
     * The list of modules to which this package is opened, if it is a
     * qualified open.
     *
     * @return the modules to which this package is opened
     */
    List<ModuleEntry> opensTo();

    /**
     * {@return a module open description}
     * @param opens the package to open
     * @param opensFlags the open flags
     * @param opensTo the packages to which this package is opened, if it is a qualified open
     */
    static ModuleOpenInfo of(PackageEntry opens, int opensFlags,
                             List<ModuleEntry> opensTo) {
        return new UnboundAttribute.UnboundModuleOpenInfo(opens, opensFlags, opensTo);
    }

    /**
     * {@return a module open description}
     * @param opens the package to open
     * @param opensFlags the open flags
     * @param opensTo the packages to which this package is opened, if it is a qualified open
     */
    static ModuleOpenInfo of(PackageEntry opens, Collection<AccessFlag> opensFlags,
                             List<ModuleEntry> opensTo) {
        return of(opens, Util.flagsToBits(AccessFlag.Location.MODULE_OPENS, opensFlags), opensTo);
    }

    /**
     * {@return a module open description}
     * @param opens the package to open
     * @param opensFlags the open flags
     * @param opensTo the packages to which this package is opened, if it is a qualified open
     */
    static ModuleOpenInfo of(PackageEntry opens,
                             int opensFlags,
                             ModuleEntry... opensTo) {
        return of(opens, opensFlags, List.of(opensTo));
    }

    /**
     * {@return a module open description}
     * @param opens the package to open
     * @param opensFlags the open flags
     * @param opensTo the packages to which this package is opened, if it is a qualified open
     */
    static ModuleOpenInfo of(PackageEntry opens,
                             Collection<AccessFlag> opensFlags,
                             ModuleEntry... opensTo) {
        return of(opens, Util.flagsToBits(AccessFlag.Location.MODULE_OPENS, opensFlags), opensTo);
    }
}
