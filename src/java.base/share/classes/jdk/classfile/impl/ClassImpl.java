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

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import jdk.classfile.ClassBuilder;
import jdk.classfile.constantpool.ClassEntry;
import jdk.classfile.jdktypes.AccessFlag;
import jdk.classfile.AccessFlags;
import jdk.classfile.Attribute;
import jdk.classfile.AttributeMapper;
import jdk.classfile.Attributes;
import jdk.classfile.ClassElement;
import jdk.classfile.ClassModel;
import jdk.classfile.ClassReader;
import jdk.classfile.ClassTransform;
import jdk.classfile.Classfile;
import jdk.classfile.ClassfileVersion;
import jdk.classfile.constantpool.ConstantPool;
import jdk.classfile.constantpool.ConstantPoolBuilder;
import jdk.classfile.FieldModel;
import jdk.classfile.Interfaces;
import jdk.classfile.MethodModel;
import jdk.classfile.Superclass;

public final class ClassImpl
        extends AbstractElement
        implements ClassModel {

    final ClassReader reader;
    private final int attributesPos;
    private final List<MethodModel> methods;
    private final List<FieldModel> fields;
    private List<Attribute<?>> attributes;
    private List<ClassEntry> interfaces;

    public ClassImpl(byte[] cfbytes,
                     Collection<Classfile.Option<?>> options) {
        this.reader = new ClassReaderImpl(cfbytes, options);
        ClassReaderImpl reader = (ClassReaderImpl) this.reader;
        int p = reader.interfacesPos;
        int icnt = reader.readU2(p);
        p += 2 + icnt * 2;
        int fcnt = reader.readU2(p);
        FieldImpl[] fields = new FieldImpl[fcnt];
        p += 2;
        for (int i = 0; i < fcnt; ++i) {
            int startPos = p;
            int attrStart = p + 6;
            p = reader.skipAttributeHolder(attrStart);
            fields[i] = new FieldImpl(reader, startPos, p, attrStart);
        }
        this.fields = List.of(fields);
        int mcnt = reader.readU2(p);
        MethodImpl[] methods = new MethodImpl[mcnt];
        p += 2;
        for (int i = 0; i < mcnt; ++i) {
            int startPos = p;
            int attrStart = p + 6;
            p = reader.skipAttributeHolder(attrStart);
            methods[i] = new MethodImpl(reader, startPos, p, attrStart);
        }
        this.methods = List.of(methods);
        this.attributesPos = p;
        reader.setContainedClass(this);
    }

    @Override
    public AccessFlags flags() {
        return AccessFlags.ofClass(reader.flags());
    }

    @Override
    public int majorVersion() {
        return reader.readU2(6);
    }

    @Override
    public int minorVersion() {
        return reader.readU2(4);
    }

    @Override
    public ConstantPool constantPool() {
        return reader;
    }

    @Override
    public ClassEntry thisClass() {
        return reader.thisClassEntry();
    }

    @Override
    public Optional<ClassEntry> superclass() {
        return reader.superclassEntry();
    }

    @Override
    public List<ClassEntry> interfaces() {
        if (interfaces == null) {
            // @@@ Could use JavaUtilCollectionAccess.listFromTrustedArrayNullsAllowed to avoid copy
            int pos = reader.thisClassPos() + 4;
            int cnt = reader.readU2(pos);
            pos += 2;
            ClassEntry[] arr = new ClassEntry[cnt];
            for (int i = 0; i < cnt; ++i) {
                arr[i] = reader.readClassEntry(pos);
                pos += 2;
            }
            this.interfaces = List.of(arr);
        }
        return interfaces;
    }

    @Override
    public List<Attribute<?>> attributes() {
        if (attributes == null) {
            @SuppressWarnings("unchecked")
            var res = (List<Attribute<?>>) BoundAttribute.readAttributes(this, reader, attributesPos, reader.customAttributes());
            attributes = res;
        }
        return attributes;
    }

    // ClassModel

    @Override
    public Kind attributedElementKind() {
        return Kind.CLASS;
    }

    @Override
    public void forEachElement(Consumer<ClassElement> consumer) {
        consumer.accept(flags());
        consumer.accept(ClassfileVersion.of(majorVersion(), minorVersion()));
        superclass().ifPresent(new Consumer<ClassEntry>() {
            @Override
            public void accept(ClassEntry entry) {
                consumer.accept(Superclass.of(entry));
            }
        });
        consumer.accept(Interfaces.of(interfaces()));
        fields().forEach(consumer);
        methods().forEach(consumer);
        for (Attribute<?> attr : attributes()) {
            if (attr instanceof ClassElement e)
                consumer.accept(e);
        }
    }

    @Override
    public byte[] transform(ClassTransform transform) {
        ConstantPoolBuilder constantPool = ConstantPoolBuilder.of(this);
        return Classfile.build(thisClass(), constantPool,
                               new Consumer<ClassBuilder>() {
                                   @Override
                                   public void accept(ClassBuilder builder) {
                                       ((DirectClassBuilder) builder).setOriginal(ClassImpl.this);
                                       ((DirectClassBuilder) builder).setSizeHint(reader.classfileLength());
                                       builder.transform(ClassImpl.this, transform);
                                   }
                               });
    }

    @Override
    public List<FieldModel> fields() {
        return fields;
    }

    @Override
    public List<MethodModel> methods() {
        return methods;
    }

    @Override
    public boolean isModuleInfo() {
        AccessFlags flags = flags();
        // move to where?
        return flags.has(AccessFlag.MODULE)
               && majorVersion() >= Classfile.JAVA_9_VERSION
               && thisClass().asInternalName().equals("module-info")
               && (superclass().isEmpty())
               && interfaces().isEmpty()
               && fields().isEmpty()
               && methods().isEmpty()
               && verifyModuleAttributes();
    }

    private boolean verifyModuleAttributes() {
        if (findAttribute(Attributes.MODULE).isEmpty())
            return false;

        Set<AttributeMapper<?>> found = attributes().stream()
                                                    .map(Attribute::attributeMapper)
                                                    .collect(Collectors.toSet());

        found.removeAll(allowedModuleAttributes);
        found.retainAll(Attributes.PREDEFINED_ATTRIBUTES.values());
        return found.isEmpty();
    }

    private static final Set<AttributeMapper<?>> allowedModuleAttributes
            = Set.of(Attributes.MODULE,
                     Attributes.MODULE_PACKAGES,
                     Attributes.MODULE_MAIN_CLASS,
                     Attributes.INNER_CLASSES,
                     Attributes.SOURCE_FILE,
                     Attributes.SOURCE_DEBUG_EXTENSION,
                     Attributes.RUNTIME_VISIBLE_ANNOTATIONS,
                     Attributes.RUNTIME_INVISIBLE_ANNOTATIONS);
}
