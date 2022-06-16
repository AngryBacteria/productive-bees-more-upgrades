package cy.jdkdigital.productivebees.common.block;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.block.entity.SolitaryNestBlockEntity;
import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.item.HoneyTreat;
import cy.jdkdigital.productivebees.common.recipe.BeeSpawningRecipe;
import cy.jdkdigital.productivebees.init.ModBlockEntityTypes;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import cy.jdkdigital.productivebees.integrations.jei.ingredients.BeeIngredient;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SolitaryNest extends AdvancedBeehiveAbstract
{
    List<BeeSpawningRecipe> recipes = new ArrayList<>();

    public SolitaryNest(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(BlockStateProperties.FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        return level.isClientSide ? null : createTickerHelper(blockEntityType, ModBlockEntityTypes.SOLITARY_NEST.get(), SolitaryNestBlockEntity::tick);
    }

    public int getMaxHoneyLevel() {
        return 0;
    }

    public Entity getNestingBeeType(Level level, Biome biome) {
        List<BeeSpawningRecipe> spawningRecipes = getSpawningRecipes(level, biome);
        if (!spawningRecipes.isEmpty()) {
            BeeSpawningRecipe spawningRecipe = spawningRecipes.get(level.random.nextInt(spawningRecipes.size()));
            BeeIngredient beeIngredient = spawningRecipe.output.get(level.random.nextInt(spawningRecipe.output.size())).get();
            if (beeIngredient != null) {
                Entity bee = beeIngredient.getBeeEntity().create(level);
                if (bee instanceof ConfigurableBee) {
                    ((ConfigurableBee) bee).setBeeType(beeIngredient.getBeeType().toString());
                    ((ConfigurableBee) bee).setAttributes();
                }
                return bee;
            } else {
                ProductiveBees.LOGGER.debug("No bee ingredient found in " + spawningRecipe);
            }
        }
        return null;
    }

    public List<BeeSpawningRecipe> getSpawningRecipes(Level world, Biome biome) {
        // Get and cache recipes for nest type
        if (recipes.isEmpty()) {
            Map<ResourceLocation, BeeSpawningRecipe> allRecipes = new HashMap<>();
            allRecipes.putAll(world.getRecipeManager().byType(ModRecipeTypes.BEE_SPAWNING_BIG_TYPE.get()));
            allRecipes.putAll(world.getRecipeManager().byType(ModRecipeTypes.BEE_SPAWNING_TYPE.get()));
            ItemStack nestItem = new ItemStack(this);
            for (Map.Entry<ResourceLocation, BeeSpawningRecipe> entry : allRecipes.entrySet()) {
                BeeSpawningRecipe recipe = entry.getValue();
                if (recipe.matches(nestItem)) {
                    recipes.add(recipe);
                }
            }
        }

        List<BeeSpawningRecipe> spawningRecipes = new ArrayList<>();
        if (!recipes.isEmpty()) {
            for (BeeSpawningRecipe recipe : recipes) {
                // TODO use biome tags
//                if ((recipe.biomes.isEmpty() && world.dimension() == Level.OVERWORLD) || recipe.biomes.contains(ForgeRegistries.BIOMES.getKey(biome).toString())) {
                    spawningRecipes.add(recipe);
//                }
            }
        }
        return spawningRecipes;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SolitaryNestBlockEntity(pos, state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext itemUseContext) {
        return this.defaultBlockState().setValue(BlockStateProperties.FACING, itemUseContext.getNearestLookingDirection().getOpposite());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(BlockStateProperties.FACING, rotation.rotate(state.getValue(BlockStateProperties.FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(BlockStateProperties.FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING);
    }

    public boolean canRepopulateIn(Level world, Biome biome) {
        return !getSpawningRecipes(world, biome).isEmpty();
    }

    @Override
    public InteractionResult use(BlockState state, Level world, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!world.isClientSide()) {
            SolitaryNestBlockEntity tileEntity = (SolitaryNestBlockEntity) world.getBlockEntity(pos);

            ItemStack heldItem = player.getItemInHand(hand);
            if (tileEntity != null && heldItem.getItem() instanceof HoneyTreat && !HoneyTreat.hasGene(heldItem)) {
                boolean itemUse = false;
                int currentCooldown = tileEntity.getNestTickCooldown();
                if (tileEntity.canRepopulate()) {
                    itemUse = true;
                    if (currentCooldown <= 0) {
                        tileEntity.setNestCooldown(ProductiveBeesConfig.GENERAL.nestSpawnCooldown.get());
                    } else {
                        tileEntity.setNestCooldown((int) (currentCooldown * 0.9));
                    }
                }

                if (itemUse) {
                    world.levelEvent(2005, pos, 0);
                    CriteriaTriggers.ITEM_USED_ON_BLOCK.trigger((ServerPlayer) player, pos, heldItem);

                    if (!player.isCreative()) {
                        heldItem.shrink(1);
                    }
                }
            }
        }

        return super.use(state, world, pos, player, hand, hit);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable BlockGetter worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        super.appendHoverText(stack, worldIn, tooltip, flagIn);

        if (stack.hasTag() && stack.getTag().getInt("spawnCount") >= ProductiveBeesConfig.BEES.cuckooSpawnCount.get()) {
            tooltip.add(Component.translatable("productivebees.hive.tooltip.nest_inactive").withStyle(ChatFormatting.BOLD));
        }
    }
}
