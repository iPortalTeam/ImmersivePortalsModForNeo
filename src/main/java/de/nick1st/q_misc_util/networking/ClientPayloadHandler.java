package de.nick1st.q_misc_util.networking;

import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import qouteall.q_misc_util.MiscNetworking;

public class ClientPayloadHandler {
    private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    public static ClientPayloadHandler getInstance() {
        return INSTANCE;
    }

    public void handleDimIdSync(final MiscNetworking.DimIdSyncPacket dimIdSyncPacket, final PlayPayloadContext payloadContext) {
        dimIdSyncPacket.handleOnNetworkingThread();
    }
}
