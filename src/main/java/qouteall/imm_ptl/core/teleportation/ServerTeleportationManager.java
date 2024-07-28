package qouteall.imm_ptl.core.teleportation;

import com.mojang.logging.LogUtils;
import de.nick1st.imm_ptl.events.DimensionEvents;
import de.nick1st.imm_ptl.events.ServerPortalTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import qouteall.imm_ptl.core.IPMcHelper;
import qouteall.imm_ptl.core.IPPerServerInfo;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.core.chunk_loading.ImmPtlChunkTracking;
import qouteall.imm_ptl.core.collision.PortalCollisionHandler;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.compat.PehkuiInterface;
import qouteall.imm_ptl.core.ducks.IEEntity;
import qouteall.imm_ptl.core.ducks.IEServerPlayNetworkHandler;
import qouteall.imm_ptl.core.ducks.IEServerPlayerEntity;
import qouteall.imm_ptl.core.mc_utils.ServerTaskList;
import qouteall.imm_ptl.core.platform_specific.IPConfig;
import qouteall.imm_ptl.core.platform_specific.O_O;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.global_portals.GlobalPortalStorage;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.api.McRemoteProcedureCall;
import qouteall.q_misc_util.my_util.MyTaskList;
import qouteall.q_misc_util.my_util.WithDim;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ServerTeleportationManager {
    private static final Logger LOGGER = LogUtils.getLogger();
    
    private final Set<Entity> teleportingEntities = new HashSet<>();
    private final WeakHashMap<Entity, Long> lastTeleportGameTime = new WeakHashMap<>();
    public boolean isFiringMyChangeDimensionEvent = false;
    public final WeakHashMap<ServerPlayer, WithDim<Vec3>> lastPosition = new WeakHashMap<>();
    
    public static ServerTeleportationManager of(MinecraftServer server) {
        return IPPerServerInfo.of(server).teleportationManager;
    }

    public static void init() {
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> {
            of(event.getServer()).tick(event.getServer());
        });

        NeoForge.EVENT_BUS.addListener(ServerPortalTickEvent.class, event -> {
            Portal portal = event.portal;
            ServerTeleportationManager serverTeleportationManager = of(portal.getServer());
            getEntitiesToTeleport(portal).forEach(entity -> {
                serverTeleportationManager.startTeleportingRegularEntity(portal, entity);
            });
        });

        NeoForge.EVENT_BUS.addListener(DimensionEvents.BeforeRemovingDimensionEvent.class,
                beforeRemovingDimensionEvent -> of(beforeRemovingDimensionEvent.getServer()).evacuatePlayersFromDimension(beforeRemovingDimensionEvent.dimension));
    }
    
    public ServerTeleportationManager() {

    }

    private void tick(MinecraftServer server) {
        teleportingEntities.clear();
        
        manageGlobalPortalTeleportation();
    }
    
    public static boolean shouldEntityTeleport(Portal portal, Entity entity) {
        if (entity.level() != portal.level()) {return false;}
        if (!portal.canTeleportEntity(entity)) {return false;}
        Vec3 lastEyePos = entity.getEyePosition(0);
        Vec3 nextEyePos = entity.getEyePosition(1);
        
        if (entity instanceof Projectile) {
            nextEyePos = nextEyePos.add(McHelper.getWorldVelocity(entity));
        }
        
        boolean movedThroughPortal = portal.isMovedThroughPortal(lastEyePos, nextEyePos);
        return movedThroughPortal;
    }
    
    public void startTeleportingRegularEntity(Portal portal, Entity entity) {
        if (entity instanceof ServerPlayer) {
            return;
        }
        if (entity instanceof Portal) {
            return;
        }
        if (entity.getVehicle() != null || doesEntityClusterContainPlayer(entity)) {
            return;
        }
        if (entity.isRemoved()) {
            return;
        }
        if (!entity.canChangeDimensions()) {
            return;
        }
        if (isJustTeleported(entity, 1)) {
            return;
        }
        //a new born entity may have last tick pos 0 0 0
        if (entity.xo == 0 && entity.yo == 0 && entity.zo == 0) {
            LOGGER.warn("Trying to teleport a fresh new entity {}", entity);
            return;
        }
        
        double motion = McHelper.lastTickPosOf(entity).distanceToSqr(entity.position());
        if (motion > 20) {
            return;
        }
        ServerTaskList.of(portal.getServer()).addTask(() -> {
            try {
                teleportRegularEntity(entity, portal);
            }
            catch (Throwable e) {
                LOGGER.error("", e);
            }
            return true;
        });
    }
    
    private static Stream<Entity> getEntitiesToTeleport(Portal portal) {
        return portal.level().getEntitiesOfClass(
            Entity.class,
            portal.getBoundingBox().inflate(2),
            e -> true
        ).stream().filter(
            e -> !(e instanceof Portal)
        ).filter(
            entity -> shouldEntityTeleport(portal, entity)
        );
    }
    
    public void onPlayerTeleportedInClient(
        ServerPlayer player,
        ResourceKey<Level> dimensionBefore,
        Vec3 eyePosBeforeTeleportation,
        UUID portalId
    ) {
        if (player.getRemovalReason() != null) {
            LOGGER.error("Trying to teleport a removed player {}", player);
            return;
        }

        Portal portal = findPortal(player.server, dimensionBefore, portalId);

        if (portal == null) {
            LOGGER.error(
                "Unable to find portal {} in {} to teleport {}",
                portalId, dimensionBefore.location(), player
            );
            return;
        }

        lastTeleportGameTime.put(player, McHelper.getServerGameTime());
        
        Vec3 oldFeetPos = eyePosBeforeTeleportation.subtract(McHelper.getEyeOffset(player));
        
        String failReason = validatePlayerTeleportationAndGetReason(
            player, dimensionBefore, oldFeetPos, portal
        );
        if (failReason == null) {
            if (isTeleporting(player)) {
                LOGGER.info("{} is teleporting frequently", player);
            }
            
            notifyChasersForPlayer(player, portal);
            
            ResourceKey<Level> dimensionTo = portal.getDestDim();
            Vec3 newEyePos = portal.transformPoint(eyePosBeforeTeleportation);
            
            recordLastPosition(player, dimensionBefore, oldFeetPos);
            
            teleportPlayer(player, dimensionTo, newEyePos);
            
            portal.onEntityTeleportedOnServer(player);
            
            PehkuiInterface.invoker.onServerEntityTeleported(player, portal);
            
            if (portal.getTeleportChangesGravity()) {
                Direction oldGravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
                GravityChangerInterface.invoker.setBaseGravityDirectionServer(
                    player, portal.getTransformedGravityDirection(oldGravityDir)
                );
            }
            
        }
        else {
            LOGGER.error(
                "Player {} {} {} cannot teleport through portal {}\nReason: {}",
                player, player.level().dimension().location(), player.position(),
                portal, failReason
            );
            teleportEntityGeneral(player, player.position(), ((ServerLevel) player.level()));
            PehkuiInterface.invoker.setBaseScale(player, PehkuiInterface.invoker.getBaseScale(player));
            GravityChangerInterface.invoker.setBaseGravityDirectionServer(
                player, GravityChangerInterface.invoker.getGravityDirection(player)
            );
        }
    }
    
    private @Nullable Portal findPortal(
        MinecraftServer server,
        ResourceKey<Level> dimensionBefore, UUID portalId
    ) {
        ServerLevel originalWorld = server.getLevel(dimensionBefore);

        if (originalWorld == null) {
            LOGGER.error("Missing world {} when finding portal", dimensionBefore.location());
            return null;
        }

        Entity portalEntity = originalWorld.getEntity(portalId);
        if (portalEntity == null) {
            portalEntity = GlobalPortalStorage.get(originalWorld).data
                .stream().filter(
                    p -> p.getUUID().equals(portalId)
                ).findFirst().orElse(null);
        }
        if (portalEntity == null) {
            return null;
        }
        if (portalEntity instanceof Portal) {
            return ((Portal) portalEntity);
        }
        return null;
    }
    
    public void recordLastPosition(ServerPlayer player, ResourceKey<Level> dim, Vec3 pos) {
        lastPosition.put(
            player, new WithDim<>(dim, pos)
        );
    }
    
    /**
     * @return null if valid. if invalid, it's the reason
     */
    private @Nullable String validatePlayerTeleportationAndGetReason(
        ServerPlayer player,
        ResourceKey<Level> dimensionBefore,
        Vec3 posBefore,
        Portal portal
    ) {
        if (player.getVehicle() != null) {
            return null;
        }

        // cannot teleport if having awaiting teleport
        if (((IEServerPlayNetworkHandler) player.connection).ip_hasAwaitingTeleport()) {
            return "has awaiting teleport";
        }

        if (!portal.canTeleportEntity(player)) {
            return "portal cannot teleport player";
        }

        if (player.level().dimension() != dimensionBefore) {
            return "player is not in the dimensionBefore in packet";
        }

        if (player.position().distanceToSqr(posBefore) > 16 * 16) {
            return "player is too far from the posBefore in packet";
        }
        
        if (portal.getDistanceToNearestPointInPortal(posBefore) > 20) {
            return "posBefore is too far from portal";
        }
        
        return null;
    }
    
    public static boolean canPlayerReachPos(
        ServerPlayer player,
        ResourceKey<Level> dimension,
        Vec3 pos
    ) {
        Vec3 playerPos = player.position();
        if (player.level().dimension() == dimension) {
            if (playerPos.distanceToSqr(pos) < 256) {
                return true;
            }
        }
        return IPMcHelper.getNearbyPortals(player, 20)
            .filter(portal -> portal.getDestDim() == dimension)
            .filter(portal -> portal.canTeleportEntity(player))
            .map(portal -> portal.transformPoint(playerPos))
            .anyMatch(mappedPos -> mappedPos.distanceToSqr(pos) < 256);
    }
    
    public static boolean canPlayerReachBlockEntity(
        ServerPlayer player, BlockEntity blockEntity
    ) {
        Level world = blockEntity.getLevel();
        if (world == null) {
            return false;
        }
        return canPlayerReachPos(
            player, world.dimension(),
            Vec3.atCenterOf(blockEntity.getBlockPos())
        );
    }
    
    public void teleportPlayer(
        ServerPlayer player,
        ResourceKey<Level> dimensionTo,
        Vec3 newEyePos
    ) {
        MinecraftServer server = player.server;
        server.getProfiler().push("portal_teleport");
        
        ServerLevel fromWorld = (ServerLevel) player.level();
        ServerLevel toWorld = server.getLevel(dimensionTo);
        
        if (player.level().dimension() == dimensionTo) {
            McHelper.setEyePos(player, newEyePos, newEyePos);
            McHelper.updateBoundingBox(player);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newEyePos);
        }
        
        McHelper.adjustVehicle(player);
        
        // reset the "authentic" player position as the current position
        player.connection.resetPosition();

        PortalCollisionHandler.updateCollidingPortalAfterTeleportation(
            player, newEyePos, newEyePos, 1
        );
        
        server.getProfiler().pop();
    }

    public void forceTeleportPlayer(
        ServerPlayer player, ResourceKey<Level> dimensionTo, Vec3 newPos
    ) {
        forceTeleportPlayer(
            player, dimensionTo, newPos, true
        );
    }

    public void forceTeleportPlayer(
        ServerPlayer player, ResourceKey<Level> dimensionTo, Vec3 newPos,
        boolean sendPacket
    ) {
        if (IPConfig.getConfig().serverTeleportLogging) {
            LOGGER.info(
                "Force teleporting {} to {} {}",
                player, dimensionTo.location(), newPos
            );
        }

        ServerLevel fromWorld = (ServerLevel) player.level();
        ServerLevel toWorld = player.server.getLevel(dimensionTo);

        if (toWorld == null) {
            LOGGER.error(
                "Cannot teleport player {} to non-existing dimension {}",
                player, dimensionTo.location()
            );
            return;
        }
        
        if (player.level().dimension() == dimensionTo) {
            player.setPos(newPos.x, newPos.y, newPos.z);
        }
        else {
            changePlayerDimension(player, fromWorld, toWorld, newPos.add(McHelper.getEyeOffset(player)));
        }
        
        if (sendPacket) {
            player.connection.teleport(
                newPos.x,
                newPos.y,
                newPos.z,
                player.getYRot(),
                player.getXRot()
            );
        }
        
        // reset the "authentic" player position as the current position
        player.connection.resetPosition();
        
        Vec3 newEyePos = McHelper.getEyePos(player);
        PortalCollisionHandler.updateCollidingPortalAfterTeleportation(
            player, newEyePos, newEyePos, 1
        );

        ImmPtlChunkTracking.immediatelyUpdateForPlayer(player);
    }
    
    /**
     * {@link ServerPlayer#changeDimension(ServerLevel)}
     */
    private void changePlayerDimension(
        ServerPlayer player,
        ServerLevel fromWorld,
        ServerLevel toWorld,
        Vec3 newEyePos
    ) {
        // avoid the player from untracking all entities when removing from the old world
        // see MixinChunkMap_E
        teleportingEntities.add(player);
        
        Entity vehicle = player.getVehicle();
        if (vehicle != null) {
            ((IEServerPlayerEntity) player).ip_stopRidingWithoutTeleportRequest();
        }
        
        Vec3 oldPos = player.position();
        
        fromWorld.removePlayerImmediately(player, Entity.RemovalReason.CHANGED_DIMENSION);
        ((IEEntity) player).ip_unsetRemoved();
        
        McHelper.setEyePos(player, newEyePos, newEyePos);
        McHelper.updateBoundingBox(player);
        
        player.setServerLevel(toWorld);
        
        // adds the player
        toWorld.addDuringPortalTeleport(player);
        
        if (vehicle != null) {
            Vec3 offset = McHelper.getVehicleOffsetFromPassenger(vehicle, player);
            Vec3 vehiclePos = player.position().add(offset);
            vehicle = teleportVehicleAcrossDimensions(
                vehicle,
                toWorld.dimension(),
                vehiclePos.add(McHelper.getEyeOffset(vehicle))
            );
            McHelper.setPosAndLastTickPos(
                vehicle,
                player.position().add(offset),
                McHelper.lastTickPosOf(player).add(offset)
            );
            ((IEServerPlayerEntity) player).ip_startRidingWithoutTeleportRequest(vehicle);
            McHelper.adjustVehicle(player);
        }
        
        if (IPConfig.getConfig().serverTeleportLogging) {
            LOGGER.info(
                "{} :: ({} {} {} {})->({} {} {} {})",
                player.getName().getContents(),
                fromWorld.dimension().location(),
                oldPos.x(), oldPos.y(), oldPos.z(),
                toWorld.dimension().location(),
                (int) player.getX(), (int) player.getY(), (int) player.getZ()
            );
        }
        
        O_O.onPlayerTravelOnServer(
            player,
            fromWorld,
            toWorld
        );
        
        //update advancements
        ((IEServerPlayerEntity) player).portal_worldChanged(fromWorld, oldPos);
    }
    
    private void manageGlobalPortalTeleportation() {
        for (ServerLevel world : MiscHelper.getServer().getAllLevels()) {
            for (Entity entity : world.getAllEntities()) {
                if (!(entity instanceof ServerPlayer)) {
                    Portal collidingPortal = ((IEEntity) entity).ip_getCollidingPortal();
                    
                    if (collidingPortal != null && collidingPortal.getIsGlobal()) {
                        if (shouldEntityTeleport(collidingPortal, entity)) {
                            startTeleportingRegularEntity(collidingPortal, entity);
                        }
                    }
                }
            }
        }
    }
    
    public boolean isTeleporting(Entity entity) {
        return teleportingEntities.contains(entity);
    }
    
    private void teleportRegularEntity(Entity entity, Portal portal) {
        Validate.isTrue(!(entity instanceof ServerPlayer));
        if (entity.getRemovalReason() != null) {
            LOGGER.error(
                "Trying to teleport an entity that is already removed {} {}",
                entity, portal
            );
            return;
        }
        
        if (entity.level() != portal.level()) {
            LOGGER.error("Cannot teleport {} from {} through {}", entity, entity.level().dimension(), portal);
            return;
        }
        
        if (portal.getDistanceToNearestPointInPortal(entity.getEyePosition()) > 5) {
            LOGGER.error("Entity is too far to teleport {} {}", entity, portal);
            return;
        }
        
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, 0L);
        if (currGameTime - lastTeleportGameTime <= 0) {
            return;
        }
        this.lastTeleportGameTime.put(entity, currGameTime);
        
        if (entity.isPassenger() || doesEntityClusterContainPlayer(entity)) {
            return;
        }
        
        Vec3 velocity = entity.getDeltaMovement();
        Vec3 oldPos = entity.position();

        List<Entity> passengerList = entity.getPassengers();
        
        Vec3 newEyePos = getRegularEntityTeleportedEyePos(entity, portal);
        
        TeleportationUtil.transformEntityVelocity(
            portal, entity, TeleportationUtil.PortalPointVelocity.ZERO, oldPos
        );
        
        if (portal.getDestDim() != entity.level().dimension()) {
            entity = changeEntityDimension(entity, portal.getDestDim(), newEyePos, true);
            
            Entity newEntity = entity;
            
            passengerList.stream().map(
                e -> changeEntityDimension(e, portal.getDestDim(), newEyePos, true)
            ).collect(Collectors.toList()).forEach(e -> {
                e.startRiding(newEntity, true);
            });
        }
        
        McHelper.setEyePos(entity, newEyePos, newEyePos);
        McHelper.updateBoundingBox(entity);
        
        // living entities do position interpolation
        // it may interpolate into unloaded chunks and stuck
        // avoid position interpolation
        McHelper.sendToTrackers(
            entity,
            McRemoteProcedureCall.createPacketToSendToClient(
                "qouteall.imm_ptl.core.teleportation.ClientTeleportationManager.RemoteCallables.updateEntityPos",
                entity.level().dimension(),
                entity.getId(),
                entity.position()
            )
        );
        
        portal.onEntityTeleportedOnServer(entity);
        
        PehkuiInterface.invoker.onServerEntityTeleported(entity, portal);
        
        // a new entity may be created
        this.lastTeleportGameTime.put(entity, currGameTime);
    }
    
    private static Vec3 getRegularEntityTeleportedEyePos(Entity entity, Portal portal) {
        // the teleportation is delayed by 1 tick
        // the entity may be behind the portal or in front of the portal at this time
        
        Vec3 eyePosThisTick = McHelper.getEyePos(entity);
        Vec3 eyePosLastTick = McHelper.getLastTickEyePos(entity);
        
        Vec3 deltaMovement = eyePosThisTick.subtract(eyePosLastTick);
        Vec3 deltaMovementDirection = deltaMovement.normalize();
        
        Vec3 collidingPoint = portal.rayTrace(
            eyePosThisTick.subtract(deltaMovementDirection.scale(5)),
            eyePosThisTick.add(deltaMovementDirection)
        );
        
        if (collidingPoint == null) {
            collidingPoint = eyePosLastTick;
        }
        
        Vec3 result = portal.transformPoint(collidingPoint)
            .add(deltaMovementDirection.scale(0.05));
        return result;
    }
    
    /**
     * {@link Entity#changeDimension(ServerLevel)}
     * Sometimes resuing the same entity object is problematic
     * because entity's AI related things may have world reference inside
     */
    public Entity changeEntityDimension(
        Entity entity,
        ResourceKey<Level> toDimension,
        Vec3 newEyePos,
        boolean recreateEntity
    ) {
        if (entity.getRemovalReason() != null) {
            LOGGER.error("Trying to teleport a removed entity {}", entity, new Throwable());
            return entity;
        }
        
        MinecraftServer server = entity.getServer();
        Validate.notNull(server, "server is null");

        ServerLevel fromWorld = (ServerLevel) entity.level();
        ServerLevel toWorld = server.getLevel(toDimension);

        if (toWorld == null) {
            LOGGER.error(
                "Invalid dest dimension {} to teleport entity {} to",
                toDimension.location(), entity
            );
            return entity;
        }

        entity.unRide();
        
        if (recreateEntity) {
            Entity oldEntity = entity;
            Entity newEntity;
            newEntity = entity.getType().create(toWorld);
            if (newEntity == null) {
                return oldEntity;
            }
            
            newEntity.restoreFrom(oldEntity);
            newEntity.setId(oldEntity.getId());
            McHelper.setEyePos(newEntity, newEyePos, newEyePos);
            McHelper.updateBoundingBox(newEntity);
            newEntity.setYHeadRot(oldEntity.getYHeadRot());
            
            // TODO check minecart item duplication
            oldEntity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            
            toWorld.addDuringTeleport(newEntity);
            
            return newEntity;
        }
        else {
            entity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
            ((IEEntity) entity).ip_unsetRemoved();
            
            McHelper.setEyePos(entity, newEyePos, newEyePos);
            McHelper.updateBoundingBox(entity);
            
            ((IEEntity) entity).ip_setWorld(toWorld);
            
            toWorld.addDuringTeleport(entity);
            
            Validate.isTrue(!entity.isRemoved());
            
            return entity;
        }
    }
    
    public Entity teleportVehicleAcrossDimensions(
        Entity entity,
        ResourceKey<Level> toDimension,
        Vec3 newEyePos
    ) {
        // avoid sending the remove entity packet
        teleportingEntities.add(entity);
        
        ServerLevel fromWorld = (ServerLevel) entity.level();
        ServerLevel toWorld = MiscHelper.getServer().getLevel(toDimension);
        
        Entity oldEntity = entity;
        Entity newEntity;
        newEntity = entity.getType().create(toWorld);
        Validate.isTrue(newEntity != null);
        
        newEntity.restoreFrom(oldEntity);
        newEntity.setId(oldEntity.getId());
        McHelper.setEyePos(newEntity, newEyePos, newEyePos);
        McHelper.updateBoundingBox(newEntity);
        newEntity.setYHeadRot(oldEntity.getYHeadRot());
        
        oldEntity.remove(Entity.RemovalReason.CHANGED_DIMENSION);
        ((IEEntity) oldEntity).ip_unsetRemoved();
        
        toWorld.addDuringTeleport(newEntity);
        
        return newEntity;
    }
    
    private boolean doesEntityClusterContainPlayer(Entity entity) {
        if (entity instanceof Player) {
            return true;
        }
        List<Entity> passengerList = entity.getPassengers();
        if (passengerList.isEmpty()) {
            return false;
        }
        return passengerList.stream().anyMatch(this::doesEntityClusterContainPlayer);
    }
    
    public boolean isJustTeleported(Entity entity, long valveTickTime) {
        long currGameTime = McHelper.getServerGameTime();
        Long lastTeleportGameTime = this.lastTeleportGameTime.getOrDefault(entity, -100000L);
        return currGameTime - lastTeleportGameTime < valveTickTime;
    }
    
    public static Entity teleportEntityGeneral(Entity entity, Vec3 targetPos, ServerLevel targetWorld) {
        if (entity instanceof ServerPlayer serverPlayer) {
            of(serverPlayer.server).forceTeleportPlayer(
                serverPlayer, targetWorld.dimension(), targetPos
            );
            return entity;
        }
        else {
            return teleportRegularEntityTo(entity, targetWorld.dimension(), targetPos);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <E extends Entity> E teleportRegularEntityTo(
        E entity, ResourceKey<Level> targetDim, Vec3 targetPos
    ) {
        if (entity.level().dimension() == targetDim) {
            entity.moveTo(
                targetPos.x,
                targetPos.y,
                targetPos.z,
                entity.getYRot(),
                entity.getXRot()
            );
            entity.setYHeadRot(entity.getYRot());
            return entity;
        }
        
        return (E) of(entity.getServer()).changeEntityDimension(
            entity,
            targetDim,
            targetPos.add(McHelper.getEyeOffset(entity)),
            true
        );
    }
    
    // make the mobs chase the player through portal
    // (only works in simple cases)
    private static void notifyChasersForPlayer(
        ServerPlayer player,
        Portal portal
    ) {
        List<Mob> chasers = McHelper.findEntitiesRough(
            Mob.class,
            player.level(),
            player.position(),
            1,
            e -> e.getTarget() == player
        );
        
        for (Mob chaser : chasers) {
            chaser.setTarget(null);
            notifyChaser(player, portal, chaser);
        }
    }
    
    private static void notifyChaser(
        ServerPlayer player,
        Portal portal,
        Mob chaser
    ) {
        Vec3 targetPos = player.position().add(portal.getNormal().scale(-0.1));
        
        UUID chaserId = chaser.getUUID();
        ServerLevel destWorld = ((ServerLevel) portal.getDestinationWorld());
        
        ServerTaskList.of(player.server).addTask(MyTaskList.withRetryNumberLimit(
            140,
            () -> {
                if (chaser.isRemoved()) {
                    // the chaser teleported
                    Entity newChaser = destWorld.getEntity(chaserId);
                    if (newChaser instanceof Mob) {
                        ((Mob) newChaser).setTarget(player);
                        return true;
                    }
                    else {
                        return false;
                    }
                }
                
                if (chaser.position().distanceTo(targetPos) < 2) {
                    chaser.getMoveControl().setWantedPosition(
                        targetPos.x, targetPos.y, targetPos.z, 1
                    );
                }
                else {
                    @Nullable
                    Path path = chaser.getNavigation().createPath(
                        BlockPos.containing(targetPos), 0
                    );
                    chaser.getNavigation().moveTo(path, 1);
                }
                return false;
            },
            () -> {}
        ));
    }
    
    private void evacuatePlayersFromDimension(ServerLevel world) {
        // teleportation modifies the player list
        List<ServerPlayer> players = new ArrayList<>(
            MiscHelper.getServer().getPlayerList().getPlayers()
        );
        for (ServerPlayer player : players) {
            if (player.level().dimension() == world.dimension()) {
                ServerLevel overWorld = McHelper.getOverWorldOnServer();
                BlockPos spawnPos = overWorld.getSharedSpawnPos();
                
                forceTeleportPlayer(
                    player, Level.OVERWORLD, Vec3.atCenterOf(spawnPos)
                );
                
                player.sendSystemMessage(Component.literal(
                    "Teleported to spawn pos because dimension %s had been removed".formatted(world.dimension().location())
                ));
            }
        }
    }
}
