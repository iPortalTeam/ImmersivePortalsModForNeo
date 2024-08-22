package qouteall.imm_ptl.core.miscellaneous;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ImageWidget;
import net.minecraft.client.gui.components.MultiLineTextWidget;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import qouteall.imm_ptl.core.platform_specific.IPConfig;

public class IPortalInitialScreen extends Screen {
    private final Runnable onClose;
    
    private final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(
        this, 80, 33
    );
    
    private final Button iKnowButton;
    
    private final ImageWidget iconWidget;
    private final StringWidget titleWidget;
    
    private final MultiLineTextWidget contentWidget;
    
    public IPortalInitialScreen(Runnable onClose) {
        super(Component.empty());
        this.onClose = onClose;
        
        this.minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        
        iKnowButton = Button.builder(
            Component.translatable("iportal.initial_screen.i_know"),
            button -> onClose()
        ).build();
        
        iconWidget = ImageWidget.texture(
            30, 30,
            ResourceLocation.fromNamespaceAndPath("immersive_portals", "icon.png"),
            30, 30
        );
        
        titleWidget = new StringWidget(
            Component.translatable("iportal.initial_screen.title"),
            font
        ).alignCenter();
        
        contentWidget = new MultiLineTextWidget(
            Component.translatable("iportal.initial_screen.content"),
            font
        );
    }
    
    @Override
    public void onClose() {
        IPConfig config = IPConfig.getConfig();
        config.initialScreenShown = true;
        config.saveConfigFile();
        
        onClose.run();
    }
    
    @Override
    public void init() {
        contentWidget.setMaxWidth(this.width - 20);
        
        addRenderableWidget(iKnowButton);
        addRenderableWidget(iconWidget);
        addRenderableWidget(titleWidget);
        addRenderableWidget(contentWidget);
        
        LinearLayout headerLayout = layout.addToHeader(LinearLayout.horizontal());
        headerLayout.addChild(iconWidget, headerLayout.defaultCellSetting().padding(5));
        headerLayout.addChild(titleWidget, headerLayout.defaultCellSetting().padding(5));
        
        LinearLayout contentLayout = layout.addToContents(LinearLayout.vertical());
        contentLayout.addChild(contentWidget, headerLayout.defaultCellSetting().padding(5));
        
        layout.addToFooter(iKnowButton);
        
        layout.arrangeElements();
    }
}
