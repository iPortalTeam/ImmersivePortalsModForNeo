package qouteall.q_misc_util;

import com.google.common.reflect.TypeToken;
import com.google.gson.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.MappedRegistry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.apache.commons.lang3.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import qouteall.q_misc_util.dimension.DimId;
import qouteall.q_misc_util.ducks.IEMinecraftServer_Misc;
import qouteall.q_misc_util.mixin.IELevelStorageAccess_Misc;

import java.lang.reflect.Type;
import java.nio.file.Path;
import java.util.Map;
import java.util.function.BiPredicate;

public class MiscHelper {
    
    private static final Logger LOGGER = LogManager.getLogger();
    
    public static final Gson gson;
    
    private static class DimensionIDJsonAdapter
        implements JsonSerializer<ResourceKey<Level>>, JsonDeserializer<ResourceKey<Level>> {
        
        @Override
        public ResourceKey<Level> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String str = json.getAsString();
            return DimId.idToKey(str);
        }
        
        @Override
        public JsonElement serialize(ResourceKey<Level> src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.location().toString());
        }
    }
    
    static {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        
        gsonBuilder.registerTypeAdapter(
            new TypeToken<ResourceKey<Level>>() {}.getType(),
            new DimensionIDJsonAdapter()
        );
        
        gson = gsonBuilder.create();
    }
    
    public static <T> MappedRegistry<T> filterAndCopyRegistry(
        MappedRegistry<T> registry, BiPredicate<ResourceKey<T>, T> predicate
    ) {
        MappedRegistry<T> newRegistry = new MappedRegistry<>(
            registry.key(),
            registry.registryLifecycle()
        );
        
        for (Map.Entry<ResourceKey<T>, T> entry : registry.entrySet()) {
            T object = entry.getValue();
            ResourceKey<T> key = entry.getKey();
            if (predicate.test(key, object)) {
                newRegistry.register(
                    key, object, registry.lifecycle(object)
                );
            }
        }
        
        return newRegistry;
    }
    
    /**
     * {@link ReentrantThreadExecutor#shouldExecuteAsync()}
     * The execution may get deferred on the render thread
     */
    //@OnlyIn(Dist.CLIENT)
    public static void executeOnRenderThread(Runnable runnable) {
        Minecraft client = Minecraft.getInstance();
        
        if (client.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on render thread", e);
            }
        }
        else {
            client.execute(runnable);
        }
    }
    
    /**
     * TODO support multi-server-in-one-JVM
     */
    @Deprecated
    public static MinecraftServer getServer() {
        return MiscGlobals.refMinecraftServer.get();
    }
    
    public static void executeOnServerThread(MinecraftServer server, Runnable runnable) {
        if (server.isSameThread()) {
            try {
                runnable.run();
            }
            catch (Exception e) {
                LOGGER.error("Processing task on server thread", e);
            }
        }
        else {
            server.execute(runnable);
        }
    }
    
    public static boolean isDedicatedServer() {
        return FMLLoader.getDist() == Dist.DEDICATED_SERVER;
    }
    
    
    public static Path getWorldSavingDirectory(MinecraftServer server) {
        Validate.notNull(server);
        Path saveDir =
            ((IELevelStorageAccess_Misc) ((IEMinecraftServer_Misc) server).ip_getStorageSource())
                .ip_getLevelPath().path();
        return saveDir;
    }
}
