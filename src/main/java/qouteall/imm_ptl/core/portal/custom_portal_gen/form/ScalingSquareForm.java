package qouteall.imm_ptl.core.portal.custom_portal_gen.form;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import qouteall.imm_ptl.core.portal.custom_portal_gen.PortalGenInfo;
import qouteall.imm_ptl.core.portal.nether_portal.BlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.FastBlockPortalShape;
import qouteall.imm_ptl.core.portal.nether_portal.FrameSearching;
import qouteall.imm_ptl.core.portal.nether_portal.NetherPortalGeneration;
import qouteall.q_misc_util.my_util.IntBox;

import java.util.function.Predicate;

public class ScalingSquareForm extends NetherPortalLikeForm {
    public static final MapCodec<ScalingSquareForm> CODEC = RecordCodecBuilder.mapCodec(instance -> {
        return instance.group(
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("from_frame_block").forGetter(o -> o.fromFrameBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("area_block").forGetter(o -> o.areaBlock),
            BuiltInRegistries.BLOCK.byNameCodec().fieldOf("to_frame_block").forGetter(o -> o.toFrameBlock),
            Codec.INT.fieldOf("from_length").forGetter(o -> o.fromLength),
            Codec.INT.fieldOf("to_length").forGetter(o -> o.toLength),
            Codec.BOOL.fieldOf("generate_frame_if_not_found").forGetter(o -> o.generateFrameIfNotFound)
        ).apply(instance, instance.stable(ScalingSquareForm::new));
    });
    
    public final Block fromFrameBlock;
    public final Block areaBlock;
    public final Block toFrameBlock;
    public final int fromLength;
    public final int toLength;
    
    public ScalingSquareForm(
        Block fromFrameBlock, Block areaBlock, Block toFrameBlock,
        int fromLength, int toLength, boolean generateFrameIfNotFound
    ) {
        super(generateFrameIfNotFound);
        this.fromFrameBlock = fromFrameBlock;
        this.areaBlock = areaBlock;
        this.toFrameBlock = toFrameBlock;
        this.fromLength = fromLength;
        this.toLength = toLength;
    }
    
    @Override
    public MapCodec<? extends PortalGenForm> getCodec() {
        return CODEC;
    }
    
    @Override
    public PortalGenForm getReverse() {
        return new ScalingSquareForm(
            toFrameBlock, areaBlock, fromFrameBlock,
            toLength, fromLength, generateFrameIfNotFound
        );
    }
    
    @Override
    public boolean testThisSideShape(ServerLevel fromWorld, BlockPortalShape fromShape) {
        boolean isSquareShape = BlockPortalShape.isSquareShape(fromShape, fromLength);
        return isSquareShape;
    }
    
    @Override
    public FrameSearching.FrameSearchingFunc<PortalGenInfo> getFrameMatchingFunc(
        ServerLevel fromWorld, ServerLevel toWorld, BlockPortalShape fromShape
    ) {
        BlockPortalShape template = getTemplateToShape(fromShape);
        FastBlockPortalShape fastTemplate = FastBlockPortalShape.fromBlockPortalShape(template);
        
        Predicate<BlockState> areaPredicate = getAreaPredicate();
        Predicate<BlockState> otherSideFramePredicate = getOtherSideFramePredicate();
        BlockPos.MutableBlockPos temp2 = new BlockPos.MutableBlockPos();
        return (blockAccess, x, y, z) -> {
            boolean matches = fastTemplate.matchShape(
                x, y, z,
                (px, py, pz) -> otherSideFramePredicate.test(blockAccess.getBlockState(px, py, pz)),
                (px, py, pz) -> areaPredicate.test(blockAccess.getBlockState(px, py, pz))
            );
            if (matches) {
                boolean matchToSelf = fromWorld == toWorld
                    && x == fastTemplate.basePosX()
                    && y == fastTemplate.basePosY()
                    && z == fastTemplate.basePosZ();
                
                if (!matchToSelf) {
                    return new PortalGenInfo(
                        fromWorld.dimension(),
                        toWorld.dimension(),
                        fromShape, fastTemplate.withNewBase(x, y, z).toBlockPortalShape(),
                        null,
                        getScale()
                    );
                }
            }
            return null;
        };
    }
    
    private double getScale() {
        return ((double) toLength) / fromLength;
    }
    
    private BlockPortalShape getTemplateToShape(BlockPortalShape fromShape) {
        return BlockPortalShape.getSquareShapeTemplate(
            fromShape.axis,
            toLength
        );
    }
    
    @Override
    public void generateNewFrame(ServerLevel fromWorld, BlockPortalShape fromShape, ServerLevel toWorld, BlockPortalShape toShape) {
        for (BlockPos blockPos : toShape.frameAreaWithCorner) {
            toWorld.setBlockAndUpdate(blockPos, toFrameBlock.defaultBlockState());
        }
    }
    
    @Override
    public @Nullable PortalGenInfo getNewPortalPlacement(ServerLevel toWorld, BlockPos toPos, ServerLevel fromWorld, BlockPortalShape fromShape, @Nullable Entity triggeringEntity) {
        BlockPortalShape templateShape = getTemplateToShape(fromShape);
        @Nullable IntBox airCubePlacement =
            NetherPortalGeneration.findAirCubePlacement(
                toWorld, toPos,
                templateShape.axis, templateShape.totalAreaBox.getSize(),
                true
            );
        
        if (airCubePlacement == null) {
            return null;
        }
        
        BlockPortalShape placedShape = templateShape.getShapeWithMovedTotalAreaBox(
            airCubePlacement
        );
        
        return new PortalGenInfo(
            fromWorld.dimension(),
            toWorld.dimension(),
            fromShape,
            placedShape,
            null,
            getScale()
        );
    }
    
    @Override
    public Predicate<BlockState> getOtherSideFramePredicate() {
        return s -> s.getBlock() == toFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getThisSideFramePredicate() {
        return s -> s.getBlock() == fromFrameBlock;
    }
    
    @Override
    public Predicate<BlockState> getAreaPredicate() {
        return s -> s.getBlock() == areaBlock;
    }
}
