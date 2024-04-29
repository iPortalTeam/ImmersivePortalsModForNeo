package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlClientChunkMap;

public final class O_OClient {
    public O_OClient() {}

    public static ClientChunkCache createMyClientChunkManager(ClientLevel world, int loadDistance) {
        return new ImmPtlClientChunkMap(world, loadDistance);
    }
}
