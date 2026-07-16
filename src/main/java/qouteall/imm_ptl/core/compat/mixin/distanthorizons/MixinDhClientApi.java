package qouteall.imm_ptl.core.compat.mixin.distanthorizons;

import com.seibel.distanthorizons.core.api.internal.ClientApi;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import qouteall.imm_ptl.core.render.context_management.PortalRendering;

/**
 * Distant Horizons' shader-pipeline LOD render call (invoked from within Iris's
 * composite/deferred passes) does synchronous LOD data prep for whatever view is
 * currently being rendered. When Immersive Portals renders a portal's alternate-world
 * content, this gets invoked a second time for that separate view, and DH generating
 * LOD data on demand for a viewpoint it hasn't cached causes multi-hundred-millisecond
 * frame stalls (confirmed via the client render profiler: a single tick spent 98% of a
 * 937ms frame inside this call while portal content was rendering). Skip it during
 * portal rendering -- normal LOD rendering for the player's actual surroundings is
 * unaffected since PortalRendering.isRendering() is only true during that sub-pass.
 */
@Mixin(value = ClientApi.class, remap = false)
public class MixinDhClientApi {
    // Both public entry points end up calling the private renderLodLayer(boolean), which is
    // where the actual expensive LOD work happens -- Iris's opaque/terrain composite pass goes
    // through renderLods() while its translucent pass goes through renderDeferredLodsForShaders(),
    // so both need cancelling during portal rendering, not just one.
    @Inject(method = "renderLods", at = @At("HEAD"), cancellable = true)
    private void onRenderLods(CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderDeferredLodsForShaders", at = @At("HEAD"), cancellable = true)
    private void onRenderDeferredLodsForShaders(CallbackInfo ci) {
        if (PortalRendering.isRendering()) {
            ci.cancel();
        }
    }
}
