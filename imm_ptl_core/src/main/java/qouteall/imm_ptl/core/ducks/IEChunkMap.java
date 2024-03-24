package qouteall.imm_ptl.core.ducks;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface IEChunkMap {
    int ip_getPlayerViewDistance(ServerPlayer player);
    
    ServerLevel ip_getWorld();
    
    ThreadedLevelLightEngine ip_getLightingProvider();
    
    ChunkHolder ip_getChunkHolder(long chunkPosLong);
    
    void ip_onPlayerUnload(ServerPlayer oldPlayer);
    
    void ip_onDimensionRemove();
    
    void ip_resendSpawnPacketToTrackers(Entity entity);
    
    Int2ObjectMap<ChunkMap.TrackedEntity> ip_getEntityTrackerMap();
    
    @Nullable ChunkHolder ip_getUpdatingChunkIfPresent(long chunkPos);
}
