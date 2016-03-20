package com.whammich.sstow.item;

import com.google.common.base.Strings;
import com.whammich.repack.tehnut.lib.annot.Handler;
import com.whammich.repack.tehnut.lib.annot.ModItem;
import com.whammich.repack.tehnut.lib.annot.Used;
import com.whammich.repack.tehnut.lib.iface.IMeshProvider;
import com.whammich.repack.tehnut.lib.util.BlockStack;
import com.whammich.repack.tehnut.lib.util.TextHelper;
import com.whammich.sstow.ConfigHandler;
import com.whammich.sstow.SoulShardsTOW;
import com.whammich.sstow.api.ISoulShard;
import com.whammich.sstow.api.ISoulWeapon;
import com.whammich.sstow.api.ShardHelper;
import com.whammich.sstow.api.SoulShardsAPI;
import com.whammich.sstow.registry.ModEnchantments;
import com.whammich.sstow.registry.ModItems;
import com.whammich.sstow.util.EntityMapper;
import com.whammich.sstow.util.PosWithStack;
import com.whammich.sstow.util.TierHandler;
import com.whammich.sstow.util.Utils;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.monster.EntitySkeleton;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@ModItem(name = "ItemSoulShard")
@Used
@Handler
public class ItemSoulShard extends Item implements ISoulShard, IMeshProvider {

    public static List<PosWithStack> multiblock = new ArrayList<PosWithStack>();
    public static BlockStack originBlock = null;

    public ItemSoulShard() {
        super();

        setUnlocalizedName(SoulShardsTOW.MODID + ".shard");
        setCreativeTab(SoulShardsTOW.soulShardsTab);
        setMaxStackSize(1);
        setHasSubtypes(true);
    }

    @Override
    public EnumActionResult onItemUse(ItemStack stack, EntityPlayer player, World world, BlockPos pos, EnumHand hand, EnumFacing facing, float hitX, float hitY, float hitZ) {
        if (!Utils.hasMaxedKills(stack) && ConfigHandler.allowSpawnerAbsorption) {
            TileEntity tile = world.getTileEntity(pos);

            if (tile instanceof TileEntityMobSpawner) {
                String name = ObfuscationReflectionHelper.getPrivateValue(MobSpawnerBaseLogic.class, ((TileEntityMobSpawner) tile).getSpawnerBaseLogic(), "mobID", "field_98288_a");
                EntityLiving ent = EntityMapper.getNewEntityInstance(world, name, pos);

                if (ent == null)
                    return EnumActionResult.FAIL;

                if (!EntityMapper.isEntityValid(name) || SoulShardsAPI.isEntityBlacklisted(ent))
                    return EnumActionResult.FAIL;

                if (ent instanceof EntitySkeleton && ((EntitySkeleton) ent).getSkeletonType() == 1)
                    name = "Wither Skeleton";

                if (ShardHelper.isBound(stack) && ShardHelper.getBoundEntity(stack).equals(name)) {
                    if (!world.isRemote)
                        Utils.increaseShardKillCount(stack, ConfigHandler.spawnerAbsorptionBonus);
                    world.destroyBlock(pos, false);
                    return EnumActionResult.SUCCESS;
                }
            }
        }

        return EnumActionResult.FAIL;
    }

    @Override
    public boolean showDurabilityBar(ItemStack stack) {
        return ConfigHandler.displayDurabilityBar && ShardHelper.getKillsFromShard(stack) < TierHandler.getMaxKills(TierHandler.tiers.size() - 1);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack stack) {
        return 1.0D - ((double) ShardHelper.getKillsFromShard(stack) / (double) TierHandler.getTier(TierHandler.tiers.size() - 1).getMinKills());
    }

