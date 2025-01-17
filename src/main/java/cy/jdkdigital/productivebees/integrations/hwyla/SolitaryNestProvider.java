package cy.jdkdigital.productivebees.integrations.hwyla;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.common.block.entity.AdvancedBeehiveBlockEntityAbstract;
import cy.jdkdigital.productivebees.common.block.entity.SolitaryNestBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.IServerDataProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.List;

public class SolitaryNestProvider implements IBlockComponentProvider, IServerDataProvider<BlockEntity>
{
    public static final ResourceLocation UID = new ResourceLocation(ProductiveBees.MODID, "solitary_nest");

    static final SolitaryNestProvider INSTANCE = new SolitaryNestProvider();

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        if (!(accessor.getBlockEntity() instanceof SolitaryNestBlockEntity tileEntity)) {
            return;
        }

        tileEntity.loadPacketNBT(accessor.getServerData());

        List<AdvancedBeehiveBlockEntityAbstract.Inhabitant> bees = tileEntity.getBeeList();
        if (!bees.isEmpty()) {
            tooltip.add(Component.translatable("productivebees.top.solitary.bee", bees.get(0).localizedName));
        } else {
            int cooldown = tileEntity.getNestTickCooldown();
            if (cooldown > 0) {
                tooltip.add(Component.translatable("productivebees.top.solitary.repopulation_countdown", Math.round(cooldown / 20f) + "s"));
            } else {
                tooltip.add(Component.translatable("productivebees.top.solitary.repopulation_countdown_inactive"));
                if (tileEntity.canRepopulate()) {
                    tooltip.add(Component.translatable("productivebees.top.solitary.can_repopulate_true"));
                } else {
                    tooltip.add(Component.translatable("productivebees.top.solitary.can_repopulate_false"));
                }
            }
        }
    }

    public void appendServerData(CompoundTag tag, ServerPlayer player, Level world, BlockEntity te, boolean showDetails) {
        tag.getAllKeys().clear();
        if (te instanceof SolitaryNestBlockEntity nest) {
            nest.savePacketNBT(tag);
        }
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
