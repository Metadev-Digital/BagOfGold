package metadev.digital.metabagofgold.bank;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.metacustomitemslib.materials.Materials;
import metadev.digital.metacustomitemslib.rewards.Reward;
import metadev.digital.metacustomitemslib.server.Servers;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

public class BankSign implements Listener {

	private BagOfGold plugin;

	public BankSign(BagOfGold plugin) {
		this.plugin = plugin;
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	// ****************************************************************************'
	// Events
	// ****************************************************************************'
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (event.isCancelled())
			return;

		if (Servers.isMC19OrNewer() && (event.getHand() == null || event.getHand().equals(EquipmentSlot.OFF_HAND)))
			return;

		Block clickedBlock = event.getClickedBlock();
		if (clickedBlock != null && isBankSign(clickedBlock) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
			Player player = event.getPlayer();
			if (player.hasPermission("bagofgold.banksign.use")) {
				Sign sign = ((Sign) clickedBlock.getState());
				String signType = sign.getLine(1);
				plugin.getMessages().debug("%s clicked on Banksign %s %s BagOfGold", player.getName(), sign.getLine(1),
						sign.getLine(2));
				double money = 0;
				double moneyInHand = 0;
				double moneyOnSign = 0;

				// deposit BankSign
				// -----------------------------------------------------------------------
				if (signType.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.deposit"))) {
					// Normal BagOfGold Rewards
					if (Reward.isReward(player.getItemInHand())
							&& (Reward.getReward(player.getItemInHand()).isBagOfGoldReward()
									|| Reward.getReward(player.getItemInHand()).isItemReward())) {

						Reward reward = Reward.getReward(player.getItemInHand());

						moneyInHand = reward.getMoney();
						money = moneyInHand;
						if (moneyInHand == 0) {
							plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
									"bagofgold.banksign.item_has_no_value", "itemname", reward.getDisplayName()));
							return;
						}

						if (sign.getLine(2).isEmpty() || sign.getLine(2).equalsIgnoreCase(
								plugin.getMessages().getString("bagofgold.banksign.line3.everything"))) {
							money = moneyInHand;
							moneyOnSign = moneyInHand;
						} else {
							try {
								moneyOnSign = Double.valueOf(sign.getLine(2));
								money = moneyInHand <= moneyOnSign ? moneyInHand : moneyOnSign;
							} catch (NumberFormatException e) {
								plugin.getMessages().playerSendMessage(player,
										plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number",
												"number", sign.getLine(2), "everything",
												plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
								return;
							}
						}

						boolean res = plugin.getEconomyManager().withdrawPlayer(player, money);
						if (res) {
							plugin.getEconomyManager().bankAccountDeposit(player.getUniqueId().toString(), money);
							plugin.getMessages().debug("%s deposit %s %s into Bank", player.getName(),
									plugin.getEconomyManager().format(money), reward.getDisplayName());
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.deposit", "money",
											plugin.getEconomyManager().format(money), "rewardname",
											ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
													+ reward.getDisplayName().trim()));
						}
						// Gringott items
					} else if (plugin.getGringottsItems().isGringottsReward(player.getItemInHand().getType())) {
						plugin.getMessages().debug("%s used %s %s on the sign", player.getName(),
								player.getItemInHand().getAmount(), player.getName(),
								player.getItemInHand().getType().name());
						moneyInHand = plugin.getGringottsItems().getMoneyInHand(player);
						plugin.getMessages().debug("%s has %s in his hand", player.getName(), moneyInHand);
						money = moneyInHand;
						if (moneyInHand == 0) {
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.item_has_no_value", "itemname",
											player.getItemInHand().getType().name()));
							return;
						}

