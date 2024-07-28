package qouteall.imm_ptl.core.mc_utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import qouteall.imm_ptl.core.CHelper;
import qouteall.imm_ptl.core.portal.Portal;
import qouteall.imm_ptl.core.portal.animation.StableClientTimer;
import qouteall.imm_ptl.core.portal.animation.UnilateralPortalState;
import qouteall.imm_ptl.core.portal.shape.PortalShape;
import qouteall.imm_ptl.core.portal.shape.SpecialFlatPortalShape;
import qouteall.imm_ptl.core.render.context_management.RenderStates;
import qouteall.q_misc_util.my_util.Circle;
import qouteall.q_misc_util.my_util.DQuaternion;
import qouteall.q_misc_util.my_util.Plane;
import qouteall.q_misc_util.my_util.Sphere;

import java.util.Random;
import java.util.function.IntSupplier;

//@OnlyIn(Dist.CLIENT)
public class WireRenderingHelper {
    
    public static void renderSmallCubeFrame(
        VertexConsumer vertexConsumer, Vec3 cameraPos, Vec3 boxCenter,
        int color, double scale,
        PoseStack matrixStack
    ) {
        Random random = new Random(color);
        
        double boxSize = Math.pow(boxCenter.distanceTo(cameraPos), 0.3) * 0.09;
        
        matrixStack.pushPose();
        matrixStack.translate(
            boxCenter.x - cameraPos.x,
            boxCenter.y - cameraPos.y,
            boxCenter.z - cameraPos.z
        );
        
        matrixStack.scale((float) scale, (float) scale, (float) scale);
        
        DQuaternion rotation = getRandomSmoothRotation(random);
        
        double periodLen = 100;
        
        matrixStack.mulPose(rotation.toMcQuaternion());
        Matrix4f matrix = matrixStack.last().pose();
        
        float alpha = ((color >> 24) & 0xff) / 255f;
        float red = ((color >> 16) & 0xff) / 255f;
        float green = ((color >> 8) & 0xff) / 255f;
        float blue = (color & 0xff) / 255f;
        
        LevelRenderer.renderLineBox(
            matrixStack,
            vertexConsumer,
            -boxSize / 2,
            -boxSize / 2,
            -boxSize / 2,
            boxSize / 2,
            boxSize / 2,
            boxSize / 2,
            red, green, blue, alpha
        );
        matrixStack.popPose();
    }
    
    public static DQuaternion getRandomSmoothRotation(Random random) {
        double time = StableClientTimer.getStableTickTime() + (double) StableClientTimer.getStablePartialTicks();
        
        DQuaternion rotation = DQuaternion.identity;
        
        for (int i = 0; i < 6; i++) {
            rotation = rotation.hamiltonProduct(
                DQuaternion.rotationByDegrees(
                    randomVec(random), CHelper.getSmoothCycles(random.nextInt(30, 60)) * 360
                )
            );
        }
        
        return rotation;
    }
    
    public static double getRandomSmoothCycle(Random random) {
        double totalFactor = 0.1;
        double total = 0;
        for (int i = 0; i < 5; i++) {
            double smoothCycle = CHelper.getSmoothCycles(random.nextInt(30, 300));
            double sin = Math.sin(2 * Math.PI * smoothCycle);
            double factor = random.nextDouble(0.1, 1);
            totalFactor += factor;
            total += sin * factor;
        }
        
        return total / totalFactor;
    }
    
    @NotNull
    public static Vec3 randomVec(Random random) {
        return new Vec3(random.nextDouble() - 0.5, random.nextDouble() - 0.5, random.nextDouble() - 0.5);
    }
    
    public static void renderPlane(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Plane plane, double renderedPlaneScale,
        int color, PoseStack matrixStack,
        boolean isLineStrip
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 planeCenter = plane.pos();
        Vec3 normal = plane.normal();
        
        Vec3 anyVecNonNormal = new Vec3(13, 29, 71).normalize();
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.99) {
            anyVecNonNormal = new Vec3(1, 0, 0);
        }
        
        Vec3 planeX = normal.cross(anyVecNonNormal).normalize();
        Vec3 planeY = normal.cross(planeX).normalize();
        
