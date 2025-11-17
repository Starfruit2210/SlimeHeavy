package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.PacketEventsAPI;
import com.github.retrooper.packetevents.protocol.particle.Particle;
import com.github.retrooper.packetevents.protocol.particle.data.ParticleBlockStateData;
import com.github.retrooper.packetevents.protocol.particle.type.ParticleTypes;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.protocol.sound.Sound;
import com.github.retrooper.packetevents.protocol.sound.SoundCategory;
import com.github.retrooper.packetevents.protocol.sound.Sounds;
import com.github.retrooper.packetevents.protocol.world.states.WrappedBlockState;
import com.github.retrooper.packetevents.util.Vector3d;
import com.github.retrooper.packetevents.util.Vector3f;
import com.github.retrooper.packetevents.util.Vector3i;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerBundle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerParticle;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSoundEffect;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import io.github.bakedlibs.dough.protection.Interaction;
import io.github.thebusybiscuit.slimefun4.api.events.AndroidMineEvent;
import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.ItemSetting;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.services.sounds.SoundEffect;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.utils.InfiniteBlockGenerator;
import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import io.github.thebusybiscuit.slimefun4.utils.tags.SlimefunTag;

import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;

/**
 * The {@link MinerAndroid} is a variant of the {@link ProgrammableAndroid} which
 * is able to break blocks.
 * The core functionalities boil down to {@link #dig(Block, BlockMenu, Block)} and
 * {@link #moveAndDig(Block, BlockMenu, BlockFace, Block)}.
 * Otherwise the functionality is similar to a regular android.
 * <p>
 * The {@link MinerAndroid} will also fire an {@link AndroidMineEvent} when breaking a {@link Block}.
 * 
 * @author TheBusyBiscuit
 * @author creator3
 * @author poma123
 * @author Sfiguz7
 * @author CyberPatriot
 * @author Redemption198
 * @author Poslovitch
 * 
 * @see AndroidMineEvent
 *
 */
public class MinerAndroid extends ProgrammableAndroid {

    // Determines the drops a miner android will get
    private final ItemStack effectivePickaxe = new ItemStack(Material.DIAMOND_PICKAXE);
    private final Map<Material, ParticleBlockStateData> BLOCK_STATE_CACHE = new ConcurrentHashMap<>();

    // Enum
    private static final Set<Material> BLOCK_TYPE = EnumSet.noneOf(Material.class);
    static {
        BLOCK_TYPE.add(Material.STONE);
        BLOCK_TYPE.add(Material.COBBLESTONE);
        // Ore
        BLOCK_TYPE.add(Material.IRON_ORE);
        BLOCK_TYPE.add(Material.GOLD_ORE);
        BLOCK_TYPE.add(Material.DIAMOND_ORE);
        BLOCK_TYPE.add(Material.EMERALD_ORE);
        BLOCK_TYPE.add(Material.LAPIS_ORE);
        BLOCK_TYPE.add(Material.REDSTONE_ORE);
        BLOCK_TYPE.add(Material.COAL_ORE);
        BLOCK_TYPE.add(Material.COPPER_ORE);
        // Deepslate ore
        BLOCK_TYPE.add(Material.DEEPSLATE_IRON_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_GOLD_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_DIAMOND_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_EMERALD_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_LAPIS_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_REDSTONE_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_COAL_ORE);
        BLOCK_TYPE.add(Material.DEEPSLATE_COPPER_ORE);
        // Nether ore
        BLOCK_TYPE.add(Material.NETHER_QUARTZ_ORE);
        BLOCK_TYPE.add(Material.NETHER_GOLD_ORE);
        BLOCK_TYPE.add(Material.ANCIENT_DEBRIS);
    }

    private final ItemSetting<Boolean> firesEvent = new ItemSetting<>(this, "trigger-event-for-generators", false);
    private final ItemSetting<Boolean> applyOptimizations = new ItemSetting<>(this, "reduced-block-updates", true);

    @ParametersAreNonnullByDefault
    public MinerAndroid(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, tier, item, recipeType, recipe);

