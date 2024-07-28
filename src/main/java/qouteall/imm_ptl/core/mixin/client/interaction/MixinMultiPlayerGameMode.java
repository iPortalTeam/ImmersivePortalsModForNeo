package qouteall.imm_ptl.core.mixin.client.interaction;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import qouteall.imm_ptl.core.ClientWorldLoader;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer;
import qouteall.imm_ptl.core.ducks.IEClientPlayerInteractionManager;
import qouteall.q_misc_util.api.McRemoteProcedureCall;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinMultiPlayerGameMode implements IEClientPlayerInteractionManager {
    @Shadow
    @Final
    private ClientPacketListener connection;

    @Shadow
    @Final
    private Minecraft minecraft;

    // the player level field is not being switched now
    @Redirect(
            method = "lambda$startDestroyBlock$1", // lambda in startDestroyBlock
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;level()Lnet/minecraft/world/level/Level;"
            )
    )
    private Level redirectPlayerLevel1(LocalPlayer instance) {
        return Minecraft.getInstance().level;
    }

    // the player level field is not being switched now
    @Redirect(
            method = "continueDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/player/LocalPlayer;level()Lnet/minecraft/world/level/Level;"
            )
    )
    private Level redirectPlayerLevel2(LocalPlayer instance) {
        return Minecraft.getInstance().level;
    }

    // use another constructor that does not use player level
    @Redirect(
            method = "performUseItemOn",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/item/context/UseOnContext;"
            )
    )
    private UseOnContext redirectNewUseOnContext(Player player, InteractionHand interactionHand, BlockHitResult blockHitResult) {
        return new UseOnContext(
                Minecraft.getInstance().level,
                player,
                interactionHand,
                player.getItemInHand(interactionHand),
                blockHitResult
        );
    }

    @ModifyArg(
            method = "startPrediction",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private Packet<?> modifyPacketInStartPrediction(Packet<?> packet) {
        return ip_redirectPacket(packet);
    }

    @ModifyArg(
            method = "startDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private Packet redirectSendInStartDestroyBlock(Packet packet) {
        return ip_redirectPacket(packet);
    }

    @ModifyArg(
            method = "stopDestroyBlock",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/ClientPacketListener;send(Lnet/minecraft/network/protocol/Packet;)V"
            )
    )
    private Packet redirectSendInStopDestroyBlock(Packet packet) {
        return ip_redirectPacket(packet);
    }

    private static Packet<?> ip_redirectPacket(Packet<?> packet) {
        if (ClientWorldLoader.getIsWorldSwitched()) {
            ResourceKey<Level> dimension = Minecraft.getInstance().level.dimension();
            if (packet instanceof ServerboundPlayerActionPacket playerActionPacket) {
                if (BlockManipulationServer.isAttackingAction(playerActionPacket.getAction())) {
                    FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                    ServerboundPlayerActionPacket.STREAM_CODEC.encode(buf, playerActionPacket);
                    return new ServerboundCustomPayloadPacket(McRemoteProcedureCall.createPacketToSendToServer(
                            "qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer.RemoteCallables.processPlayerActionPacket",
                            dimension,
                            IPMcHelper.bufToBytes(buf)
                    ));
                }
            } else if (packet instanceof ServerboundUseItemOnPacket useItemOnPacket) {
                FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
                ServerboundUseItemOnPacket.STREAM_CODEC.encode(buf, useItemOnPacket);
                return new ServerboundCustomPayloadPacket(McRemoteProcedureCall.createPacketToSendToServer(
                        "qouteall.imm_ptl.core.block_manipulation.BlockManipulationServer.RemoteCallables.processUseItemOnPacket",
                        dimension,
                        IPMcHelper.bufToBytes(buf)
                ));
            }
            // ServerboundUseItemPacket is not redirected
        }

        return packet;
    }

}
