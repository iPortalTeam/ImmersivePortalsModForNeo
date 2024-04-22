package qouteall.imm_ptl.core.mixin.common;

import de.nick1st.imm_ptl.events.ServerCleanupEvent;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.common.NeoForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.IPGlobal;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.ducks.IEMinecraftServer;

@Mixin(MinecraftServer.class)
public abstract class MixinMinecraftServer implements IEMinecraftServer {
    @Unique
    IPPerServerInfo ipPerServerInfo = new IPPerServerInfo();
    
    @Inject(
        method = "Lnet/minecraft/server/MinecraftServer;runServer()V",
        at = @At("RETURN")
    )
    private void onServerClose(CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new ServerCleanupEvent((MinecraftServer) (Object) this));
    }
    
    @Override
    public IPPerServerInfo ip_getPerServerInfo() {
        return ipPerServerInfo;
    }
}
