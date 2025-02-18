package de.nick1st.imm_ptl.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import java.util.stream.Collectors;

public class OpCodeStringHelper {

    private OpCodeStringHelper() {}
    
    public static String stringifyMethod(MethodNode method) {
        return method.name + "(" + method.desc + ")\n" + stringifyInstructions(method.instructions);
    }
    
    public static String stringifyInstructions(Iterable<AbstractInsnNode> instructions) {
        StringBuilder builder = new StringBuilder();
        for (AbstractInsnNode node : instructions) {
            String s = switch (node.getType()) {
                case AbstractInsnNode.INSN -> "\t" + stringifyInsn((InsnNode) node);
                case AbstractInsnNode.INT_INSN -> "\t" + stringifyIntInsn((IntInsnNode) node);
                case AbstractInsnNode.VAR_INSN -> "\t" + stringifyVarInsn((VarInsnNode) node);
                case AbstractInsnNode.TYPE_INSN -> "\t" + stringifyTypeInsn((TypeInsnNode) node);
                case AbstractInsnNode.FIELD_INSN -> "\t" + stringifyFieldInsn((FieldInsnNode) node);
                case AbstractInsnNode.METHOD_INSN -> "\t" + stringifyMethodInsn((MethodInsnNode) node);
                case AbstractInsnNode.INVOKE_DYNAMIC_INSN ->
                        "\t" + stringifyInvokeDynamicInsn((InvokeDynamicInsnNode) node);
                case AbstractInsnNode.JUMP_INSN -> "\t" + stringifyJumpInsn((JumpInsnNode) node);
                case AbstractInsnNode.LABEL -> stringifyLabelInsn((LabelNode) node);
                case AbstractInsnNode.LDC_INSN -> "\t" + stringifyLdcInsn((LdcInsnNode) node);
                case AbstractInsnNode.IINC_INSN -> "\t" + stringifyIincInsn((IincInsnNode) node);
                case AbstractInsnNode.TABLESWITCH_INSN -> "\t" + stringifyTableSwitchInsn((TableSwitchInsnNode) node);
                case AbstractInsnNode.LOOKUPSWITCH_INSN ->
                        "\t" + stringifyLookupSwitchInsn((LookupSwitchInsnNode) node);
                case AbstractInsnNode.MULTIANEWARRAY_INSN ->
                        "\t" + stringifyMultiANewArrayInsn((MultiANewArrayInsnNode) node);
                case AbstractInsnNode.FRAME -> stringifyFrameInsn((FrameNode) node);
                case AbstractInsnNode.LINE -> stringifyLineNumberInsn((LineNumberNode) node);
                default -> "UNKNOWN OPCODE TYPE: " + node.getType();
            };
            builder.append(s);
            builder.append("\n");
        }
        return builder.toString();
    }
    
