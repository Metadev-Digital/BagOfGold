package one.lindegaard.MobHunting.compatability;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.CitizensPlugin;
import net.citizensnpcs.api.event.CitizensDisableEvent;
import net.citizensnpcs.api.event.CitizensEnableEvent;
import net.citizensnpcs.api.event.NPCDamageByEntityEvent;
import net.citizensnpcs.api.event.NPCDamageEvent;
import net.citizensnpcs.api.event.NPCDeathEvent;
import net.citizensnpcs.api.event.NPCDespawnEvent;
import net.citizensnpcs.api.event.NPCSpawnEvent;
import net.citizensnpcs.api.event.PlayerCreateNPCEvent;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.npc.NPCRegistry;
import net.citizensnpcs.api.trait.TraitInfo;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.MobPlugins;
import one.lindegaard.MobHunting.MobRewardData;
import one.lindegaard.MobHunting.npc.MasterMobHunter;
import one.lindegaard.MobHunting.npc.MasterMobHunterManager;
import one.lindegaard.MobHunting.npc.MasterMobHunterTrait;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;

public class CitizensCompat implements Listener {

	private static boolean supported = false;
	private static CitizensPlugin citizensAPI;
	private static HashMap<String, MobRewardData> mMobRewardData = new HashMap<String, MobRewardData>();
	private File fileMobRewardData = new File(MobHunting.getInstance().getDataFolder(), "citizens-rewards.yml");
	private YamlConfiguration config = new YamlConfiguration();

	private static MasterMobHunterManager masterMobHunterManager = new MasterMobHunterManager();

	public CitizensCompat() {
		initialize();
	}

	private void initialize() {
		if (isDisabledInConfig()) {
			MobHunting.getInstance().getLogger().info("Compatability with Citizens2 is disabled in config.yml");
		} else {
			citizensAPI = (CitizensPlugin) Bukkit.getPluginManager().getPlugin("Citizens");
			if (citizensAPI == null)
				return;

			TraitInfo trait = TraitInfo.create(MasterMobHunterTrait.class).withName("MasterMobHunter");
			citizensAPI.getTraitFactory().registerTrait(trait);

			MobHunting.getInstance().getLogger().info(
					"Enabling compatability with Citizens (" + getCitizensPlugin().getDescription().getVersion() + ")");

			supported = true;

			loadCitizensData();
			saveCitizensData();

			// wait x seconds or until Citizens is fully loaded.
			// TODO: wait until MasterMobHunterTrait is loaded.
			MobHunting.getInstance().getServer().getScheduler().scheduleSyncDelayedTask(MobHunting.getInstance(),
					new Runnable() {
						public void run() {
							masterMobHunterManager.initialize();
							findMissingSentry();
							loadBountyDataForSentry();
						}
					}, 20 * 3); // 20ticks/sec * 10 sec.

		}
	}

