package qouteall.q_misc_util.api;

import net.minecraft.client.Minecraft;

import static qouteall.q_misc_util.api.McRemoteProcedureCall.createPacketToSendToServer;

public class McRemoteProcedureCallClient {
    public static void tellServerToInvoke(
            String methodPath, Object... arguments
    ) {
        var packet = createPacketToSendToServer(methodPath, arguments);
        Minecraft.getInstance().getConnection().send(packet);
    }
}
