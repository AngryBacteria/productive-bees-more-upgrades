package cy.jdkdigital.productivebees.event;

import cy.jdkdigital.productivebees.ProductiveBees;
import cy.jdkdigital.productivebees.ProductiveBeesConfig;
import cy.jdkdigital.productivebees.common.entity.bee.ConfigurableBee;
import cy.jdkdigital.productivebees.common.entity.bee.ProductiveBee;
import cy.jdkdigital.productivebees.common.entity.bee.solitary.BlueBandedBee;
import cy.jdkdigital.productivebees.handler.bee.IInhabitantStorage;
import cy.jdkdigital.productivebees.init.ModAdvancements;
import cy.jdkdigital.productivebees.init.ModBlocks;
import cy.jdkdigital.productivebees.init.ModEntities;
import cy.jdkdigital.productivebees.init.ModItems;
import cy.jdkdigital.productivebees.setup.BeeReloadListener;
import cy.jdkdigital.productivebees.util.BeeHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.furnace.FurnaceFuelBurnTimeEvent;
import net.minecraftforge.event.world.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = ProductiveBees.MODID)
public class EventHandler
{
    @SubscribeEvent
    public static void entityRightClicked(PlayerInteractEvent.EntityInteract entityInteract) {
        ItemStack itemStack = entityInteract.getItemStack();
        Entity entity = entityInteract.getTarget();

        if (!itemStack.isEmpty() && entity instanceof Bee) {
            Level world = entityInteract.getWorld();
            if (world instanceof ServerLevel) {
                Player player = entityInteract.getPlayer();
                BlockPos pos = entity.blockPosition();

                Entity newBee = BeeHelper.itemInteract((Bee) entity, itemStack, (ServerLevel) world, entity.serializeNBT(), player);

                if (newBee != null) {
                    // PLay event with smoke
                    world.addParticle(ParticleTypes.POOF, pos.getX(), pos.getY() + 1, pos.getZ(), 0.2D, 0.1D, 0.2D);
                    world.playSound(player, pos.getX(), pos.getY(), pos.getZ(), SoundEvents.BEEHIVE_WORK, SoundSource.NEUTRAL, 1.0F, 1.0F);

                    world.addFreshEntity(newBee);
                    entity.discard();
                }
            }
        }
    }

    @SubscribeEvent
    public static void cocoaBreakSpawn(BlockEvent.BreakEvent event) {
        if (event.getState().getBlock().equals(Blocks.COCOA) && event.getState().getValue(CocoaBlock.AGE) == 2) {
            Player player = event.getPlayer();
            Level level = player.level;
            if (level instanceof ServerLevel && player instanceof ServerPlayer && level.random.nextFloat() < ProductiveBeesConfig.BEES.sugarbagBeeChance.get()) {
                ConfigurableBee bee = ModEntities.CONFIGURABLE_BEE.get().create(level);
                BlockPos pos = event.getPos();
                if (bee != null && BeeReloadListener.INSTANCE.getData("productivebees:sugarbag") != null) {
                    bee.setBeeType("productivebees:sugarbag");
                    bee.setAttributes();

                    bee.moveTo(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, bee.getYRot(), bee.getXRot());

                    level.addParticle(ParticleTypes.POOF, pos.getX(), pos.getY() + 1, pos.getZ(), 0.2D, 0.1D, 0.2D);
                    level.playSound(player, pos, SoundEvents.BEEHIVE_WORK, SoundSource.NEUTRAL, 1.0F, 1.0F);

                    level.addFreshEntity(bee);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntityAttributeCreate(EntityAttributeCreationEvent event) {
        // Entity attribute assignments
        for (RegistryObject<EntityType<?>> registryObject : ModEntities.HIVE_BEES.getEntries()) {
            EntityType<ProductiveBee> bee = (EntityType<ProductiveBee>) registryObject.get();
            event.put(bee, Bee.createAttributes().build());
        }
        for (RegistryObject<EntityType<?>> registryObject : ModEntities.SOLITARY_BEES.getEntries()) {
            EntityType<ProductiveBee> bee = (EntityType<ProductiveBee>) registryObject.get();
            if (!bee.getDescriptionId().contains("blue_banded_bee")) {
                event.put(bee, Bee.createAttributes().build());
            }
        }
        event.put(ModEntities.BLUE_BANDED_BEE.get(), BlueBandedBee.getDefaultAttributes().build());
    }

    @SubscribeEvent
    public static void fuelValues(FurnaceFuelBurnTimeEvent event) {
        Item item = event.getItemStack().getItem();
        if (item.equals(ModItems.WAX.get())) {
            event.setBurnTime(100);
        } else if (item.equals(ModBlocks.WAX_BLOCK.get().asItem())) {
            event.setBurnTime(900);
        }
    }

    @SubscribeEvent
    public static void onItemFished(ItemFishedEvent event) {
        Player player = event.getPlayer();
        if (player != null) {
            boolean willSpawn = player.level.random.nextDouble() < ProductiveBeesConfig.BEES.fishingBeeChance.get();
            int fishingLuck = EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FISHING_LUCK, player.getMainHandItem());
            for (int i = 0; i < (1 + fishingLuck); i++) {
                willSpawn = willSpawn || player.level.random.nextDouble() < ProductiveBeesConfig.BEES.fishingBeeChance.get();
            }

            if (willSpawn) {
                ConfigurableBee bee = ModEntities.CONFIGURABLE_BEE.get().create(player.level);
                BlockPos pos = event.getHookEntity().blockPosition();
                if (bee != null && BeeReloadListener.INSTANCE.getData("productivebees:prismarine") != null) {
                    Holder<Biome> fishingBiome = player.level.getBiome(pos);
                    if (fishingBiome.is(BiomeTags.IS_OCEAN) || fishingBiome.is(BiomeTags.IS_DEEP_OCEAN)) {
                        bee.setBeeType("productivebees:prismarine");
                        if (BeeReloadListener.INSTANCE.getData("productivebees:oily") != null && player.level.random.nextBoolean()) {
                            bee.setBeeType("productivebees:oily");
                        }
                        bee.setAttributes();

                        bee.moveTo(pos.getX() + 0.5D, pos.getY() + 1, pos.getZ() + 0.5D, bee.getYRot(), bee.getXRot());

                        player.level.addParticle(ParticleTypes.POOF, pos.getX(), pos.getY() + 1, pos.getZ(), 0.2D, 0.1D, 0.2D);
                        player.level.playSound(player, pos, SoundEvents.BEE_HURT, SoundSource.NEUTRAL, 1.0F, 1.0F);

                        player.level.addFreshEntity(bee);

                        bee.setTarget(player);

                        ModAdvancements.FISH_BEE.trigger((ServerPlayer) player, bee);
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public void registerCaps(RegisterCapabilitiesEvent event) {
        event.register(IInhabitantStorage.class);
    }
}
