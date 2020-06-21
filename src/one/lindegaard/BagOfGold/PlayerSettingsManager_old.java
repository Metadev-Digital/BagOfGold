package one.lindegaard.BagOfGold;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import one.lindegaard.BagOfGold.rewards.CustomItems;
import one.lindegaard.Core.storage.DataStoreException;
//import one.lindegaard.BagOfGold.storage.IDataCallback;
import one.lindegaard.Core.Core;
import one.lindegaard.Core.PlayerSettings;

public class PlayerSettingsManager_old implements Listener {

	private HashMap<UUID, PlayerSettings> mPlayerSettings = new HashMap<UUID, PlayerSettings>();

	private BagOfGold plugin;

	/**
	 * Constructor for the PlayerSettingsmanager
	 */
	PlayerSettingsManager_old(BagOfGold plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	/**
	 * Get playerSettings from memory
	 * 
	 * @param offlinePlayer
	 * @return PlayerSettings
	 */
	/**public PlayerSettings getPlayerSettings(OfflinePlayer offlinePlayer) {
		if (mPlayerSettings.containsKey(offlinePlayer.getUniqueId()))
			return mPlayerSettings.get(offlinePlayer.getUniqueId());
		else {
			PlayerSettings ps;
			try {
				ps = plugin.getStoreManager().loadPlayerSettings(offlinePlayer);
			} catch (UserNotFoundException e) {
				String worldgroup = offlinePlayer.isOnline()
						? Core.getWorldGroupManager().getCurrentWorldGroup(offlinePlayer)
						: Core.getWorldGroupManager().getDefaultWorldgroup();
				plugin.getMessages().debug("Insert new PlayerSettings for %s to database.", offlinePlayer.getName());
				ps = new PlayerSettings(offlinePlayer, worldgroup, plugin.getConfigManager().learningMode, false, null,
						null, System.currentTimeMillis(), System.currentTimeMillis());
				setPlayerSettings(offlinePlayer, ps);
				return ps;
			} catch (DataStoreException e) {
				plugin.getMessages().debug("Error reading %s's data from the database", offlinePlayer.getName(),
						offlinePlayer.hasPlayedBefore());
				return new PlayerSettings(offlinePlayer);
			}
			mPlayerSettings.put(offlinePlayer.getUniqueId(), ps);
			return ps;
		}

	}**/

	/**
	 * Store playerSettings in memory
	 * 
	 * @param playerSettings
	 */
	/**public void setPlayerSettings(OfflinePlayer player, PlayerSettings playerSettings) {
		mPlayerSettings.put(player.getUniqueId(), playerSettings);
		plugin.getDataStoreManager().updatePlayerSettings(player, playerSettings);
	}**/

	/**
	 * Remove PlayerSettings from Memory
	 * 
	 * @param player
	 */
	public void removePlayerSettings(OfflinePlayer player) {
		plugin.getMessages().debug("Removing %s from player settings cache", player.getName());
		mPlayerSettings.remove(player.getUniqueId());
	}

	/**
	 * Read PlayerSettings From database into Memory when player joins
	 * 
	 * @param event
	 */
	/**@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!containsKey(player))
			load(player);
	}**/

	/**@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		PlayerSettings ps = mPlayerSettings.get(player.getUniqueId());
		ps.setLastKnownWorldGrp(Core.getWorldGroupManager().getCurrentWorldGroup(player));
		setPlayerSettings(player, ps);
		plugin.getMessages().debug("Saving lastKnownWorldGroup: %s",
				Core.getWorldGroupManager().getCurrentWorldGroup(player));
	}**/

	/**
	 * Load PlayerSettings asynchronously from Database
	 * 
	 * @param offlinePlayer
	 */
	/**public void load(final OfflinePlayer offlinePlayer) {
		plugin.getDataStoreManager().requestPlayerSettings(offlinePlayer, new IDataCallback<PlayerSettings>() {

			@Override
			public void onCompleted(PlayerSettings ps) {
				ps.setLast_logon(System.currentTimeMillis());
				mPlayerSettings.put(offlinePlayer.getUniqueId(), ps);

				if (ps.getTexture() == null || ps.getTexture().equals("")) {
					plugin.getMessages().debug("Store %s skin in BagOfGold Skin Cache", offlinePlayer.getName());
					new CustomItems().getPlayerHead(offlinePlayer.getUniqueId(), offlinePlayer.getName(), 1, 0);
				}

			}

			@Override
			public void onError(Throwable error) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BagOfGold][ERROR] " + offlinePlayer.getName()
						+ " is new, creating user in database.");
				mPlayerSettings.put(offlinePlayer.getUniqueId(), new PlayerSettings(offlinePlayer));
			}
		});
	}**/

	/**
	 * Test if PlayerSettings contains data for Player
	 * 
	 * @param player
	 * @return true if player exists in PlayerSettings in Memory
	 */
	public boolean containsKey(final OfflinePlayer player) {
		return mPlayerSettings.containsKey(player.getUniqueId());
	}

}
