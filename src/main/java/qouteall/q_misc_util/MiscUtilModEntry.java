package qouteall.q_misc_util;

import de.nick1st.q_misc_util.networking.Payloads;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import qouteall.q_misc_util.dimension.DimensionIntId;

import static qouteall.q_misc_util.MiscUtilModEntry.MOD_ID;

@Mod(MOD_ID)
public class MiscUtilModEntry {
    public static final String MOD_ID = "q_misc_util";

    public MiscUtilModEntry(IEventBus eventBus) {
        onInitialize();
        eventBus.addListener(RegisterPayloadHandlerEvent.class, Payloads::register);
    }

    public void onInitialize() {
        DimensionIntId.init();
    }
}
