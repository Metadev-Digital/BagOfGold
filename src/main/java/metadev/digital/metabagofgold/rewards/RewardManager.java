package metadev.digital.metabagofgold.rewards;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.metacustomitemslib.rewards.CoreCustomItems;
import metadev.digital.metacustomitemslib.rewards.Reward;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.UUID;

public class RewardManager {

	private BagOfGold plugin;

	

	public RewardManager(BagOfGold plugin) {
		this.plugin = plugin;

		Bukkit.getPluginManager().registerEvents(new RewardListeners(plugin), plugin);

	}



	/**
	 * getBalance : calculate the player balance
	 * 
	 * @param offlinePlayer
	 * @return
	 */
	public double getBalance(OfflinePlayer offlinePlayer) {
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		return ps.getBalance() + ps.getBalanceChanges();
	}

	/**
	 * set the player balance to the amount of money in both the player inventory
	 * and in the balance stored in memory.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return
	 */
	public boolean setbalance(OfflinePlayer offlinePlayer, double amount) {
		double bal = getBalance(offlinePlayer);
		if (offlinePlayer.isOnline() && ((Player) offlinePlayer).getGameMode() != GameMode.SPECTATOR) {
			if (amount >= bal)
				addMoneyToPlayer((Player) offlinePlayer, amount - bal);
			else
				removeMoneyFromPlayer((Player) offlinePlayer, bal - amount);
		} else {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			ps.setBalanceChanges(ps.getBalanceChanges() + (amount - bal));
			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
		}
		return true;
	}

	/**
	 * depositPlayer : deposit the amount to the players balance and add the mount
	 * to the players inventory. Do not deposit negative amounts!
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return EconomyResponce containing amount, balance and ResponseType
	 *         (Success/failure)
	 */
	public boolean depositPlayer(OfflinePlayer offlinePlayer, double amount) {
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		double drop = 0, give = amount;
		if (amount == 0) {
			return true;
		} else if (amount > 0) {
			if (offlinePlayer.isOnline()) {
				Player player = (Player) offlinePlayer;
				double space = getSpaceForMoney(player);
				if (amount > space) {
					give = space;
					drop = amount - give;
				}
				addMoneyToPlayer(player, Tools.round(ps.getBalanceChanges()) + Tools.round(give));
				dropMoneyOnGround(player, null, player.getLocation(), drop);
				ps.setBalance(Tools.round(ps.getBalance() + ps.getBalanceChanges() + give));
				ps.setBalanceChanges(0);
			} else {
				ps.setBalanceChanges(Tools.round(ps.getBalanceChanges() + give));
			}
			plugin.getMessages().debug("Deposit %s to %s's account, new balance is %s", Tools.format(give),
					offlinePlayer.getName(), Tools.format(ps.getBalance() + ps.getBalanceChanges()));
			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
			return true;
		} else {
			plugin.getMessages().debug("Could not deposit %s to %s's account, because the number is negative",
					Tools.format(amount), offlinePlayer.getName());
			return false;
		}
	}