        addItemSetting(firesEvent, applyOptimizations);
    }

    @Override
    @Nonnull
    public AndroidType getAndroidType() {
        return AndroidType.MINER;
    }

    @Override
    @ParametersAreNonnullByDefault
    protected void dig(Block b, BlockMenu menu, Block block) {
        if (!block.isPreferredTool(effectivePickaxe)) {
            return;
        }

        if (!Tag.MINEABLE_PICKAXE.isTagged(block.getType())) {
            return;
        }

        if (block.isEmpty() || block.isLiquid()) return;
        if (!BLOCK_TYPE.contains(block.getType())) return;

        // Get the drops before breaking the block
        Collection<ItemStack> drops = DropRule.computeFastDrops(block.getType());

        if (!SlimefunTag.UNBREAKABLE_MATERIALS.isTagged(block.getType()) && !drops.isEmpty()) {
            String ownerRaw = BlockStorage.getLocationInfo(b.getLocation(), "owner");
            OfflinePlayer owner = null;
            if (ownerRaw != null) {
                try {
                    owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerRaw));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (!Slimefun.getProtectionManager().hasPermission(owner, block, Interaction.BREAK_BLOCK)) {
                return;
            }

            AndroidMineEvent event = new AndroidMineEvent(block, new AndroidInstance(this, b));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            // We only want to break non-Slimefun blocks
            if (!BlockStorage.hasBlockInfo(block)) {
                breakBlock(menu, drops, block);
            }
        }

    }

    @Override
    @ParametersAreNonnullByDefault
    protected void moveAndDig(Block b, BlockMenu menu, BlockFace face, Block block) {
        if (!block.isPreferredTool(effectivePickaxe)) {
            return;
        }

        if (!Tag.MINEABLE_PICKAXE.isTagged(block.getType())) {
            return;
        }

        if (block.isEmpty() || block.isLiquid()) return;
        if (!BLOCK_TYPE.contains(block.getType())) return;

        Collection<ItemStack> drops = DropRule.computeFastDrops(block.getType());

        if (!SlimefunTag.UNBREAKABLE_MATERIALS.isTagged(block.getType()) && !drops.isEmpty()) {
            String ownerRaw = BlockStorage.getLocationInfo(b.getLocation(), "owner");
            OfflinePlayer owner = null;
            if (ownerRaw != null) {
                try {
                    owner = Bukkit.getOfflinePlayer(UUID.fromString(ownerRaw));
                } catch (IllegalArgumentException ignored) {
                }
            }

            if (!Slimefun.getProtectionManager().hasPermission(owner, block, Interaction.BREAK_BLOCK)) {
                return;
            }

            AndroidMineEvent event = new AndroidMineEvent(block, new AndroidInstance(this, b));
            Bukkit.getPluginManager().callEvent(event);

            if (event.isCancelled()) {
                return;
            }

            // We only want to break non-Slimefun blocks
            if (!BlockStorage.hasBlockInfo(block)) {
                breakBlock(menu, drops, block);
                move(b, face, block);
            }
        } else {
            move(b, face, block);
        }
    }

    @ParametersAreNonnullByDefault
    private void breakBlock(BlockMenu blockMenu, Collection<ItemStack> blockDrops, Block targetBlock) {
        final World world = targetBlock.getWorld();

        final double centerX = targetBlock.getX() + 0.5;
        final double centerY = targetBlock.getY() + 1.0;
        final double centerZ = targetBlock.getZ() + 0.5;

        final Location effectLocation = new Location(world, centerX, centerY, centerZ);

        // Main-thread only: border & recipients snapshot
        if (!world.getWorldBorder().isInside(effectLocation)) {
            return;
        }
        final List<Player> recipients = new ArrayList<>(world.getNearbyPlayers(effectLocation, 16.0));
        final Material brokenMaterial = targetBlock.getType();

        // Kirim particle pecahan block (BLOCK) – async & hanya jika ada penerima
        if (!recipients.isEmpty()) {
            Bukkit.getScheduler().runTaskAsynchronously(Slimefun.instance(), () -> {
                ParticleBlockStateData cachedState = BLOCK_STATE_CACHE.computeIfAbsent(brokenMaterial, mat -> {
                    WrappedBlockState wrapped = WrappedBlockState.getByString(mat.getKey().toString());
                    return new ParticleBlockStateData(wrapped);
                });

                Particle<?> blockParticle = new Particle<>(ParticleTypes.BLOCK, cachedState);
                WrapperPlayServerParticle blockPacket = new WrapperPlayServerParticle(
                        blockParticle, true,
                        new Vector3d(centerX, centerY, centerZ),
                        new Vector3f(0.35f, 0.35f, 0.35f),
                        0.02f, 20
                );

                for (Player player : recipients) {
                    if (player.isOnline()) {
                        PacketEvents.getAPI().getPlayerManager().sendPacket(player, blockPacket);
                    }
                }
            });
        }

        // Merge & push drops (main thread)
        Map<ItemStack, Integer> merged = new LinkedHashMap<>();
        for (ItemStack drop : blockDrops) {
            if (drop == null || drop.getType().isAir()) continue;

            boolean appended = false;
            for (Map.Entry<ItemStack, Integer> entry : merged.entrySet()) {
                if (entry.getKey().isSimilar(drop)) {
                    entry.setValue(entry.getValue() + drop.getAmount());
                    appended = true;
                    break;
                }
            }
            if (!appended) {
                ItemStack template = drop.clone();
                template.setAmount(1);
                merged.put(template, drop.getAmount());
            }
        }
        for (Map.Entry<ItemStack, Integer> entry : merged.entrySet()) {
            ItemStack stack = entry.getKey().clone();
            int remaining = entry.getValue();
            while (remaining > 0) {
                int add = Math.min(remaining, stack.getMaxStackSize());
                stack.setAmount(add);
                blockMenu.pushItem(stack, getOutputSlots());
                remaining -= add;
            }
        }

        // Generator optimizations (main thread) + satu async untuk SMOKE + SFX
        if (applyOptimizations.getValue()) {
            InfiniteBlockGenerator generator = InfiniteBlockGenerator.findAt(targetBlock);

            if (generator != null) {
                if (firesEvent.getValue()) {
                    generator.callEvent(targetBlock);
                }

                if (!recipients.isEmpty()) {
                    final int soundX = (int) Math.floor((targetBlock.getX() + 0.5) * 8.0);
                    final int soundY = (int) Math.floor((targetBlock.getY() + 1.0) * 8.0);
                    final int soundZ = (int) Math.floor((targetBlock.getZ() + 0.5) * 8.0);
                    final long randomSeed = java.util.concurrent.ThreadLocalRandom.current().nextLong();

                    Bukkit.getScheduler().runTaskAsynchronously(Slimefun.instance(), () -> {
                        WrapperPlayServerSoundEffect soundPacket = new WrapperPlayServerSoundEffect(
                                Sounds.BLOCK_FIRE_EXTINGUISH, SoundCategory.BLOCK,
                                new Vector3i(soundX, soundY, soundZ),
                                0.075F, 0.8F, randomSeed
                        );

                        Particle<?> smokeParticle = new Particle<>(ParticleTypes.SMOKE);
                        WrapperPlayServerParticle smokePacket = new WrapperPlayServerParticle(
                                smokeParticle, true,
                                new Vector3d(centerX, centerY, centerZ),
                                new Vector3f(0.5f, 0.5f, 0.5f),
                                0.015f, 8
                        );

                        for (Player player : recipients) {
                            if (player.isOnline()) {
                                PacketEvents.getAPI().getPlayerManager().sendPacket(player, smokePacket);
                                PacketEvents.getAPI().getPlayerManager().sendPacket(player, soundPacket);
                            }
                        }
                    });
                }
            } else if (targetBlock.getType() != Material.AIR) {
                targetBlock.setType(Material.AIR, false);
            }
        } else if (targetBlock.getType() != Material.AIR) {
            targetBlock.setType(Material.AIR, false);
        }
    }

    private final static class DropRule {
        private final Material material;
        private final int min;
        private final int max;
        private final ItemStack drop;

        private static final Map<Material, DropRule> FAST_DROP_RULES = new EnumMap<>(Material.class);
        static {
            // Stone & cobble
            FAST_DROP_RULES.put(Material.STONE, DropRule.single(new ItemStack(Material.COBBLESTONE, 1)));
            FAST_DROP_RULES.put(Material.COBBLESTONE, DropRule.single(new ItemStack(Material.COBBLESTONE, 1)));

            // Overworld ores
            FAST_DROP_RULES.put(Material.COAL_ORE,        DropRule.single(new ItemStack(Material.COAL, 1)));
            FAST_DROP_RULES.put(Material.IRON_ORE,        DropRule.single(new ItemStack(Material.RAW_IRON, 1)));
            FAST_DROP_RULES.put(Material.GOLD_ORE,        DropRule.single(new ItemStack(Material.RAW_GOLD, 1)));
            FAST_DROP_RULES.put(Material.COPPER_ORE,      DropRule.range(Material.RAW_COPPER, 2, 5));
            FAST_DROP_RULES.put(Material.DIAMOND_ORE,     DropRule.single(new ItemStack(Material.DIAMOND, 1)));
            FAST_DROP_RULES.put(Material.EMERALD_ORE,     DropRule.single(new ItemStack(Material.EMERALD, 1)));
            FAST_DROP_RULES.put(Material.LAPIS_ORE,       DropRule.range(Material.LAPIS_LAZULI, 4, 9));
            FAST_DROP_RULES.put(Material.REDSTONE_ORE,    DropRule.range(Material.REDSTONE, 4, 5));

            // Deepslate
            FAST_DROP_RULES.put(Material.DEEPSLATE_COAL_ORE,     DropRule.single(new ItemStack(Material.COAL, 1)));
            FAST_DROP_RULES.put(Material.DEEPSLATE_IRON_ORE,     DropRule.single(new ItemStack(Material.RAW_IRON, 1)));
            FAST_DROP_RULES.put(Material.DEEPSLATE_GOLD_ORE,     DropRule.single(new ItemStack(Material.RAW_GOLD, 1)));
            FAST_DROP_RULES.put(Material.DEEPSLATE_COPPER_ORE,   DropRule.range(Material.RAW_COPPER, 2, 5));
            FAST_DROP_RULES.put(Material.DEEPSLATE_DIAMOND_ORE,  DropRule.single(new ItemStack(Material.DIAMOND, 1)));
            FAST_DROP_RULES.put(Material.DEEPSLATE_EMERALD_ORE,  DropRule.single(new ItemStack(Material.EMERALD, 1)));
            FAST_DROP_RULES.put(Material.DEEPSLATE_LAPIS_ORE,    DropRule.range(Material.LAPIS_LAZULI, 4, 9));
            FAST_DROP_RULES.put(Material.DEEPSLATE_REDSTONE_ORE, DropRule.range(Material.REDSTONE, 4, 5));

            // Nether
            FAST_DROP_RULES.put(Material.NETHER_QUARTZ_ORE, DropRule.single(new ItemStack(Material.QUARTZ, 1)));
            FAST_DROP_RULES.put(Material.NETHER_GOLD_ORE,   DropRule.range(Material.GOLD_NUGGET, 2, 6));
            FAST_DROP_RULES.put(Material.ANCIENT_DEBRIS,    DropRule.single(new ItemStack(Material.ANCIENT_DEBRIS, 1)));
        }

        private DropRule(Material material, int min, int max) {
            this.material = material;
            this.min = min;
            this.max = max;
            this.drop = null;
        }

        public DropRule(ItemStack fixedDrop) {
            this.material = null;
            this.min = 0;
            this.max = 0;
            this.drop = fixedDrop;
        }

        static DropRule single(ItemStack stack) {
            return new DropRule(stack);
        }
        static DropRule range(Material type, int min, int max) {
            return new DropRule(type, min, max);
        }

        public ItemStack create(ThreadLocalRandom rng) {
            if (drop != null) return drop.clone();
            int amount = min == max ? min : rng.nextInt(min, max + 1);
            return new ItemStack(material, amount);
        }

        public static List<ItemStack> computeFastDrops(Material brokenMaterial) {
            DropRule rule = FAST_DROP_RULES.get(brokenMaterial);
            if (rule == null) {
                // fallback jarang terjadi, tapi biar aman kalo kepanggil di masa depan
                return Collections.emptyList();
            }
            ItemStack drop = rule.create(ThreadLocalRandom.current());
            return drop.getAmount() > 0 ? Collections.singletonList(drop) : Collections.emptyList();
        }
    }

}
