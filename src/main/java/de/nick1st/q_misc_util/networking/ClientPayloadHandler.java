package de.nick1st.q_misc_util.networking;

import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;
import qouteall.q_misc_util.MiscNetworking;

public class ClientPayloadHandler implements IPayloadHandler<MiscNetworking.DimIdSyncPacket> {
    private static final ClientPayloadHandler INSTANCE = new ClientPayloadHandler();

    public static ClientPayloadHandler getInstance() {
        return INSTANCE;
    }

    @Override
    public void handle(final MiscNetworking.DimIdSyncPacket dimIdSyncPacket, final IPayloadContext payloadContext) {
        dimIdSyncPacket.handleOnNetworkingThread();
    }
}
