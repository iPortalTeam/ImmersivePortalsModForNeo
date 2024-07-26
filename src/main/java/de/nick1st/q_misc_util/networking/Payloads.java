package de.nick1st.q_misc_util.networking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.ClientPayloadContext;
import net.neoforged.neoforge.network.handling.ServerPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import qouteall.q_misc_util.ImplRemoteProcedureCall;
import qouteall.q_misc_util.MiscNetworking;
import qouteall.q_misc_util.MiscUtilModEntry;

public class Payloads {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(MiscUtilModEntry.MOD_ID);
        registrar.playToClient(MiscNetworking.DimIdSyncPacket.TYPE, MiscNetworking.DimIdSyncPacket.CODEC, ClientPayloadHandler.getInstance());

        registrar.playToClient(ImplRemoteProcedureCall.S2CRPCPayload.TYPE, ImplRemoteProcedureCall.S2CRPCPayload.CODEC, (s2CRPCPayload, iPayloadContext) -> s2CRPCPayload.handle((ClientPayloadContext) iPayloadContext));
        registrar.playToServer(ImplRemoteProcedureCall.C2SRPCPayload.TYPE, ImplRemoteProcedureCall.C2SRPCPayload.CODEC, (s2CRPCPayload, iPayloadContext) -> s2CRPCPayload.handle((ServerPayloadContext) iPayloadContext));
    }
}
