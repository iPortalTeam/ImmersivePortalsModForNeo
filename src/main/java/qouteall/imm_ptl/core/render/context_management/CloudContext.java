package qouteall.imm_ptl.core.render.context_management;

import com.mojang.blaze3d.vertex.VertexBuffer;
import de.nick1st.imm_ptl.events.ClientCleanupEvent;
import de.nick1st.imm_ptl.events.DimensionEvents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import qouteall.q_misc_util.Helper;

import java.util.ArrayList;

public class CloudContext {
    
    //keys
    public int lastCloudsBlockX = 0;
    public int lastCloudsBlockY = 0;
    public int lastCloudsBlockZ = 0;
    public ResourceKey<Level> dimension = null;
    public Vec3 cloudColor;
    
    public VertexBuffer cloudsBuffer = null;
    
    public static final ArrayList<CloudContext> contexts = new ArrayList<>();
    
    public static void init() {
        NeoForge.EVENT_BUS.addListener(ClientCleanupEvent.class, e -> CloudContext.cleanup());
        NeoForge.EVENT_BUS.addListener(DimensionEvents.CLIENT_DIMENSION_DYNAMIC_REMOVE_EVENT.class, e -> CloudContext.cleanup());
    }
    
    public CloudContext() {
    
    }
    
    private static void cleanup() {
        for (CloudContext context : contexts) {
            context.dispose();
        }
        contexts.clear();
    }
    
    public void dispose() {
        if (cloudsBuffer != null) {
            cloudsBuffer.close();
            cloudsBuffer = null;
        }
    }
    
    @Nullable
    public static CloudContext findAndTakeContext(
        int lastCloudsBlockX, int lastCloudsBlockY, int lastCloudsBlockZ,
        ResourceKey<Level> dimension, Vec3 cloudColor
    ) {
        int i = Helper.indexOf(contexts, c ->
            c.lastCloudsBlockX == lastCloudsBlockX &&
                c.lastCloudsBlockY == lastCloudsBlockY &&
                c.lastCloudsBlockZ == lastCloudsBlockZ &&
                c.dimension == dimension &&
                c.cloudColor.distanceToSqr(cloudColor) < 2.0E-4D
        );
        
        if (i == -1) {
            return null;
        }
        
        CloudContext result = contexts.get(i);
        contexts.remove(i);
        
        return result;
    }
    
    public static void appendContext(CloudContext context) {
        contexts.add(context);
        
        if (contexts.size() > 15) {
            contexts.remove(0).dispose();
        }
    }
}
