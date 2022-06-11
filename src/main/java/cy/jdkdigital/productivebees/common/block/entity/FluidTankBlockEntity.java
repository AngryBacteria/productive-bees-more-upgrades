package cy.jdkdigital.productivebees.common.block.entity;

import cy.jdkdigital.productivebees.common.recipe.BottlerRecipe;
import cy.jdkdigital.productivebees.init.ModRecipeTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidActionResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.CapabilityFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class FluidTankBlockEntity extends CapabilityBlockEntity
{
    private int tankTick = 0;

    public FluidTankBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity blockEntity) {
        if (++blockEntity.tankTick > 21) {
            blockEntity.tankTick = 0;
            blockEntity.tickFluidTank(level, pos, state, blockEntity);
        }
    }

    public void tickFluidTank(Level level, BlockPos pos, BlockState state, FluidTankBlockEntity blockEntity) {
        blockEntity.getCapability(CapabilityFluidHandler.FLUID_HANDLER_CAPABILITY).ifPresent(fluidHandler -> {
            FluidStack fluidStack = fluidHandler.getFluidInTank(0);
            if (fluidStack.getAmount() >= 0 && level instanceof ServerLevel) {
                blockEntity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY).ifPresent(invHandler -> {
                    ItemStack fluidContainerItem = invHandler.getStackInSlot(InventoryHandlerHelper.BOTTLE_SLOT);
                    ItemStack existingOutput = invHandler.getStackInSlot(InventoryHandlerHelper.FLUID_ITEM_OUTPUT_SLOT);
                    if (fluidContainerItem.getCount() > 0 && (existingOutput.isEmpty() || (existingOutput.getCount() < existingOutput.getMaxStackSize()))) {
                        // Look up bottler recipes from input
                        List<BottlerRecipe> recipes = new ArrayList<>();
                        Map<ResourceLocation, BottlerRecipe> allRecipes = level.getRecipeManager().byType(ModRecipeTypes.BOTTLER_TYPE.get());
                        for (Map.Entry<ResourceLocation, BottlerRecipe> entry : allRecipes.entrySet()) {
                            BottlerRecipe recipe = entry.getValue();
                            if (recipe.matches(fluidStack, fluidContainerItem)) {
                                recipes.add(recipe);
                            }
                        }

                        if (recipes.size() > 0) {
                            BottlerRecipe recipe = recipes.iterator().next();
                            if (existingOutput.isEmpty() || existingOutput.getItem().equals(recipe.getResultItem().getItem())) {
                                processOutput(fluidHandler, invHandler, recipe.getResultItem(), recipe.fluidInput.getSecond(), true);
                            }
                        } else if (fluidContainerItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).isPresent()) {
                            // try filling fluid container
                            fluidContainerItem.getCapability(CapabilityFluidHandler.FLUID_HANDLER_ITEM_CAPABILITY).ifPresent(h -> {
                                int amount = h.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
                                processOutput(fluidHandler, invHandler, h.getFluidInTank(0).getAmount() == h.getTankCapacity(0) ? fluidContainerItem : null, amount, false);
                            });
                        } else {
                            // try to fill bucket
                            FluidActionResult fillResult = FluidUtil.tryFillContainer(fluidContainerItem, fluidHandler, Integer.MAX_VALUE, null, true);
                            if (fillResult.isSuccess()) {
                                processOutput(fluidHandler, invHandler, fillResult.getResult(), 0, true);
                            }
                        }
                    }
                });
            }
        });
    }

    private static void processOutput(IFluidHandler fluidHandler, IItemHandler itemHandler, ItemStack outputItem, int drainedAmount, boolean shrinkInputStack) {
        if (shrinkInputStack) {
            itemHandler.getStackInSlot(InventoryHandlerHelper.BOTTLE_SLOT).shrink(1);
        }
        if (outputItem != null) {
            itemHandler.insertItem(InventoryHandlerHelper.FLUID_ITEM_OUTPUT_SLOT, outputItem, false);
        }
        fluidHandler.drain(drainedAmount, IFluidHandler.FluidAction.EXECUTE);
    }
}
