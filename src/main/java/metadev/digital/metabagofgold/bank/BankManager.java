package metadev.digital.metabagofgold.bank;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.hover.content.Text;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metabagofgold.PlayerBalances;
import metadev.digital.metabagofgold.compatibility.CitizensCompat;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.PlayerSettings;
import metadev.digital.metacustomitemslib.Tools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

public class BankManager {

	private BagOfGold plugin;

	private File file;
	private YamlConfiguration config = new YamlConfiguration();
	// private boolean dataChanged = false;

	private long period = 168000;
	private BukkitTask mBankInterestCalculator = null;
	private HashMap<String, Bank> banks = new HashMap<>(); // unique bankId , bank

	public BankManager(BagOfGold plugin) {
		this.plugin = plugin;

		file = new File(plugin.getDataFolder().getParent(), "BagOfGold/banks.yml");
		load();

		start();

	}

	// ***************************************************************************************************************************************
	// BANK - with a collection of vaults
	// ***************************************************************************************************************************************

	public HashMap<String, Bank> getBanks() {
		return banks;
	}

	public Bank getBank(String regionId) {
		Iterator<Entry<String, Bank>> itr = banks.entrySet().iterator();
		itr.hasNext();
		while (itr.hasNext()) {
			Entry<String, Bank> id = itr.next();
			Bank bank = id.getValue();
			if (bank.getRegionId().equalsIgnoreCase(regionId))
				return bank;
		}
		return null;
	}

	public void setBanks(HashMap<String, Bank> banks) {
		this.banks = banks;
		// dataChanged=true;
	}

	public void addBank(String bankname, OfflinePlayer owner, String region) {
		Bank bank = new Bank(plugin);
		bank.setBanknumber(getNextID());
		bank.setDisplayName(bankname);
		if (owner != null)
			bank.setOwner(owner);
		bank.setRegionId(region);
		getBanks().put(bank.getBankId(), bank);
		// dataChanged=true;
	}

	public void addBank(Bank bank) {
		getBanks().put(bank.getBankId(), bank);
		// dataChanged=true;
	}

	public void removeBank(int banknumber) {
		Iterator<Entry<String, Bank>> itr = banks.entrySet().iterator();
		itr.hasNext();
		while (itr.hasNext()) {
			Entry<String, Bank> id = itr.next();
			Bank bank = id.getValue();
			if (bank.getBanknumber() == banknumber) {
				banks.remove(bank.getBankId());
			}
		}
		// dataChanged=true;
	}

	public int getNextID() {
		int max = 0;
		for (String uuid : getBanks().keySet()) {
			Bank bank = getBanks().get(uuid);
			max = Math.max(max, bank.getBanknumber());
		}
		return max + 1;
	}

	public boolean hasBank(String regionId) {
		Iterator<Entry<String, Bank>> itr = banks.entrySet().iterator();
		itr.hasNext();
		while (itr.hasNext()) {
			Entry<String, Bank> id = itr.next();
			Bank bank = id.getValue();
			if (bank.getRegionId().equalsIgnoreCase(regionId))
				return true;
		}
		return false;
	}

	// *****************************************************************************************************************************************
	// SAVE and LOAD the vaults to disk
	// *****************************************************************************************************************************************

	public void save() {
		int n = 0;
		// if (dataChanged) {
		try {
			config.options()
					.setHeader(Arrays.asList(new String[] { "This is BagOfGold vaults.",
							"Do not edit this file manually!!!",
							"If you remove a section, the vault will loose its value on next server restart." }));

			ConfigurationSection section = config.createSection("banks");
			Iterator<Entry<String, Bank>> itr = banks.entrySet().iterator();
			while (itr.hasNext()) {
				Entry<String, Bank> id = itr.next();
				Bank bank = id.getValue();
				ConfigurationSection bankSection = section.createSection(bank.getBankId());
				bank.save(bankSection);
				n++;
			}
			config.save(file);
			if (n > 0) {
				plugin.getMessages().debug("Saved %s Banks to disk", n);
				// dataChanged = false;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// }
	}

	public void load() {
		int n = 0;
		plugin.getMessages().debug("Loading banks.yml");

		try {
			if (file.exists())
				config.load(file);
		} catch (IllegalArgumentException | InvalidConfigurationException e) {
			Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX + ChatColor.RED
					+ " The banks.yml file contain broken vaults. They will be deleted. ");
		} catch (IOException e) {
			e.printStackTrace();
		}

		ConfigurationSection banksSection = config.getConfigurationSection("banks");

		if (banksSection == null)
			return;

		for (String key : banksSection.getKeys(false)) {
			ConfigurationSection section = banksSection.getConfigurationSection(key);
			Bank bank = new Bank(plugin);
			bank.read(section);
			banks.put(bank.getBankId(), bank);
			n++;
		}
		if (n > 0)
			plugin.getMessages().debug("%s banks was loaded from BagOfGold/banks.yml", n);

	}

	// ***************************************************************************************************************************************
	// Bank Interest caculation
	// ***************************************************************************************************************************************
	public void start() {
		if (plugin.getConfigManager().calculateInterests) {
			// https://minecraft.gamepedia.com/Day-night_cycle
			switch (plugin.getConfigManager().interestPeriod) {
			case "DAY":
				period = 24000; // 1 minecraft day = 20 min
				break;

			case "WEEK":
				period = 168000; // 1 minecraft week = 2.3 hours
				break;

			case "MONTH":
				period = 720000; // 1 minecraft months = 10 hours
				break;

			case "YEAR":
				period = 8766000; // 1 minecraaddTaskft year = 121.75 hours
				break;

			default:

				if (plugin.getConfigManager().interestPeriod.matches("\\d+$")) {
					period = Integer.valueOf(plugin.getConfigManager().interestPeriod) * 20;
				} else {
					Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX_WARNING
							+ "The interest period in the config.yml must be an integer (seconds) or 'DAY/WEEK/MONTH/YEAR'.");
				}

				break;
			}
			mBankInterestCalculator = Bukkit.getScheduler().runTaskTimer(plugin, new InterestUpdater2(), period,
					period);
		}

	}

