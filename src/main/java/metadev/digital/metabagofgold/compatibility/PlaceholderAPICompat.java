package metadev.digital.metabagofgold.compatibility;

import me.clip.placeholderapi.PlaceholderAPI;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.placeholder.BagOfGoldPlaceholderExpansion;
import metadev.digital.metabagofgold.placeholder.PlaceHolderData;
import metadev.digital.metabagofgold.placeholder.PlaceHolderManager;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

public class PlaceholderAPICompat {

	private static Plugin mPlugin;
	private static boolean supported = false;
	private static PlaceHolderManager mPlaceHolderManager;

	// https://www.spigotmc.org/resources/placeholderapi.6245/
	// https://github.com/PlaceholderAPI/PlaceholderAPI/wiki/PlaceholderExpansion#without-external-plugin

	public PlaceholderAPICompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					BagOfGold.PREFIX_WARNING + "Compatibility with PlaceholderAPI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.PlaceholderAPI.getName());
			if (mPlugin.getDescription().getVersion().compareTo("2.11.1") >= 0) {
				Bukkit.getConsoleSender().sendMessage(BagOfGold.PREFIX + "Enabling compatibility with PlaceholderAPI ("
						+ mPlugin.getDescription().getVersion() + ").");
				new BagOfGoldPlaceholderExpansion().register();
				mPlaceHolderManager = new PlaceHolderManager(BagOfGold.getInstance());
				supported = true;
			} else {
				Bukkit.getConsoleSender()
						.sendMessage(BagOfGold.PREFIX_WARNING + "Your current version of PlaceholderAPI ("
								+ mPlugin.getDescription().getVersion()
								+ ") is not supported by BagOfGold, please upgrade to 2.11.1 or newer.");
			}
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return BagOfGold.getInstance().getConfigManager().enableIntegrationPlaceholderAPI;
	}

	public static HashMap<UUID, PlaceHolderData> getPlaceHolders() {
		return mPlaceHolderManager.getPlaceHolders();
	}

	public static String setPlaceholders(Player player, String messages_with_placeholders) {
		if (isSupported())
			return PlaceholderAPI.setPlaceholders(player, messages_with_placeholders);
		return messages_with_placeholders;
	}

}
