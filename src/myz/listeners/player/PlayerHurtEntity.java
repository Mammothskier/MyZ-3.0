/**
 * 
 */
package myz.listeners.player;

import java.util.Random;

import myz.support.interfacing.Configuration;
import myz.support.interfacing.Messenger;
import myz.utilities.Validate;

import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.PigZombie;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

/**
 * @author Jordan
 * 
 */
public class PlayerHurtEntity implements Listener {

	private static final Random random = new Random();

	/**
	 * Whether or not a material is an axe material.
	 * 
	 * @param material
	 *            The material to compare.
	 * @return True if the material is a wooden, stone, gold, iron or diamond
	 *         axe.
	 */
	private boolean isAxe(Material material) {
		return material == Material.WOOD_AXE || material == Material.STONE_AXE || material == Material.GOLD_AXE
				|| material == Material.IRON_AXE || material == Material.DIAMOND_AXE;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	private void onDamage(EntityDamageByEntityEvent e) {
		if (!Validate.inWorld(e.getEntity().getLocation()))
			return;
		// Cancel damage inside spawn room.
		if (Configuration.isInLobby(e.getDamager().getLocation()) && e.getEntityType() == EntityType.PLAYER)
			e.setCancelled(true);

		// Do widespread hits with axes when crouched.
		if (e.getDamager() instanceof Player && isAxe(((Player) e.getDamager()).getItemInHand().getType())
				&& ((Player) e.getDamager()).isSneaking())
			if (random.nextDouble() <= 0.33)
				for (Entity nearby : e.getDamager().getNearbyEntities(1.5, 2, 1.5))
					if (nearby instanceof Zombie || nearby instanceof PigZombie || nearby instanceof Horse || nearby instanceof Player
							&& nearby != e.getDamager()) {
						LivingEntity living = (LivingEntity) nearby;
						living.damage(e.getDamage(), e.getDamager());
					}
		// Do headshots and pulling
		if (e.getCause() == DamageCause.PROJECTILE) {
			Projectile projectile = (Projectile) e.getDamager();
			if (wasHeadshot(e.getEntity(), projectile))
				e.setDamage(e.getDamage() * 2);
		} else if (e.getCause() == DamageCause.ENTITY_ATTACK && e.getDamager() instanceof Player) {
			Location otherLocation = e.getEntity().getLocation();
			Location playerLocation = e.getDamager().getLocation();

			// Pull a player off a ledge.
			if (playerLocation.distance(otherLocation) >= 1 && playerLocation.getY() > otherLocation.getY())
				e.getDamager().setVelocity(otherLocation.toVector().subtract(playerLocation.toVector()).normalize().multiply(0.15));
		}

		if (!e.isCancelled())
			// Bleeding effect (not PDE but for general EDE)
			if ((Boolean) Configuration.getConfig("mobs.bleed") && e.getDamage() > 0) {
				e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, 55);
				e.getEntity().getWorld().playEffect(e.getEntity().getLocation(), Effect.STEP_SOUND, 55);
			}
	}

	/**
	 * Whether or not a specific EntityType resembles a human.
	 * 
	 * @param type
	 *            The EntityType.
	 * @return True if the type is a player, zombie, pigman, skeleton or
	 *         creeper.
	 */
	private boolean isHumanoid(EntityType type) {
		return type == EntityType.PLAYER || type == EntityType.ZOMBIE || type == EntityType.PIG_ZOMBIE || type == EntityType.SKELETON
				|| type == EntityType.CREEPER;
	}

	/**
	 * Whether or not a shot was a headshot on a specified entity.
	 * 
	 * @param entity
	 *            The Entity that was hit by the projectile.
	 * @param arrow
	 *            The projectile.
	 * @return True if
	 */
	private boolean wasHeadshot(Entity entity, Projectile arrow) {
		if (!(arrow instanceof Arrow) || !(arrow.getShooter() instanceof Player))
			return false;

		if (!isHumanoid(entity.getType()))
			return false;

		double projectileY = arrow.getLocation().getY();
		double entityY = entity.getLocation().getY();
		boolean headshot = projectileY - entityY > 1.75d;

		if (headshot)
			Messenger.sendConfigMessage((Player) arrow.getShooter(), "damage.headshot");
		return headshot;
	}
}
