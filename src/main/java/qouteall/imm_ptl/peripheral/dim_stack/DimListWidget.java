package qouteall.imm_ptl.peripheral.dim_stack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

public class DimListWidget extends AbstractSelectionList<DimEntryWidget> {
    
    public static final int ROW_WIDTH = 300;
    
    public interface DraggingCallback {
        void run(int selectedIndex, int mouseOnIndex);
    }
    
    public final Screen parent;
    private final Type type;
    @Nullable
    private final DraggingCallback draggingCallback;
    
    
    public enum Type {
        mainDimensionList, addDimensionList
    }
    
    public DimListWidget(
        int width,
        int height,
        int top,
        int itemHeight,
        Screen parent,
        Type type,
        @Nullable DraggingCallback draggingCallback
    ) {
        super(Minecraft.getInstance(), width, height, top, itemHeight);
        this.parent = parent;
        this.type = type;
        this.draggingCallback = draggingCallback;
    }
    
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (type == Type.mainDimensionList && draggingCallback != null) {
            DimEntryWidget selected = getSelected();
        
            if (selected != null) {
                DimEntryWidget mouseOn = getEntryAtPosition(mouseX, mouseY);
                if (mouseOn != null) {
                    if (mouseOn != selected) {
                        int selectedIndex = children().indexOf(selected);
                        int mouseOnIndex = children().indexOf(mouseOn);
                        if (selectedIndex != -1 && mouseOnIndex != -1) {
                            draggingCallback.run(selectedIndex, mouseOnIndex);
                        }
                        else {
                            Helper.err("Invalid dragging");
                        }
                    }
                }
            }
        }
        
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }
    
    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    
    }
    
    // make it wider
    @Override
    public int getRowWidth() {
        return ROW_WIDTH;
    }
    
    @Override
    protected int getScrollbarPosition() {
        return (width - ROW_WIDTH) / 2 + ROW_WIDTH;
    }
}
