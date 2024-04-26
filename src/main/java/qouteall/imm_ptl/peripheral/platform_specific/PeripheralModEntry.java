package qouteall.imm_ptl.peripheral.platform_specific;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

@Mod(PeripheralModEntry.MODID)
public class PeripheralModEntry {

    public static final String MODID = "imm_ptl";

    public PeripheralModEntry(IEventBus modEventBus) {
        // TODO @Nick1st - This will probably cause frozen registries
        PeripheralModMain.registerBlocks((id, ele) -> Registry.register(
            BuiltInRegistries.BLOCK, id, ele
        ));
        PeripheralModMain.registerItems((id, ele) -> Registry.register(
            BuiltInRegistries.ITEM, id, ele
        ));
        PeripheralModMain.registerChunkGenerators((id, ele) -> Registry.register(
            BuiltInRegistries.CHUNK_GENERATOR, id, ele
        ));
        PeripheralModMain.registerBiomeSources((id, ele) -> Registry.register(
            BuiltInRegistries.BIOME_SOURCE, id, ele
        ));
        PeripheralModMain.registerCreativeTabs((id, ele) -> Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB, id, ele
        ));
        
        PeripheralModMain.init();
    }
}
