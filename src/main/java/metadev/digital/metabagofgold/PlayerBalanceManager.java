package metadev.digital.metabagofgold;

import metadev.digital.metabagofgold.compatibility.EssentialsCompat;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.PlayerSettings;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.rewards.CoreCustomItems;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.storage.IDataCallback;
import metadev.digital.metacustomitemslib.storage.UserNotFoundException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PlayerBalanceManager implements Listener {

	private BagOfGold plugin;
	private HashMap<UUID, PlayerBalances> mBalances = new HashMap<UUID, PlayerBalances>();

	/**
	 * Constructor for the PlayerBalanceManager
	 */
	PlayerBalanceManager(BagOfGold plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	public HashMap<UUID, PlayerBalances> getBalances() {
		return mBalances;
	}

	public PlayerBalance getPlayerBalance(OfflinePlayer offlinePlayer, String world) {
		return null;
	}

	public PlayerBalance getPlayerBalance(OfflinePlayer offlinePlayer) {
		if (offlinePlayer.isOnline()) {
			String worldGroup = Core.getWorldGroupManager().getCurrentWorldGroup(offlinePlayer);
			GameMode gamemode = Core.getWorldGroupManager().getCurrentGameMode(offlinePlayer);
			return getPlayerBalance(offlinePlayer, worldGroup, gamemode);
		} else {
			String worldGroup = Core.getPlayerSettingsManager().getPlayerSettings(offlinePlayer).getLastKnownWorldGrp();
			GameMode gamemode = Core.getWorldGroupManager().getDefaultGameMode();
			return getPlayerBalance(offlinePlayer, worldGroup, gamemode);
		}
	}

	public PlayerBalance getPlayerBalanceInWorld(OfflinePlayer offlinePlayer, String world, GameMode gamemode) {
		String worldGroup = Core.getWorldGroupManager().getWorldGroup(world);
		return getPlayerBalance(offlinePlayer, worldGroup, gamemode);
	}

	public PlayerBalance getPlayerBalance(OfflinePlayer offlinePlayer, String worldGroup, GameMode gamemode) {
		if (mBalances.containsKey(offlinePlayer.getUniqueId()))
			// offlinePlayer is in the Database
			if (mBalances.get(offlinePlayer.getUniqueId()).has(worldGroup, gamemode)) {
				return mBalances.get(offlinePlayer.getUniqueId()).getPlayerBalance(worldGroup, gamemode);
			} else {
				plugin.getMessages().debug("PlayerBalanceManager: creating new %s and %s", worldGroup, gamemode);
				PlayerBalances ps = mBalances.get(offlinePlayer.getUniqueId());
				PlayerBalance pb = new PlayerBalance(offlinePlayer, worldGroup, gamemode);
				ps.putPlayerBalance(pb);
				setPlayerBalance(offlinePlayer, pb);
				return pb;
			}
		else {
			// offlinePlayer is NOT in memory, try loading from DB
			PlayerBalances ps = new PlayerBalances();
			PlayerBalance pb = new PlayerBalance(offlinePlayer, worldGroup, gamemode);
			try {
				plugin.getMessages().debug("PlayerBalanceManager: loading %s balance (%s,%s) from DB",
						offlinePlayer.getName(), worldGroup, gamemode);
				ps = plugin.getStoreManager().loadPlayerBalances(offlinePlayer);
				pb = ps.getPlayerBalance(worldGroup, gamemode);
			} catch (UserNotFoundException e) {
				plugin.getMessages().debug("PlayerBalanceManager: UserNotFoundException - setPlayerBalances:%s",
						pb.toString());
				setPlayerBalance(offlinePlayer, pb);
			} catch (DataStoreException e) {
				e.printStackTrace();
			}
			if (!ps.has(worldGroup, gamemode)) {
				plugin.getMessages().debug("PlayerBalanceManager: creating new balance:%s", pb.toString());
				setPlayerBalance(offlinePlayer, pb);
			}
			mBalances.put(offlinePlayer.getUniqueId(), ps);
			return pb;
		}
	}

	// TODO: remove parameter offlinePlayer
	public void setPlayerBalance(OfflinePlayer offlinePlayer, PlayerBalance playerBalance) {
		if (!mBalances.containsKey(offlinePlayer.getUniqueId())) {
			plugin.getMessages().debug("PlayerBalanceManager - insert PlayerBlance to Memory");
			PlayerBalances ps = new PlayerBalances();
			ps.putPlayerBalance(playerBalance);
			mBalances.put(offlinePlayer.getUniqueId(), ps);
		} else {
			mBalances.get(offlinePlayer.getUniqueId()).putPlayerBalance(playerBalance);
		}
		plugin.getDataStoreManager().updatePlayerBalance(offlinePlayer, playerBalance);
	}

	/**
	 * Remove PlayerSettings from Memory minecraftMob.getFriendlyName()
	 * 
	 * @param offlinePlayer
	 */
	public void removePlayerBalance(OfflinePlayer offlinePlayer) {
		plugin.getMessages().debug("Removing %s from player settings cache", offlinePlayer.getName());
		mBalances.remove(offlinePlayer.getUniqueId());
	}

	/**
	 * Read PlayerSettings From database into Memory when player joins
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	private void onPlayerJoin(PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		if (!containsKey(player)) {
			PlayerBalances playerBalances = new PlayerBalances();
			PlayerBalance playerBalance = new PlayerBalance(player);
			playerBalances.putPlayerBalance(playerBalance);
			mBalances.put(player.getUniqueId(), playerBalances);
			load(player);
		} else {
			plugin.getRewardManager().adjustAmountOfMoneyInInventoryToPlayerBalance(player);
		}

	}

	/**
	 * Write PlayerSettings to Database when Player Quit and remove PlayerSettings
	 * from memory
	 * 
	 * @param event
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	private void onPlayerQuit(PlayerQuitEvent event) {
		final Player player = event.getPlayer();
		PlayerSettings ps = Core.getPlayerSettingsManager().getPlayerSettings(player);
		ps.setLastKnownWorldGrp(Core.getWorldGroupManager().getCurrentWorldGroup(player));
		Core.getPlayerSettingsManager().setPlayerSettings(ps);

		// update Essentials balance
		if (EssentialsCompat.isSupported()) {
			final double balance = getPlayerBalance(player).getBalance();
			Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
				@Override
				public void run() {
					EssentialsCompat.setEssentialsBalance(player, balance);
				}
			}, 100L);
		}
	}

	/**
	 * Load PlayerSettings asynchronously from Database Misc
	 * 
	 * @param offlinePlayer
	 */
	public void load(final OfflinePlayer offlinePlayer) {
		plugin.getDataStoreManager().requestPlayerBalances(offlinePlayer, new IDataCallback<PlayerBalances>() {

			@Override
			public void onCompleted(PlayerBalances ps) {
				String worldGroup;
				GameMode gamemode;
				if (offlinePlayer.isOnline()) {
					Player player = (Player) offlinePlayer;
					worldGroup = Core.getWorldGroupManager().getCurrentWorldGroup(player);
					gamemode = player.getGameMode();
					// Next line is important, to adjust the AmountInInventory to Balance
					plugin.getRewardManager().getAmountInInventory(player);
				} else {
					worldGroup = Core.getWorldGroupManager().getDefaultWorldgroup();
					gamemode = Core.getWorldGroupManager().getDefaultGameMode();
				}
				if (!ps.has(worldGroup, gamemode)) {
					PlayerBalance pb = new PlayerBalance(offlinePlayer, worldGroup, gamemode);
					ps.putPlayerBalance(pb);
					setPlayerBalance(offlinePlayer, pb);
				}
				mBalances.put(offlinePlayer.getUniqueId(), ps);

				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						if (offlinePlayer.isOnline() && ((Player) offlinePlayer).isValid()) {
							double amountInInventory = plugin.getRewardManager()
									.getAmountInInventory((Player) offlinePlayer);
							PlayerBalance pb = getPlayerBalance(offlinePlayer);
							if (Tools.round(amountInInventory) != Tools.round(pb.getBalance())
									+ Tools.round(pb.getBalanceChanges())) {
								double change = pb.getBalanceChanges();
								plugin.getMessages().debug(
										"Balance was changed while %s was offline. New balance is %s.",
										offlinePlayer.getName(), pb.getBalance() + change);
								pb.setBalance(pb.getBalance() + change);
								pb.setBalanceChanges(0);
								setPlayerBalance(offlinePlayer, pb);
								plugin.getRewardManager()
										.adjustAmountOfMoneyInInventoryToPlayerBalance((Player) offlinePlayer);
							}
						}
					}
				}, 40L);

			}

			@Override
			public void onError(Throwable error) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "[BagOfGold][ERROR] Could not load "
						+ offlinePlayer.getName() + "'s balance from the database.");
				mBalances.put(offlinePlayer.getUniqueId(), new PlayerBalances());
			}

		});
	}

	public void loadTop54(final CommandSender sender, final int n, final String worldGroup, final int gamemode) {
		plugin.getDataStoreManager().requestTop54PlayerBalances(n, worldGroup, gamemode,
				new IDataCallback<List<PlayerBalance>>() {

					@Override
					public void onCompleted(List<PlayerBalance> playerBalances) {
						showTopPlayers(sender, playerBalances);
					}

					@Override
					public void onError(Throwable error) {

					}
				});
	}

	/**
	 * Test if PlayerSettings contains data for Player
	 * 
	 * @param player
	 * @return true if player exists in PlayerSettings in Memory
	 */
	public boolean containsKey(final OfflinePlayer player) {
		return mBalances.containsKey(player.getUniqueId());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(player);
		ps.setBalance(0);
		ps.setBalanceChanges(0);
		setPlayerBalance(player, ps);
		plugin.getMessages().debug("PlayerBalancManager: player died balance=0");
	}

	private HashMap<CommandSender, Inventory> inventoryMap = new HashMap<CommandSender, Inventory>();

	public void showTopPlayers(CommandSender sender, List<PlayerBalance> playerBalances) {
		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (!playerBalances.isEmpty()) {
				Inventory inventory = Bukkit.createInventory(null, 54,
						ChatColor.BLUE + "" + ChatColor.BOLD + "TOP wealth players");
				int n = 0;
				for (PlayerBalance playerBalance : playerBalances) {
					addInventoryDetails(
							CoreCustomItems.getPlayerHead(playerBalance.getPlayer().getUniqueId(),
									playerBalance.getPlayer().getName(), 1,
									playerBalance.getBalance() + playerBalance.getBalanceChanges()
											+ playerBalance.getBankBalance() + playerBalance.getBankBalanceChanges()),
							inventory, n, ChatColor.GREEN + playerBalance.getPlayer().getName(),

							// Lores
							new String[] { ChatColor.GRAY + "" + ChatColor.ITALIC,
									ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
											+ plugin.getMessages().getString("bagofgold.commands.money.top", "total",
													playerBalance.getBalance() + playerBalance.getBalanceChanges()
															+ playerBalance.getBankBalance()
															+ playerBalance.getBankBalanceChanges(),
													"rewardname", Core.getConfigManager().bagOfGoldName.trim())

									,
									ChatColor.DARK_PURPLE + "WorldGrp:" + ChatColor.GREEN
											+ Core.getWorldGroupManager().getCurrentWorldGroup(player) + " ",

									ChatColor.DARK_PURPLE + "Mode:" + ChatColor.GREEN + player.getGameMode().toString()

							});
					if (n < 53)
						n++;
				}
				inventoryMap.put((Player) sender, inventory);
				((Player) sender).openInventory(inventoryMap.get(sender));
			}
		} else {
			sender.sendMessage("[BagOgGold] You cant use this command in the console");
		}
	}

	public static void addInventoryDetails(ItemStack itemStack, Inventory inv, int Slot, String name, String[] lores) {
		final int max = 40;
		ItemMeta meta = itemStack.getItemMeta();
		meta.setDisplayName(name);
		ArrayList<String> lore = new ArrayList<String>();
		for (int n = 0; n < lores.length; n = n + 2) {
			String color = lores[n];
			String line, rest = lores[n + 1];
			while (!rest.isEmpty()) {
				if (rest.length() < max) {
					lore.add(color + rest);
					break;
				} else {
					int splitPos = rest.substring(0, max).lastIndexOf(" ");
					if (splitPos != -1) {
						line = rest.substring(0, splitPos);
						rest = rest.substring(splitPos + 1);
					} else {
						line = rest.substring(0, max);
						rest = rest.substring(max);
					}
					lore.add(color + line);
				}
			}
		}
		meta.setLore(lore);
		itemStack.setItemMeta(meta);

		inv.setItem(Slot, itemStack);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
	public void onInventoryClick(InventoryClickEvent event) {
		if (!event.getView().getTitle().isEmpty()
				&& ChatColor.stripColor(event.getView().getTitle()).startsWith("TOP wealth players")) {
			event.setCancelled(true);
			event.getWhoClicked().closeInventory();
		}
	}

}
