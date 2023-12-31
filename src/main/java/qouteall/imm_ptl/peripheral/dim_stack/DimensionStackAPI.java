package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.LevelStem;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.api.DimensionAPI;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

public class DimensionStackAPI {
    public interface DimensionCollectionCallback {
        Collection<ResourceKey<Level>> getExtraDimensionKeys(
            RegistryAccess.Frozen registryAccess,
            WorldOptions options
        );
    }
    
    /**
     * See {@link DimensionStackAPI.DimensionStackPreUpdateEvent}
     */
    public interface DimensionStackPreUpdateCallback {
        void run(MinecraftServer server, @Nullable DimStackInfo dimStackInfo);
    }

    public static class DimensionStackCandidateCollectionEvent extends Event {
        public final RegistryAccess.Frozen registryAccess;
        public final WorldOptions options;

        final Set<ResourceKey<Level>> result = new LinkedHashSet<>();

        public DimensionStackCandidateCollectionEvent(RegistryAccess.Frozen registryAccess, WorldOptions options) {
            this.registryAccess = registryAccess;
            this.options = options;
        }

        public void addToResult(Collection<ResourceKey<Level>> dimensions) {
            result.addAll(dimensions);
        }

        public Set<ResourceKey<Level>> getDimensionsResult() {
            return result;
        }
    }
//    public static final Event<DimensionCollectionCallback> DIMENSION_STACK_CANDIDATE_COLLECTION_EVENT =
//        EventFactory.createArrayBacked(
//            DimensionCollectionCallback.class,
//            (listeners) -> ((registryAccess, options) -> {
//                Set<ResourceKey<Level>> totalResult = new LinkedHashSet<>();
//                for (DimensionCollectionCallback listener : listeners) {
//                    totalResult.addAll(
//                        listener.getExtraDimensionKeys(registryAccess, options)
//                    );
//                }
//                return totalResult;
//            })
//        );
    
    /**
     * This event is fired before new dimension stack configuration gets applied.
     * This event will be fired in two cases:
     * 1.
     * When initializing a server and is initializing dimension stack, this event will fire.
     * In this case, the `dimStackInfo` parameter will not be null.
     * It will fire inside {@link DimensionAPI#SERVER_DIMENSIONS_LOAD_EVENT} so you can add dimensions via {@link DimensionAPI#addDimension(MinecraftServer, ResourceLocation, LevelStem)} at this time.
     *
     * 2.
     * After using `/portal dimension_stack` command, this event will also fire,
     * but this time the `dimStackInfo` parameter will be null.
     */
    public static class DimensionStackPreUpdateEvent extends Event {
        public final MinecraftServer server;
        public final @Nullable DimStackInfo dimStackInfo;

        public DimensionStackPreUpdateEvent(MinecraftServer server, @Nullable DimStackInfo dimStackInfo) {
            this.server = server;
            this.dimStackInfo = dimStackInfo;
        }
    }
//    public static final Event<DimensionStackPreUpdateCallback> DIMENSION_STACK_PRE_UPDATE_EVENT =
//        EventFactory.createArrayBacked(
//            DimensionStackPreUpdateCallback.class,
//            (listeners) -> ((server, dimStackInfo) -> {
//                for (DimensionStackPreUpdateCallback listener : listeners) {
//                    listener.run(server, dimStackInfo);
//                }
//            })
//        );
}
