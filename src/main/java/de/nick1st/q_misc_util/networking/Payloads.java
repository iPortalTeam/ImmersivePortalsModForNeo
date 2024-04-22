package de.nick1st.q_misc_util.networking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import qouteall.q_misc_util.ImplRemoteProcedureCall;
import qouteall.q_misc_util.MiscHelper;
import qouteall.q_misc_util.MiscNetworking;
import qouteall.q_misc_util.MiscUtilModEntry;

public class Payloads {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(MiscUtilModEntry.MOD_ID);
        registrar.play(MiscNetworking.DimIdSyncPacket.ID, MiscNetworking.DimIdSyncPacket::read, handler ->
                handler.client(ClientPayloadHandler.getInstance()::handleDimIdSync));

        registrar.play(MiscNetworking.id_stcRemote, ImplRemoteProcedureCall::clientReadPacketAndGetHandler, handler ->
                handler.client((payload, context) -> MiscHelper.executeOnRenderThread(payload.runnable)));

        registrar.play(MiscNetworking.id_ctsRemote, ImplRemoteProcedureCall::serverReadPacket, handler ->
                handler.server((payload, context) -> MiscHelper.executeOnServerThread(context.player().get().getServer(),
                        ImplRemoteProcedureCall.handleServerPayload(payload, context))));
    }
}
