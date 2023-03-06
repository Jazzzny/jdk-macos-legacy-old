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
package jdk.internal.classfile.impl;

import java.lang.constant.ConstantDesc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jdk.internal.classfile.Classfile;
import jdk.internal.classfile.Instruction;
import jdk.internal.classfile.constantpool.ClassEntry;
import jdk.internal.classfile.instruction.SwitchCase;
import jdk.internal.classfile.constantpool.FieldRefEntry;
import jdk.internal.classfile.constantpool.InterfaceMethodRefEntry;
import jdk.internal.classfile.constantpool.InvokeDynamicEntry;
import jdk.internal.classfile.constantpool.LoadableConstantEntry;
import jdk.internal.classfile.constantpool.MemberRefEntry;
import jdk.internal.classfile.instruction.ArrayLoadInstruction;
import jdk.internal.classfile.instruction.ArrayStoreInstruction;
import jdk.internal.classfile.instruction.BranchInstruction;
import jdk.internal.classfile.instruction.ConstantInstruction;
import jdk.internal.classfile.instruction.ConvertInstruction;
import jdk.internal.classfile.instruction.FieldInstruction;
import jdk.internal.classfile.instruction.IncrementInstruction;
import jdk.internal.classfile.instruction.InvokeDynamicInstruction;
import jdk.internal.classfile.instruction.InvokeInstruction;
import jdk.internal.classfile.instruction.LoadInstruction;
import jdk.internal.classfile.instruction.LookupSwitchInstruction;
import jdk.internal.classfile.instruction.MonitorInstruction;
import jdk.internal.classfile.instruction.NewMultiArrayInstruction;
import jdk.internal.classfile.instruction.NewObjectInstruction;
import jdk.internal.classfile.instruction.NewPrimitiveArrayInstruction;
import jdk.internal.classfile.instruction.NewReferenceArrayInstruction;
import jdk.internal.classfile.instruction.NopInstruction;
import jdk.internal.classfile.instruction.OperatorInstruction;
import jdk.internal.classfile.instruction.ReturnInstruction;
import jdk.internal.classfile.instruction.StackInstruction;
import jdk.internal.classfile.instruction.StoreInstruction;
import jdk.internal.classfile.instruction.TableSwitchInstruction;
import jdk.internal.classfile.instruction.ThrowInstruction;
import jdk.internal.classfile.instruction.TypeCheckInstruction;
import jdk.internal.classfile.Label;
import jdk.internal.classfile.Opcode;
import jdk.internal.classfile.TypeKind;


/**
 * AbstractInstruction.
 */
