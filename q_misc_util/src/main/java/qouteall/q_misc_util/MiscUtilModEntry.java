package qouteall.q_misc_util;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TickEvent;
import qouteall.q_misc_util.dimension.DimensionMisc;
import qouteall.q_misc_util.dimension.DimensionTemplate;
import qouteall.q_misc_util.dimension.DimsCommand;
import qouteall.q_misc_util.dimension.DynamicDimensionsImpl;

@Mod(MiscUtilModEntry.MODID)
public class MiscUtilModEntry {
    public static final String MODID = "q_misc_util";

    public MiscUtilModEntry(IEventBus modEventBus) {
        modEventBus.addListener((FMLCommonSetupEvent event) -> onInitialize());
        modEventBus.addListener((FMLClientSetupEvent event) -> new MiscUtilModEntryClient().onInitializeClient());
    }

    public void onInitialize() {
        DimensionMisc.init();
        
        DynamicDimensionsImpl.init();
        
        ImplRemoteProcedureCall.init();
        
        MiscNetworking.init();

        NeoForge.EVENT_BUS.addListener(TickEvent.ServerTickEvent.class, serverTickEvent -> {
            if (serverTickEvent.phase == TickEvent.Phase.END) {
                MiscGlobals.serverTaskList.processTasks();
            }
        });

        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, registerCommandsEvent -> {
            DimsCommand.register(registerCommandsEvent.getDispatcher());
        });
        
        DimensionTemplate.init();
    }
}