	// **************************************************************************
	// LOAD & SAVE
	// **************************************************************************
	public void loadCitizensData() {
		try {
			if (!fileMobRewardData.exists())
				return;
			MobHunting.debug("Loading extra MobRewards.");

			config.load(fileMobRewardData);
			for (String key : config.getKeys(false)) {
				ConfigurationSection section = config.getConfigurationSection(key);
				MobRewardData mrd = new MobRewardData();
				mrd.read(section);
				mMobRewardData.put(key, mrd);
			}
			MobHunting.debug("Loaded %s extra MobRewards.", mMobRewardData.size());
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	public void saveCitizensData() {
		try {
			config.options().header("This a extra MobHunting config data for the Citizens/NPC's on your server.");

			if (mMobRewardData.size() > 0) {

				int n = 0;
				for (String key : mMobRewardData.keySet()) {
					ConfigurationSection section = config.createSection(key);
					mMobRewardData.get(key).save(section);
					n++;
				}

				if (n != 0) {
					MobHunting.debug("Saving %s MobRewards to file.", mMobRewardData.size());
					config.save(fileMobRewardData);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void saveCitizensData(String key) {
		try {
			if (mMobRewardData.containsKey(key)) {
				ConfigurationSection section = config.createSection(key);
				mMobRewardData.get(key).save(section);
				MobHunting.debug("Saving Sentry Trait Reward data for ID=%s.", key);
				config.save(fileMobRewardData);
			} else {
				MobHunting.debug("ERROR! Mob ID (%s) is not found in mMobRewardData", key);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static void shutdown() {
		if (supported) {
			masterMobHunterManager.shutdown();
			TraitInfo trait = TraitInfo.create(MasterMobHunterTrait.class).withName("MasterMobHunter");
			citizensAPI.getTraitFactory().deregisterTrait(trait);
		}
	}

	public static CitizensPlugin getCitizensPlugin() {
		return citizensAPI;
	}

	public static boolean isCitizensSupported() {
		if (citizensAPI != null && CitizensAPI.hasImplementation())
			return supported;
		else
			return false;
	}

	public static MasterMobHunterManager getManager() {
		return masterMobHunterManager;
	}

	public static boolean isNPC(Entity entity) {
		if (isCitizensSupported())
			return CitizensAPI.getNPCRegistry().isNPC(entity);
		else
			return false;
	}

	public static int getNPCId(Entity entity) {
		return CitizensAPI.getNPCRegistry().getNPC(entity).getId();
	}

	public static String getNPCName(Entity entity) {
		return CitizensAPI.getNPCRegistry().getNPC(entity).getName();
	}

	public static NPC getNPC(Entity entity) {
		return CitizensAPI.getNPCRegistry().getNPC(entity);
	}

	public static boolean isSentry(Entity entity) {
		if (CitizensAPI.getNPCRegistry().isNPC(entity))
			return CitizensAPI.getNPCRegistry().getNPC(entity)
					.hasTrait(CitizensAPI.getTraitFactory().getTraitClass("Sentry"));
		else
			return false;
	}

	public static HashMap<String, MobRewardData> getMobRewardData() {
		return mMobRewardData;
	}

	public static boolean isDisabledInConfig() {
		return MobHunting.getConfigManager().disableIntegrationCitizens;
	}

	public static boolean isEnabledInConfig() {
		return !MobHunting.getConfigManager().disableIntegrationCitizens;
	}

	public void findMissingSentry() {
		NPCRegistry n = CitizensAPI.getNPCRegistry();
		for (Iterator<NPC> npcList = n.iterator(); npcList.hasNext();) {
			NPC npc = npcList.next();
			if (isSentry(npc.getEntity())) {
				if (mMobRewardData != null && !mMobRewardData.containsKey(String.valueOf(npc.getId()))) {
					MobHunting.debug("A new Sentry NPC found. ID=%s,%s", npc.getId(), npc.getName());
					mMobRewardData.put(String.valueOf(npc.getId()),
							new MobRewardData(MobPlugins.MobPluginNames.Citizens, "npc", npc.getFullName(), "10",
									"give {player} iron_sword 1", "You got an Iron sword.", 100, 100));
					saveCitizensData(String.valueOf(npc.getId()));
				}
			}
		}
	}

	private void loadBountyDataForSentry() {
		NPCRegistry n = CitizensAPI.getNPCRegistry();
		for (Iterator<NPC> npcList = n.iterator(); npcList.hasNext();) {
			NPC npc = npcList.next();
			if (isSentry(npc.getEntity())) {
				// MobHunting.getBountyManager().loadBounties(npc);
			}
		}
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onNPCDeathEvent(NPCDeathEvent event) {

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onNPCDamageEvent(NPCDamageEvent event) {

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onNPCDamageByEntityEvent(NPCDamageByEntityEvent event) {

	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onCitizensEnableEvent(CitizensEnableEvent event) {
		MobHunting.debug("onCitizensEnableEvent:%s", event.getEventName());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onCitizensDisableEvent(CitizensDisableEvent event) {
		// MobHunting.debug("CitizensDisableEvent - saving");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onNPCSpawnEvent(NPCSpawnEvent event) {
		NPC npc = event.getNPC();
		if (npc.getId() == event.getNPC().getId()) {
			if (isSentry(npc.getEntity())) {
				if (mMobRewardData != null && !mMobRewardData.containsKey(String.valueOf(npc.getId()))) {
					MobHunting.debug("A new Sentry NPC found. ID=%s,%s", npc.getId(), npc.getName());
					mMobRewardData.put(String.valueOf(npc.getId()),
							new MobRewardData(MobPlugins.MobPluginNames.Citizens, "npc", npc.getFullName(), "10",
									"give {player} iron_sword 1", "You got an Iron sword.", 100, 100));
					saveCitizensData(String.valueOf(npc.getId()));
				}
			}
			if (masterMobHunterManager.isMasterMobHunter(npc.getEntity())) {
				if (!masterMobHunterManager.contains(npc.getId())) {
					MobHunting.debug("A New MasterMobHunter NPC found. ID=%s,%s", npc.getId(), npc.getName());
					masterMobHunterManager.put(npc.getId(), new MasterMobHunter(npc));
				}
			} else {
				MobHunting.debug("The spawned NPC was not Sentry and MasterMobHunter. Traits=s%",
						npc.getTraits().toString());
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onNPCDespawnEvent(NPCDespawnEvent event) {
		// MobHunting.debug("NPCDespawnEvent");
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	private void onPlayerCreateNPCEvent(PlayerCreateNPCEvent event) {
		// MobHunting.debug("NPCCreateNPCEvent");
	}

}