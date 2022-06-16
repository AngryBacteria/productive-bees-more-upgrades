package cy.jdkdigital.productivebees.client.render.ingredient;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector3f;
import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.entity.bee.ProductiveBee;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;

public class BeeRenderer
{
    public static void render(PoseStack poseStack, BeeIngredient beeIngredient, Minecraft minecraft) {
        render(poseStack, 0, 0, beeIngredient, minecraft);
    }

    public static void render(PoseStack poseStack, int xPosition, int yPosition, BeeIngredient beeIngredient, Minecraft minecraft) {
        Entity bee = beeIngredient.getCachedEntity(minecraft.level);

        if (minecraft.player != null && bee != null) {
            if (bee instanceof ConfigurableBee) {
                ((ConfigurableBee) bee).setBeeType(beeIngredient.getBeeType().toString());
            }

            if (bee instanceof ProductiveBee) {
                ((ProductiveBee) bee).setRenderStatic();
            }

            bee.tickCount = minecraft.player.tickCount;
            bee.setYBodyRot(-20);

            float scaledSize = 18;

            poseStack.pushPose();
            poseStack.translate(7D + xPosition, 12D + yPosition, 1.5);
            poseStack.mulPose(Vector3f.ZP.rotationDegrees(190.0F));
            poseStack.mulPose(Vector3f.YP.rotationDegrees(20.0F));
            poseStack.mulPose(Vector3f.XP.rotationDegrees(20.0F));
            poseStack.translate(0.0F, -0.2F, 1);
            poseStack.scale(scaledSize, scaledSize, scaledSize);

            EntityRenderDispatcher entityRendererManager = minecraft.getEntityRenderDispatcher();
            MultiBufferSource.BufferSource buffer = minecraft.renderBuffers().bufferSource();
            entityRendererManager.render(bee, 0, 0, 0.0D, minecraft.getFrameTime(), 1, poseStack, buffer, 15728880);
            buffer.endBatch();
            poseStack.popPose();
        }
    }
}