        matrixStack.pushPose();
        matrixStack.translate(
            planeCenter.x - cameraPos.x,
            planeCenter.y - cameraPos.y,
            planeCenter.z - cameraPos.z
        );
        
        matrixStack.mulPose(
            DQuaternion.rotationByDegrees(normal, CHelper.getSmoothCycles(211) * 360)
                .toMcQuaternion()
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        double cameraDistanceToCenter = player.getEyePosition(RenderStates.getPartialTick())
            .distanceTo(planeCenter);
        
        int lineNumPerSide = 10;
        double lineInterval = cameraDistanceToCenter * 0.2 * renderedPlaneScale;
        double lineLenPerSide = lineNumPerSide * lineInterval;
        
        for (int ix = -lineNumPerSide; ix <= lineNumPerSide; ix++) {
            Vec3 lineStart = planeX.scale(ix * lineInterval)
                .add(planeY.scale(-lineLenPerSide));
            Vec3 lineEnd = planeX.scale(ix * lineInterval)
                .add(planeY.scale(lineLenPerSide));
            
            if (isLineStrip) {
                putLineToLineStrip(vertexConsumer, color, planeY, matrix, lineStart, lineEnd);
            }
            else {
                putLine(
                    vertexConsumer, color, planeY, matrix, matrixStack.last().normal(),
                    lineStart, lineEnd
                );
            }
        }
        
        for (int iy = -lineNumPerSide; iy <= lineNumPerSide; iy++) {
            Vec3 lineStart = planeY.scale(iy * lineInterval)
                .add(planeX.scale(-lineLenPerSide));
            Vec3 lineEnd = planeY.scale(iy * lineInterval)
                .add(planeX.scale(lineLenPerSide));
            
            if (isLineStrip) {
                putLineToLineStrip(vertexConsumer, color, planeX, matrix, lineStart, lineEnd);
            }
            else {
                putLine(
                    vertexConsumer, color, planeX, matrix, matrixStack.last().normal(),
                    lineStart, lineEnd
                );
            }
        }
        
        matrixStack.popPose();
    }
    
    // NOTE it uses line strip
    public static void renderCircle(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Circle circle,
        int color, PoseStack matrixStack
    ) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        
        Vec3 planeCenter = circle.plane().pos();
        Vec3 normal = circle.plane().normal();
        
        Vec3 anyVecNonNormal = new Vec3(0, 1, 0);
        if (Math.abs(normal.dot(anyVecNonNormal)) > 0.9) {
            anyVecNonNormal = new Vec3(1, 0, 0);
        }
        
        Vec3 planeX = normal.cross(anyVecNonNormal).normalize();
        Vec3 planeY = normal.cross(planeX).normalize();
        
        Vec3 circleCenter = circle.circleCenter();
        double circleRadius = circle.radius();
        
        matrixStack.pushPose();
        
        matrixStack.translate(
            circleCenter.x - cameraPos.x,
            circleCenter.y - cameraPos.y,
            circleCenter.z - cameraPos.z
        );
        
        Matrix4f matrix = matrixStack.last().pose();
        
        int vertexNum = Mth.clamp((int) Math.round(circleRadius * 40), 40, 400);
        
        for (int i = 0; i < vertexNum; i++) {
            double angle = i * 2 * Math.PI / vertexNum;
            double nextAngle = (i + 1) * 2 * Math.PI / vertexNum;
            boolean isBegin = i == 0;
            boolean isEnd = i == vertexNum - 1;
            
            Vec3 lineStart = planeX.scale(Math.cos(angle) * circleRadius)
                .add(planeY.scale(Math.sin(angle) * circleRadius));
            Vec3 lineEnd = planeX.scale(Math.cos(nextAngle) * circleRadius)
                .add(planeY.scale(Math.sin(nextAngle) * circleRadius));
            
            if (isBegin) {
                vertexConsumer
                    .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
                    .color(0)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
                
                vertexConsumer
                    .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
                    .color(color)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
            }
            
            vertexConsumer
                .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
                .color(color)
                .normal((float) normal.x, (float) normal.y, (float) normal.z)
                .endVertex();
            
            if (isEnd) {
                vertexConsumer
                    .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
                    .color(0)
                    .normal((float) normal.x, (float) normal.y, (float) normal.z)
                    .endVertex();
            }
        }
        
