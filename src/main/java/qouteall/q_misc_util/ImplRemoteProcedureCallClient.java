package qouteall.q_misc_util;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class ImplRemoteProcedureCallClient {
    static void clientTellFailure() {
        Minecraft.getInstance().gui.getChat().addMessage(Component.literal(
                "The client failed to process a packet from server. See the log for details."
        ).withStyle(ChatFormatting.RED));
    }
}
