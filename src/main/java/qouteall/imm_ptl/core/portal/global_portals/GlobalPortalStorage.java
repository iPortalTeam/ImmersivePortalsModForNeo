package qouteall.imm_ptl.core.portal.global_portals;

import de.nick1st.imm_ptl.events.ClientCleanupEvent;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientCommonPacketListener;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.api.PortalAPI;
import qouteall.imm_ptl.core.network.ImmPtlNetworking;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.MiscHelper;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Stores global portals.
 * Also stores bedrock replacement block state for dimension stack.
 */
public class GlobalPortalStorage extends SavedData {
    public List<Portal> data;
    public final WeakReference<ServerLevel> world;
    private int version = 1;
    private boolean shouldReSync = false;

    @Nullable
    public BlockState bedrockReplacement;

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            event.getServer().getAllLevels().forEach(world1 -> {
                GlobalPortalStorage gps = GlobalPortalStorage.get(world1);
                gps.tick();
            });
        });

        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            MinecraftServer s = event.getServer();
            for (ServerLevel world : s.getAllLevels()) {
                get(world).onServerClose();
            }
        });

        // @Nick1st - DimLib removal
//        NeoForge.EVENT_BUS.addListener(DimensionEvents.ServerDimensionDynamicUpdateEvent.class, event -> {
//            for (ServerLevel world : server.getAllLevels()) {
//                GlobalPortalStorage gps = get(world);
//                gps.clearAbnormalPortals();
//                gps.syncToAllPlayers();
//            }
//        });

        if (!O_O.isDedicatedServer()) {
            initClient();
        }
    }

    public static GlobalPortalStorage get(
            ServerLevel world
    ) {
        return world.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(
                        () -> {
                            Helper.log("Global portal storage initialized " + world.dimension().location());
                            return new GlobalPortalStorage(world);
                        },
                        (nbt, holderLookup) -> {
                            GlobalPortalStorage globalPortalStorage = new GlobalPortalStorage(world);
                            globalPortalStorage.fromNbt(nbt);
                            return globalPortalStorage;
                        },
                        null
                ),
                "global_portal"
        );
    }

    //@OnlyIn(Dist.CLIENT)
    private static void initClient() {
        NeoForge.EVENT_BUS.addListener(ClientCleanupEvent.class, e -> GlobalPortalStorageClient.onClientCleanup());
    }

    // @Nick1st - Moved to client class
