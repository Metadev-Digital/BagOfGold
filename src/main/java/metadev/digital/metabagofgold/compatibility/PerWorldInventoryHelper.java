package metadev.digital.metabagofgold.compatibility;

import me.ebonjaeger.perworldinventory.event.InventoryLoadEvent;
import metadev.digital.metabagofgold.BagOfGold;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class PerWorldInventoryHelper {

	public static void registerPWIEvents(BagOfGold plugin) {
		Bukkit.getPluginManager().registerEvents(new Listener() {
			@EventHandler(priority = EventPriority.HIGHEST)
			public void onInventoryLoad(InventoryLoadEvent event) {
				Bukkit.getScheduler().runTaskLater(plugin, new Runnable() {
					@Override
					public void run() {
						if (event.getPlayer().getGameMode() != GameMode.SPECTATOR )//&& event.getPlayer().getGameMode() != GameMode.CREATIVE)
							plugin.getRewardManager().adjustAmountOfMoneyInInventoryToPlayerBalance(event.getPlayer());
					}
				}, 20);
			}
		}, plugin);
	}

}
