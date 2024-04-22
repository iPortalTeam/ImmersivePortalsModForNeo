package de.nick1st.q_misc_util.networking;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import qouteall.q_misc_util.ImplRemoteProcedureCall;

// TODO @Nick1st implement concrete server and client package to ensure type and side safety
public abstract class ImplRPCPayload implements CustomPacketPayload {
    public final Object[] arguments;
    public final Runnable runnable;
    public final String methodPath;

    public ImplRPCPayload(Runnable runnable) {
        this.arguments = null;
        this.runnable = runnable;
        this.methodPath = null;
    }

    public ImplRPCPayload(String methodPath, Object[] arguments) {
        this.methodPath = methodPath;
        this.arguments = arguments;
        this.runnable = null;
    }

    @Override
    public void write(FriendlyByteBuf pBuffer) {
        ImplRemoteProcedureCall.serializeStringWithArguments(methodPath, arguments, pBuffer);
    }
}

