package metadev.digital.metabagofgold.commands;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.storage.DataStoreManager;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.PlayerSettings;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class MuteCommand implements ICommand {

	private BagOfGold plugin;

	public MuteCommand(BagOfGold plugin) {
		this.plugin = plugin;
	}

	// Used case
	// /mh mute - No args, args.length = 0 || arg[0]=""

	@Override
	public String getName() {
		return "mute";
	}

	@Override
	public String[] getAliases() {
		return new String[] { "silent", "notify" };
	}

	@Override
	public String getPermission() {
		return "bagofgold.mute";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.WHITE + " - to mute/unmute.",
				ChatColor.GOLD + label + ChatColor.GREEN + " <playername>" + ChatColor.WHITE
						+ " - to mute/unmute a the notifications for a specific player." };
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("bagofgold.commands.mute.description");
	}

	@Override
	public boolean canBeConsole() {
		return false;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		return null;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {

		if (args.length == 0) {
			togglePlayerMuteMode((Player) sender);
			return true;
		} else if (args.length == 1) {
			DataStoreManager ds = plugin.getDataStoreManager();
			Player player;
			try {
				player = (Player) Core.getStoreManager().getPlayerByName(args[0]);
				if (player != null) {
					if (sender.hasPermission("bagofgold.mute.other") || sender instanceof ConsoleCommandSender) {
						togglePlayerMuteMode(player);
					} else {
						plugin.getMessages().senderSendMessage(sender,
								ChatColor.RED + "You dont have permission " + ChatColor.AQUA + "'bagofgold.mute.other'");
					}
					return true;
				} else {
					plugin.getMessages().senderSendMessage(sender, ChatColor.RED + "Player " + args[0] + " is not online.");
					return false;
				}
			} catch (DataStoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		return false;
	}

	private void togglePlayerMuteMode(Player player) {
		PlayerSettings ps = Core.getPlayerSettingsManager().getPlayerSettings(player);
		ps.setMuteMode(!ps.isMuted());
		Core.getPlayerSettingsManager().setPlayerSettings(ps);
		if (ps.isMuted())
			plugin.getMessages().playerActionBarMessageQueue(player, 
					plugin.getMessages().getString("bagofgold.commands.mute.muted", Core.PH_PLAYERNAME, player.getName()));
		else
			plugin.getMessages().playerActionBarMessageQueue(player, 
					plugin.getMessages().getString("bagofgold.commands.mute.unmuted", Core.PH_PLAYERNAME, player.getName()));
	}
}
