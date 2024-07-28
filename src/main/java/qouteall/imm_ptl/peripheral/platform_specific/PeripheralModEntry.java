package qouteall.imm_ptl.peripheral.platform_specific;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.RegisterEvent;
import qouteall.imm_ptl.peripheral.PeripheralModMain;

@Mod(PeripheralModEntry.MODID)
public class PeripheralModEntry {

    public static final String MODID = "imm_ptl";

    public static final DeferredRegister.DataComponents DATA_COMPONENTS_REGISTRAR = DeferredRegister.createDataComponents(MODID);

    public PeripheralModEntry(IEventBus modEventBus) {
        DATA_COMPONENTS_REGISTRAR.register(modEventBus);
        modEventBus.addListener(RegisterEvent.class, registerEvent -> {
            registerEvent.register(BuiltInRegistries.BLOCK.key(), helper -> PeripheralModMain.registerBlocks(helper::register));
            registerEvent.register(BuiltInRegistries.ITEM.key(), helper -> PeripheralModMain.registerItems(helper::register));
            registerEvent.register(BuiltInRegistries.CHUNK_GENERATOR.key(), helper ->
                    PeripheralModMain.registerChunkGenerators(helper::register));
            registerEvent.register(BuiltInRegistries.BIOME_SOURCE.key(), helper ->
                    PeripheralModMain.registerBiomeSources(helper::register));
            registerEvent.register(BuiltInRegistries.CREATIVE_MODE_TAB.key(), helper ->
                    PeripheralModMain.registerCreativeTabs(helper::register));
        });


        PeripheralModMain.init();

        if (FMLEnvironment.dist.isClient()) {
            new PeripheralModEntryClient().onInitializeClient();
        }
    }
}
