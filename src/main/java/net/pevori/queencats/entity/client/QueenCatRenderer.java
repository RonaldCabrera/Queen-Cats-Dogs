package net.pevori.queencats.entity.client;

import java.util.Map;

import com.google.common.collect.Maps;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.pevori.queencats.QueenCats;
import net.pevori.queencats.entity.custom.PrincessCatEntity;
import net.pevori.queencats.entity.custom.QueenCatEntity;
import net.pevori.queencats.entity.variant.QueenCatVariant;
import software.bernie.geckolib3.renderers.geo.GeoEntityRenderer;

public class QueenCatRenderer extends GeoEntityRenderer<QueenCatEntity> {
    public static final Map<QueenCatVariant, Identifier> LOCATION_BY_VARIANT =
    Util.make(Maps.newEnumMap(QueenCatVariant.class), (map) -> {
        map.put(QueenCatVariant.WHITE,
                new Identifier(QueenCats.MOD_ID, "textures/entity/queen_cat/humanoid_cat_white.png"));
        map.put(QueenCatVariant.BLACK,
                new Identifier(QueenCats.MOD_ID, "textures/entity/queen_cat/humanoid_cat_black.png"));
    });

    public QueenCatRenderer(EntityRendererFactory.Context ctx) {
        super(ctx, new QueenCatModel());
    }

    @Override
    public Identifier getTextureLocation(QueenCatEntity instance) {
        return LOCATION_BY_VARIANT.get(instance.getVariant());
    }

    @Override
    public RenderLayer getRenderType(QueenCatEntity animatable, float partialTicks, MatrixStack stack,
            VertexConsumerProvider renderTypeBuffer, VertexConsumer vertexBuilder, int packedLightIn,
            Identifier textureLocation) {

        /*if(animatable.isBaby()) {
            stack.scale(0.5f, 0.5f, 0.5f);
        } else {
            stack.scale(1f, 1f, 1f);
        }*/

        return super.getRenderType(animatable, partialTicks, stack, renderTypeBuffer, vertexBuilder, packedLightIn,
                textureLocation);
    }
}