public abstract sealed class AbstractInstruction
        extends AbstractElement
        implements Instruction {
    final Opcode op;
    final int size;

    @Override
    public Opcode opcode() {
        return op;
    }

    @Override
    public int sizeInBytes() {
        return size;
    }

    public AbstractInstruction(Opcode op, int size) {
        this.op = op;
        this.size = size;
    }

    @Override
    public abstract void writeTo(DirectCodeBuilder writer);

    public static abstract sealed class BoundInstruction extends AbstractInstruction {
        final CodeImpl code;
        final int pos;

        protected BoundInstruction(Opcode op, int size, CodeImpl code, int pos) {
            super(op, size);
            this.code = code;
            this.pos = pos;
        }

        protected Label offsetToLabel(int offset) {
            return code.getLabel(pos - code.codeStart + offset);
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            // Override this if the instruction has any CP references or labels!
            code.classReader.copyBytesTo(writer.bytecodesBufWriter, pos, size);
        }
    }

    public static final class BoundLoadInstruction
            extends BoundInstruction implements LoadInstruction {

        public BoundLoadInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }


        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }

        @Override
        public String toString() {
            return String.format("Load[OP=%s, slot=%d]", this.opcode(), slot());
        }

        @Override
        public int slot() {
            return switch (size) {
                case 2 -> code.classReader.readU1(pos + 1);
                case 4 -> code.classReader.readU2(pos + 2);
                default -> throw new IllegalArgumentException("Unexpected op size: " + op.sizeIfFixed() + " -- " + op);
            };
        }

    }

    public static final class BoundStoreInstruction
            extends BoundInstruction implements StoreInstruction {

        public BoundStoreInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }

        @Override
        public String toString() {
            return String.format("Store[OP=%s, slot=%d]", this.opcode(), slot());
        }

        @Override
        public int slot() {
            return switch (size) {
                case 2 -> code.classReader.readU1(pos + 1);
                case 4 -> code.classReader.readU2(pos + 2);
                default -> throw new IllegalArgumentException("Unexpected op size: " + size + " -- " + op);
            };
        }

    }

    public static final class BoundIncrementInstruction
            extends BoundInstruction implements IncrementInstruction {

        public BoundIncrementInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public int slot() {
            return size == 6 ? code.classReader.readU2(pos + 2) : code.classReader.readU1(pos + 1);
        }

        @Override
        public int constant() {
            return size == 6 ? code.classReader.readS2(pos + 4) : (byte) code.classReader.readS1(pos + 2);
        }

        @Override
        public String toString() {
            return String.format("Inc[OP=%s, slot=%d, val=%d]", this.opcode(), slot(), constant());
        }

    }

    public static final class BoundBranchInstruction
            extends BoundInstruction implements BranchInstruction {

        public BoundBranchInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public Label target() {
            return offsetToLabel(branchByteOffset());
        }

        public int branchByteOffset() {
            return size == 3
                   ? (int) (short) code.classReader.readU2(pos + 1)
                   : code.classReader.readInt(pos + 1);
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeBranch(opcode(), target());
        }

        @Override
        public String toString() {
            return String.format("Branch[OP=%s]", this.opcode());
        }

    }

    public record SwitchCaseImpl(int caseValue, Label target)
            implements SwitchCase {
    }

    public static final class BoundLookupSwitchInstruction
            extends BoundInstruction implements LookupSwitchInstruction {

        // will always need size, cache everything to there
        private final int afterPad;
        private final int npairs;

        BoundLookupSwitchInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, size(code, code.codeStart, pos), code, pos);

            this.afterPad = pos + 1 + ((4 - ((pos + 1 - code.codeStart) & 3)) & 3);
            this.npairs = code.classReader.readInt(afterPad + 4);
        }

        static int size(CodeImpl code, int codeStart, int pos) {
            int afterPad = pos + 1 + ((4 - ((pos + 1 - codeStart) & 3)) & 3);
            int pad = afterPad - (pos + 1);
            int npairs = code.classReader.readInt(afterPad + 4);
            return 1 + pad + 8 + npairs * 8;
        }

        private int defaultOffset() {
            return code.classReader.readInt(afterPad);
        }

        @Override
        public List<SwitchCase> cases() {
            var cases = new SwitchCase[npairs];
            for (int i = 0; i < npairs; ++i) {
                int z = afterPad + 8 + 8 * i;
                cases[i] = SwitchCase.of(code.classReader.readInt(z), offsetToLabel(code.classReader.readInt(z + 4)));
            }
            return List.of(cases);
        }

        @Override
        public Label defaultTarget() {
            return offsetToLabel(defaultOffset());
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeLookupSwitch(defaultTarget(), cases());
        }

        @Override
        public String toString() {
            return String.format("LookupSwitch[OP=%s]", this.opcode());
        }

    }

    public static final class BoundTableSwitchInstruction
            extends BoundInstruction implements TableSwitchInstruction {

        BoundTableSwitchInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, size(code, code.codeStart, pos), code, pos);
        }

        static int size(CodeImpl code, int codeStart, int pos) {
            int ap = pos + 1 + ((4 - ((pos + 1 - codeStart) & 3)) & 3);
            int pad = ap - (pos + 1);
            int low = code.classReader.readInt(ap + 4);
            int high = code.classReader.readInt(ap + 8);
            int cnt = high - low + 1;
            return 1 + pad + 12 + cnt * 4;
        }

        private int afterPadding() {
            int p = pos;
            return p + 1 + ((4 - ((p + 1 - code.codeStart) & 3)) & 3);
        }

        @Override
        public Label defaultTarget() {
            return offsetToLabel(defaultOffset());
        }

        @Override
        public int lowValue() {
            return code.classReader.readInt(afterPadding() + 4);
        }

        @Override
        public int highValue() {
            return code.classReader.readInt(afterPadding() + 8);
        }

        @Override
        public List<SwitchCase> cases() {
            int low = lowValue();
            int high = highValue();
            int defOff = defaultOffset();
            var cases = new ArrayList<SwitchCase>(high - low + 1);
            int z = afterPadding() + 12;
            for (int i = lowValue(); i <= high; ++i) {
                int off = code.classReader.readInt(z);
                if (defOff != off) {
                    cases.add(SwitchCase.of(i, offsetToLabel(off)));
                }
                z += 4;
            }
            return Collections.unmodifiableList(cases);
        }

        private int defaultOffset() {
            return code.classReader.readInt(afterPadding());
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeTableSwitch(lowValue(), highValue(), defaultTarget(), cases());
        }

        @Override
        public String toString() {
            return String.format("TableSwitch[OP=%s]", this.opcode());
        }

    }

    public static final class BoundFieldInstruction
            extends BoundInstruction implements FieldInstruction {

        private FieldRefEntry fieldEntry;

        public BoundFieldInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public FieldRefEntry field() {
            if (fieldEntry == null)
                fieldEntry = (FieldRefEntry) code.classReader.readEntry(pos + 1);
            return fieldEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeFieldAccess(op, field());
        }

        @Override
        public String toString() {
            return String.format("Field[OP=%s, field=%s.%s:%s]", this.opcode(), owner().asInternalName(), name().stringValue(), type().stringValue());
        }

    }

    public static final class BoundInvokeInstruction
            extends BoundInstruction implements InvokeInstruction {
        MemberRefEntry methodEntry;

        public BoundInvokeInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public MemberRefEntry method() {
            if (methodEntry == null)
                methodEntry = (MemberRefEntry) code.classReader.readEntry(pos + 1);
            return methodEntry;
        }

        @Override
        public boolean isInterface() {
            return method().tag() == Classfile.TAG_INTERFACEMETHODREF;
        }

        @Override
        public int count() {
            return Util.parameterSlots(type().stringValue());
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeInvokeNormal(op, method());
        }

        @Override
        public String toString() {
            return String.format("Invoke[OP=%s, m=%s.%s%s]", this.opcode(), owner().asInternalName(), name().stringValue(), type().stringValue());
        }

    }

    public static final class BoundInvokeInterfaceInstruction
            extends BoundInstruction implements InvokeInstruction {
        InterfaceMethodRefEntry methodEntry;

        public BoundInvokeInterfaceInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public MemberRefEntry method() {
            if (methodEntry == null)
                methodEntry = (InterfaceMethodRefEntry) code.classReader.readEntry(pos + 1);
            return methodEntry;
        }

        @Override
        public int count() {
            return code.classReader.readU1(pos + 3);
        }

        @Override
        public boolean isInterface() {
            return true;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeInvokeInterface(op, (InterfaceMethodRefEntry) method(), count());
        }

        @Override
        public String toString() {
            return String.format("InvokeInterface[OP=%s, m=%s.%s%s]", this.opcode(), owner().asInternalName(), name().stringValue(), type().stringValue());
        }

    }

    public static final class BoundInvokeDynamicInstruction
            extends BoundInstruction implements InvokeDynamicInstruction {
        InvokeDynamicEntry indyEntry;

        BoundInvokeDynamicInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public InvokeDynamicEntry invokedynamic() {
            if (indyEntry == null)
                indyEntry = (InvokeDynamicEntry) code.classReader.readEntry(pos + 1);
            return indyEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeInvokeDynamic(invokedynamic());
        }

        @Override
        public String toString() {
            return String.format("InvokeDynamic[OP=%s, bsm=%s %s]", this.opcode(), bootstrapMethod(), bootstrapArgs());
        }

    }

    public static final class BoundNewObjectInstruction
            extends BoundInstruction implements NewObjectInstruction {
        ClassEntry classEntry;

        BoundNewObjectInstruction(CodeImpl code, int pos) {
            super(Opcode.NEW, Opcode.NEW.sizeIfFixed(), code, pos);
        }

        @Override
        public ClassEntry className() {
            if (classEntry == null)
                classEntry = code.classReader.readClassEntry(pos + 1);
            return classEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeNewObject(className());
        }

        @Override
        public String toString() {
            return String.format("NewObj[OP=%s, type=%s]", this.opcode(), className().asInternalName());
        }

    }

    public static final class BoundNewPrimitiveArrayInstruction
            extends BoundInstruction implements NewPrimitiveArrayInstruction {

        public BoundNewPrimitiveArrayInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public TypeKind typeKind() {
            return TypeKind.fromNewArrayCode(code.classReader.readU1(pos + 1));
        }

        @Override
        public String toString() {
            return String.format("NewPrimitiveArray[OP=%s, type=%s]", this.opcode(), typeKind());
        }

    }

    public static final class BoundNewReferenceArrayInstruction
            extends BoundInstruction implements NewReferenceArrayInstruction {

        public BoundNewReferenceArrayInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public ClassEntry componentType() {
            return code.classReader.readClassEntry(pos + 1);
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeNewReferenceArray(componentType());
        }

        @Override
        public String toString() {
            return String.format("NewRefArray[OP=%s, type=%s]", this.opcode(), componentType().asInternalName());
        }
    }

    public static final class BoundNewMultidimensionalArrayInstruction
            extends BoundInstruction implements NewMultiArrayInstruction {

        public BoundNewMultidimensionalArrayInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public int dimensions() {
            return code.classReader.readU1(pos + 3);
        }

        @Override
        public ClassEntry arrayType() {
            return code.classReader.readClassEntry(pos + 1);
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeNewMultidimensionalArray(dimensions(), arrayType());
        }

        @Override
        public String toString() {
            return String.format("NewMultiArray[OP=%s, type=%s[%d]]", this.opcode(), arrayType().asInternalName(), dimensions());
        }

    }

    public static final class BoundTypeCheckInstruction
            extends BoundInstruction implements TypeCheckInstruction {
        ClassEntry typeEntry;

        public BoundTypeCheckInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public ClassEntry type() {
            if (typeEntry == null)
                typeEntry = code.classReader.readClassEntry(pos + 1);
            return typeEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeTypeCheck(op, type());
        }

        @Override
        public String toString() {
            return String.format("TypeCheck[OP=%s, type=%s]", this.opcode(), type().asInternalName());
        }

    }

    public static final class BoundArgumentConstantInstruction
            extends BoundInstruction implements ConstantInstruction.ArgumentConstantInstruction {

        public BoundArgumentConstantInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public Integer constantValue() {
            return constantInt();
        }

        public int constantInt() {
            return size == 3 ? code.classReader.readS2(pos + 1) : code.classReader.readS1(pos + 1);
        }

        @Override
        public String toString() {
            return String.format("ArgumentConstant[OP=%s, val=%s]", this.opcode(), constantValue());
        }

    }

    public static final class BoundLoadConstantInstruction
            extends BoundInstruction implements ConstantInstruction.LoadConstantInstruction {

        public BoundLoadConstantInstruction(Opcode op, CodeImpl code, int pos) {
            super(op, op.sizeIfFixed(), code, pos);
        }

        @Override
        public LoadableConstantEntry constantEntry() {
            return (LoadableConstantEntry)
                    code.classReader.entryByIndex(op == Opcode.LDC
                                                  ? code.classReader.readU1(pos + 1)
                                                  : code.classReader.readU2(pos + 1));
        }

        @Override
        public ConstantDesc constantValue() {
            return constantEntry().constantValue();
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (writer.canWriteDirect(code.constantPool()))
                super.writeTo(writer);
            else
                writer.writeLoadConstant(op, constantEntry());
        }

        @Override
        public String toString() {
            return String.format("LoadConstant[OP=%s, val=%s]", this.opcode(), constantValue());
        }

    }

    public static abstract sealed class UnboundInstruction extends AbstractInstruction {

        UnboundInstruction(Opcode op) {
            super(op, op.sizeIfFixed());
        }

        @Override
        // Override this if there is anything more that just the bytecode
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeBytecode(op);
        }

        @Override
        public String toString() {
            return String.format("%s[op=%s]", this.getClass().getSimpleName(), op);
        }
    }

    public static final class UnboundLoadInstruction
            extends UnboundInstruction implements LoadInstruction {
        final int slot;

        public UnboundLoadInstruction(Opcode op, int slot) {
            super(op);
            this.slot = slot;
        }

        @Override
        public int slot() {
            return slot;
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeLoad(op, slot);
        }

        @Override
        public String toString() {
            return String.format("Load[OP=%s, slot=%d]", this.opcode(), slot());
        }

    }

    public static final class UnboundStoreInstruction
            extends UnboundInstruction implements StoreInstruction {
        final int slot;

        public UnboundStoreInstruction(Opcode op, int slot) {
            super(op);
            this.slot = slot;
        }

        @Override
        public int slot() {
            return slot;
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeStore(op, slot);
        }

        @Override
        public String toString() {
            return String.format("Store[OP=%s, slot=%d]", this.opcode(), slot());
        }

    }

    public static final class UnboundIncrementInstruction
            extends UnboundInstruction implements IncrementInstruction {
        final int slot;
        final int constant;

        public UnboundIncrementInstruction(int slot, int constant) {
            super(slot <= 255 && constant < 128 && constant > -127
                  ? Opcode.IINC
                  : Opcode.IINC_W);
            this.slot = slot;
            this.constant = constant;
        }

        @Override
        public int slot() {
            return slot;
        }

        @Override
        public int constant() {
            return 0;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeIncrement(slot, constant);
        }

        @Override
        public String toString() {
            return String.format("Increment[OP=%s, slot=%d, constant=%d]", this.opcode(), slot(), constant());
        }
    }

    public static final class UnboundBranchInstruction
            extends UnboundInstruction implements BranchInstruction {
        final Label target;

        public UnboundBranchInstruction(Opcode op, Label target) {
            super(op);
            this.target = target;
        }

        @Override
        public Label target() {
            return target;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeBranch(op, target);
        }

        @Override
        public String toString() {
            return String.format("Branch[OP=%s]", this.opcode());
        }
    }

    public static final class UnboundLookupSwitchInstruction
            extends UnboundInstruction implements LookupSwitchInstruction {

        private final Label defaultTarget;
        private final List<SwitchCase> cases;

        public UnboundLookupSwitchInstruction(Label defaultTarget, List<SwitchCase> cases) {
            super(Opcode.LOOKUPSWITCH);
            this.defaultTarget = defaultTarget;
            this.cases = List.copyOf(cases);
        }

        @Override
        public List<SwitchCase> cases() {
            return cases;
        }

        @Override
        public Label defaultTarget() {
            return defaultTarget;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeLookupSwitch(defaultTarget, cases);
        }

        @Override
        public String toString() {
            return String.format("LookupSwitch[OP=%s]", this.opcode());
        }
    }

    public static final class UnboundTableSwitchInstruction
            extends UnboundInstruction implements TableSwitchInstruction {

        private final int lowValue, highValue;
        private final Label defaultTarget;
        private final List<SwitchCase> cases;

        public UnboundTableSwitchInstruction(int lowValue, int highValue, Label defaultTarget, List<SwitchCase> cases) {
            super(Opcode.TABLESWITCH);
            this.lowValue = lowValue;
            this.highValue = highValue;
            this.defaultTarget = defaultTarget;
            this.cases = List.copyOf(cases);
        }

        @Override
        public int lowValue() {
            return lowValue;
        }

        @Override
        public int highValue() {
            return highValue;
        }

        @Override
        public Label defaultTarget() {
            return defaultTarget;
        }

        @Override
        public List<SwitchCase> cases() {
            return cases;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeTableSwitch(lowValue, highValue, defaultTarget, cases);
        }

        @Override
        public String toString() {
            return String.format("LookupSwitch[OP=%s]", this.opcode());
        }
    }

    public static final class UnboundReturnInstruction
            extends UnboundInstruction implements ReturnInstruction {

        public UnboundReturnInstruction(Opcode op) {
            super(op);
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }

        @Override
        public String toString() {
            return String.format("Return[OP=%s]", this.opcode());
        }

    }

    public static final class UnboundThrowInstruction
            extends UnboundInstruction implements ThrowInstruction {

        public UnboundThrowInstruction() {
            super(Opcode.ATHROW);
        }

        @Override
        public String toString() {
            return String.format("Throw[OP=%s]", this.opcode());
        }

    }

    public static final class UnboundFieldInstruction
            extends UnboundInstruction implements FieldInstruction {
        final FieldRefEntry fieldEntry;

        public UnboundFieldInstruction(Opcode op,
                                       FieldRefEntry fieldEntry) {
            super(op);
            this.fieldEntry = fieldEntry;
        }

        @Override
        public FieldRefEntry field() {
            return fieldEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeFieldAccess(op, fieldEntry);
        }

        @Override
        public String toString() {
            return String.format("FieldAccess[OP=%s, field=%s.%s:%s]", this.opcode(), this.owner().asInternalName(), this.name().stringValue(), this.type().stringValue());
        }
    }

    public static final class UnboundInvokeInstruction
            extends UnboundInstruction implements InvokeInstruction {
        final MemberRefEntry methodEntry;

        public UnboundInvokeInstruction(Opcode op, MemberRefEntry methodEntry) {
            super(op);
            this.methodEntry = methodEntry;
        }

        @Override
        public MemberRefEntry method() {
            return methodEntry;
        }

        @Override
        public boolean isInterface() {
            return op == Opcode.INVOKEINTERFACE || methodEntry instanceof InterfaceMethodRefEntry;
        }

        @Override
        public int count() {
            return op == Opcode.INVOKEINTERFACE
                   ? Util.parameterSlots(methodEntry.nameAndType().type().stringValue()) + 1
                   : 0;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            if (op == Opcode.INVOKEINTERFACE)
                writer.writeInvokeInterface(op, (InterfaceMethodRefEntry) method(), count());
            else
                writer.writeInvokeNormal(op, method());
        }

        @Override
        public String toString() {
            return String.format("Invoke[OP=%s, m=%s.%s%s]", this.opcode(), owner().asInternalName(), name().stringValue(), type().stringValue());
        }
    }

    public static final class UnboundInvokeDynamicInstruction
            extends UnboundInstruction implements InvokeDynamicInstruction {
        final InvokeDynamicEntry indyEntry;

        public UnboundInvokeDynamicInstruction(InvokeDynamicEntry indyEntry) {
            super(Opcode.INVOKEDYNAMIC);
            this.indyEntry = indyEntry;
        }

        @Override
        public InvokeDynamicEntry invokedynamic() {
            return indyEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeInvokeDynamic(invokedynamic());
        }

        @Override
        public String toString() {
            return String.format("InvokeDynamic[OP=%s, bsm=%s %s]", this.opcode(), bootstrapMethod(), bootstrapArgs());
        }
    }

    public static final class UnboundNewObjectInstruction
            extends UnboundInstruction implements NewObjectInstruction {
        final ClassEntry classEntry;

        public UnboundNewObjectInstruction(ClassEntry classEntry) {
            super(Opcode.NEW);
            this.classEntry = classEntry;
        }

        @Override
        public ClassEntry className() {
            return classEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeNewObject(className());
        }

        @Override
        public String toString() {
            return String.format("NewObj[OP=%s, type=%s]", this.opcode(), className().asInternalName());
        }
    }

    public static final class UnboundNewPrimitiveArrayInstruction
            extends UnboundInstruction implements NewPrimitiveArrayInstruction {
        final TypeKind typeKind;

        public UnboundNewPrimitiveArrayInstruction(TypeKind typeKind) {
            super(Opcode.NEWARRAY);
            this.typeKind = typeKind;
        }

        @Override
        public TypeKind typeKind() {
            return typeKind;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeNewPrimitiveArray(typeKind.newarraycode());
        }

        @Override
        public String toString() {
            return String.format("NewPrimitiveArray[OP=%s, type=%s]", this.opcode(), typeKind());
        }
    }

    public static final class UnboundNewReferenceArrayInstruction
            extends UnboundInstruction implements NewReferenceArrayInstruction {
        final ClassEntry componentTypeEntry;

        public UnboundNewReferenceArrayInstruction(ClassEntry componentTypeEntry) {
            super(Opcode.ANEWARRAY);
            this.componentTypeEntry = componentTypeEntry;
        }

        @Override
        public ClassEntry componentType() {
            return componentTypeEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeNewReferenceArray(componentType());
        }

        @Override
        public String toString() {
            return String.format("NewRefArray[OP=%s, type=%s]", this.opcode(), componentType().asInternalName());
        }
    }

    public static final class UnboundNewMultidimensionalArrayInstruction
            extends UnboundInstruction implements NewMultiArrayInstruction {
        final ClassEntry arrayTypeEntry;
        final int dimensions;

        public UnboundNewMultidimensionalArrayInstruction(ClassEntry arrayTypeEntry,
                                                          int dimensions) {
            super(Opcode.MULTIANEWARRAY);
            this.arrayTypeEntry = arrayTypeEntry;
            this.dimensions = dimensions;
        }

        @Override
        public int dimensions() {
            return dimensions;
        }

        @Override
        public ClassEntry arrayType() {
            return arrayTypeEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeNewMultidimensionalArray(dimensions(), arrayType());
        }

        @Override
        public String toString() {
            return String.format("NewMultiArray[OP=%s, type=%s[%d]]", this.opcode(), arrayType().asInternalName(), dimensions());
        }

    }

    public static final class UnboundArrayLoadInstruction
            extends UnboundInstruction implements ArrayLoadInstruction {

        public UnboundArrayLoadInstruction(Opcode op) {
            super(op);
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }
    }

    public static final class UnboundArrayStoreInstruction
            extends UnboundInstruction implements ArrayStoreInstruction {

        public UnboundArrayStoreInstruction(Opcode op) {
            super(op);
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }
    }

    public static final class UnboundTypeCheckInstruction
            extends UnboundInstruction implements TypeCheckInstruction {
        final ClassEntry typeEntry;

        public UnboundTypeCheckInstruction(Opcode op, ClassEntry typeEntry) {
            super(op);
            this.typeEntry = typeEntry;
        }

        @Override
        public ClassEntry type() {
            return typeEntry;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeTypeCheck(op, type());
        }

        @Override
        public String toString() {
            return String.format("TypeCheck[OP=%s, type=%s]", this.opcode(), type().asInternalName());
        }
    }

    public static final class UnboundStackInstruction
            extends UnboundInstruction implements StackInstruction {

        public UnboundStackInstruction(Opcode op) {
            super(op);
        }

    }

    public static final class UnboundConvertInstruction
            extends UnboundInstruction implements ConvertInstruction {

        public UnboundConvertInstruction(Opcode op) {
            super(op);
        }

        @Override
        public TypeKind fromType() {
            return op.primaryTypeKind();
        }

        @Override
        public TypeKind toType() {
            return op.secondaryTypeKind();
        }
    }

    public static final class UnboundOperatorInstruction
            extends UnboundInstruction implements OperatorInstruction {

        public UnboundOperatorInstruction(Opcode op) {
            super(op);
        }

        @Override
        public TypeKind typeKind() {
            return op.primaryTypeKind();
        }
    }

    public static final class UnboundIntrinsicConstantInstruction
            extends UnboundInstruction implements ConstantInstruction.IntrinsicConstantInstruction {
        final ConstantDesc constant;

        public UnboundIntrinsicConstantInstruction(Opcode op) {
            super(op);
            constant = op.constantValue();
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            super.writeTo(writer);
        }

        @Override
        public ConstantDesc constantValue() {
            return constant;
        }
    }

    public static final class UnboundArgumentConstantInstruction
            extends UnboundInstruction implements ConstantInstruction.ArgumentConstantInstruction {
        final int value;

        public UnboundArgumentConstantInstruction(Opcode op, int value) {
            super(op);
            this.value = value;
        }

        @Override
        public Integer constantValue() {
            return value;
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeArgumentConstant(op, value);
        }

        @Override
        public String toString() {
            return String.format("ArgumentConstant[OP=%s, val=%s]", this.opcode(), constantValue());
        }
    }

    public static final class UnboundLoadConstantInstruction
            extends UnboundInstruction implements ConstantInstruction.LoadConstantInstruction {
        final LoadableConstantEntry constant;

        public UnboundLoadConstantInstruction(Opcode op, LoadableConstantEntry constant) {
            super(op);
            this.constant = constant;
        }

        @Override
        public LoadableConstantEntry constantEntry() {
            return constant;
        }

        @Override
        public ConstantDesc constantValue() {
            return constant.constantValue();
        }

        @Override
        public void writeTo(DirectCodeBuilder writer) {
            writer.writeLoadConstant(op, constant);
        }

        @Override
        public String toString() {
            return String.format("LoadConstant[OP=%s, val=%s]", this.opcode(), constantValue());
        }
    }

    public static final class UnboundMonitorInstruction
            extends UnboundInstruction implements MonitorInstruction {

        public UnboundMonitorInstruction(Opcode op) {
            super(op);
        }

    }

    public static final class UnboundNopInstruction
            extends UnboundInstruction implements NopInstruction {

        public UnboundNopInstruction() {
            super(Opcode.NOP);
        }

    }
}