    @Override
    public String getUnlocalizedName(ItemStack stack) {
        return super.getUnlocalizedName(stack) + (ShardHelper.isBound(stack) ? "" : ".unbound");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack stack) {
        return Utils.hasMaxedKills(stack);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(Item item, CreativeTabs tabs, List<ItemStack> list) {
        for (int i = 0; i <= TierHandler.tiers.size() - 1; i++) {
            ItemStack stack = new ItemStack(item, 1);

            ShardHelper.setKillsForShard(stack, TierHandler.getMinKills(i));
            ShardHelper.setTierForShard(stack, i);

            list.add(stack);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean bool) {
        if (ShardHelper.isBound(stack))
            list.add(TextHelper.localizeEffect("tooltip.SoulShardsTOW.bound", Utils.getEntityNameTranslated(ShardHelper.getBoundEntity(stack))));

        if (ShardHelper.getKillsFromShard(stack) >= 0)
            list.add(TextHelper.localizeEffect("tooltip.SoulShardsTOW.kills", ShardHelper.getKillsFromShard(stack)));

        list.add(TextHelper.localizeEffect("tooltip.SoulShardsTOW.tier", ShardHelper.getTierFromShard(stack)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public ItemMeshDefinition getMeshDefinition() {
        return new ItemMeshDefinition() {
            @Override
            public ModelResourceLocation getModelLocation(ItemStack stack) {
                if (ShardHelper.isBound(stack))
                    return new ModelResourceLocation(new ResourceLocation("soulshardstow:item/ItemSoulShard"), "tier=" + ShardHelper.getTierFromShard(stack));

                return new ModelResourceLocation(new ResourceLocation("soulshardstow:item/ItemSoulShard"), "tier=unbound");
            }
        };
    }

    @Override
    public List<String> getVariants() {
        List<String> ret = new ArrayList<String>();
        ret.add("tier=unbound");
        for (int i = 0; i < TierHandler.tiers.size(); i++)
            ret.add("tier=" + i);
        return ret;
    }

    public static void buildMultiblock() {
        originBlock = new BlockStack(Blocks.glowstone);
        multiblock.add(new PosWithStack(new BlockPos(0, 0, 0), new BlockStack(Blocks.glowstone)));
        multiblock.add(new PosWithStack(new BlockPos(1, 0, 0), new BlockStack(Blocks.end_stone)));
        multiblock.add(new PosWithStack(new BlockPos(-1, 0, 0), new BlockStack(Blocks.end_stone)));
        multiblock.add(new PosWithStack(new BlockPos(0, 0, 1), new BlockStack(Blocks.end_stone)));
        multiblock.add(new PosWithStack(new BlockPos(0, 0, -1), new BlockStack(Blocks.end_stone)));
        multiblock.add(new PosWithStack(new BlockPos(1, 0, 1), new BlockStack(Blocks.obsidian)));
        multiblock.add(new PosWithStack(new BlockPos(1, 0, -1), new BlockStack(Blocks.obsidian)));
        multiblock.add(new PosWithStack(new BlockPos(-1, 0, 1), new BlockStack(Blocks.obsidian)));
        multiblock.add(new PosWithStack(new BlockPos(-1, 0, -1), new BlockStack(Blocks.obsidian)));
    }

    @SubscribeEvent
    @Used
    public void onEntityKill(LivingDeathEvent event) {
        World world = event.entity.worldObj;

        if (world.isRemote || !(event.entity instanceof EntityLiving) || !(event.source.getEntity() instanceof EntityPlayer) || event.source.getEntity() instanceof FakePlayer)
            return;

        EntityLiving dead = (EntityLiving) event.entity;
        EntityPlayer player = (EntityPlayer) event.source.getEntity();
        String entName = EntityList.getEntityString(dead);

        if (Strings.isNullOrEmpty(entName)) {
            SoulShardsTOW.instance.getLogHelper().severe("Player killed an entity with no mapped name: {}", dead);
            return;
        }

        if (dead instanceof EntitySkeleton && ((EntitySkeleton) dead).getSkeletonType() == 1)
            entName = "Wither Skeleton";

        ItemStack shard = Utils.getShardFromInv(player, entName);

        if (shard == null)
            return;

        if (!ConfigHandler.entityList.contains(entName) || SoulShardsAPI.isEntityBlacklisted(dead))
            return;

        if (!EntityMapper.isEntityValid(entName))
            return;

        if (!ConfigHandler.countCageBornForShard && Utils.isCageBorn(dead))
            return;

        if (!ShardHelper.isBound(shard))
            ShardHelper.setBoundEntity(shard, entName);

        int soulStealer = EnchantmentHelper.getEnchantmentLevel(ModEnchantments.soulStealer, player.getHeldItemMainhand());
        soulStealer *= ConfigHandler.soulStealerBonus;

        if (player.getHeldItemMainhand() != null && player.getHeldItemMainhand().getItem() instanceof ISoulWeapon)
            soulStealer += ((ISoulWeapon) player.getHeldItemMainhand().getItem()).getBonusSouls(player.getHeldItemMainhand());

        Utils.increaseShardKillCount(shard, 1 + soulStealer);
    }

    @SubscribeEvent
    @Used
    public void onInteract(PlayerInteractEvent event) {
        if (multiblock.isEmpty())
            buildMultiblock();

        // TODO - Change back to `PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK` when Forge re-implements
        if (event.action != PlayerInteractEvent.Action.LEFT_CLICK_BLOCK)
            return;

        if (event.entityPlayer.getHeldItemMainhand() != null && event.entityPlayer.getHeldItemMainhand().getItem() == Items.diamond && originBlock.equals(BlockStack.getStackFromPos(event.world, event.pos))) {
            for (PosWithStack posWithStack : multiblock) {
                BlockStack worldStack = BlockStack.getStackFromPos(event.world, event.pos.add(posWithStack.getPos()));
                if (!posWithStack.getBlock().equals(worldStack))
                    return;
            }

            for (PosWithStack posWithStack : multiblock)
                event.world.destroyBlock(event.pos.add(posWithStack.getPos()), false);

            if (!event.world.isRemote) {
                EntityItem invItem = new EntityItem(event.world, event.entityPlayer.posX, event.entityPlayer.posY + 0.25, event.entityPlayer.posZ, new ItemStack(ModItems.getItem(getClass()), 1, 0));
                event.world.spawnEntityInWorld(invItem);
            }
            if (!event.entityPlayer.capabilities.isCreativeMode)
                event.entityPlayer.getHeldItemMainhand().stackSize--;
            event.entityPlayer.swingArm(EnumHand.MAIN_HAND);
        }
    }
}
