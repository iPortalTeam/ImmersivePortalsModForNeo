package qouteall.imm_ptl.core;

import de.nick1st.imm_ptl.asm.OpCodeStringHelper;
import net.neoforged.fml.loading.LoadingModList;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import org.openjdk.nashorn.internal.runtime.regexp.joni.constants.OPCode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class IPMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) {
    
    }
    
    @Override
    public String getRefMapperConfig() {
        return null;
    }
    
    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (LoadingModList.get().getModFileById("porting_lib") != null) {
            return !mixinClassName.contains("MixinRenderTarget") && !mixinClassName.contains("MixinMainTarget");
        }
        return true;
    }
    
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    
    }
    
    @Override
    public List<String> getMixins() {
        return null;
    }
    
    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    
    }
    
    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
//        if (targetClassName.equals("net.minecraft.server.level.ChunkMap") && mixinClassName.equals("qouteall.imm_ptl.core.mixin.common.chunk_sync.MixinChunkMap_C")) {
//            List<MethodNode> methods = targetClass.methods.stream().filter(methodNode -> methodNode.name.equals("onChunkReadyToSend")).toList();
//            if (methods.size() != 1) {
//                System.out.println("Skipping post transform on method onChunkReadyToSend, as it has " + methods.size() + " methods instead of 1");
//                return;
//            }
//            MethodNode method = methods.getFirst();
//            System.out.println("ChunkMap.onChunkReadyToSend after mixins applied: ");
//            System.out.println(OpCodeStringHelper.stringifyMethod(method));
//            cutoutRegularChunkSync(targetClassName, targetClass, "onChunkReadyToSend"); // Cutout the regular sync
//            List<MethodInsnNode> targetNodes = Arrays.stream(method.instructions.toArray())
//                    .filter(abstractInsnNode -> abstractInsnNode.getOpcode() == Opcodes.INVOKESPECIAL)
//                    .map(MethodInsnNode.class::cast)
//                    .filter(methodInsnNode ->
//                            methodInsnNode.owner.equals("net/minecraft/server/level/ChunkMap") &&
//                                    methodInsnNode.name.startsWith("handler$"))
//                    .toList();
//            System.out.println("Trying to post transform the following mixins: ");
//            System.out.println(targetNodes.stream()
//                    .map(node -> node.owner + "." + node.name + " " + node.desc)
//                    .collect(Collectors.joining("\n"))
//            );
//
//            targetNodes.forEach(methodInsnNode -> transformChunkSync(targetClassName, targetClass, methodInsnNode.name));
//
//            System.out.println(targetClassName);
//        }
    }

    private void transformChunkSync(String targetClassName, ClassNode targetClass, String method) {
        try {
            ClassReader cr = new ClassReader(targetClassName);
            cr.accept(targetClass, 0);
            List<MethodNode> methodNodes = targetClass.methods.stream().filter(methodNode -> methodNode.name.equals(method)).toList();
            if (methodNodes.size() != 1) {
                throw new IOException("More than one method matching " + method + " found in " + targetClassName);
            }
            MethodNode methodNode = methodNodes.getFirst();
            System.out.println(OpCodeStringHelper.stringifyMethod(methodNode));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void cutoutRegularChunkSync(String targetClassName, ClassNode targetClass, String method) {
        try {
            ClassReader cr = new ClassReader(targetClassName);
            cr.accept(targetClass, 0);
            List<MethodNode> methodNodes = targetClass.methods.stream().filter(methodNode -> methodNode.name.equals(method)).toList();
            if (methodNodes.size() != 1) {
                System.err.println("More than one method matching " + method + " found in " + targetClassName);
            }
            for (MethodNode methodNode : methodNodes) {
                methodNode.instructions.remove(new MethodInsnNode(Opcodes.INVOKESTATIC, "net/minecraft/server/level/ChunkMap", "markChunkPendingToSend", "(Lnet/minecraft/server/level/ServerPlayer;Lnet/minecraft/world/level/chunk/LevelChunk;)V"));
                ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
                targetClass.accept(cw);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