        matrixStack.popPose();
    }
    
    
    public static void renderLockShape(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        Vec3 center, double scale,
        int color, PoseStack matrixStack
    ) {
        double w = 380;
        double h = 270;
        double ringWidth = 60;
        double ringAreaWidth = 152;
        double rightAreaHeight = 136;
        
        Vec3[] lineVertices = new Vec3[]{
            // the body
            new Vec3(w / 2, h / 2, 0),
            new Vec3(-w / 2, h / 2, 0),
            new Vec3(-w / 2, h / 2, 0),
            new Vec3(-w / 2, -h / 2, 0),
            new Vec3(-w / 2, -h / 2, 0),
            new Vec3(w / 2, -h / 2, 0),
            new Vec3(w / 2, -h / 2, 0),
            new Vec3(w / 2, h / 2, 0),
            
            // the ring inner edges
            new Vec3(ringAreaWidth / 2, h / 2, 0),
            new Vec3(ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2 + rightAreaHeight, 0),
            new Vec3(-ringAreaWidth / 2, h / 2, 0),
            
            // the ring outer edges
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2, 0),
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(ringAreaWidth / 2 + ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2 + rightAreaHeight + ringWidth, 0),
            new Vec3(-ringAreaWidth / 2 - ringWidth, h / 2, 0),
        };
        
        float renderScale = (float) (scale * (1.0 / 1000.0));
        
        DQuaternion rotation = DQuaternion.rotationByDegrees(
            new Vec3(0, 1, 0),
            CHelper.getSmoothCycles(60) * 360
        );
        
        renderLines(vertexConsumer, cameraPos, center, lineVertices, renderScale, rotation, color, matrixStack);
    }
    
    public static void renderLines(
        VertexConsumer vertexConsumer,
        Vec3 cameraPos, Vec3 center,
        Vec3[] lineVertices,
        double scale, DQuaternion rotation,
        int color, PoseStack matrixStack
    ) {
        matrixStack.pushPose();
        
        matrixStack.translate(
            center.x - cameraPos.x,
            center.y - cameraPos.y,
            center.z - cameraPos.z
        );
        
        matrixStack.mulPose(rotation.toMcQuaternion());
        
        matrixStack.scale((float) scale, (float) scale, (float) scale);
        
        Matrix4f matrix = matrixStack.last().pose();
        Matrix3f normalMatrix = matrixStack.last().normal();
        
        for (int i = 0; i < lineVertices.length / 2; i++) {
            putLine(vertexConsumer, color, matrix, normalMatrix, lineVertices[i * 2], lineVertices[i * 2 + 1]);
        }
        
        matrixStack.popPose();
    }
    
    public static void putLine(VertexConsumer vertexConsumer, int color, Matrix4f matrix, Matrix3f normalMatrix, Vec3 lineStart, Vec3 lineEnd) {
        putLine(vertexConsumer, color, lineEnd.subtract(lineStart), matrix, normalMatrix, lineStart, lineEnd);
    }
    
    public static void putLine(
        VertexConsumer vertexConsumer,
        int color, Vec3 normal, Matrix4f matrix, Matrix3f normalMatrix,
        Vec3 lineStart, Vec3 lineEnd
    ) {
        Vector3f normalTemp = new Vector3f();

        normalTemp.set(normal.x(), normal.y(), normal.z());
        normalMatrix.transform(normalTemp);

        vertexConsumer
                .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
                .color(color)
                .normal(normalTemp.x(), normalTemp.y(), normalTemp.z())
                .endVertex();

        vertexConsumer
                .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
                .color(color)
                .normal(normalTemp.x(), normalTemp.y(), normalTemp.z())
                .endVertex();
    }
    
    private static void putLineToLineStrip(
        VertexConsumer vertexConsumer, int color, Vec3 normal,
        Matrix4f matrix, Vec3 lineStart, Vec3 lineEnd
    ) {
        // use alpha 0 vertices to "jump" without leaving visible line
        vertexConsumer
            .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
            .color(0)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineStart.x), (float) (lineStart.y), (float) (lineStart.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
            .color(color)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
        
        vertexConsumer
            .vertex(matrix, (float) (lineEnd.x), (float) (lineEnd.y), (float) (lineEnd.z))
            .color(0)
            .normal((float) normal.x, (float) normal.y, (float) normal.z)
            .endVertex();
    }
    
    public static void renderRectLine(
        VertexConsumer vertexConsumer, Vec3 cameraPos,
        UnilateralPortalState rect,
        int flowCount, int color, double shrinkFactor,
        int flowDirection,
        PoseStack matrixStack
    ) {
        matrixStack.pushPose();
        
        matrixStack.translate(
            rect.position().x - cameraPos.x,
            rect.position().y - cameraPos.y,
            rect.position().z - cameraPos.z
        );
        
        Vec3[] vertices = getRectVertices(rect, shrinkFactor);
        
        Random random = new Random(color);
        renderFlowLines(
            vertexConsumer, vertices, flowCount, color, flowDirection, matrixStack,
            () -> random.nextInt(30, 300)
        );
        
        matrixStack.popPose();
    }
    
    public static void renderFlowLines(
        VertexConsumer vertexConsumer,
        Vec3[] vertices,
        int flowCount, int color, int flowDirection,
        PoseStack matrixStack, IntSupplier randCycleSupplier
    ) {
        Matrix4f matrix = matrixStack.last().pose();
        Matrix3f normalMatrix = matrixStack.last().normal();
        
        for (int i = 0; i < flowCount; i++) {
            double offset = flowDirection * CHelper.getSmoothCycles(randCycleSupplier.getAsInt());
            
            double totalStartRatio = ((double) i * 2) / (flowCount * 2) + offset;
            double totalEndRatio = ((double) i * 2 + 1) / (flowCount * 2) + offset;
            
            renderSubLineInLineLoop(
                vertexConsumer, matrix, normalMatrix,
                vertices, color, totalStartRatio, totalEndRatio
            );
        }
    }
    
    private static Vec3[] getRectVertices(UnilateralPortalState rect, double shrinkFactor) {
        Vec3 normal = rect.orientation().getNormal();
        Vec3 axisW = rect.orientation().getAxisW();
        Vec3 axisH = rect.orientation().getAxisH();
        
        Vec3 facingOffset = normal.scale(0.01);
        
        Vec3[] vertices = new Vec3[]{
            axisW.scale(shrinkFactor * rect.width() / 2)
                .add(axisH.scale(shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(shrinkFactor * rect.width() / 2)
                .add(axisH.scale(-1 * shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(-1 * shrinkFactor * rect.width() / 2)
                .add(axisH.scale(-1 * shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            axisW.scale(-1 * shrinkFactor * rect.width() / 2)
                .add(axisH.scale(shrinkFactor * rect.height() / 2))
                .add(facingOffset),
            
            // repeat the first because it's in a loop
            axisW.scale(shrinkFactor * rect.width() / 2)
                .add(axisH.scale(shrinkFactor * rect.height() / 2))
                .add(facingOffset),
        };
        return vertices;
    }
    
    public static void renderSubLineInLineLoop(
        VertexConsumer vertexConsumer, Matrix4f matrix, Matrix3f normalMatrix,
        Vec3[] lineVertices, int color,
        double totalStartRatio, double totalEndRatio
    ) {
        int lineNum = lineVertices.length - 1;
        
        double startRatioByLine = totalStartRatio * lineNum;
        double endRatioByLine = totalEndRatio * lineNum;
        
        int startRatioLineIndex = (int) Math.floor(startRatioByLine);
        int endRatioLineIndex = (int) Math.floor(endRatioByLine);
        
        for (int lineIndex = startRatioLineIndex; lineIndex <= endRatioLineIndex; lineIndex++) {
            double startLimit = lineIndex;
            double endLimit = lineIndex + 1;
            
            double startRatio = Math.max(startLimit, startRatioByLine);
            double endRatio = Math.min(endLimit, endRatioByLine);
            
            putLinePart(
                vertexConsumer, color, matrix, normalMatrix,
                lineVertices[Math.floorMod(lineIndex, lineNum)],
                lineVertices[Math.floorMod(lineIndex, lineNum) + 1],
                startRatio - lineIndex,
                endRatio - lineIndex
            );
        }
    }
    
    private static void putLinePart(
        VertexConsumer vertexConsumer, int color,
        Matrix4f matrix, Matrix3f normalMatrix, Vec3 lineStart, Vec3 lineEnd,
        double startRatio, double endRatio
    ) {
        Vec3 vec = lineEnd.subtract(lineStart);
        
        Vec3 partStartPos = lineStart.add(vec.scale(startRatio));
        Vec3 partEndPos = lineStart.add(vec.scale(endRatio));
        
        putLine(
            vertexConsumer, color, matrix, normalMatrix, partStartPos, partEndPos
        );
    }
    
    // NOTE it uses line strip
    public static void renderSphere(
        VertexConsumer vertexConsumer, int color,
        PoseStack matrixStack, Vec3 cameraPos,
        Sphere sphere, DQuaternion sphereOrientation,
        double animationProgress, double rotationProgress
    ) {
        matrixStack.pushPose();
        
        matrixStack.translate(
            sphere.center().x - cameraPos.x,
            sphere.center().y - cameraPos.y,
            sphere.center().z - cameraPos.z
        );
        
        matrixStack.mulPose(sphereOrientation.toMcQuaternion());
        matrixStack.scale((float) sphere.radius(), (float) sphere.radius(), (float) sphere.radius());
        
        Matrix4f matrix = matrixStack.last().pose();
        
        int meridianCount = 30;
        int parallelCount = 15;
        
        int vertexNum = Mth.clamp((int) Math.round(sphere.radius() * 40), 20, 400);
        
        int transparentColor = color & 0x00ffffff;
        
        // render meridians
        for (int i = 0; i < meridianCount; i++) {
            double longitude = ((double) i / meridianCount + rotationProgress) * Math.PI * 2;
            
            for (int j = 0; j < vertexNum; j++) {
                double latitudeRatio = (double) j / vertexNum;
                latitudeRatio = Math.min(latitudeRatio, animationProgress);
                
                double latitude = latitudeRatio * Math.PI - 0.5 * Math.PI;
                
                double x = Math.cos(latitude) * Math.cos(longitude);
                double y = Math.sin(latitude);
                double z = Math.cos(latitude) * Math.sin(longitude);
                
                boolean isFirst = j == 0;
                if (isFirst) {
                    // use transparent vertices to jump in line strip
                    vertexConsumer
                        .vertex(matrix, (float) (x), (float) (y), (float) (z))
                        .color(transparentColor)
                        .normal(0, 1, 0)
                        .endVertex();
                }
                
                vertexConsumer
                    .vertex(matrix, (float) (x), (float) (y), (float) (z))
                    .color(color)
                    .normal(0, 1, 0)
                    .endVertex();
                
                boolean isLast = j == vertexNum - 1;
                if (isLast) {
                    // use transparent vertices to jump in line strip
                    vertexConsumer
                        .vertex(matrix, (float) (x), (float) (y), (float) (z))
                        .color(transparentColor)
                        .normal(0, 1, 0)
                        .endVertex();
                }
            }
        }
        
        // render parallels (also rotating)
        for (int i = 0; i < parallelCount; i++) {
            double latitudeRatio = ((double) i / parallelCount) + rotationProgress;
            latitudeRatio = latitudeRatio - Math.floor(latitudeRatio);
            
            double latitude = latitudeRatio * Math.PI - 0.5 * Math.PI;
            
            for (int j = 0; j <= vertexNum; j++) {
                double longitudeRatio = (double) j / vertexNum;
                longitudeRatio = Math.min(longitudeRatio, animationProgress);
                
                double longitude = (longitudeRatio) * Math.PI * 2;
                
                double x = Math.cos(latitude) * Math.cos(longitude);
                double y = Math.sin(latitude);
                double z = Math.cos(latitude) * Math.sin(longitude);
                
                boolean isFirst = j == 0;
                if (isFirst) {
                    // use transparent vertices to jump in line strip
                    vertexConsumer
                        .vertex(matrix, (float) (x), (float) (y), (float) (z))
                        .color(transparentColor)
                        .normal(0, 1, 0)
                        .endVertex();
                }
                
                vertexConsumer
                    .vertex(matrix, (float) (x), (float) (y), (float) (z))
                    .color(color)
                    .normal(0, 1, 0)
                    .endVertex();
                
                boolean isLast = j == vertexNum;
                if (isLast) {
                    // use transparent vertices to jump in line strip
                    vertexConsumer
                        .vertex(matrix, (float) (x), (float) (y), (float) (z))
                        .color(transparentColor)
                        .normal(0, 1, 0)
                        .endVertex();
                }
            }
        }
        
        matrixStack.popPose();
    }
    
    public static void renderRectFrameFlow(
        PoseStack matrixStack, Vec3 cameraPos,
        VertexConsumer vertexConsumer, UnilateralPortalState rect,
        int innerColor, int outerColor
    ) {
        matrixStack.pushPose();
        matrixStack.scale(0.5f, 0.5f, 0.5f); // make it closer to camera to see it through block
        
        renderRectLine(
            vertexConsumer, cameraPos, rect,
            10, innerColor, 0.99, 1,
            matrixStack
        );
        renderRectLine(
            vertexConsumer, cameraPos, rect,
            10, outerColor, 1.01, -1,
            matrixStack
        );
        
        matrixStack.popPose();
    }
    
    public static void renderPortalShapeMeshDebug(
        PoseStack matrixStack, VertexConsumer vertexConsumer, Portal portal
    ) {
        double cycle = (Math.sin(CHelper.getSmoothCycles(50) * 2 * Math.PI) + 1) / 2;
        double shrink = 0.03 * cycle;
        
        PortalShape portalShape = portal.getPortalShape();
        
        if (portalShape instanceof SpecialFlatPortalShape shape) {
            shape.mesh.compact();
            
            int triangleNum = shape.mesh.getStoredTriangleNum();
            int vertexNum = triangleNum * 3;
            Vec3[] vertexes = new Vec3[vertexNum];
            double halfWidth = portal.getWidth() / 2;
            double halfHeight = portal.getHeight() / 2;
            Vec3 X = portal.getAxisW().scale(halfWidth);
            Vec3 Y = portal.getAxisH().scale(halfHeight);
            
            matrixStack.pushPose();
            
            Matrix4f matrix = matrixStack.last().pose();
            Matrix3f normalMatrix = matrixStack.last().normal();
            
            for (int i = 0; i < triangleNum; i++) {
                int p0Index = shape.mesh.getTrianglePointIndex(i, 0);
                int p1Index = shape.mesh.getTrianglePointIndex(i, 1);
                int p2Index = shape.mesh.getTrianglePointIndex(i, 2);
                
                double p0x = shape.mesh.pointCoords.getDouble(p0Index * 2);
                double p0y = shape.mesh.pointCoords.getDouble(p0Index * 2 + 1);
                double p1x = shape.mesh.pointCoords.getDouble(p1Index * 2);
                double p1y = shape.mesh.pointCoords.getDouble(p1Index * 2 + 1);
                double p2x = shape.mesh.pointCoords.getDouble(p2Index * 2);
                double p2y = shape.mesh.pointCoords.getDouble(p2Index * 2 + 1);
                
                double centerX = (p0x + p1x + p2x) / 3;
                double centerY = (p0y + p1y + p2y) / 3;
                
                double x0 = p0x * (1 - shrink) + centerX * shrink;
                double y0 = p0y * (1 - shrink) + centerY * shrink;
                double x1 = p1x * (1 - shrink) + centerX * shrink;
                double y1 = p1y * (1 - shrink) + centerY * shrink;
                double x2 = p2x * (1 - shrink) + centerX * shrink;
                double y2 = p2y * (1 - shrink) + centerY * shrink;
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x0).add(Y.scale(y0)), X.scale(x1).add(Y.scale(y1))
                );
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x1).add(Y.scale(y1)), X.scale(x2).add(Y.scale(y2))
                );
                
                WireRenderingHelper.putLine(
                    vertexConsumer, 0x80ff0000, matrix, normalMatrix,
                    X.scale(x2).add(Y.scale(y2)), X.scale(x0).add(Y.scale(y0))
                );
            }
            
            matrixStack.popPose();
        }
    }
}
