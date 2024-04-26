package de.nick1st.imm_ptl.networking;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.registration.IPayloadRegistrar;
import qouteall.imm_ptl.core.network.ImmPtlNetworkConfig;
import qouteall.imm_ptl.core.network.ImmPtlNetworking;
import qouteall.q_misc_util.MiscUtilModEntry;

public class Payloads {
    @SubscribeEvent
    public static void register(final RegisterPayloadHandlerEvent event) {
        final IPayloadRegistrar registrar = event.registrar(MiscUtilModEntry.MOD_ID);

        // Configuration
        registrar.configuration(ImmPtlNetworkConfig.S2CConfigStartPacket.ID,
                ImmPtlNetworkConfig.S2CConfigStartPacket::read,
                handler -> handler.client(ImmPtlNetworkConfig.S2CConfigStartPacket::handle));

        registrar.configuration(ImmPtlNetworkConfig.C2SConfigCompletePacket.ID,
                ImmPtlNetworkConfig.C2SConfigCompletePacket::read,
                handler -> handler.client(ImmPtlNetworkConfig.C2SConfigCompletePacket::handle));

        // Play
        registrar.play(ImmPtlNetworking.TeleportPacket.ID, ImmPtlNetworking.TeleportPacket::read, handler ->
                handler.server(ImmPtlNetworking.TeleportPacket::handle));
        registrar.play(ImmPtlNetworking.GlobalPortalSyncPacket.ID, ImmPtlNetworking.GlobalPortalSyncPacket::read, handler ->
                handler.client(ImmPtlNetworking.GlobalPortalSyncPacket::handle));
        registrar.play(ImmPtlNetworking.PortalSyncPacket.ID, ImmPtlNetworking.PortalSyncPacket::read, handler ->
                handler.client(ImmPtlNetworking.PortalSyncPacket::handle));
    }
}
