package cy.jdkdigital.productivebees.gen.feature;

import com.mojang.serialization.Codec;
import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.block.SolitaryNest;
import cy.jdkdigital.productivebees.common.block.entity.SolitaryNestBlockEntity;
import cy.jdkdigital.productivebees.common.block.nest.WoodNest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.feature.configurations.ReplaceBlockConfiguration;
import net.minecraftforge.registries.ForgeRegistries;

public class SolitaryNestFeature extends Feature<ReplaceBlockConfiguration>
{
    private final float probability;
    protected boolean placeOntop;

    public SolitaryNestFeature(float probability, Codec<ReplaceBlockConfiguration> configFactory) {
        this(probability, configFactory, false);
    }

    public SolitaryNestFeature(float probability, Codec<ReplaceBlockConfiguration> configFactory, boolean placeOntop) {
        super(configFactory);
        this.probability = probability;
        this.placeOntop = placeOntop;
    }

    @Override
    public boolean place(FeaturePlaceContext<ReplaceBlockConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource rand = context.random();
        BlockPos blockPos = context.origin();
        ReplaceBlockConfiguration featureConfig = context.config();

        for(OreConfiguration.TargetBlockState targetBlockState : featureConfig.targetStates) {
            boolean probabilityCheck = rand.nextFloat() > this.probability;
            if (nestShouldNotGenerate(targetBlockState.state) || probabilityCheck) {
                return false;
            }

            // Get random block in chunk
            blockPos = blockPos.south(rand.nextInt(14)).east(rand.nextInt(14));

            // Go to surface
            while (blockPos.getY() < 50 || !level.isEmptyBlock(blockPos)) {
                blockPos = blockPos.above();
            }

            if (!placeOntop) {
                blockPos = blockPos.below();
            }

            BlockState state = placeOntop ? level.getBlockState(blockPos.below()) : level.getBlockState(blockPos);
            if (targetBlockState.target.test(state, rand)) {
                return placeNest(level, blockPos, targetBlockState.state, rand);
            }
        }
        return false;
    }

    protected boolean nestShouldNotGenerate(BlockState state) {
        return !ProductiveBeesConfig.WORLD_GEN.nestConfigs.get("enable_" + ForgeRegistries.BLOCKS.getKey(state.getBlock()).toString()).get();
    }

    protected boolean placeNest(WorldGenLevel level, BlockPos pos, BlockState state, RandomSource rand) {
        // Check if there's air around and face that way, default to UP
        Direction direction = state.getBlock() instanceof WoodNest ? Direction.SOUTH : Direction.UP;
        for (Direction dir : BlockStateProperties.FACING.getPossibleValues()) {
            BlockPos blockPos = pos.relative(dir, 1);
            if (level.isEmptyBlock(blockPos)) {
                direction = dir;
                break;
            }
        }

        // Replace target block with nest
        BlockState newState = state.setValue(BlockStateProperties.FACING, direction);

        boolean result = level.setBlock(pos, newState, 1);

        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (blockEntity instanceof SolitaryNestBlockEntity) {
            ProductiveBees.LOGGER.debug("Spawned nest at " + pos + " " + newState);
            Entity newBee = ((SolitaryNest) newState.getBlock()).getNestingBeeType(level.getLevel(), level.getLevel().getBiome(pos).value(), rand);
            if (newBee instanceof Bee) {
                ((Bee) newBee).setHealth(((Bee) newBee).getMaxHealth());
                newBee.setPos(pos.getX(), pos.getY(), pos.getZ());
                ((SolitaryNestBlockEntity) blockEntity).addOccupantWithPresetTicks(newBee, false, level.getRandom().nextInt(599));
            }
        }

        return result;
    }
}
