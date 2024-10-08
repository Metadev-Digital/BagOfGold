package metadev.digital.metabagofgold.commands;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.Messages;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class ReloadCommand implements ICommand {

	private BagOfGold plugin;

	public ReloadCommand(BagOfGold plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "bagofgold.reload";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.WHITE + " - to reload BagOfGold configuration." };
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("bagofgold.commands.reload.description");
	}

	@Override
	public boolean canBeConsole() {
		return true;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {

		long starttime = System.currentTimeMillis();
		int i = 1;
		while (plugin.getDataStoreManager().isRunning() && (starttime + 10000 > System.currentTimeMillis())) {
			if (((int) (System.currentTimeMillis() - starttime)) / 1000 == i) {
				plugin.getMessages().debug("saving data (%s)");
				i++;
			}
		}

		plugin.setMessages(new Messages(plugin));

		if (Core.getConfigManager().loadConfig() || plugin.getConfigManager().loadConfig()) {
			Core.getWorldGroupManager().load();

			plugin.getBankManager().shutdown();
			plugin.getBankManager().start();

			int n = Tools.getOnlinePlayersAmount();
			if (n > 0) {
				plugin.getMessages().debug("Reloading %s PlayerSettings & PlayerBalancees from the database", n);
				for (Player player : Tools.getOnlinePlayers()) {
					Core.getPlayerSettingsManager().load(player);
					plugin.getPlayerBalanceManager().load(player);
				}
			}

			plugin.getMessages().senderSendMessage(sender,
					ChatColor.GREEN + plugin.getMessages().getString("bagofgold.commands.reload.reload-complete"));

		} else
			plugin.getMessages().senderSendMessage(sender,
					ChatColor.RED + plugin.getMessages().getString("bagofgold.commands.reload.reload-error"));

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		return null;
	}

}
