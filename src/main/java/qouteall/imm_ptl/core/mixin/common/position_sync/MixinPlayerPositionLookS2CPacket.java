package qouteall.imm_ptl.core.mixin.common.position_sync;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.ducks.IEPlayerPositionLookS2CPacket;

import java.util.Set;

@Mixin(ClientboundPlayerPositionPacket.class)
public class MixinPlayerPositionLookS2CPacket implements IEPlayerPositionLookS2CPacket {
    @Mutable
    @Shadow
    @Final
    public static StreamCodec<FriendlyByteBuf, ClientboundPlayerPositionPacket> STREAM_CODEC;

    @Unique
    private ResourceKey<Level> immersivePortals$playerDimension;
    
    @Override
    public ResourceKey<Level> ip_getPlayerDimension() {
        return immersivePortals$playerDimension;
    }
    
    @Override
    public void ip_setPlayerDimension(ResourceKey<Level> dimension) {
        immersivePortals$playerDimension = dimension;
    }

    @Unique
    private static ClientboundPlayerPositionPacket ip_staticFactory(int id, PositionMoveRotation positionMoveRotation, Set<Relative> relativeArguments, ResourceKey<Level> playerDimension) {
        ClientboundPlayerPositionPacket p = ClientboundPlayerPositionPacket.of(id, positionMoveRotation, relativeArguments);
        ((IEPlayerPositionLookS2CPacket) (Object) p).ip_setPlayerDimension(playerDimension);
        return p;
    }
//
//    @Inject(method = "<clinit>", at = @At("RETURN"))
//    private static void ip_clinit(CallbackInfo ci) {
//        STREAM_CODEC = StreamCodec.composite(
//                ByteBufCodecs.VAR_INT, ClientboundPlayerPositionPacket::id,
//                PositionMoveRotation.STREAM_CODEC, ClientboundPlayerPositionPacket::change,
//                Relative.SET_STREAM_CODEC, ClientboundPlayerPositionPacket::relatives,
//                ResourceKey.streamCodec(Registries.DIMENSION), (p) -> ((IEPlayerPositionLookS2CPacket) (Object) p).ip_getPlayerDimension(),
//                MixinPlayerPositionLookS2CPacket::ip_staticFactory);
//    }


    // TODO @Nick1st 21.3
//    @Inject(method = "Lnet/minecraft/network/protocol/game/ClientboundPlayerPositionPacket;write(Lnet/minecraft/network/FriendlyByteBuf;)V", at = @At("RETURN"))
//    private void onWrite(FriendlyByteBuf buf, CallbackInfo ci) {
//        buf.writeResourceKey(playerDimension);
//    }
}
