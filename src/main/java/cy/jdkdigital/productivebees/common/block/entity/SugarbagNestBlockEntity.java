package cy.jdkdigital.productivebees.common.block.entity;

import cy.jdkdigital.productivebees.init.ModBlockEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public class SugarbagNestBlockEntity extends AdvancedBeehiveBlockEntityAbstract
{
    public SugarbagNestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntityTypes.SUGARBAG_NEST.get(), pos, state);
    }
}