						if (sign.getLine(2).isEmpty() || sign.getLine(2).equalsIgnoreCase(
								plugin.getMessages().getString("bagofgold.banksign.line3.everything"))) {
							money = moneyInHand;
							moneyOnSign = moneyInHand;
						} else {
							try {
								moneyOnSign = Double.valueOf(sign.getLine(2));
								money = moneyInHand <= moneyOnSign ? moneyInHand : moneyOnSign;
							} catch (NumberFormatException e) {
								plugin.getMessages().playerSendMessage(player,
										plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number",
												"number", sign.getLine(2), "everything",
												plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
								return;
							}
						}

						boolean res = plugin.getEconomyManager().withdrawPlayer(player, money);
						if (res) {
							plugin.getEconomyManager().bankAccountDeposit(player.getUniqueId().toString(), money);
							plugin.getMessages().debug("%s deposit %s %s into Bank", player.getName(),
									plugin.getEconomyManager().format(money), player.getItemInHand().getType());
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.deposit", "money",
											plugin.getEconomyManager().format(money), "rewardname",
											ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
													+ Core.getConfigManager().bagOfGoldName));
						}
					} else {
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("bagofgold.banksign.hold_bag_in_hand", "rewardname",
										ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
												+ Core.getConfigManager().bagOfGoldName.trim()));
					}

					// Withdraw BankSign
					// -----------------------------------------------------------------------
				} else if (signType
						.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.withdraw"))) {
					double space = plugin.getRewardManager().getSpaceForMoney(player);
					plugin.getMessages().debug("BankSign: space=%s", space);
					if (sign.getLine(2).isEmpty() || sign.getLine(2)
							.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line3.everything"))) {
						moneyOnSign = plugin.getEconomyManager().bankBalance(player.getUniqueId().toString());

					} else {
						try {
							moneyOnSign = Double.valueOf(sign.getLine(2));
						} catch (NumberFormatException e) {
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number", "number",
											sign.getLine(2), "everything",
											plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
							return;
						}
					}
					plugin.getMessages().debug("BankSign: moneyOnSign=%s, bankBal=%s", moneyOnSign,
							plugin.getEconomyManager().bankBalance(player.getUniqueId().toString()));
					if (Tools.round(plugin.getEconomyManager().bankBalance(player.getUniqueId().toString())) >= Tools
							.round(moneyOnSign)) {

						if (space < moneyOnSign)
							moneyOnSign = space;
						if (plugin.getEconomyManager().bankAccountWithdraw(player.getUniqueId().toString(),
								moneyOnSign)) {
							plugin.getEconomyManager().depositPlayer(player, moneyOnSign);

							plugin.getMessages().debug("%s withdraw %s %s from Bank", player.getName(),
									plugin.getEconomyManager().format(Tools.round(moneyOnSign)),
									Core.getConfigManager().bagOfGoldName.trim());
							plugin.getMessages().playerSendMessage(player,
									plugin.getMessages().getString("bagofgold.banksign.withdraw", "money",
											plugin.getEconomyManager().format(moneyOnSign), "rewardname",
											ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
													+ Core.getConfigManager().bagOfGoldName.trim()));
						} else {
							plugin.getMessages().debug("%s could not withdraw %s %s from Bank", player.getName(),
									plugin.getEconomyManager().format(moneyOnSign),
									Core.getConfigManager().bagOfGoldName.trim());

						}
					} else {
						plugin.getMessages().debug("Space=%s, bankbal=%s", space,
								Tools.round(plugin.getEconomyManager().bankBalance(player.getUniqueId().toString())));
						double bal = Tools
								.round(plugin.getEconomyManager().bankBalance(player.getUniqueId().toString()));
						if (space < bal)
							bal = space;
						plugin.getEconomyManager().bankAccountWithdraw(player.getUniqueId().toString(), bal);
						plugin.getEconomyManager().depositPlayer(player, bal);
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("bagofgold.banksign.withdraw", "money", bal,
										"rewardname", ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
												+ Core.getConfigManager().bagOfGoldName.trim()));
						// plugin.getMessages().playerSendMessage(player,
						// plugin.getMessages().getString("bagofgold.banksign.not_enough_money"));
					}

					// Balance BankSign
					// -----------------------------------------------------------------------
				} else if (signType
						.equalsIgnoreCase(plugin.getMessages().getString("bagofgold.banksign.line2.balance"))) {
					double bal = plugin.getEconomyManager().bankBalance(player.getUniqueId().toString());
					plugin.getMessages().playerSendMessage(player,
							plugin.getMessages().getString("bagofgold.banksign.balance", "money",
									plugin.getEconomyManager().format(bal), "rewardname",
									ChatColor.valueOf(Core.getConfigManager().rewardTextColor)
											+ Core.getConfigManager().bagOfGoldName.trim()));
				}
			} else {
				plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
						"bagofgold.banksign.no_permission_to_use", Core.PH_PERMISSION, "bagofgold.banksign.use"));
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onSignChangeEvent(SignChangeEvent event) {
		if (event.isCancelled())
			return;

		Player player = event.getPlayer();
		if (isBagOfGoldSign(event.getLine(0))) {
			if (event.getPlayer().hasPermission("bagofgold.banksign.create")) {

				// Check line 2
				if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.deposit")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.deposit"));

				} else if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.withdraw")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.withdraw"));
				} else if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.balance")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.balance"));
				} else {
					plugin.getMessages().playerSendMessage(player, plugin.getMessages()
							.getString("bagofgold.banksign.line2.mustbe_deposit_or_withdraw_or_balance"));
					event.setLine(3,
							plugin.getMessages().getString("bagofgold.banksign.line4.error_on_sign", "line", "2"));
					return;
				}

				// Check line 3
				if (ChatColor.stripColor(event.getLine(1)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line2.balance")))) {
					event.setLine(1, plugin.getMessages().getString("bagofgold.banksign.line2.balance"));
					event.setLine(2, "");
				} else if (event.getLine(2).isEmpty() || ChatColor.stripColor(event.getLine(2)).equalsIgnoreCase(
						ChatColor.stripColor(plugin.getMessages().getString("bagofgold.banksign.line3.everything")))) {
					event.setLine(2, plugin.getMessages().getString("bagofgold.banksign.line3.everything"));
				} else {

					try {
						if (Double.valueOf(event.getLine(2)) > 0) {
							plugin.getMessages().debug("%s created a BagOfGold Bank Sign", event.getPlayer().getName());
						}
					} catch (NumberFormatException e) {
						plugin.getMessages().playerSendMessage(player,
								plugin.getMessages().getString("bagofgold.banksign.line3.not_a_number", "number",
										event.getLine(2), "everything",
										plugin.getMessages().getString("bagofgold.banksign.line3.everything")));
						event.setLine(3,
								plugin.getMessages().getString("bagofgold.banksign.line4.error_on_sign", "line", "3"));
						return;
					}
				}

				event.setLine(0, plugin.getMessages().getString("bagofgold.banksign.line1", "bankname",
						plugin.getConfigManager().bankname));
				event.setLine(3, plugin.getMessages().getString("bagofgold.banksign.line4.ok"));

			} else {
				plugin.getMessages().playerSendMessage(player, plugin.getMessages().getString(
						"bagofgold.banksign.no_permission", Core.PH_PERMISSION, "bagofgold.banksign.create"));
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = false)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		if (event.isCancelled())
			return;

		Block b = event.getBlock();
		if (isBankSign(b)) {
			if (event.getPlayer().hasPermission("bagofgold.banksign.destroy")) {
				plugin.getMessages().debug("%s destroyed a BagOfGold Bank Sign", event.getPlayer().getName());
			} else {
				plugin.getMessages().debug("%s tried to destroy a BagOfGold Bank Sign without permission",
						event.getPlayer().getName());
				event.getPlayer().sendMessage(plugin.getMessages().getString("bagofgold.banksign.no_permission",
						Core.PH_PERMISSION, "bagofgold.banksign.destroy"));
				event.setCancelled(true);
			}
		}
	}

	// ************************************************************************************
	// TESTS
	// ************************************************************************************

	private boolean isBankSign(Block block) {
		if (Materials.isSign(block)) {
			return ChatColor.stripColor(((Sign) block.getState()).getLine(0)).equalsIgnoreCase(
					ChatColor.stripColor(BagOfGold.getInstance().getMessages().getString("bagofgold.banksign.line1",
							"bankname", BagOfGold.getInstance().getConfigManager().bankname.trim())))
					|| ChatColor.stripColor(((Sign) block.getState()).getLine(0)).equalsIgnoreCase("[BagOfGold Bank]");
		}
		return false;
	}

	private boolean isBagOfGoldSign(String line) {
		return ChatColor.stripColor(line).equalsIgnoreCase(
				ChatColor.stripColor(BagOfGold.getInstance().getMessages().getString("bagofgold.banksign.line1",
						"bankname", BagOfGold.getInstance().getConfigManager().bankname.trim())))
				|| ChatColor.stripColor(line).equalsIgnoreCase("[BagOfGold Bank]");
	}

}
