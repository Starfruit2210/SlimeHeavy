package io.github.thebusybiscuit.slimefun4.implementation.items.androids;

import java.util.function.Predicate;

import javax.annotation.ParametersAreNonnullByDefault;

import io.github.thebusybiscuit.slimefun4.utils.compatibility.VersionedParticle;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItemStack;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;

public class ButcherAndroid extends ProgrammableAndroid {

    private static final String METADATA_KEY = "android_killer";

    @ParametersAreNonnullByDefault
    public ButcherAndroid(ItemGroup itemGroup, int tier, SlimefunItemStack item, RecipeType recipeType, ItemStack[] recipe) {
        super(itemGroup, tier, item, recipeType, recipe);
    }

    @Override
    public AndroidType getAndroidType() {
        return AndroidType.FIGHTER;
    }

    @Override
    protected void attack(Block b, BlockFace face, Predicate<LivingEntity> predicate) {
        double damage = getTier() >= 3 ? 20D : 4D * getTier();
        double radius = 4.0 + getTier();

        double originX = b.getX() + 0.5;
        double originY = b.getY() + 1.0;
        double originZ = b.getZ() + 0.5;

        double dirX, dirY = 0.0, dirZ;
        switch (face) {
            case NORTH -> { dirX =  0.0; dirZ = -1.0; }
            case SOUTH -> { dirX =  0.0; dirZ =  1.0; }
            case WEST  -> { dirX = -1.0; dirZ =  0.0; }
            case EAST  -> { dirX =  1.0; dirZ =  0.0; }
            default    -> { return; }
        }

        final World world = b.getWorld();

        final RayTraceResult hit = world.rayTraceEntities(
                new Location(world, originX, originY, originZ),
                new Vector(dirX, dirY, dirZ), radius, entity -> {
                    if (!(entity instanceof LivingEntity living)) return false;
                    if (entity instanceof ArmorStand) return false;
                    if (entity instanceof Player) return false;
                    if (!entity.isValid()) return false;
                    return predicate.test(living);
                }
        );

        if (hit == null) return;

        final Entity entityHit = hit.getHitEntity();
        if (!(entityHit instanceof LivingEntity livingEntity)) return;
        if (entityHit.isDead()) return;

        // Attach/refresh metadata only once (no remove then set)
        entityHit.setMetadata(METADATA_KEY,
                new FixedMetadataValue(Slimefun.instance(), new AndroidInstance(this, b)));

        livingEntity.damage(damage);

        // (optional) one small particle for feedback; schedule on-region if Folia
        world.spawnParticle(VersionedParticle.SMOKE, originX, originY + 0.25, originZ, 8, 0.5, 0.5, 0.5, 0.015);
    }

}
