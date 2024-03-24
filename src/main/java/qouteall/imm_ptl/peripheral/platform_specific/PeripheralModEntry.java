package qouteall.imm_ptl.peripheral.platform_specific;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

@Mod(PeripheralModEntry.MODID)
public class PeripheralModEntry {
    
    public static final String MODID = "immersive_portals";

    public PeripheralModEntry(IEventBus modEventBus) {
        modEventBus.addListener(this::onInitialize);
        modEventBus.addListener((FMLClientSetupEvent event) -> new PeripheralModEntryClient().onInitializeClient());
    }

    public void onInitialize(RegisterEvent event) {
        PeripheralModMain.registerBlocks((id1, block1) -> event.register(
            Registries.BLOCK,
            id1,
            () -> block1
        ));
        PeripheralModMain.registerItems((id1, item1) -> event.register(
            Registries.ITEM,
            id1,
            () -> item1
        ));
        
        PeripheralModMain.init();
    }
}
