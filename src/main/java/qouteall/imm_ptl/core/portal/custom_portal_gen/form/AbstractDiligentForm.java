package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.FastBlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.FrameSearching;

import java.util.List;
import java.util.function.Predicate;

public abstract class AbstractDiligentForm extends NetherPortalLikeForm {
    public AbstractDiligentForm(boolean generateFrameIfNotFound) {
        super(generateFrameIfNotFound);
    }
    
    @Override
    public FrameSearching.FrameSearchingFunc<PortalGenInfo> getFrameMatchingFunc(
        ServerLevel fromWorld, ServerLevel toWorld, BlockPortalShape fromShape
    ) {
        List<DiligentMatcher.TransformedShape> matchableShapeVariants =
            DiligentMatcher.getMatchableShapeVariants(fromShape, BlockPortalShape.defaultLengthLimit);
        
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        
        return (blockAccess, x, y, z) -> {
            for (DiligentMatcher.TransformedShape matchableShapeVariant : matchableShapeVariants) {
                FastBlockPortalShape template = matchableShapeVariant.fastTransformedShape;
                boolean matches = template.matchShape(
                    x, y, z,
                    (px, py, pz) -> otherSideFramePredicate.test(blockAccess.getBlockState(px, py, pz)),
                    (px, py, pz) -> areaPredicate.test(blockAccess.getBlockState(px, py, pz))
                );
                
                if (matches) {
                    boolean matchToSelf = fromWorld == toWorld
                        && x == template.basePosX()
                        && y == template.basePosY()
                        && z == template.basePosZ();
                    
                    if (!matchToSelf) {
                        FastBlockPortalShape moved = template.withNewBase(x, y, z);
                        
                        return new PortalGenInfo(
                            fromWorld.dimension(),
                            toWorld.dimension(),
                            fromShape, moved.toBlockPortalShape(),
                            matchableShapeVariant.rotation.toQuaternion(),
                            matchableShapeVariant.scale
                        );
                    }
                }
            }
            
            return null;
        };
    }
}
