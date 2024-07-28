package qouteall.imm_ptl.core.render;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Direction;
import net.minecraft.util.Tuple;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import qouteall.imm_ptl.core.compat.GravityChangerInterface;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.imm_ptl.core.render.context_management.WorldRenderInfo;
import qouteall.q_misc_util.Helper;
import qouteall.q_misc_util.my_util.DQuaternion;

/**
 * The camera rotations are applied in this order:
 * 1. portal transformation (rotation and mirror)
 * 2. animation delta
 * 3. gravity rotation
 * 4. raw camera rotation (by pitch and yaw)
 * The right side of multiplication applies first.
 * finalRot = rawCameraRotation * gravity * animationDelta * portalRot
 */
@OnlyIn(Dist.CLIENT)
public class TransformationManager {
    
    // the animation delta gradually reduces to identity
    private static DQuaternion animationDeltaStart;
    
    private static long interpolationStartTime = 0;
    private static long interpolationEndTime = 1;
    
    public static final Minecraft client = Minecraft.getInstance();
    
    public static boolean isIsometricView = false;
    public static float isometricViewLength = 50;
    
    public static DQuaternion getPlayerCameraRotation() {
        LocalPlayer player = client.player;
        if (player == null) {
            return DQuaternion.identity;
        }
        
        Direction gravity = GravityChangerInterface.invoker.getGravityDirection(player);
        
        return getCameraRotationWithGravity(
            gravity, player.getXRot(), player.getYRot()
        );
    }
    
    // gets rawCameraRotation * gravity
    private static DQuaternion getCameraRotationWithGravity(
        Direction gravityDirection,
        float pitch, float yaw
    ) {
        DQuaternion gravity = GravityChangerInterface.invoker.getExtraCameraRotation(gravityDirection);
        DQuaternion rawCameraRotation = DQuaternion.getCameraRotation(pitch, yaw);
        if (gravity == null) {
            return rawCameraRotation;
        }
        else {
            return rawCameraRotation.hamiltonProduct(gravity);
        }
    }
    
    @Nullable
    private static DQuaternion getCurrentAnimationDelta() {
        Double animationProgress = getAnimationProgress();
        if (animationProgress != null) {
            double progress = animationProgress;
            return DQuaternion.interpolate(
                animationDeltaStart,
                DQuaternion.identity,
                mapProgress(progress)
            );
        }
        return null;
    }
    
    public static void processTransformation(Camera camera, PoseStack matrixStack) {
        DQuaternion currentAnimationDelta = getCurrentAnimationDelta();
        if (currentAnimationDelta != null) {
            matrixStack.mulPose(currentAnimationDelta.toMcQuaternion());
        }
        
        WorldRenderInfo.applyAdditionalTransformations(matrixStack);
    }
    
    public static Matrix4f processTransformation(Camera camera, Matrix4f matrix4f) {
        PoseStack poseStack = new PoseStack();
        poseStack.last().pose().set(matrix4f);
        processTransformation(camera, poseStack);
        return poseStack.last().pose();
    }
    
    public static boolean isAnimationRunning() {
        return getAnimationProgress() != null;
    }
    
    @Nullable
    public static Double getAnimationProgress() {
        if (interpolationStartTime == 0) {
            return null;
        }
        if (animationDeltaStart == null) {
            return null;
        }
        
        double progress = (RenderStates.renderStartNanoTime - interpolationStartTime) /
            ((double) interpolationEndTime - interpolationStartTime);
        
        if (progress >= 0 && progress <= 1) {
            return progress;
        }
        else {
            return null;
        }
    }
    
    public static double mapProgress(double progress) {
//        return progress;
        return Math.sin(progress * (Math.PI / 2));
//        return Math.sqrt(1 - (1 - progress) * (1 - progress));
    }
    
