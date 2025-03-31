package qouteall.imm_ptl.peripheral;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.*;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import qouteall.imm_ptl.core.McHelper;
import qouteall.imm_ptl.peripheral.alternate_dimension.ChaosBiomeSource;
import qouteall.imm_ptl.peripheral.alternate_dimension.ErrorTerrainGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.FormulaGenerator;
import qouteall.imm_ptl.peripheral.alternate_dimension.NormalSkylandGenerator;
import qouteall.imm_ptl.peripheral.dim_stack.DimStackManagement;
import qouteall.imm_ptl.peripheral.portal_generation.IntrinsicPortalGeneration;
import qouteall.imm_ptl.peripheral.wand.ClientPortalWandPortalDrag;
import qouteall.imm_ptl.peripheral.wand.PortalWandInteraction;
import qouteall.imm_ptl.peripheral.wand.PortalWandItem;

import java.util.function.BiConsumer;

import static net.minecraft.world.item.Items.registerItem;

public class PeripheralModMain {

    // TODO @Nick1st - Rework registry (Best would be at fabrics side)
    public static Block portalHelperBlock;

    public static final CreativeModeTab TAB = CreativeModeTab.builder()
            .icon(() -> new ItemStack(PeripheralModMain.PORTAL_WAND.get()))
            .title(Component.translatable("imm_ptl.item_group"))
            .displayItems((enabledFeatures, entries) -> {
                PortalWandItem.addIntoCreativeTag(entries);
                
                CommandStickItem.addIntoCreativeTag(entries);
                
                entries.accept(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("immersive_portals", "portal_helper")).get().value());
            })
            .build();
    
    //@OnlyIn(Dist.CLIENT)
    public static void initClient() {
        IPOuterClientMisc.initClient();
        
        PortalWandItem.initClient();
        
        ClientPortalWandPortalDrag.init();
    }
    
    public static void init() {
        FormulaGenerator.init();
        
        IntrinsicPortalGeneration.init();
        
        DimStackManagement.init();

        // TODO @Nick1st - DynDim removal
//        AlternateDimensions.init();
//
//        DimensionAPI.suppressExperimentalWarningForNamespace("immersive_portals");

        PortalWandItem.init();
        
        CommandStickItem.init();
        
        PortalWandInteraction.init();
        
        CommandStickItem.registerCommandStickTypes();

    }

    static DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("immersive_portals");

    public static DeferredBlock<Block> PORTAL_HELPER = BLOCKS.registerBlock("portal_helper",
            properties -> new Block(BlockBehaviour.Properties.of().noOcclusion().isRedstoneConductor((a, b, c) -> false)));

    static DeferredRegister.Items ITEMS = DeferredRegister.createItems("immersive_portals");

    static DeferredItem<PortalHelperItem> PORTAL_HELPER_ITEM = ITEMS.registerItem("portal_helper",
            properties -> new PortalHelperItem(PORTAL_HELPER.get(), new Item.Properties()));
    public static DeferredItem<CommandStickItem> COMMAND_STICK = ITEMS.registerItem("command_stick",
            properties -> new CommandStickItem(new Item.Properties()));
    public static DeferredItem<PortalWandItem> PORTAL_WAND = ITEMS.registerItem("portal_wand",
            properties -> new PortalWandItem(new Item.Properties()));


   public static void registerItems(BiConsumer<ResourceLocation, Item> regFunc) {
//        regFunc.accept(
//            McHelper.newResourceLocation("immersive_portals", "portal_helper"),
//                new PortalHelperItem(PeripheralModMain.portalHelperBlock, new Item.Properties())
//        );

//        CommandStickItem commandStickItem = new CommandStickItem(new Item.Properties());
//        regFunc.accept(
//            McHelper.newResourceLocation("immersive_portals:command_stick"),
//                commandStickItem
//        );
//        CommandStickItem.instance = PeripheralModMain.COMMAND_STICK.get();

//        PortalWandItem portalWandItem = new PortalWandItem(new Item.Properties());
//        regFunc.accept(
//            McHelper.newResourceLocation("immersive_portals:portal_wand"),
//                portalWandItem
//        );
//        PortalWandItem.instance = portalWandItem;
    }
    
    public static void registerBlocks(BiConsumer<ResourceLocation, Block> regFunc) {
//        Block block = new Block(BlockBehaviour.Properties.of().noOcclusion().isRedstoneConductor((a, b, c) -> false));
//        regFunc.accept(
//            McHelper.newResourceLocation("immersive_portals", "portal_helper"),
//                block
//        );
//        portalHelperBlock = block;
    }

    public static void registerChunkGenerators(
        BiConsumer<ResourceLocation, MapCodec<? extends ChunkGenerator>> regFunc
    ) {
        regFunc.accept(
            McHelper.newResourceLocation("immersive_portals:error_terrain_generator"),
            ErrorTerrainGenerator.MAP_CODEC
        );
        regFunc.accept(
            McHelper.newResourceLocation("immersive_portals:normal_skyland_generator"),
            NormalSkylandGenerator.MAP_CODEC
        );
    }

    public static void registerBiomeSources(
        BiConsumer<ResourceLocation, MapCodec<? extends BiomeSource>> regFunc
    ) {
        regFunc.accept(
            McHelper.newResourceLocation("immersive_portals:chaos_biome_source"),
            ChaosBiomeSource.MAP_CODEC
        );
    }

    public static void registerCreativeTabs(
        BiConsumer<ResourceLocation, CreativeModeTab> regFunc
    ) {
        regFunc.accept(
            McHelper.newResourceLocation("immersive_portals", "general"),
            TAB
        );
    }
}
