package cy.jdkdigital.productivebees.client.model;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.mojang.datafixers.util.Pair;
import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.setup.BeeReloadListener;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeRenderTypes;
import net.minecraftforge.client.RenderTypeGroup;
import net.minecraftforge.client.model.CompositeModel;
import net.minecraftforge.client.model.geometry.IGeometryBakingContext;
import net.minecraftforge.client.model.geometry.IGeometryLoader;
import net.minecraftforge.client.model.geometry.IUnbakedGeometry;
import net.minecraftforge.client.model.geometry.UnbakedGeometryHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class CombModel implements IUnbakedGeometry<CombModel>
{
    public static final CombModel INSTANCE = new CombModel(ImmutableList.of());

    public ImmutableList<Material> textures;
    public final ImmutableSet<Integer> fullBrightLayers;

    public CombModel(ImmutableList<Material> textures) {
        this(textures, ImmutableSet.of());
    }

    public CombModel(@Nullable ImmutableList<Material> textures, ImmutableSet<Integer> fullBrightLayers) {
        this.textures = textures;
        this.fullBrightLayers = fullBrightLayers;
    }

    @Override
    public BakedModel bake(IGeometryBakingContext context, ModelBakery bakery, Function<Material, TextureAtlasSprite> spriteGetter, ModelState modelTransform, ItemOverrides overrides, ResourceLocation modelLocation) {
        TextureAtlasSprite particle = spriteGetter.apply(
            context.hasMaterial("particle") ? context.getMaterial("particle") : textures.get(0)
        );

        CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(context, particle, new Overrides(this, overrides, modelTransform, context, spriteGetter), context.getTransforms());

        return builder.build();
    }

    @Override
    public Collection<Material> getMaterials(IGeometryBakingContext owner, Function<ResourceLocation, UnbakedModel> modelGetter, Set<Pair<String, String>> missingTextureErrors) {
        ImmutableList.Builder<Material> builder = ImmutableList.builder();
        for (int i = 0; owner.hasMaterial("layer" + i); i++) {
            Material mat = owner.getMaterial("layer" + i);
            builder.add(mat);
        }
        textures =  builder.build();
        return textures;
    }

    public static class Overrides extends ItemOverrides {
        private final Map<String, BakedModel> modelCache = Maps.newHashMap();
        private final CombModel combModel;
        private final ItemOverrides nested;
        private final ModelState modelState;
        private final IGeometryBakingContext context;
        private final Function<Material, TextureAtlasSprite> spriteGetter;

        private Overrides(CombModel combModel, ItemOverrides nested, ModelState modelState, IGeometryBakingContext context, Function<Material, TextureAtlasSprite> spriteGetter)
        {
            this.combModel = combModel;
            this.nested = nested;
            this.modelState = modelState;
            this.context = context;
            this.spriteGetter = spriteGetter;
        }

        @Nullable
        @Override
        public BakedModel resolve(@Nonnull BakedModel model, ItemStack stack, @Nullable ClientLevel world, @Nullable LivingEntity livingEntity, int seed) {
            CompoundTag tag = stack.getTagElement("EntityTag");
            if (tag != null && tag.contains("type")) {
                String beeType = tag.getString("type");

                if (!modelCache.containsKey(beeType)) {
                    CompoundTag nbt = BeeReloadListener.INSTANCE.getData(beeType);
                    if (nbt == null) {
                        // There's a broken honeycomb definition
                        modelCache.put(beeType, model);
                    } else {
                        TextureAtlasSprite sprite;
                        if (nbt.contains("combTexture")) {
                            Material texture = new Material(InventoryMenu.BLOCK_ATLAS, new ResourceLocation(nbt.getString("combTexture")));
                            sprite = spriteGetter.apply(texture);
                        } else {
                            sprite = spriteGetter.apply(combModel.textures.get(0));
                        }
                        CompositeModel.Baked.Builder builder = CompositeModel.Baked.builder(context, sprite, nested, context.getTransforms());
                        boolean fullBright = combModel.fullBrightLayers.contains(0);

                        var unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, sprite);
                        var quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> sprite, modelState, new ResourceLocation(ProductiveBees.MODID, "base"));

                        builder.addQuads(renderType(fullBright), quads);

                        // Crystal bees have glowing bits on the comb texture
                        if (nbt.contains("renderer") && nbt.getString("renderer").equals("default_crystal")) {
                            TextureAtlasSprite crystalSprite = spriteGetter.apply(combModel.textures.get(1));
                            fullBright = combModel.fullBrightLayers.contains(1);

                            unbaked = UnbakedGeometryHelper.createUnbakedItemElements(0, crystalSprite);
                            quads = UnbakedGeometryHelper.bakeElements(unbaked, $ -> crystalSprite, modelState, new ResourceLocation(ProductiveBees.MODID, "crystal"));

                            builder.addQuads(renderType(fullBright), quads);
                        }

                        modelCache.put(beeType, builder.build());
                    }
                }
                return modelCache.getOrDefault(beeType, model);
            }
            return model;
        }
    }

    public static class Loader implements IGeometryLoader<CombModel>
    {
        public static final Loader INSTANCE = new Loader();

        @Nonnull
        @Override
        public CombModel read(JsonObject jsonObject, JsonDeserializationContext deserializationContext) throws JsonParseException {
            ImmutableSet.Builder<Integer> fullbrightLayers = ImmutableSet.builder();
            if (jsonObject.has("fullbright_layers")) {
                JsonArray arr = GsonHelper.getAsJsonArray(jsonObject, "fullbright_layers");
                for (int i = 0; i < arr.size(); i++) {
                    fullbrightLayers.add(arr.get(i).getAsInt());
                }
            }
            return new CombModel(null, fullbrightLayers.build());
        }
    }

    private static RenderTypeGroup renderType(boolean isFullbright) {
        return new RenderTypeGroup(RenderType.translucent(), isFullbright ? ForgeRenderTypes.ITEM_UNSORTED_UNLIT_TRANSLUCENT.get() : ForgeRenderTypes.ITEM_UNSORTED_TRANSLUCENT.get());
    }
}
