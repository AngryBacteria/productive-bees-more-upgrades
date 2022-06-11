//package cy.jdkdigital.productivebees.integrations.jei;
//
//import cy.jdkdigital.productivebees.ProductiveBees;
//import cy.jdkdigital.productivebees.common.recipe.BeeSpawningRecipe;
//import cy.jdkdigital.productivebees.init.ModBlocks;
//import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
//import mezz.jei.api.gui.IRecipeLayout;
//import mezz.jei.api.gui.drawable.IDrawable;
//import mezz.jei.api.gui.ingredient.IGuiIngredientGroup;
//import mezz.jei.api.gui.ingredient.IGuiItemStackGroup;
//import mezz.jei.api.helpers.IGuiHelper;
//import mezz.jei.api.ingredients.IIngredients;
//import net.minecraft.resources.ResourceLocation;
//import net.minecraft.world.item.ItemStack;
//
//import javax.annotation.Nonnull;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.stream.IntStream;
//
//public class BeeSpawningRecipeBigCategory extends BeeSpawningRecipeCategory
//{
//    private final IDrawable background;
//    private final IDrawable icon;
//
//    public static final HashMap<Integer, List<Integer>> BEE_POSITIONS = new HashMap<Integer, List<Integer>>()
//    {{
//        put(0, new ArrayList<Integer>()
//        {{
//            add(66);
//            add(18);
//        }});
//        put(1, new ArrayList<Integer>()
//        {{
//            add(66);
//            add(38);
//        }});
//        put(2, new ArrayList<Integer>()
//        {{
//            add(84);
//            add(28);
//        }});
//        put(3, new ArrayList<Integer>()
//        {{
//            add(102);
//            add(18);
//        }});
//        put(4, new ArrayList<Integer>()
//        {{
//            add(102);
//            add(38);
//        }});
//    }};
//
//    public BeeSpawningRecipeBigCategory(IGuiHelper guiHelper) {
//        super(guiHelper);
//        ResourceLocation location = new ResourceLocation(ProductiveBees.MODID, "textures/gui/jei/bee_spawning_recipe_big.png");
//        this.background = guiHelper.createDrawable(location, 0, 0, 126, 70);
//        this.icon = guiHelper.createDrawableIngredient(new ItemStack(ModBlocks.OAK_WOOD_NEST.get()));
//    }
//
//    @Nonnull
//    @Override
//    public ResourceLocation getUid() {
//        return ProductiveBeesJeiPlugin.CATEGORY_BEE_SPAWNING_BIG_UID;
//    }
//
//    @Nonnull
//    @Override
//    public IDrawable getBackground() {
//        return this.background;
//    }
//
//    @Nonnull
//    @Override
//    public IDrawable getIcon() {
//        return this.icon;
//    }
//
//    @Override
//    public void setRecipe(IRecipeLayout recipeLayout, BeeSpawningRecipe recipe, IIngredients ingredients) {
//        IGuiItemStackGroup itemStacks = recipeLayout.getItemStacks();
//        itemStacks.init(0, true, 4, 26);
//        itemStacks.set(ingredients);
//
//        IGuiIngredientGroup<BeeIngredient> ingredientStacks = recipeLayout.getIngredientsGroup(ProductiveBeesJeiPlugin.BEE_INGREDIENT);
//
//        int offset = ingredients.getInputs(ProductiveBeesJeiPlugin.BEE_INGREDIENT).size();
//        IntStream.range(offset, recipe.output.size() + offset).forEach((i) -> {
//            if (i - offset > 3) {
//                return;
//            }
//            List<Integer> pos = BEE_POSITIONS.get(i - offset);
//            ingredientStacks.init(i, false, pos.get(0), pos.get(1));
//        });
//        ingredientStacks.set(ingredients);
//    }
//}
