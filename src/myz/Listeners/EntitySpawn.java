/**
 * 
 */
package myz.Listeners;

import java.util.Random;

import myz.MyZ;
import myz.Support.Configuration;
import myz.Utilities.WorldGuardManager;
import myz.mobs.support.EntityCreator;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Horse.Variant;
import org.bukkit.entity.Zombie;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

/**
 * @author Jordan
 * 
 */
public class EntitySpawn implements Listener {

	private Random random = new Random();

	@EventHandler(priority = EventPriority.LOWEST)
	private void onSpawn(CreatureSpawnEvent e) {
		// MultiWorld support, yay!
		if (!MyZ.instance.getWorlds().contains(e.getLocation().getWorld().getName())) {
			if (e.getEntity().getMetadata("MyZ.bypass") != null && !e.getEntity().getMetadata("MyZ.bypass").isEmpty())
				return;
			switch (e.getEntityType()) {
			case HORSE:
			case PIG_ZOMBIE:
			case ZOMBIE:
			case SKELETON:
				EntityCreator.create(e.getLocation(), e.getEntityType(), e.getSpawnReason());
				e.setCancelled(true);
				return;
			default:
				return;
			}
		}

		// Cancel spawning inside spawn room.
		if (Configuration.isInLobby(e.getEntity().getLocation())) {
			e.setCancelled(true);
			return;
		}

		if (isTooFarFromLandmark(e.getEntity())) {
			e.setCancelled(true);
			return;
		}

		EntityType type = e.getEntityType();

		// Override mooshroom spawns with giant spawns.
		if (e.getSpawnReason() == SpawnReason.SPAWNER_EGG && e.getEntityType() == EntityType.MUSHROOM_COW) {
			EntityCreator.create(e.getLocation(), EntityType.GIANT, SpawnReason.CUSTOM);
			e.setCancelled(true);
			return;
		}

		// Override villager trades.
		if (e.getEntityType() == EntityType.VILLAGER)
			EntityCreator.overrideVillager(e.getEntity());

		if (type == EntityType.ZOMBIE && random.nextDouble() <= 0.1 && e.getSpawnReason() != SpawnReason.CUSTOM && Configuration.isNPC()) {
			e.setCancelled(true);
			if (random.nextDouble() <= 0.9)
				return;
			EntityCreator.disguiseNPC(e.getLocation());
			return;
		}

		if (e.getSpawnReason() != SpawnReason.DEFAULT && e.getSpawnReason() != SpawnReason.CHUNK_GEN
				&& e.getSpawnReason() != SpawnReason.NATURAL && e.getSpawnReason() != SpawnReason.VILLAGE_INVASION)
			return;

		if (MyZ.instance.getServer().getPluginManager().isPluginEnabled("WorldGuard")) {
			if (!WorldGuardManager.isMobSpawning(e.getLocation())) {
				e.setCancelled(true);
				return;
			}
			if (WorldGuardManager.isAmplifiedRegion(e.getLocation())) {
				// Increase natural spawns inside towns.
				if (random.nextDouble() >= 0.6) {
					Location newLocation = e.getLocation().clone();
					newLocation.add(random.nextInt(8) * random.nextInt(2) == 0 ? -1 : 1, 0, random.nextInt(8) * random.nextInt(2) == 0 ? -1
							: 1);
					boolean doSpawn = true;
					while (newLocation.getBlock().getType() != Material.AIR) {
						newLocation.add(0, 1, 0);
						if (newLocation.getY() > newLocation.getWorld().getMaxHeight()) {
							doSpawn = false;
							break;
						}
					}
					if (doSpawn)
						e.getLocation().getWorld().spawnEntity(newLocation, e.getEntityType());
				}
			} else if (random.nextDouble() <= 0.6) {// Decrease natural spawns
													// outside of towns.
				e.setCancelled(true);
				return;
			}
		}
		// Make sure we only spawn our desired mobs.
		if (type != EntityType.ZOMBIE && type != EntityType.GIANT && type != EntityType.HORSE && type != EntityType.PLAYER
				&& type != EntityType.PIG_ZOMBIE && type != EntityType.VILLAGER) {
			e.setCancelled(true);
			return;
		}

		if (e.getEntityType() == EntityType.ZOMBIE)
			((Zombie) e.getEntity()).setBaby(random.nextInt(20) < 3);

		// Make some natural pigmen spawn.
		if (e.getLocation().getZ() >= 2000 && type == EntityType.ZOMBIE && random.nextInt(30) == 1) {
			EntityCreator.create(e.getLocation(), EntityType.PIG_ZOMBIE, SpawnReason.NATURAL, true);
			e.setCancelled(true);
			return;
		}

		// Undead and skeletal horses.
		if (type == EntityType.HORSE) {
			Horse horse = (Horse) e.getEntity();
			switch (random.nextInt(10)) {
			case 0:
			case 1:
				horse.setVariant(Variant.UNDEAD_HORSE);
				break;
			case 2:
			case 3:
			case 4:
				horse.setVariant(Variant.SKELETON_HORSE);
				break;
			default:
				break;
			}
		}
	}

	/**
	 * Make sure we only spawn creatures close enough to a player or a chest.
	 * 
	 * @param entity
	 *            The creature that is spawning.
	 * @return True if we should cancel the underlying event (too far from a
	 *         player or chest), false otherwise.
	 */
	private boolean isTooFarFromLandmark(Entity entity) {
		// TODO Caused too much lag to use.

		/*List<Entity> nearby = entity.getNearbyEntities(Configuration.spawnRadius(), 10, Configuration.spawnRadius());
		for (Entity near : nearby)
			if (near instanceof Player)
				return false;
		Location location = entity.getLocation();
		World world = location.getWorld();
		int X = location.getBlockX(), Y = location.getBlockY(), Z = location.getBlockZ();
		for (int x = -Configuration.spawnRadius(); x < Configuration.spawnRadius(); x++)
			for (int y = -10; y < 10; y++)
				for (int z = -Configuration.spawnRadius(); z < Configuration.spawnRadius(); z++)
					if (world.getBlockAt(X + x, Y + y, Z + z).getType() == Material.CHEST)
						return false;*/
		return false;// true;
	}
}
