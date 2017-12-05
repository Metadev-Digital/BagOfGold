package one.lindegaard.BagOfGold;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;

import net.milkbowl.vault.economy.EconomyResponse;
import net.milkbowl.vault.economy.EconomyResponse.ResponseType;
import one.lindegaard.BagOfGold.storage.PlayerSettings;
import one.lindegaard.BagOfGold.util.Misc;
//import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.MobHunting;

public class EconomyManager {

	private BagOfGold plugin;

	public EconomyManager(BagOfGold plugin) {
		this.plugin = plugin;
	}

	public EconomyResponse depositPlayer(OfflinePlayer offlinePlayer, double amount) {
		PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
		if (offlinePlayer.isOnline()) {
			addBagOfGoldPlayer_EconomyManager((Player) offlinePlayer, ps.getBalanceChanges() + amount);
			ps.setBalanceChanges(0);
		} else {
			ps.setBalanceChanges(Misc.round(ps.getBalanceChanges() + amount));
		}
		ps.setBalance(Misc.round(ps.getBalance() + ps.getBalanceChanges() + amount));
		plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
		plugin.getDataStoreManager().updatePlayerSettings(offlinePlayer, ps);
		return new EconomyResponse(amount, ps.getBalance() + ps.getBalanceChanges(), ResponseType.SUCCESS, null);
	}

	public EconomyResponse withdrawPlayer(OfflinePlayer offlinePlayer, double amount) {
		PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
		if (has(offlinePlayer, amount)) {
			if (offlinePlayer.isOnline()) {
				removeBagOfGoldPlayer_EconomyManager((Player) offlinePlayer, amount);
				ps.setBalanceChanges(0);
			} else {
				ps.setBalanceChanges(Misc.round(ps.getBalanceChanges() - amount));
			}
			ps.setBalance(Misc.round(ps.getBalance() + ps.getBalanceChanges() - amount));
			plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
			plugin.getDataStoreManager().updatePlayerSettings(offlinePlayer, ps);
			return new EconomyResponse(amount, ps.getBalance() + ps.getBalanceChanges(), ResponseType.SUCCESS, null);
		} else
			return new EconomyResponse(0, ps.getBalance(), ResponseType.FAILURE, plugin.getMessages()
					.getString("bagofgold.commands.money.not-enough-money", "money", ps.getBalance()));
	}