    // this may change player velocity, must change it back
    public static void managePlayerRotationAndChangeGravity(
        Portal portal
    ) {
        if (portal.getRotation() != null) {
            LocalPlayer player = client.player;
            
            // finalRot = rawCameraRotation * gravity * animationDelta * portalRot
            
            Direction oldGravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
            
            DQuaternion oldCameraRotation = getCameraRotationWithGravity(
                oldGravityDir,
                player.getViewXRot(RenderStates.getPartialTick()), player.getViewYRot(RenderStates.getPartialTick())
            );
            DQuaternion currentAnimationDelta = getCurrentAnimationDelta();
            if (currentAnimationDelta != null) {
                oldCameraRotation = oldCameraRotation.hamiltonProduct(currentAnimationDelta);
            }
            
            DQuaternion immediateFinalRot =
                oldCameraRotation.hamiltonProduct(
                    portal.getRotation().getConjugated()
                );
            
            Direction oldBaseGravityDir = GravityChangerInterface.invoker.getBaseGravityDirection(player);
            Direction newBaseGravityDir = portal.getTeleportedGravityDirection(oldBaseGravityDir);
            
            if (newBaseGravityDir != oldBaseGravityDir) {
                GravityChangerInterface.invoker.setClientPlayerGravityDirection(
                    player, newBaseGravityDir
                );
            }
            
            // if there is some gravity effect
            // the immediate gravity direction may be different to base gravity direction
            Direction immediateNewGravityDir = GravityChangerInterface.invoker.getGravityDirection(player);
            
            // rawCameraRotation = finalRot * portalRot^-1 * animationDelta^-1 * gravity^-1
            // when getting the new pitch yaw, no need to consider portalRot and animation
            // rawCameraRotation = finalRot * gravity^-1
            
            DQuaternion newGravityRot = DQuaternion.fromNullable(
                GravityChangerInterface.invoker.getExtraCameraRotation(immediateNewGravityDir)
            );
            
            DQuaternion newRawCameraRotation = immediateFinalRot.hamiltonProduct(newGravityRot.getConjugated());
            
            Tuple<Double, Double> pitchYaw =
                DQuaternion.getPitchYawFromRotation(newRawCameraRotation);
            
            float finalYaw = (float) (double) (pitchYaw.getB());
            float finalPitch = (float) (double) (pitchYaw.getA());
            
            if (finalPitch > 90) {
                finalPitch = 90 - (finalPitch - 90);
            }
            else if (finalPitch < -90) {
                finalPitch = -90 + (-90 - finalPitch);
            }
            
            player.setYRot(finalYaw);
            player.setXRot(finalPitch);
            
            player.yRotO = finalYaw;
            player.xRotO = finalPitch;
            player.yBob = finalYaw;
            player.xBob = finalPitch;
            player.yBobO = finalYaw;
            player.xBobO = finalPitch;
            
            // now we need to keep immediate final rotation unchanged, to keep teleportation seamless.
            // no need to consider portalRot for now.
            // finalRot = rawCameraRotation * gravity * animationDelta
            // animationDelta = gravity^-1 * rawCameraRotation^-1 * finalRot
            // animationDelta = (rawCameraRotation * gravity)^-1 * finalRot
            
            DQuaternion newCameraRotationWithGravity = getCameraRotationWithGravity(immediateNewGravityDir, finalPitch, finalYaw);
            
            DQuaternion newAnimationDelta = newCameraRotationWithGravity.getConjugated().hamiltonProduct(immediateFinalRot);
            
            if (newAnimationDelta.getRotatingAngleDegrees() > 0.1) {
                animationDeltaStart = newAnimationDelta;
                interpolationStartTime = RenderStates.renderStartNanoTime;
                interpolationEndTime = interpolationStartTime +
                    Helper.secondToNano(getAnimationDurationSeconds());
            }
            
            updateCamera(client);
        }
    }
    
    private static double getAnimationDurationSeconds() {
        return 1;
    }
    
    private static void updateCamera(Minecraft client) {
        Camera camera = client.gameRenderer.getMainCamera();
        camera.setup(
            client.level,
            client.player,
            !client.options.getCameraType().isFirstPerson(),
            client.options.getCameraType().isMirrored(),
            RenderStates.getPartialTick()
        );
    }
    
    public static Matrix4f getMirrorTransformation(Vec3 normal) {
        float x = (float) normal.x;
        float y = (float) normal.y;
        float z = (float) normal.z;
//        float[] arr = new float[]{
//            1 - 2 * x * x, 0 - 2 * x * y, 0 - 2 * x * z, 0,
//            0 - 2 * y * x, 1 - 2 * y * y, 0 - 2 * y * z, 0,
//            0 - 2 * z * x, 0 - 2 * z * y, 1 - 2 * z * z, 0,
//            0, 0, 0, 1
//        };
        Matrix4f matrix = new Matrix4f();
//        matrix.set(arr);
        matrix.reflection(x, y, z, 0);
        return matrix;
    }
    
    // https://docs.microsoft.com/en-us/windows/win32/opengl/glortho
    public static Matrix4f getIsometricProjection() {
        int w = client.getWindow().getWidth();
        int h = client.getWindow().getHeight();
        
        float wView = (isometricViewLength / h) * w;
        
        float near = -2000;
        float far = 2000;
        
        float left = -wView / 2;
        float right = wView / 2;
        
        float top = isometricViewLength / 2;
        float bottom = -isometricViewLength / 2;
        
        float[] arr = new float[]{
            2.0f / (right - left), 0, 0, -(right + left) / (right - left),
            0, 2.0f / (top - bottom), 0, -(top + bottom) / (top - bottom),
            0, 0, -2.0f / (far - near), -(far + near) / (far - near),
            0, 0, 0, 1
        };
        Matrix4f m1 = new Matrix4f();
        m1.set(arr);
        
        return m1;
    }
    
    public static boolean isCalculatingViewBobbingOffset = false;
    
    @OnlyIn(Dist.CLIENT)
    public static class RemoteCallables {
        public static void enableIsometricView(float viewLength) {
            isometricViewLength = viewLength;
            isIsometricView = true;
            
            client.smartCull = false;
        }
        
        public static void disableIsometricView() {
            isIsometricView = false;
            
            client.smartCull = true;
        }
    }
    
    // isometric is equivalent to the camera being in infinitely far place
    public static Vec3 getIsometricAdjustedCameraPos() {
        Camera camera = client.gameRenderer.getMainCamera();
        return getIsometricAdjustedCameraPos(camera);
    }
    
    public static Vec3 getIsometricAdjustedCameraPos(Camera camera) {
        Vec3 cameraPos = camera.getPosition();
        
        if (!isIsometricView) {
            return cameraPos;
        }
        
        Quaternionf rotation = camera.rotation();
        Vector3f vec = new Vector3f(0, 0, client.options.getEffectiveRenderDistance() * -10);
        rotation.transform(vec);
        
        return cameraPos.add(vec.x(), vec.y(), vec.z());
    }
}