    private static String stringifyInsn(InsnNode node) {
        return switch (node.getOpcode()) {
            case Opcodes.NOP -> "NOP";
            case Opcodes.ACONST_NULL -> "ACONST_NULL";
            case Opcodes.ICONST_M1 -> "ICONST_M1";
            case Opcodes.ICONST_0 -> "ICONST_0";
            case Opcodes.ICONST_1 -> "ICONST_1";
            case Opcodes.ICONST_2 -> "ICONST_2";
            case Opcodes.ICONST_3 -> "ICONST_3";
            case Opcodes.ICONST_4 -> "ICONST_4";
            case Opcodes.ICONST_5 -> "ICONST_5";
            case Opcodes.LCONST_0 -> "LCONST_0";
            case Opcodes.LCONST_1 -> "LCONST_1";
            case Opcodes.FCONST_0 -> "FCONST_0";
            case Opcodes.FCONST_1 -> "FCONST_1";
            case Opcodes.FCONST_2 -> "FCONST_2";
            case Opcodes.DCONST_0 -> "DCONST_0";
            case Opcodes.DCONST_1 -> "DCONST_1";
            case Opcodes.IALOAD -> "IALOAD";
            case Opcodes.LALOAD -> "LALOAD";
            case Opcodes.FALOAD -> "FALOAD";
            case Opcodes.DALOAD -> "DALOAD";
            case Opcodes.AALOAD -> "AALOAD";
            case Opcodes.BALOAD -> "BALOAD";
            case Opcodes.CALOAD -> "CALOAD";
            case Opcodes.SALOAD -> "SALOAD";
            case Opcodes.IASTORE -> "IASTORE";
            case Opcodes.LASTORE -> "LASTORE";
            case Opcodes.FASTORE -> "FASTORE";
            case Opcodes.DASTORE -> "DASTORE";
            case Opcodes.AASTORE -> "AASTORE";
            case Opcodes.BASTORE -> "BASTORE";
            case Opcodes.CASTORE -> "CASTORE";
            case Opcodes.SASTORE -> "SASTORE";
            case Opcodes.POP -> "POP";
            case Opcodes.POP2 -> "POP2";
            case Opcodes.DUP -> "DUP";
            case Opcodes.DUP_X1 -> "DUP_X1";
            case Opcodes.DUP_X2 -> "DUP_X2";
            case Opcodes.DUP2 -> "DUP2";
            case Opcodes.DUP2_X1 -> "DUP2_X1";
            case Opcodes.DUP2_X2 -> "DUP2_X2";
            case Opcodes.SWAP -> "SWAP";
            case Opcodes.IADD -> "IADD";
            case Opcodes.LADD -> "LADD";
            case Opcodes.FADD -> "FADD";
            case Opcodes.DADD -> "DADD";
            case Opcodes.ISUB -> "ISUB";
            case Opcodes.LSUB -> "LSUB";
            case Opcodes.FSUB -> "FSUB";
            case Opcodes.DSUB -> "DSUB";
            case Opcodes.IMUL -> "IMUL";
            case Opcodes.LMUL -> "LMUL";
            case Opcodes.FMUL -> "FMUL";
            case Opcodes.DMUL -> "DMUL";
            case Opcodes.IDIV -> "IDIV";
            case Opcodes.LDIV -> "LDIV";
            case Opcodes.FDIV -> "FDIV";
            case Opcodes.DDIV -> "DDIV";
            case Opcodes.IREM -> "IREM";
            case Opcodes.LREM -> "LREM";
            case Opcodes.FREM -> "FREM";
            case Opcodes.DREM -> "DREM";
            case Opcodes.INEG -> "INEG";
            case Opcodes.LNEG -> "LNEG";
            case Opcodes.FNEG -> "FNEG";
            case Opcodes.DNEG -> "DNEG";
            case Opcodes.ISHL -> "ISHL";
            case Opcodes.LSHL -> "LSHL";
            case Opcodes.ISHR -> "ISHR";
            case Opcodes.LSHR -> "LSHR";
            case Opcodes.IUSHR -> "IUSHR";
            case Opcodes.LUSHR -> "LUSHR";
            case Opcodes.IAND -> "IAND";
            case Opcodes.LAND -> "LAND";
            case Opcodes.IOR -> "IOR";
            case Opcodes.LOR -> "LOR";
            case Opcodes.IXOR -> "IXOR";
            case Opcodes.LXOR -> "LXOR";
            case Opcodes.I2L -> "I2L";
            case Opcodes.I2F -> "I2F";
            case Opcodes.I2D -> "I2D";
            case Opcodes.L2I -> "L2I";
            case Opcodes.L2F -> "L2F";
            case Opcodes.L2D -> "L2D";
            case Opcodes.F2I -> "F2I";
            case Opcodes.F2L -> "F2L";
            case Opcodes.F2D -> "F2D";
            case Opcodes.D2I -> "D2I";
            case Opcodes.D2L -> "D2L";
            case Opcodes.D2F -> "D2F";
            case Opcodes.I2B -> "I2B";
            case Opcodes.I2C -> "I2C";
            case Opcodes.I2S -> "I2S";
            case Opcodes.LCMP -> "LCMP";
            case Opcodes.FCMPL -> "FCMPL";
            case Opcodes.FCMPG -> "FCMPG";
            case Opcodes.DCMPL -> "DCMPL";
            case Opcodes.DCMPG -> "DCMPG";
            case Opcodes.IRETURN -> "IRETURN";
            case Opcodes.LRETURN -> "LRETURN";
            case Opcodes.FRETURN -> "FRETURN";
            case Opcodes.DRETURN -> "DRETURN";
            case Opcodes.ARETURN -> "ARETURN";
            case Opcodes.RETURN -> "RETURN";
            case Opcodes.ARRAYLENGTH -> "ARRAYLENGTH";
            case Opcodes.ATHROW -> "ATHROW";
            case Opcodes.MONITORENTER -> "MONITORENTER";
            case Opcodes.MONITOREXIT -> "MONITOREXIT";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
    }

    private static String stringifyIntInsn(IntInsnNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.BIPUSH -> "BIPUSH";
            case Opcodes.SIPUSH -> "SIPUSH";
            case Opcodes.NEWARRAY -> "NEWARRAY";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + "(" + node.operand + ")";
    }

    private static String stringifyVarInsn(VarInsnNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.ILOAD -> "ILOAD";
            case Opcodes.LLOAD -> "LLOAD";
            case Opcodes.FLOAD -> "FLOAD";
            case Opcodes.DLOAD -> "DLOAD";
            case Opcodes.ALOAD -> "ALOAD";
            case Opcodes.ISTORE -> "ISTORE";
            case Opcodes.LSTORE -> "LSTORE";
            case Opcodes.FSTORE -> "FSTORE";
            case Opcodes.DSTORE -> "DSTORE";
            case Opcodes.ASTORE -> "ASTORE";
            case Opcodes.RET -> "RET";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + "(" + node.var + ")";
    }

    private static String stringifyTypeInsn(TypeInsnNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.NEW -> "NEW";
            case Opcodes.ANEWARRAY -> "ANEWARRAY";
            case Opcodes.CHECKCAST -> "CHECKCAST";
            case Opcodes.INSTANCEOF -> "INSTANCEOF";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + " " + node.desc;
    }

    private static String stringifyFieldInsn(FieldInsnNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.GETSTATIC -> "GETSTATIC";
            case Opcodes.PUTSTATIC -> "PUTSTATIC";
            case Opcodes.GETFIELD -> "GETFIELD";
            case Opcodes.PUTFIELD -> "PUTFIELD";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + " " + node.desc + " " + node.owner + "." + node.name;
    }

    private static String stringifyMethodInsn(MethodInsnNode node) {
        String itf = node.itf ? "I: " : "";
        String operand = switch (node.getOpcode()) {
            case Opcodes.INVOKEVIRTUAL -> "INVOKEVIRTUAL";
            case Opcodes.INVOKESPECIAL -> "INVOKESPECIAL";
            case Opcodes.INVOKESTATIC -> "INVOKESTATIC";
            case Opcodes.INVOKEINTERFACE -> "INVOKEINTERFACE";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + " " + itf + node.owner + "." + node.name + "(" + node.desc + ")";
    }

    private static String stringifyInvokeDynamicInsn(InvokeDynamicInsnNode node) {
        return "INVOKEDYNAMIC" + " " + node.name + "(" + node.desc + ")";
    }

    private static String stringifyJumpInsn(JumpInsnNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.IFEQ -> "IFEQ";
            case Opcodes.IFNE -> "IFNE";
            case Opcodes.IFLT -> "IFLT";
            case Opcodes.IFGE -> "IFGE";
            case Opcodes.IFGT -> "IFGT";
            case Opcodes.IFLE -> "IFLE";
            case Opcodes.IF_ICMPEQ -> "IF_ICMPEQ";
            case Opcodes.IF_ICMPNE -> "IF_ICMPNE";
            case Opcodes.IF_ICMPLT -> "IF_ICMPLT";
            case Opcodes.IF_ICMPGE -> "IF_ICMPGE";
            case Opcodes.IF_ICMPGT -> "IF_ICMPGT";
            case Opcodes.IF_ICMPLE -> "IF_ICMPLE";
            case Opcodes.IF_ACMPEQ -> "IF_ACMPEQ";
            case Opcodes.IF_ACMPNE -> "IF_ACMPNE";
            case Opcodes.GOTO -> "GOTO";
            case Opcodes.JSR -> "JSR";
            case Opcodes.IFNULL -> "IFNULL";
            case Opcodes.IFNONNULL -> "IFNONNULL";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        return operand + " " + node.label.getLabel();
    }

    private static String stringifyLabelInsn(LabelNode node) {
        return String.valueOf(node.getLabel());
    }

    private static String stringifyLdcInsn(LdcInsnNode node) {
        return "LDC" + " " + node.cst;
    }

    private static String stringifyIincInsn(IincInsnNode node) {
        return "IINC" + "(" + node.var + ", +" + node.incr + ")";
    }

    private static String stringifyTableSwitchInsn(TableSwitchInsnNode node) {
        return "TABLESWITCH" + " min: " + node.min + "; max: " + node.max + "; dftl: " + node.dflt.getLabel() + "; " +
                node.labels.stream().map(labelNode -> labelNode.getLabel().toString()).collect(Collectors.joining(", "));
    }

    private static String stringifyLookupSwitchInsn(LookupSwitchInsnNode node) {
        return "LOOKUPSWITCH" + " dftl: " + node.dflt.getLabel() + "; " + "; keys: " +
                node.keys.stream().map(Object::toString).collect(Collectors.joining(", ")) + "; " +
                node.labels.stream().map(labelNode -> labelNode.getLabel().toString()).collect(Collectors.joining(", "));
    }

    private static String stringifyMultiANewArrayInsn(MultiANewArrayInsnNode node) {
        return "MULTIANEWARRAY" + " " + node.desc + " " + node.dims + "d";
    }

    private static String stringifyFrameInsn(FrameNode node) {
        String operand = switch (node.getOpcode()) {
            case Opcodes.F_NEW -> "NEW";
            case Opcodes.F_FULL -> "FULL";
            case Opcodes.F_APPEND -> "APPEND";
            case Opcodes.F_CHOP -> "CHOP";
            case Opcodes.F_SAME -> "SAME";
            case Opcodes.F_SAME1 -> "SAME1";
            default -> "UNKNOWN OPCODE: " + node.getOpcode();
        };
        String locals = node.local == null ? "null" : node.local.stream().map(Object::toString).collect(Collectors.joining(", "));
        String stack = node.stack == null ? "null" : node.stack.stream().map(Object::toString).collect(Collectors.joining(", "));
        return operand + " FRAME locals: " + locals + "; stack: " + stack;
    }

    private static String stringifyLineNumberInsn(LineNumberNode node) {
        return node.start.getLabel() + " Line: " + node.line;
    }
}
