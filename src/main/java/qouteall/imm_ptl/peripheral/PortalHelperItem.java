package qouteall.imm_ptl.peripheral;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PortalHelperItem extends BlockItem {
    private static boolean deprecationInformed = false;

    public PortalHelperItem(Block block, Properties settings) {
        super(block, settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide()) {
            if (context.getPlayer() != null) {
                if (!deprecationInformed) {
                    deprecationInformed = true;
                    context.getPlayer().sendSystemMessage(
                            Component.translatable(
                                    "imm_ptl.portal_helper_deprecated",
                                    Component.literal("/portal shape sculpt")
                                            .withStyle(ChatFormatting.GOLD)
                            )
                    );
                }
            }
        }

        return super.useOn(context);
    }

    @Override
    public void appendHoverText(ItemStack pStack, TooltipContext pContext, List<Component> tooltip, TooltipFlag pTooltipFlag) {
        super.appendHoverText(pStack, pContext, tooltip, pTooltipFlag);
        tooltip.add(Component.translatable("imm_ptl.portal_helper_tooltip"));
    }
}