	public void shutdown() {
		if (mBankInterestCalculator != null)
			mBankInterestCalculator.cancel();
	}

	private class InterestUpdater2 implements Runnable {

		@Override
		public void run() {
			plugin.getMessages().debug(ChatColor.BLUE + "Start bank interest calculation.");
			Collection<Player> onlinePlayers = Tools.getOnlinePlayers();
			for (Player p : onlinePlayers) {
				PlayerSettings ps = Core.getPlayerSettingsManager().getPlayerSettings(p);
				if (ps.getLast_interest() == 0)
					ps.setLast_interest(System.currentTimeMillis() - period);
				PlayerBalances pbs = plugin.getPlayerBalanceManager().getBalances().get(p.getUniqueId());
				for (PlayerBalance pb : pbs.getPlayerBalances().values()) {
					plugin.getMessages()
							.debug(ChatColor.BLUE
									+ "Calculating Bank interest for %s in worldGroup:%s (Balance=%s, new balance=%s",
									p.getName(), pb.getWorldGroup(), pb.getBankBalance(),
									Tools.round(pb.getBankBalance() * (1 + plugin.getConfigManager().interest / 100)));
					pb.setBankBalance(
							Tools.round(pb.getBankBalance() * (1 + plugin.getConfigManager().interest / 100)));
					plugin.getPlayerBalanceManager().setPlayerBalance(p, pb);
				}
				ps.setLast_interest(ps.getLast_interest() + period);
			}
		}
	}

	// ***************************************************************************************************************************************
	// The BagOfGoldBanker (NPC)
	// ***************************************************************************************************************************************
	public boolean isBagOfGoldBanker(Entity entity) {
		if (CitizensAPI.getNPCRegistry().isNPC(entity)) {
			NPC npc = CitizensCompat.getCitizensPlugin().getNPCRegistry().getNPC(entity);
			return (npc.hasTrait(BagOfGoldBankerTrait.class));
		} else
			return false;
	}

	public void sendBankerMessage(Player player) {

		PlayerBalance ps = plugin.getPlayerBalanceManager().getPlayerBalance(player);

		player.spigot()
				.sendMessage(new ComponentBuilder(plugin.getMessages().getString("bagofgold.banker.balance") + ": "
						+ plugin.getEconomyManager().format(ps.getBalance() + ps.getBalanceChanges()) + " "
						+ plugin.getMessages().getString("bagofgold.banker.bankbalance") + ": "
						+ plugin.getEconomyManager().format(ps.getBankBalance() + ps.getBankBalanceChanges()))
						.color(ChatColor.GREEN).bold(true).create());

		plugin.getMessages()
				.debug("BankManager actions(" + plugin.getMessages().getString("bagofgold.banker.deposit") + "/"
						+ plugin.getMessages().getString("bagofgold.banker.withdraw") + ")="
						+ plugin.getConfigManager().actions.entrySet().toString());

		ComponentBuilder deposit = new ComponentBuilder(
				plugin.getMessages().getString("bagofgold.banker.deposit") + ": ").color(ChatColor.GREEN).bold(true)
				.append(" ");
		Iterator<Entry<String, String>> itr1 = plugin.getConfigManager().actions.entrySet().iterator();
		while (itr1.hasNext()) {
			Entry<String, String> set = itr1.next();
			if (set.getKey().toLowerCase()
					.startsWith(plugin.getMessages().getString("bagofgold.banker.deposit").toLowerCase())) {
				Text clickToDeposit = new Text(
						plugin.getMessages().getString("bagofgold.banker.click2deposit") + " " + set.getValue() + ".");
				deposit.append("[" + set.getValue() + "]").color(ChatColor.RED).bold(true)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, clickToDeposit))
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
								"/bagofgold money deposit " + set.getValue()))
						.append(" ");
			}
		}
		player.spigot().sendMessage(deposit.create());

		ComponentBuilder withdraw = new ComponentBuilder(
				plugin.getMessages().getString("bagofgold.banker.withdraw") + ": ").color(ChatColor.GREEN).bold(true)
				.append(" ");
		Iterator<Entry<String, String>> itr2 = plugin.getConfigManager().actions.entrySet().iterator();
		while (itr2.hasNext()) {
			Entry<String, String> set = itr2.next();
			if (set.getKey().toLowerCase()
					.startsWith(plugin.getMessages().getString("bagofgold.banker.withdraw").toLowerCase())) {
				Text clickToWithdraw = new Text(
						plugin.getMessages().getString("bagofgold.banker.click2withdraw") + " " + set.getValue() + ".");
				withdraw.append("[" + set.getValue() + "]").color(ChatColor.RED).bold(true)
						.event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, clickToWithdraw))
						.event(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
								"/bagofgold money withdraw " + set.getValue()))
						.append(" ");
			}
		}
		player.spigot().sendMessage(withdraw.create());

	}

}
