package qouteall.imm_ptl.peripheral.mixin.common.dfu;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.OptionalDynamic;
import net.minecraft.util.datafix.fixes.ItemStackComponentizationFix;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStackComponentizationFix.class)
public class MixinItemStackComponentizationFix {
    @Unique
    private static final Logger LOGGER = LogManager.getLogger("iPortal_DFU");
    
    @Inject(method = "fixItemStack", at = @At("RETURN"))
    private static void onFixItemStack(
        ItemStackComponentizationFix.ItemStackData itemStackData, Dynamic<?> tag, CallbackInfo ci
    ) {
        if (itemStackData.is("immersive_portals:command_stick")) {
            var commandTag = itemStackData.removeTag("command").result();
            var nameTranslationKeyTag =
                itemStackData.removeTag("nameTranslationKey").result();
            var descriptionTranslationKeysTag =
                itemStackData.removeTag("descriptionTranslationKeys").result();
            
            if (commandTag.isEmpty() || nameTranslationKeyTag.isEmpty() || descriptionTranslationKeysTag.isEmpty()) {
                LOGGER.error("Broken command stick item data {} {} {}", commandTag, nameTranslationKeyTag, descriptionTranslationKeysTag);
            } else {
                Dynamic<?> componentData = tag.createMap(
                    ImmutableMap.of(
                        tag.createString("command"),
                        commandTag.get(),
                        tag.createString("nameTranslationKey"),
                        nameTranslationKeyTag.get(),
                        tag.createString("descriptionTranslationKeys"),
                        descriptionTranslationKeysTag.get()
                    )
                );
                itemStackData.setComponent(
                    "iportal:command_stick_data", componentData
                );
                LOGGER.info("Fixed command stick item data {}", componentData);
            }
        }
        
        if (itemStackData.is("immersive_portals:portal_wand")) {
            itemStackData.moveTagToComponent("mode", "iportal:portal_wand_data");
        }
    }
}
