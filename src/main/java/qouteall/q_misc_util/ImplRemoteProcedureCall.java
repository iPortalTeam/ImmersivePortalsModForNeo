package qouteall.q_misc_util;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import qouteall.q_misc_util.my_util.CountDownInt;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.LimitedLogger;

import javax.annotation.Nullable;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ImplRemoteProcedureCall {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final LimitedLogger LIMITED_LOGGER = new LimitedLogger(100);

    public static final Gson gson = MiscHelper.gson;

    private static final ConcurrentHashMap<String, Method> methodCache = new ConcurrentHashMap<>();

    private static final CountDownInt LOGGING_LIMIT = new CountDownInt(100);

    private static final CountDownInt ERROR_MESSAGE_LIMIT = new CountDownInt(10);

    private static final ImmutableMap<Class, BiConsumer<RegistryFriendlyByteBuf, Object>> serializerMap;
    private static final ImmutableMap<Type, Function<RegistryFriendlyByteBuf, Object>> deserializerMap;

    private static final JsonParser jsonParser = new JsonParser();

    static {
        serializerMap = ImmutableMap.<Class, BiConsumer<RegistryFriendlyByteBuf, Object>>builder()
                .put(ResourceLocation.class, (buf, o) -> buf.writeResourceLocation(((ResourceLocation) o)))
                .put(ResourceKey.class, (buf, o) -> buf.writeResourceLocation(((ResourceKey) o).location()))
                .put(BlockPos.class, (buf, o) -> buf.writeBlockPos(((BlockPos) o)))
                .put(Vec3.class, (buf, o) -> {
                    Vec3 vec = (Vec3) o;
                    buf.writeDouble(vec.x);
                    buf.writeDouble(vec.y);
                    buf.writeDouble(vec.z);
                })
                .put(UUID.class, (buf, o) -> buf.writeUUID(((UUID) o)))
                .put(Block.class, (buf, o) -> serializeByCodec(buf, BuiltInRegistries.BLOCK.byNameCodec(), o))
                .put(Item.class, (buf, o) -> serializeByCodec(buf, BuiltInRegistries.ITEM.byNameCodec(), o))
                .put(BlockState.class, (buf, o) -> serializeByCodec(buf, BlockState.CODEC, o))
                .put(ItemStack.class, (buf, o) -> serializeByCodec(buf, ItemStack.CODEC, o))
                .put(CompoundTag.class, (buf, o) -> buf.writeNbt(((CompoundTag) o)))
                .put(Component.class, (buf, o) ->
                        ComponentSerialization.TRUSTED_STREAM_CODEC.encode(buf, ((Component) o))
                ).put(DQuaternion.class, (buf, o) -> {
                    DQuaternion dQuaternion = (DQuaternion) o;
                    buf.writeDouble(dQuaternion.x);
                    buf.writeDouble(dQuaternion.y);
                    buf.writeDouble(dQuaternion.z);
                    buf.writeDouble(dQuaternion.w);
                })
                .put(byte[].class, (buf, o) -> buf.writeByteArray(((byte[]) o)))
                .build();

        deserializerMap = ImmutableMap.<Type, Function<RegistryFriendlyByteBuf, Object>>builder()
                .put(ResourceLocation.class, buf -> buf.readResourceLocation())
                .put(
                        new TypeToken<ResourceKey<Level>>() {
                        }.getType(),
                        buf -> ResourceKey.create(
                                Registries.DIMENSION, buf.readResourceLocation()
                        )
                )
                .put(
                        new TypeToken<ResourceKey<Biome>>() {
                        }.getType(),
                        buf -> ResourceKey.create(
                                Registries.BIOME, buf.readResourceLocation()
                        )
                )
                .put(BlockPos.class, buf -> buf.readBlockPos())
                .put(Vec3.class, buf ->
                        new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble())
                )
                .put(UUID.class, buf -> buf.readUUID())
                .put(Block.class, buf -> deserializeByCodec(buf, BuiltInRegistries.BLOCK.byNameCodec()))
                .put(Item.class, buf -> deserializeByCodec(buf, BuiltInRegistries.ITEM.byNameCodec()))
                .put(BlockState.class, buf -> deserializeByCodec(buf, BlockState.CODEC))
                .put(ItemStack.class, buf -> deserializeByCodec(buf, ItemStack.CODEC))
                .put(CompoundTag.class, buf -> buf.readNbt())
                .put(Component.class, ComponentSerialization.TRUSTED_STREAM_CODEC::decode).put(DQuaternion.class, buf ->
                        new DQuaternion(
                                buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble()
                        )
                )
                .put(byte[].class, buf -> buf.readByteArray())
                .build();
    }

    @SuppressWarnings("rawtypes")
    private static Object deserializeByCodec(FriendlyByteBuf buf, Codec codec) {
        String jsonString = buf.readUtf();
        JsonElement jsonElement = JsonParser.parseString(jsonString);

        return codec.parse(JsonOps.INSTANCE, jsonElement).getOrThrow();
    }

    private static Object deserializeArgument(RegistryFriendlyByteBuf buf, Type type) {
        Function<RegistryFriendlyByteBuf, Object> deserializer = deserializerMap.get(type);
        if (deserializer == null) {
            String json = buf.readUtf();
            return gson.fromJson(json, type);
        }

        return deserializer.apply(buf);
    }

    private static void serializeArgument(RegistryFriendlyByteBuf buf, Object object) {
        BiConsumer<RegistryFriendlyByteBuf, Object> serializer = serializerMap.get(object.getClass());

        if (serializer == null) {
            // TODO optimize it
            serializer = serializerMap.entrySet().stream().filter(
                    e -> e.getKey().isAssignableFrom(object.getClass())
            ).findFirst().map(Map.Entry::getValue).orElse(null);
        }

        if (serializer == null) {
            String json = gson.toJson(object);
            buf.writeUtf(json);
            return;
        }

        serializer.accept(buf, object);
    }

    @SuppressWarnings("rawtypes")
    private static void serializeByCodec(FriendlyByteBuf buf, Codec codec, Object object) {
        JsonElement result = (JsonElement) codec.encodeStart(JsonOps.INSTANCE, object).getOrThrow();

        String jsonString = gson.toJson(result);
        buf.writeUtf(jsonString);
    }

    public static record S2CRPCPayload(
            // only used in receiver side
            boolean deserializeSuccess,
            @Nullable String methodPath,
            // only used in receiver side
            @Nullable Method method,
            @Nullable List<Object> args
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<S2CRPCPayload> TYPE =
                CustomPacketPayload.createType("iportal:remote_s2c");

        public static final StreamCodec<RegistryFriendlyByteBuf, S2CRPCPayload> CODEC = StreamCodec.of(
                (b, p) -> p.write(b), S2CRPCPayload::read
        );

        public static S2CRPCPayload read(RegistryFriendlyByteBuf buf) {
            String methodPath = null;
            try {
                methodPath = buf.readUtf();

                Method method = getMethodByPath(methodPath);

                java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();

                List<Object> args = new ArrayList<>();

                for (java.lang.reflect.Type parameterType : genericParameterTypes) {
                    Object obj = deserializeArgument(buf, parameterType);
                    args.add(obj);
                }

                return new S2CRPCPayload(true, methodPath, method, args);
            } catch (Exception e) {
                if (LOGGING_LIMIT.tryDecrement()) {
                    LOGGER.error("Failed to parse remote procedure call {}", methodPath, e);
                }
                return new S2CRPCPayload(
                        false, methodPath, null, null
                );
            }
        }

        public void write(RegistryFriendlyByteBuf buf) {
            Validate.notNull(args, "args must not be null");
            Validate.notNull(methodPath, "methodPath must not be null");
            buf.writeUtf(methodPath);
            for (Object arg : args) {
                serializeArgument(buf, arg);
            }
        }

        @OnlyIn(Dist.CLIENT)
        public void handle(ClientPayloadContext c) {
            if (!deserializeSuccess) {
                if (ERROR_MESSAGE_LIMIT.tryDecrement()) {
                    clientTellFailure();
                }
                return;
            }

            Validate.notNull(args, "args must not be null");
            Validate.notNull(method, "method must not be null");
            try {
                Object[] argArray = args.toArray(new Object[0]);
                method.invoke(null, argArray);
            } catch (Exception e) {
                if (LOGGING_LIMIT.tryDecrement()) {
                    LOGGER.error("Failed to invoke remote procedure call {}", methodPath, e);
                    clientTellFailure();
                }
            }
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private static void clientTellFailure() {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(
                "The client failed to process a packet from server. See the log for details."
        ).withStyle(ChatFormatting.RED));
    }

    // @Nick1st split into two methods on Neo Port (serverReadPacket(), handleServerPayload())
//    public static ImplRPCPayload serverReadPacketAndGetHandler(ServerPlayer player, FriendlyByteBuf buf) {
//        String methodPath = null;
//        Runnable r;
//        try {
//            methodPath = buf.readUtf();
//            Method method = getMethodByPath(methodPath);
//
//            Type[] genericParameterTypes = method.getGenericParameterTypes();
//
//            Object[] arguments = new Object[genericParameterTypes.length];
//            arguments[0] = player;
//
//            //the first argument is the player
//            for (int i = 1; i < genericParameterTypes.length; i++) {
//                Type parameterType = genericParameterTypes[i];
//                Object obj = deserializeArgument(buf, parameterType);
//                arguments[i] = obj;
//            }
//
//            r = () -> {
//                try {
//                    method.invoke(null, arguments);
//                }
//                catch (Exception e) {
//                    LIMITED_LOGGER.invoke(() -> {
//                        LOGGER.error("Processing remote procedure call {}", player, e);
//                        serverTellFailure(player);
//                    });
//                }
//            };
//        }
//        catch (Exception e) {
//            String methodPath_ = methodPath;
//            LIMITED_LOGGER.invoke(() -> {
//                LOGGER.error("Failed to parse remote procedure call {}", methodPath_, e);
//                serverTellFailure(player);
//            });
//            r = () -> {};
//        }
//        return new ImplRPCPayload(r) {
//            @Override
//            public ResourceLocation intId() {
//                return MiscNetworking.id_ctsRemote;
//            }
//        };
//    }

    public static record C2SRPCPayload(
            // only used in receiver side
            boolean deserializeSuccess,
            @Nullable String methodPath,
            // only used in receiver side
            @Nullable Method method,
            @Nullable List<Object> args
    ) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<C2SRPCPayload> TYPE =
                CustomPacketPayload.createType("iportal:remote_c2s");

        public static final StreamCodec<RegistryFriendlyByteBuf, C2SRPCPayload> CODEC = StreamCodec.of(
                (b, p) -> p.write(b), C2SRPCPayload::read
        );

        public static C2SRPCPayload read(RegistryFriendlyByteBuf buf) {
            String methodPath = null;

            try {
                methodPath = buf.readUtf();

                Method method = getMethodByPath(methodPath);

                java.lang.reflect.Type[] genericParameterTypes = method.getGenericParameterTypes();

                List<Object> args = new ArrayList<>();

                //the first argument is the player
                for (int i = 1; i < genericParameterTypes.length; i++) {
                    java.lang.reflect.Type parameterType = genericParameterTypes[i];
                    Object obj = deserializeArgument(buf, parameterType);
                    args.add(obj);
                }

                return new C2SRPCPayload(true, methodPath, method, args);
            } catch (Exception e) {
                if (LOGGING_LIMIT.tryDecrement()) {
                    LOGGER.error("Failed to parse remote procedure call {}", methodPath, e);
                }
                return new C2SRPCPayload(
                        false, methodPath, null, null
                );
            }
        }

        public void write(RegistryFriendlyByteBuf buf) {
            Validate.notNull(args, "args must not be null");
            Validate.notNull(methodPath, "methodPath must not be null");
            buf.writeUtf(methodPath);
            for (Object arg : args) {
                serializeArgument(buf, arg);
            }
        }

        public void handle(ServerPayloadContext c) {
            if (!deserializeSuccess) {
                if (ERROR_MESSAGE_LIMIT.tryDecrement()) {
                    serverTellFailure(c.player());
                }
                return;
            }

            Validate.notNull(args, "args must not be null");
            Validate.notNull(method, "method must not be null");

            ServerPlayer player = c.player();

            try {
                Object[] argArray = new Object[args.size() + 1];
                argArray[0] = player;
                for (int i = 0; i < args.size(); i++) {
                    argArray[i + 1] = args.get(i);
                }
                method.invoke(null, argArray);
            } catch (Exception e) {
                if (LOGGING_LIMIT.tryDecrement()) {
                    LOGGER.error(
                            "Failed to invoke remote procedure call {} {}", methodPath, player, e
                    );
                    serverTellFailure(c.player());
                }
            }
        }

        @Override
        public @NotNull Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private static void serverTellFailure(ServerPlayer player) {
        player.sendSystemMessage(Component.literal(
                "The server failed to process a packet sent from client."
        ).withStyle(ChatFormatting.RED));
    }

    public static void serializeStringWithArguments(
            String methodPath, Object[] arguments, RegistryFriendlyByteBuf buf
    ) {
        buf.writeUtf(methodPath);

        for (Object argument : arguments) {
            serializeArgument(buf, argument);
        }
    }

    public static Method getMethodByPath(String methodPath) {
        Method result = methodCache.get(methodPath);
        if (result != null) {
            return result;
        }

        //because it may throw exception, does not use computeIfAbsent
        Method method = findMethodByPath(methodPath);
        Validate.notNull(method);

        methodCache.put(methodPath, method);
        return method;
    }

    private static Method findMethodByPath(String methodPath) {
        int lastDotIndex = methodPath.lastIndexOf('.');

        Validate.isTrue(lastDotIndex != -1);
        String classPath = methodPath.substring(0, lastDotIndex);
        String methodName = methodPath.substring(lastDotIndex + 1);

        if (!classPath.contains("RemoteCallable")) {
            throw new RuntimeException("The class path must contain \"RemoteCallable\"");
        }

        Class<?> aClass;
        try {
            aClass = Class.forName(classPath);
        } catch (ClassNotFoundException e) {
            int dotIndex = classPath.lastIndexOf('.');
            if (dotIndex != -1) {
                String newClassPath =
                        classPath.substring(0, dotIndex) + "$" + classPath.substring(dotIndex + 1);
                try {
                    aClass = Class.forName(newClassPath);
                } catch (ClassNotFoundException e1) {
                    throw new RuntimeException("Cannot find class " + classPath, e);
                }
            } else {
                throw new RuntimeException("Cannot find class " + classPath, e);
            }
        }

        Method method = Arrays.stream(aClass.getMethods()).filter(
                m -> m.getName().equals(methodName)
        ).findFirst().orElseThrow(() -> new RuntimeException(
                "Cannot find method " + methodPath + " . If it's a private method, make it public."
        ));

        return method;
    }

}
