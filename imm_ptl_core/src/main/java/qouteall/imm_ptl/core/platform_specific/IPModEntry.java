package qouteall.imm_ptl.core.platform_specific;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import qouteall.imm_ptl.core.IPModMain;
import qouteall.imm_ptl.core.commands.AxisArgumentType;
import qouteall.imm_ptl.core.commands.SubCommandArgumentType;
import qouteall.imm_ptl.core.commands.TimingFunctionArgumentType;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.q_misc_util.Helper;

@Mod(IPModEntry.MODID)
public class IPModEntry {

    public static final String MODID = "immersive_portals_core";

    public IPModEntry(IEventBus modEventBus) {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RegisterEvent.class, registerEvent ->
                registerEvent.register(BuiltInRegistries.ENTITY_TYPE.key(), IPModMain::registerEntityTypesForge));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(RegisterEvent.class, registerEvent ->
                registerEvent.register(BuiltInRegistries.BLOCK.key(), IPModMain::registerBlocksForge));
        modEventBus.addListener(FMLCommonSetupEvent.class, event -> onInitialize());
        modEventBus.addListener(EntityRenderersEvent.RegisterRenderers.class, IPModEntryClient::initPortalRenderers);
        modEventBus.addListener(FMLClientSetupEvent.class, event -> new IPModEntryClient().onInitializeClient(modEventBus));
        modEventBus.addListener(FMLDedicatedServerSetupEvent.class, event -> new IPModEntryDedicatedServer().onInitializeServer());

        SubCommandArgumentType.init();
        TimingFunctionArgumentType.init();
        AxisArgumentType.init();
    }

    public void onInitialize() {
        IPModMain.init();
        RequiemCompat.init();
        
//        IPModMain.registerEntityTypes(
//            (id, entityType) -> Registry.register(BuiltInRegistries.ENTITY_TYPE, id, entityType)
//        );
        
//        IPModMain.registerBlocks((id, obj) -> Registry.register(BuiltInRegistries.BLOCK, id, obj));
        
        if (ModList.get().isLoaded("dimthread")) {
            O_O.isDimensionalThreadingPresent = true;
            Helper.log("Dimensional Threading is present");
        }
        else {
            Helper.log("Dimensional Threading is not present");
        }
        
        if (O_O.getIsPehkuiPresent()) {
            PehkuiInterfaceInitializer.init();
            Helper.log("Pehkui is present");
        }
        else {
            Helper.log("Pehkui is not present");
        }
        
        if (ModList.get().isLoaded("gravity_changer_q")) {
            GravityChangerInterface.invoker = new GravityChangerInterface.OnGravityChangerPresent();
            Helper.log("Gravity API is present");
        }
        else {
            Helper.log("Gravity API is not present");
        }
        
    }
    
}