	public boolean has(OfflinePlayer offlinePlayer, double amount) {
		return plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer).getBalance() >= amount;
	}

	public void addBagOfGoldPlayer_EconomyManager(OfflinePlayer offlinePlayer, double amount) {
		Player player = ((Player) Bukkit.getServer().getOfflinePlayer(offlinePlayer.getUniqueId()));
		boolean found = false;
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward rewardInSlot = Reward.getReward(is);
				if ((rewardInSlot.isBagOfGoldReward() || rewardInSlot.isItemReward())) {

					rewardInSlot.setMoney(rewardInSlot.getMoney() + amount);
					is = setDisplayNameAndHiddenLores(is, rewardInSlot);

					plugin.getMessages().debug(
							"Added %s to item in slot %s, new value is %s (addBagOfGoldPlayer_EconomyManager)",
							Misc.format(Misc.round(amount)), slot, Misc.format(Misc.round(rewardInSlot.getMoney())));
					found = true;
					break;
				}
			}
		}
		if (!found) {
			if (player.getInventory().firstEmpty() == -1)
				dropMoneyOnGround_EconomyManager(player, null, player.getLocation(), Misc.round(amount));
			else {
				ItemStack is;
				if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("SKULL"))
					is = new CustomItems(plugin).getCustomtexture(UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID),
							plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
							plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
							plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, Misc.round(amount),
							UUID.randomUUID(), UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID));
				else {
					is = new ItemStack(Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem), 1);
					setDisplayNameAndHiddenLores(is,
							new Reward(plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
									Misc.round(amount), UUID.fromString(Reward.MH_REWARD_ITEM_UUID), UUID.randomUUID(),
									null));
				}
				player.getInventory().addItem(is);
			}
		}
	}

	public double removeBagOfGoldPlayer_EconomyManager(Player player, double amount) {
		double taken = 0;
		double toBeTaken = Misc.round(amount);
		for (int slot = 0; slot < player.getInventory().getSize(); slot++) {
			ItemStack is = player.getInventory().getItem(slot);
			if (Reward.isReward(is)) {
				Reward reward = Reward.getReward(is);
				if (reward.isBagOfGoldReward() || reward.isItemReward()) {
					double saldo = Misc.round(reward.getMoney());
					if (saldo > toBeTaken) {
						reward.setMoney(Misc.round(saldo - toBeTaken));
						is = setDisplayNameAndHiddenLores(is, reward);
						player.getInventory().setItem(slot, is);
						taken = taken + toBeTaken;
						toBeTaken = 0;
						return Misc.round(taken);
					} else {
						//is.setItemMeta(null);
						//is.setType(Material.AIR);
						//is.setAmount(0);
						//player.getInventory().setItem(slot, is);
						player.getInventory().clear(slot);
						taken = taken + saldo;
						toBeTaken = toBeTaken - saldo;
					}
				}
			}
		}

		return taken;

	}

	public void dropMoneyOnGround_EconomyManager(Player player, Entity killedEntity, Location location, double money) {
		Item item = null;
		money = Misc.ceil(money);

		ItemStack is;
		UUID uuid = null, skinuuid = null;
		if (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("SKULL")) {
			uuid = UUID.fromString(Reward.MH_REWARD_BAG_OF_GOLD_UUID);
			skinuuid = uuid;
			is = new CustomItems(plugin).getCustomtexture(uuid,
					plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(),
					plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
					plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature, Misc.round(money),
					UUID.randomUUID(), skinuuid);
		} else { // ITEM
			uuid = UUID.fromString(Reward.MH_REWARD_ITEM_UUID);
			skinuuid = null;
			is = new ItemStack(Material.valueOf(plugin.getConfigManager().dropMoneyOnGroundItem), 1);
		}

		item = location.getWorld().dropItem(location, is);
		if (item != null) {
			MobHunting.getInstance().getRewardManager().getDroppedMoney().put(item.getEntityId(), Misc.round(money));
			item.setMetadata(Reward.MH_REWARD_DATA,
					new FixedMetadataValue(plugin,
							new Reward(
									plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM") ? ""
											: Reward.getReward(is).getDisplayname(),
									money, uuid, UUID.randomUUID(), skinuuid)));
			if (Misc.isMC18OrNewer()) {
				item.setCustomName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
						+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
								? Misc.format(money)
								: Reward.getReward(is).getDisplayname() + " (" + Misc.format(Misc.round(money)) + ")"));
				item.setCustomNameVisible(true);
			}
			plugin.getMessages().debug("%s was dropped on the ground as item %s (# of rewards=%s)",
					Misc.format(Misc.round(money)), plugin.getConfigManager().dropMoneyOnGroundItemtype,
					MobHunting.getInstance().getRewardManager().getDroppedMoney().size());
		}
	}

	public EconomyResponse bankDeposit(String account, double amount) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(account));
		if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
			PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Misc.round(ps.getBankBalance() + ps.getBankBalanceChanges() + amount));
				ps.setBankBalanceChanges(0);
			} else {
				ps.setBankBalanceChanges(ps.getBankBalanceChanges() + amount);
			}
			plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
			plugin.getDataStoreManager().updatePlayerSettings(offlinePlayer, ps);
			return new EconomyResponse(amount, ps.getBankBalance() + ps.getBankBalanceChanges(), ResponseType.SUCCESS,
					null);
		}
		return new EconomyResponse(0, 0, ResponseType.FAILURE, "Player has no bank account");
	}

	public EconomyResponse bankWithdraw(String account, double amount) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(account));
		if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
			PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Misc.round(ps.getBankBalance() + ps.getBankBalanceChanges() - amount));
				ps.setBankBalanceChanges(0);
			} else {
				ps.setBankBalanceChanges(ps.getBankBalanceChanges() - amount);
			}
			plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
			plugin.getDataStoreManager().updatePlayerSettings(offlinePlayer, ps);
			return new EconomyResponse(amount, ps.getBankBalance() + ps.getBankBalanceChanges(), ResponseType.SUCCESS,
					null);
		}
		return new EconomyResponse(0, 0, ResponseType.FAILURE, offlinePlayer.getName() + " has no bank account");

	}

	public EconomyResponse bankBalance(String account) {
		OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(UUID.fromString(account));
		if (offlinePlayer != null && offlinePlayer.hasPlayedBefore()) {
			PlayerSettings ps = plugin.getPlayerSettingsManager().getPlayerSettings(offlinePlayer);
			if (offlinePlayer.isOnline()) {
				ps.setBankBalance(Misc.round(ps.getBankBalance() + ps.getBankBalanceChanges()));
				ps.setBankBalanceChanges(0);
			}
			plugin.getPlayerSettingsManager().setPlayerSettings(offlinePlayer, ps);
			plugin.getDataStoreManager().updatePlayerSettings(offlinePlayer, ps);
			return new EconomyResponse(0, ps.getBankBalance() + ps.getBankBalanceChanges(), ResponseType.SUCCESS, null);
		}
		return new EconomyResponse(0, 0, ResponseType.FAILURE, offlinePlayer.getName() + " has no bank account");

	}

	public ItemStack setDisplayNameAndHiddenLores(ItemStack skull, Reward reward) {
		ItemMeta skullMeta = skull.getItemMeta();
		skullMeta.setLore(new ArrayList<String>(Arrays.asList("Hidden:" + reward.getDisplayname(),
				"Hidden:" + reward.getMoney(), "Hidden:" + reward.getRewardUUID(),
				reward.getMoney() == 0 ? "Hidden:" : "Hidden:" + UUID.randomUUID(), "Hidden:" + reward.getSkinUUID())));
		if (reward.getMoney() == 0)
			skullMeta.setDisplayName(
					ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor) + reward.getDisplayname());
		else
			skullMeta.setDisplayName(ChatColor.valueOf(plugin.getConfigManager().dropMoneyOnGroundTextColor)
					+ (plugin.getConfigManager().dropMoneyOnGroundItemtype.equalsIgnoreCase("ITEM")
							? Misc.format(reward.getMoney())
							: reward.getDisplayname() + " (" + Misc.format(reward.getMoney()) + ")"));
		skull.setItemMeta(skullMeta);
		return skull;
	}

	public void removeMoneyFromBalance(OfflinePlayer player, double amount) {
		PlayerSettings ps = BagOfGold.getInstance().getPlayerSettingsManager().getPlayerSettings(player);
		plugin.getMessages().debug("removing %s from balance %s", Misc.floor(amount),
				Misc.floor(ps.getBalance() + ps.getBalanceChanges()));
		ps.setBalance(Misc.floor(ps.getBalance() + ps.getBalanceChanges() - amount));
		BagOfGold.getInstance().getPlayerSettingsManager().setPlayerSettings(player, ps);
		BagOfGold.getInstance().getDataStoreManager().updatePlayerSettings(player, ps);
	}

	public void addMoneyToBalance(OfflinePlayer player, double amount) {
		PlayerSettings ps = BagOfGold.getInstance().getPlayerSettingsManager().getPlayerSettings(player);
		plugin.getMessages().debug("adding %s to balance %s", Misc.floor(amount),
				Misc.floor(ps.getBalance() + ps.getBalanceChanges()));
		ps.setBalance(Misc.floor(ps.getBalance() + ps.getBalanceChanges() + amount));
		BagOfGold.getInstance().getPlayerSettingsManager().setPlayerSettings(player, ps);
		BagOfGold.getInstance().getDataStoreManager().updatePlayerSettings(player, ps);
	}
}