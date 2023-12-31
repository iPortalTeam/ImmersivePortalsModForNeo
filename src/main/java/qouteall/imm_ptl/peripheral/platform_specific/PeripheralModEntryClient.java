package qouteall.imm_ptl.peripheral.platform_specific;

import qouteall.imm_ptl.peripheral.PeripheralModMain;

public class PeripheralModEntryClient {
    public static void registerBlockRenderLayers() {
//        BlockRenderLayerMap.INSTANCE.putBlock(
//            PeripheralModMain.portalHelperBlock,
//            RenderType.cutout()
//        );
    }

    public void onInitializeClient() {
        PeripheralModEntryClient.registerBlockRenderLayers();
        
        PeripheralModMain.initClient();
    }
}
