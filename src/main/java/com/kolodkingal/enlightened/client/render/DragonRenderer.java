package com.kolodkingal.enlightened.client.render;

import com.kolodkingal.enlightened.Enlightened;
import com.kolodkingal.enlightened.entity.DragonEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A dragon as the show draws Ran and Shaw: a long serpent body of scales, a white crest of spines along its back, a
 * white mane round its neck, swept horns, long whiskers, four short legs. The head is the entity; the body follows
 * the path the head has flown (kept on the client), and where the path runs out (a dragon that has not moved) it
 * coils. Its scale is the dragon's (the masters are bigger, a pup small).
 */
public class DragonRenderer extends EntityRenderer<DragonEntity> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(Enlightened.id("dragon"), "main");
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[DragonEntity.Colour.values().length];
    /** How many segments the body has, and how far apart (blocks, at scale 1). */
    public static final int SEGMENTS = 22;
    public static final double SPACING = 0.75;
    /** The body's height over the dragon's feet, at scale 1. */
    private static final double LIFT = 0.8;

    static {
        for (DragonEntity.Colour c : DragonEntity.Colour.values()) {
            TEXTURES[c.ordinal()] = Enlightened.id("textures/entity/dragon/" + c.id + ".png");
        }
    }

    private final ModelPart segment;
    private final ModelPart fin;
    private final ModelPart legL;
    private final ModelPart legR;
    private final ModelPart wingL;
    private final ModelPart wingR;
    private final ModelPart head;
    private final ModelPart jaw;

    public DragonRenderer(EntityRendererProvider.Context context) {
        super(context);
        ModelPart root = context.bakeLayer(LAYER);
        segment = root.getChild("segment");
        fin = root.getChild("fin");
        legL = root.getChild("leg_l");
        legR = root.getChild("leg_r");
        wingL = root.getChild("wing_l");
        wingR = root.getChild("wing_r");
        head = root.getChild("head");
        jaw = head.getChild("jaw");
        shadowRadius = 1.2F;
    }

    /** The parts, in the usual model space (y down, pixels), facing +z (the renderer turns them along the body). */
    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("segment", CubeListBuilder.create().texOffs(0, 0).addBox(-8, -8, -8, 16, 16, 16), PartPose.ZERO);
        root.addOrReplaceChild("fin", CubeListBuilder.create().texOffs(64, 0).addBox(-1, -13, -5, 2, 5, 10), PartPose.ZERO);
        // A leg each side: thigh, shin and a clawed foot, set at a segment (the left built, the right mirrored).
        for (int side = -1; side <= 1; side += 2) {
            String n = side < 0 ? "r" : "l";
            CubeListBuilder thigh = CubeListBuilder.create().texOffs(32, 64);
            if (side < 0) {
                thigh.mirror();
            }
            PartDefinition leg = root.addOrReplaceChild("leg_" + n, thigh.addBox(-2.5F, 0, -2.5F, 5, 9, 5), PartPose.offset(side * 7.5F, 3, 0));
            PartDefinition shin = leg.addOrReplaceChild("shin", CubeListBuilder.create().texOffs(52, 64).addBox(-2, 0, -2, 4, 8, 4),
                    PartPose.offsetAndRotation(0, 8, 0.5F, -0.6F, 0, 0));
            shin.addOrReplaceChild("foot", CubeListBuilder.create().texOffs(68, 64).addBox(-2.5F, 0, -4.5F, 5, 2, 6), PartPose.offsetAndRotation(0, 7.5F, 0, 0.6F, 0, 0));
            // A wing of middling size: a bony leading edge and the membrane behind it.
            CubeListBuilder wing = CubeListBuilder.create().texOffs(0, 80);
            if (side < 0) {
                wing.mirror();
            }
            float x0 = side < 0 ? -20 : 0;
            root.addOrReplaceChild("wing_" + n, wing.addBox(x0, 0, -2, 20, 1, 15)
                    .texOffs(0, 112).addBox(side < 0 ? -20 : 0, -1, -3, 20, 2, 2), PartPose.offset(side * 7.0F, -6, 0));
        }
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create()
                .texOffs(0, 32).addBox(-6, -6, -6, 12, 10, 12)
                .texOffs(48, 32).addBox(-4, -3, 6, 8, 6, 10)
                .texOffs(82, 32).addBox(-8, -9, -9, 16, 14, 4)
                .texOffs(82, 50).addBox(-6, -4.5F, 3, 12, 2, 2, new CubeDeformation(0.1F))
                // Heavy brows over the eyes, a broad nose pad at the snout's tip.
                .texOffs(0, 100).addBox(-6.5F, -7, 1.5F, 13, 2, 4)
                .texOffs(0, 64).addBox(-3.5F, -3.8F, 15, 7, 4, 2), PartPose.ZERO);
        head.addOrReplaceChild("fang_r", CubeListBuilder.create().texOffs(28, 64).addBox(-0.5F, 0, -0.5F, 1, 4, 1), PartPose.offset(-3, 3, 13));
        head.addOrReplaceChild("fang_l", CubeListBuilder.create().texOffs(28, 64).addBox(-0.5F, 0, -0.5F, 1, 4, 1), PartPose.offset(3, 3, 13));
        head.addOrReplaceChild("jaw", CubeListBuilder.create().texOffs(48, 48).addBox(-3.5F, 0, 0, 7, 2, 11), PartPose.offset(0, 3, 4));
        // Long horns swept back from the brow, curving up at their tips.
        for (int side = -1; side <= 1; side += 2) {
            CubeListBuilder horn = CubeListBuilder.create().texOffs(88, 0);
            if (side > 0) {
                horn.mirror();
            }
            PartDefinition h = head.addOrReplaceChild(side < 0 ? "horn_r" : "horn_l", horn.addBox(-1.5F, -1.5F, -11, 3, 3, 11),
                    PartPose.offsetAndRotation(side * 4, -6, -1, 0.25F, side * 0.22F, 0));
            h.addOrReplaceChild("tip", CubeListBuilder.create().texOffs(88, 16).addBox(-1, -1, -8, 2, 2, 8), PartPose.offsetAndRotation(0, 0, -10.5F, -0.5F, 0, 0));
        }
        head.addOrReplaceChild("whisker_r", CubeListBuilder.create().texOffs(88, 12).addBox(-10, 0, 0, 10, 1, 1),
                PartPose.offsetAndRotation(-3.5F, 0, 16, 0, 0.6F, 0.35F));
        head.addOrReplaceChild("whisker_l", CubeListBuilder.create().texOffs(88, 12).mirror().addBox(0, 0, 0, 10, 1, 1),
                PartPose.offsetAndRotation(3.5F, 0, 16, 0, -0.6F, -0.35F));
        return LayerDefinition.create(mesh, 128, 128);
    }

    /** How thick the body is along its length (0 the neck, 1 the tip of the tail). */
    static float girth(float t) {
        if (t < 0.12F) {
            return 0.7F + t * 2.5F;
        }
        if (t < 0.45F) {
            return 1.0F;
        }
        return Math.max(0.22F, 1.0F - (t - 0.45F) * 1.45F);
    }

    @Override
    public void render(DragonEntity dragon, float entityYaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float scale = (float) dragon.getAttributeValue(Attributes.SCALE) * dragon.getAgeScale();
        shadowRadius = 1.2F * scale;
        Vec3 base = dragon.getPosition(partial);
        float headYaw = Mth.rotLerp(partial, dragon.yHeadRotO, dragon.yHeadRot) * Mth.DEG_TO_RAD;
        float pitch = Mth.lerp(partial, dragon.xRotO, dragon.getXRot()) * Mth.DEG_TO_RAD;
        Vec3 facing = new Vec3(-Mth.sin(headYaw) * Mth.cos(pitch), -Mth.sin(pitch), Mth.cos(headYaw) * Mth.cos(pitch));
        List<Vec3> spine = spine(dragon, base, facing, scale, partial);
        VertexConsumer vc = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURES[dragon.colour().ordinal()]));
        int overlay = LivingEntityRenderer.getOverlayCoords(dragon, 0.0F);
        double lift = LIFT * scale;
        float age = dragon.tickCount + partial;

        // The body, from the neck back: each segment set along the line to the one before it.
        Vec3 prev = new Vec3(0, lift, 0);
        for (int i = 0; i < spine.size(); i++) {
            Vec3 at = spine.get(i).subtract(base).add(0, lift, 0);
            Vec3 dir = prev.subtract(at);
            if (dir.lengthSqr() < 1.0E-6) {
                dir = facing;
            }
            float t = (i + 1.0F) / SEGMENTS;
            float g = girth(t) * scale * 1.1F;
            pose.pushPose();
            pose.translate(at.x, at.y, at.z);
            orient(pose, dir.normalize());
            // Long enough to meet the next segment, however thin the tail grows.
            pose.scale(-g, -g, (float) Math.max(g * 1.1F, SPACING * scale * 1.3F));
            segment.render(pose, vc, light, overlay);
            if (t < 0.92F) {
                fin.render(pose, vc, light, overlay);
            }
            pose.popPose();
            if (i == 2 || i == 3 || i == 10) {
                // Limbs at the segment, drawn at its girth (not stretched along the body).
                pose.pushPose();
                pose.translate(at.x, at.y, at.z);
                orient(pose, dir.normalize());
                pose.scale(-g, -g, g);
                boolean air = dragon.airborne();
                if (i == 2) {
                    // The wings: beating in flight, folded along the back on the ground.
                    float beat = air ? Mth.sin(age * 0.35F) * 0.7F : 1.25F;
                    wingL.zRot = -beat;
                    wingR.zRot = beat;
                    wingL.render(pose, vc, light, overlay);
                    wingR.render(pose, vc, light, overlay);
                } else {
                    float stride = Mth.sin(age * 0.3F + i) * (air ? 0.15F : 0.5F) * Math.min(1.0F, dragon.walkAnimation.speed(partial) * 3.0F + (air ? 1 : 0));
                    legL.xRot = (air ? 0.9F : 0.0F) + stride;
                    legR.xRot = (air ? 0.9F : 0.0F) - stride;
                    legL.render(pose, vc, light, overlay);
                    legR.render(pose, vc, light, overlay);
                }
                pose.popPose();
            }
            prev = at;
        }

        // The head, last, where the entity is.
        pose.pushPose();
        pose.translate(0, lift, 0);
        orient(pose, facing);
        float hs = scale * 1.45F;
        pose.scale(-hs, -hs, hs);
        jaw.xRot = dragon.breathing() ? 0.6F : 0.05F + Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.1F;
        head.render(pose, vc, light, overlay);
        pose.popPose();
        super.render(dragon, entityYaw, partial, pose, buffers, light);
    }

    /** Turns +z to point along a direction (and y to stay up). */
    private static void orient(PoseStack pose, Vec3 dir) {
        float yaw = (float) Math.atan2(dir.x, dir.z);
        float pitch = (float) -Math.asin(Mth.clamp(dir.y, -1.0, 1.0));
        pose.mulPose(Axis.YP.rotation(yaw));
        pose.mulPose(Axis.XP.rotation(pitch));
    }

    /**
     * Where each segment is (world, at the feet's height): along the path the head has flown, a segment every
     * {@link #SPACING}; where the path runs out the body coils behind.
     */
    static List<Vec3> spine(DragonEntity dragon, Vec3 head, Vec3 facing, float scale, float partial) {
        List<Vec3> out = new ArrayList<>(SEGMENTS);
        double gap = SPACING * scale;
        double need = gap;
        Vec3 last = head;
        Iterator<Vec3> it = dragon.trail.iterator();
        while (it.hasNext() && out.size() < SEGMENTS) {
            Vec3 p = it.next();
            double d = last.distanceTo(p);
            if (d > gap * 6) {
                // A jump (it was teleported): the path ends here.
                break;
            }
            while (d >= need && out.size() < SEGMENTS) {
                last = last.add(p.subtract(last).scale(need / d));
                out.add(last);
                d = last.distanceTo(p);
                need = gap;
            }
            need -= d;
            last = p;
        }
        // The rest lies behind, in a slow serpent's S that ripples as it breathes.
        Vec3 back;
        if (out.size() >= 2) {
            back = out.get(out.size() - 1).subtract(out.get(out.size() - 2));
        } else if (out.size() == 1) {
            back = out.get(0).subtract(head);
        } else {
            back = facing.scale(-1);
        }
        back = new Vec3(back.x, 0, back.z);
        if (back.lengthSqr() < 1.0E-6) {
            back = new Vec3(-facing.x, 0, -facing.z);
            if (back.lengthSqr() < 1.0E-6) {
                back = new Vec3(0, 0, -1);
            }
        }
        back = back.normalize();
        Vec3 side = new Vec3(-back.z, 0, back.x);
        Vec3 at = out.isEmpty() ? head : out.get(out.size() - 1);
        float time = (dragon.tickCount + partial) * 0.04F;
        int start = out.size();
        double prevSway = 0;
        while (out.size() < SEGMENTS) {
            int k = out.size() - start + 1;
            double sway = Math.sin(k * 0.45 + time) * 1.1 * scale * Math.min(1.0, k / 4.0);
            at = at.add(back.scale(gap * 0.92)).add(side.scale(sway - prevSway));
            prevSway = sway;
            out.add(at);
        }
        return out;
    }

    @Override
    public ResourceLocation getTextureLocation(DragonEntity dragon) {
        return TEXTURES[dragon.colour().ordinal()];
    }

    @Override
    public boolean shouldRender(DragonEntity dragon, net.minecraft.client.renderer.culling.Frustum frustum, double x, double y, double z) {
        // The body reaches far beyond the head's box.
        return frustum.isVisible(dragon.getBoundingBox().inflate(SEGMENTS * SPACING * dragon.getAttributeValue(Attributes.SCALE)));
    }
}