//    @OnlyIn(Dist.CLIENT)
//    private static void onClientCleanup() {
//        if (ClientWorldLoader.getIsInitialized()) {
//            for (ClientLevel clientWorld : ClientWorldLoader.getClientWorlds()) {
//                for (Portal globalPortal : getGlobalPortals(clientWorld)) {
//                    globalPortal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
//                }
//            }
//        }
//    }

    public GlobalPortalStorage(ServerLevel world_) {
        world = new WeakReference<>(world_);
        data = new ArrayList<>();
    }

    public static void onPlayerLoggedIn(ServerPlayer player) {
        MiscHelper.getServer().getAllLevels().forEach(
                world -> {
                    GlobalPortalStorage storage = get(world);
                    if (!storage.data.isEmpty()) {
                        Packet<ClientCommonPacketListener> packet = createSyncPacket(world, storage);
                        player.connection.send(packet);
                    }
                }
        );

    }

    public static Packet<ClientCommonPacketListener> createSyncPacket(
            ServerLevel world, GlobalPortalStorage storage
    ) { // TODO @Nick1st Check
        return new ClientboundCustomPayloadPacket(
                new ImmPtlNetworking.GlobalPortalSyncPacket(
                        PortalAPI.serverDimKeyToInt(world.getServer(), world.dimension()),
                        storage.save(new CompoundTag(), world.registryAccess())));
    }

    public void onDataChanged() {
        setDirty(true);

        shouldReSync = true;
    }

    public void removePortal(Portal portal) {
        data.remove(portal);
        portal.remove(Entity.RemovalReason.KILLED);
        onDataChanged();
    }

    public void addPortal(Portal portal) {
        Validate.isTrue(!data.contains(portal));

        Validate.isTrue(portal.isPortalValid());

        portal.isGlobalPortal = true;
        portal.myUnsetRemoved();
        data.add(portal);
        onDataChanged();
    }

    public void removePortals(Predicate<Portal> predicate) {
        data.removeIf(portal -> {
            final boolean shouldRemove = predicate.test(portal);
            if (shouldRemove) {
                portal.remove(Entity.RemovalReason.KILLED);
            }
            return shouldRemove;
        });
        onDataChanged();
    }

    private void syncToAllPlayers() {
        ServerLevel currWorld = world.get();
        Validate.notNull(currWorld);
        Packet packet = createSyncPacket(currWorld, this);
        McHelper.getRawPlayerList().forEach(
                player -> player.connection.send(packet)
        );
    }

    public void fromNbt(CompoundTag tag) {

        ServerLevel currWorld = world.get();
        Validate.notNull(currWorld);
        List<Portal> newData = getPortalsFromTag(tag, currWorld);

        data = newData;

        if (tag.contains("version")) {
            version = tag.getInt("version");
        }

        if (tag.contains("bedrockReplacement")) {
            bedrockReplacement = NbtUtils.readBlockState(
                    currWorld.holderLookup(Registries.BLOCK),
                    tag.getCompound("bedrockReplacement")
            );
        } else {
            bedrockReplacement = null;
        }

        clearAbnormalPortals();
    }

    static List<Portal> getPortalsFromTag(
            CompoundTag tag,
            Level currWorld
    ) {
        /**{@link CompoundTag#getType()}*/
        ListTag listTag = tag.getList("data", 10);

        List<Portal> newData = new ArrayList<>();

        for (int i = 0; i < listTag.size(); i++) {
            CompoundTag compoundTag = listTag.getCompound(i);
            Portal e = readPortalFromTag(currWorld, compoundTag);
            if (e != null) {
                newData.add(e);
            } else {
                Helper.err("error reading portal" + compoundTag);
            }
        }
        return newData;
    }

    private static Portal readPortalFromTag(Level currWorld, CompoundTag compoundTag) {
        ResourceLocation entityId = new ResourceLocation(compoundTag.getString("entity_type"));
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.get(entityId);

        Entity e = entityType.create(currWorld);
        e.load(compoundTag);

        ((Portal) e).isGlobalPortal = true;

        // normal portals' bounding boxes are limited
        // update to non-limited bounding box
        ((Portal) e).updateCache();

        return (Portal) e;
    }

    @Override
    public @NotNull CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (data == null) {
            return tag;
        }

        ListTag listTag = new ListTag();
        ServerLevel currWorld = world.get();
        Validate.notNull(currWorld);

        for (Portal portal : data) {
            Validate.isTrue(portal.level() == currWorld);
            CompoundTag portalTag = new CompoundTag();
            portal.saveWithoutId(portalTag);
            portalTag.putString(
                    "entity_type",
                    EntityType.getKey(portal.getType()).toString()
            );
            listTag.add(portalTag);
        }

        tag.put("data", listTag);

        tag.putInt("version", version);

        if (bedrockReplacement != null) {
            tag.put("bedrockReplacement", NbtUtils.writeBlockState(bedrockReplacement));
        }

        return tag;
    }

    public void tick() {
        if (shouldReSync) {
            syncToAllPlayers();
            shouldReSync = false;
        }

        if (version <= 1) {
            upgradeData(world.get());
            version = 2;
            setDirty(true);
        }
    }

    public void clearAbnormalPortals() {
        data.removeIf(e -> {
            ResourceKey<Level> dimensionTo = ((Portal) e).getDestDim();
            if (MiscHelper.getServer().getLevel(dimensionTo) == null) {
                Helper.err("Missing Dimension for global portal " + dimensionTo.location());
                return true;
            }
            return false;
        });
    }

    private static void upgradeData(ServerLevel world) {
        //removed
    }

    // @Nick1st - Moved to client class
//    @OnlyIn(Dist.CLIENT)
//    public static void receiveGlobalPortalSync(ResourceKey<Level> dimension, CompoundTag compoundTag) {
//        ClientLevel world = ClientWorldLoader.getWorld(dimension);
//
//        List<Portal> oldGlobalPortals = ((IEClientWorld) world).ip_getGlobalPortals();
//        if (oldGlobalPortals != null) {
//            for (Portal p : oldGlobalPortals) {
//                p.remove(Entity.RemovalReason.KILLED);
//            }
//        }
//
//        List<Portal> newPortals = getPortalsFromTag(compoundTag, world);
//        for (Portal p : newPortals) {
//            p.myUnsetRemoved();
//            p.isGlobalPortal = true;
//
//            Validate.isTrue(p.isPortalValid());
//
//            ClientWorldLoader.getWorld(p.getDestDim());
//        }
//
//        ((IEClientWorld) world).ip_setGlobalPortals(newPortals);
//
//        Helper.log("Global Portals Updated " + dimension.location());
//    }

    public static void convertNormalPortalIntoGlobalPortal(Portal portal) {
        Validate.isTrue(!portal.getIsGlobal());
        Validate.isTrue(!portal.level().isClientSide());

        // global portal can only be square
        portal.setPortalShapeToDefault();

        portal.remove(Entity.RemovalReason.KILLED);

        Portal newPortal = McHelper.copyEntity(portal);

        get(((ServerLevel) portal.level())).addPortal(newPortal);
    }

    public static void convertGlobalPortalIntoNormalPortal(Portal portal) {
        Validate.isTrue(portal.getIsGlobal());
        Validate.isTrue(!portal.level().isClientSide());

        get(((ServerLevel) portal.level())).removePortal(portal);

        Portal newPortal = McHelper.copyEntity(portal);

        McHelper.spawnServerEntity(newPortal);
    }

    private void onServerClose() {
        for (Portal portal : data) {
            portal.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
        }
    }

    @NotNull
    public static List<Portal> getGlobalPortals(Level world) {
        List<Portal> result;
        if (world.isClientSide()) {
            result = CHelper.getClientGlobalPortal(world);
        } else if (world instanceof ServerLevel) {
            result = get(((ServerLevel) world)).data;
        } else {
            result = null;
        }
        return result != null ? result : Collections.emptyList();
    }
}