	/**
	 * withdrawPlayer : withdraw the amount from the players balance and remove the
	 * mount to the players inventory. Do not withdraw negative amounts.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return EconomyResponse containing amount withdraw, balance and ResponseType
	 *         (Success/Failure).
	 */
	public boolean withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		if (amount >= 0) {
			if (hasMoney(offlinePlayer, amount)) {
				if (offlinePlayer.isOnline()) {
					removeMoneyFromPlayer((Player) offlinePlayer, amount + Tools.round(ps.getBalanceChanges()));
					ps.setBalance(Tools.round(ps.getBalance() + ps.getBalanceChanges() - amount));
					ps.setBalanceChanges(0);
				} else
					ps.setBalanceChanges(Tools.round(ps.getBalanceChanges() - amount));
				plugin.getMessages().debug("Withdraw %s from %s's account, new balance is %s", Tools.format(amount),
						offlinePlayer.getName(), Tools.format(ps.getBalance() + ps.getBalanceChanges())); //OK
				plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
	/**			
				if (offlinePlayer.isOnline() && ((Player) offlinePlayer).isValid()) {
					Player player = (Player) offlinePlayer;
					if (player.getGameMode() == GameMode.SURVIVAL) {
						plugin.getMessages().debug(
								"EconomyManager: withdrawPlayer adjusting Player Balance to Amount of BagOfGold in Inventory",
								player.getName());
						//plugin.getRewardManager().adjustAmountOfMoneyInInventoryToPlayerBalance(player);
						plugin.getRewardManager().adjustAmountOfMoneyInInventoryToPlayerBalance(player);
					} else {
						plugin.getMessages().debug(
								"EconomyManager: withdrawPlayer %s adjusting Amount of BagOfGold in Inventory To Balance",
								player.getName());
						plugin.getRewardManager().adjustAmountOfMoneyInInventoryToPlayerBalance(player);
					}
				}
		**/		
				return true;
			} else {
				double remove = Tools.round(ps.getBalance() + ps.getBalanceChanges());
				plugin.getMessages().debug("%s has not enough bagofgold, Withdrawing only %s , new balance is %s",
						offlinePlayer.getName(), Tools.format(remove), Tools.format(0));
				if (remove > 0) {
					removeMoneyFromPlayer((Player) offlinePlayer, remove);
					ps.setBalance(0);
					ps.setBalanceChanges(0);
					plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
					return true;
				}
				return false;
			}
		} else
			return true;
	}

	/**
	 * MobHunting has : checks if the player has amount of mount on his balance.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 * @return true if the player has the amount on his money.
	 */
	public boolean hasMoney(OfflinePlayer offlinePlayer, double amount) {
		PlayerBalance pb = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		plugin.getMessages().debug("Check if %s has %s %s on the balance=%s)", offlinePlayer.getName(), Tools.format(amount),
				Core.getConfigManager().bagOfGoldName, Tools.format(pb.getBalance() + pb.getBalanceChanges()));
		return Tools.round(pb.getBalance()) + Tools.round(pb.getBalanceChanges()) >= Tools.round(amount);
	}

	/**
	 * dropMoneyOnGround_EconomyManager: drop the amount of money in the location
	 * 
	 * @param player       - not used in EconomyManager
	 * @param killedEntity -Misc not used in EconomyManager
	 * @param location
	 * @param money
	 */
	public void dropMoneyOnGround(Player player, Entity killedEntity, Location location, double amount) {
		if (plugin.getBagOfGoldItems().isBagOfGoldStyle())
			plugin.getBagOfGoldItems().dropBagOfGoldMoneyOnGround(player, killedEntity, location, amount);
		else if (plugin.getGringottsItems().isGringottsStyle())
			plugin.getGringottsItems().dropGringottsMoneyOnGround(player, killedEntity, location, amount);
		else {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_WARNING
							+ "Error in config.sys: unknown 'drop-money-on-ground-itemtype: "
							+ Core.getConfigManager().rewardItemtype + "'");
		}
	}

	/**
	 * Dropes an Reward Item at the specified location
	 * 
	 * @param location - where the Item is dropped.
	 * @param reward   - the reward to be dropped
	 */
	public void dropRewardOnGround(Location location, Reward reward) {
		if (reward.isBagOfGoldReward()) {
			dropMoneyOnGround(null, null, location, reward.getMoney());
		} else if (reward.isItemReward()) {
			ItemStack is = new ItemStack(Material.valueOf(Core.getConfigManager().rewardItem), 1);
			Item item = location.getWorld().dropItem(location, is);
			Core.getCoreRewardManager().getDroppedMoney().put(item.getEntityId(), reward.getMoney());
		} else if (reward.isKilledHeadReward()) {
			MobType mob = MobType.getMobType(reward.getSkinUUID());
			if (mob != null) {
				ItemStack is = CoreCustomItems.getCustomHead(mob, reward.getDisplayName(), 1,
						reward.getMoney(), reward.getSkinUUID());
				Item item = location.getWorld().dropItem(location, is);
				item.setMetadata(Reward.MH_REWARD_DATA_NEW, new FixedMetadataValue(plugin, new Reward(reward)));
				Core.getCoreRewardManager().getDroppedMoney().put(item.getEntityId(), reward.getMoney());
			}
		} else if (reward.isKillerHeadReward()) {
			ItemStack is = CoreCustomItems.getPlayerHead(reward.getSkinUUID(), reward.getDisplayName(), 1,
					reward.getMoney());
			Item item = location.getWorld().dropItem(location, is);
			item.setMetadata(Reward.MH_REWARD_DATA_NEW, new FixedMetadataValue(plugin, new Reward(reward)));
			Core.getCoreRewardManager().getDroppedMoney().put(item.getEntityId(), reward.getMoney());
		} else {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING
					+ "Unhandled reward type in RewardManager (DropRewardOnGround).");
		}
	}

	/**
	 * bankDeposit: deposit the amount on the account.
	 * 
	 * @param account - This is the player UUID as a String
	 * @param amount
	 * @return EconomyResponse containing amount, balance and ResponseType
	 *         (Success/Failure).
	 */
	public boolean bankDeposit(String account, double amount) {
		OfflinePlayer offlinePlayer = Tools.isUUID(account) ? Bukkit.getOfflinePlayer(UUID.fromString(account))
				: Bukkit.getOfflinePlayer(account);
		if (offlinePlayer != null) {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Tools.round(ps.getBankBalance() + ps.getBankBalanceChanges() + amount));
				ps.setBankBalanceChanges(0);
			} else {
				ps.setBankBalanceChanges(ps.getBankBalanceChanges() + amount);
			}
			plugin.getMessages().debug("bankDeposit: %s", ps.toString());

			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
			return true;
		} else
			return false;
	}

	/**
	 * bankWithdraw: withdraw the amount from the account.
	 * 
	 * @param account - This is the player UUID as a String
	 * @param amount
	 * @return EconomyResponse containing amount, balance and ResponseType
	 *         (Success/Failure).
	 */
	public boolean bankWithdraw(String account, double amount) {
		OfflinePlayer offlinePlayer = Tools.isUUID(account) ? Bukkit.getOfflinePlayer(UUID.fromString(account))
				: Bukkit.getOfflinePlayer(account);
		if (offlinePlayer != null) {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Tools.round(ps.getBankBalance() + ps.getBankBalanceChanges() - amount));
				ps.setBankBalanceChanges(0);
			} else {
				ps.setBankBalanceChanges(ps.getBankBalanceChanges() - amount);
			}
			plugin.getMessages().debug("bankWithdraw: %s -  ", ps.toString());

			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
			return true;
		} else
			return false;
	}

	/**
	 * bankBalance: withdraw the amount from the account.
	 * 
	 * @param account - This is the player UUID as a String
	 * @return EconomyResponse containing amount, bank balance and ResponseType
	 *         (Success/Failure).
	 */
	public double bankBalance(String account) {
		OfflinePlayer offlinePlayer = Tools.isUUID(account) ? Bukkit.getOfflinePlayer(UUID.fromString(account))
				: Bukkit.getOfflinePlayer(account);
		if (offlinePlayer != null) {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Tools.round(ps.getBankBalance() + ps.getBankBalanceChanges()));
				ps.setBankBalanceChanges(0);
				plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
			}
			return ps.getBankBalance() + ps.getBankBalanceChanges();
		} else
			return 0;
	}

	public boolean setBankBalance(String account, double amount) {
		OfflinePlayer offlinePlayer = Tools.isUUID(account) ? Bukkit.getOfflinePlayer(UUID.fromString(account))
				: Bukkit.getOfflinePlayer(account);
		if (offlinePlayer != null) {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			ps.setBankBalance(Tools.round(amount));
			ps.setBankBalanceChanges(0);
			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
		}
		return true;
	}

	/**
	 * Remove the amount from the player balance in memory/databse without removing
	 * amount from the player inventory
	 * 
	 * @param offlinePlayer
	 * @param amount
	 */
	public void removeMoneyFromPlayerBalance(OfflinePlayer offlinePlayer, double amount) {
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		plugin.getMessages().debug("Removing %s from %s's balance %s", Tools.format(amount), offlinePlayer.getName(),
				Tools.format(ps.getBalance() + ps.getBalanceChanges()));
		if (offlinePlayer.isOnline()) {
			ps.setBalance(Tools.round(ps.getBalance() + ps.getBalanceChanges() - amount));
			ps.setBalanceChanges(0);
		} else {
			ps.setBalanceChanges(Tools.round(ps.getBalanceChanges() - amount));
		}
		plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
	}

	/**
	 * Add amount to player balance in memory/database but NOT on in the players
	 * inventory
	 * 
	 * @param offlinePlayer
	 * @param amount
	 */
	public void addMoneyToPlayerBalance(OfflinePlayer offlinePlayer, double amount) {
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
		plugin.getMessages().debug("Adding %s to %s's balance %s", Tools.format(amount), offlinePlayer.getName(),
				Tools.format(ps.getBalance() + ps.getBalanceChanges()));
		if (offlinePlayer.isOnline()) {
			ps.setBalance(Tools.round(ps.getBalance() + ps.getBalanceChanges() + amount));
			ps.setBalanceChanges(0);
		} else {
			ps.setBalance(Tools.round(ps.getBalance() + ps.getBalanceChanges() + amount));
		}
		plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
	}

	/**
	 * deleteBank: Reset the account and set the bank balance to 0.
	 * 
	 * @param account - this is the player UUID.
	 * @return ResponseType (Success/Failure)
	 */
	public boolean deleteBank(String account) {
		OfflinePlayer offlinePlayer = Tools.isUUID(account) ? Bukkit.getOfflinePlayer(UUID.fromString(account))
				: Bukkit.getOfflinePlayer(account);
		if (offlinePlayer != null) {
			PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(offlinePlayer);
			ps.setBankBalance(0);
			ps.setBankBalanceChanges(0);
			plugin.getPlayerBalanceManager().setPlayerBalance(offlinePlayer, ps);
			return true;
		} else
			return false;
	}

	public boolean isBankOwner(String account, OfflinePlayer offlinePlayer) {
		return account.equalsIgnoreCase(offlinePlayer.getUniqueId().toString());
	}

	/**
	 * Check if the player is a member of the bank account
	 * 
	 * @param name          of the account
	 * @param offlinePlayer to check membership
	 * @return EconomyResponse Object
	 */
	public boolean isBankMember(String account, OfflinePlayer offlinePlayer) {
		return account.equalsIgnoreCase(offlinePlayer.getUniqueId().toString());
	}

	/**
	 * Calculate the total amount of money in the player inventory.
	 * 
	 * @param player
	 * @return
	 */
	public double getAmountInInventory(Player player) {
		if (plugin.getBagOfGoldItems().isBagOfGoldStyle())
			return plugin.getBagOfGoldItems().getAmountOfBagOfGoldMoneyInInventory(player);
		else if (plugin.getGringottsItems().isGringottsStyle())
			return plugin.getGringottsItems().getAmountOfGringottsMoneyInInventory(player);
		else {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_WARNING
							+ "Error in config.sys: unknown 'drop-money-on-ground-itemtype: "
							+ Core.getConfigManager().rewardItemtype + "'");
			return 0;
		}
	}

	/**
	 * Add amount to the player inventory, but NOT on player balance in
	 * memory/database.
	 * 
	 * @param offlinePlayer
	 * @param amount
	 */
	public double addMoneyToPlayer(Player player, double amount) {
		if (plugin.getBagOfGoldItems().isBagOfGoldStyle())
			return Core.getCoreRewardManager().addBagOfGoldMoneyToPlayer(player, amount);
		else if (plugin.getGringottsItems().isGringottsStyle())
			return plugin.getGringottsItems().addGringottsMoneyToPlayer(player, amount);
		else {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_WARNING
							+ "Error in config.sys: unknown 'drop-money-on-ground-itemtype: "
							+ Core.getConfigManager().rewardItemtype + "'");
			return 0;
		}
	}

	/**
	 * Remove the amount from the player inventory but NOT from the player balance
	 * in memory/database.
	 * 
	 * @param player
	 * @param amount
	 * @return
	 */
	public double removeMoneyFromPlayer(Player player, double amount) {
		if (plugin.getBagOfGoldItems().isBagOfGoldStyle())
			return Core.getCoreRewardManager().removeBagOfGoldFromPlayer(player, amount);
		else if (plugin.getGringottsItems().isGringottsStyle())
			return plugin.getGringottsItems().removeGringottsMoneyFromPlayer(player, amount);
		else {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_WARNING
							+ "Error in config.sys: unknown 'drop-money-on-ground-itemtype: "
							+ Core.getConfigManager().rewardItemtype + "'");
			return 0;
		}
	}

	/**
	 * Change the players amount of money in his inventory to the players balance in
	 * memory/database
	 * 
	 * @param player
	 */
	public void adjustAmountOfMoneyInInventoryToPlayerBalance(Player player) {

		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(player);
		double amountInInventory = getAmountInInventory(player);
		plugin.getMessages().debug("amountInInventory = %s", amountInInventory);
		if (ps != null) {
			double diff = (Tools.round(ps.getBalance()) + Tools.round(ps.getBalanceChanges()))
					- Tools.round(amountInInventory);
			double space = getSpaceForMoney(player); // writes Playername has room for nnnn money
			if (diff > space) {
				plugin.getMessages().debug("Not enough space for the money. Space=%s", space);
				diff = space;
				ps.setBalance(ps.getBalance() + space);
			}
			if (Tools.round(diff) != 0)
				plugin.getMessages().debug("Adjusting amt to Balance: amt=%s, bal=%s(+%s)", amountInInventory,
						ps.getBalance(), ps.getBalanceChanges());
			if (Tools.round(diff) > 0) {
				plugin.getMessages().debug("Add %s money to balance", diff);
				addMoneyToPlayer(player, Tools.round(diff));
			} else if (Tools.round(diff) < 0) {
				plugin.getMessages().debug("remove %s money from balance", -diff);
				removeMoneyFromPlayer(player, -diff);
			} else
				plugin.getMessages().debug("There was no difference");
		}
	}

	/**
	 * Change the players balance in memory/database to the amount of money in his
	 * inventory
	 * 
	 * @param player
	 */
	public void adjustPlayerBalanceToAmounOfMoneyInInventory(Player player) {
		double amountInInventory = getAmountInInventory(player);
		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(player);
		ItemStack is = player.getItemOnCursor();
		double inHand = 0;
		if (Reward.isReward(is)) {
			Reward reward = Reward.getReward(is);
			int amount = is.getAmount();
			if (reward.isBagOfGoldReward() || reward.isItemReward())
				inHand = reward.getMoney() * amount;
		}
		if (ps != null) {
			double diff = Tools.round(amountInInventory + inHand)
					- (Tools.round(ps.getBalance()) + Tools.round(ps.getBalanceChanges()));
			// plugin.getMessages().debug("Adjusting Balance to amt: diff=%s", diff);
			if (Tools.round(diff) != 0)
				plugin.getMessages().debug("Adjusting Balance to amt: amt=%s, inHand=%s, bal=%s(+%s)",
						amountInInventory, inHand, ps.getBalance(), ps.getBalanceChanges());
			if (Tools.round(diff) > 0)
				addMoneyToPlayerBalance(player, Tools.round(diff));
			else if (Tools.round(diff) < 0)
				removeMoneyFromPlayerBalance(player, -diff);
			else
				plugin.getMessages().debug("there was no difference");

		}
	}

	/**
	 * Get the amount of money which the player has room for in his inventory.
	 * 
	 * @param player
	 * @return
	 */
	public double getSpaceForMoney(Player player) {
		if (plugin.getBagOfGoldItems().isBagOfGoldStyle())
			return plugin.getBagOfGoldItems().getSpaceForBagOfGoldMoney(player);
		else if (plugin.getGringottsItems().isGringottsStyle()) {
			return plugin.getGringottsItems().getSpaceForGringottsMoney(player);
		} else {
			Bukkit.getConsoleSender()
					.sendMessage(BagOfGold.PREFIX_WARNING
							+ "Error in config.sys: unknown 'drop-money-on-ground-itemtype: "
							+ Core.getConfigManager().rewardItemtype + "'");
			return 0;
		}
	}

}
