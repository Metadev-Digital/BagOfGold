package metadev.digital.metabagofgold;

import metadev.digital.metabagofgold.compatibility.*;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.HttpTools;
import metadev.digital.metacustomitemslib.HttpTools.httpCallback;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

public class MetricsManager {

	private BagOfGold plugin;
	private boolean started = false;

	private Metrics bStatsMetrics;

	public MetricsManager(BagOfGold plugin) {
		this.plugin = plugin;
	}

	public void start() {
		Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
			public void run() {
				try {
					URL url = new URL("https://bstats.org/");
					if (!started) {
						HttpTools.isHomePageReachable(url, new httpCallback() {

							@Override
							public void onSuccess() {
								startBStatsMetrics();
								plugin.getMessages().debug("Metrics reporting to Https://bstats.org has started.");
								started = true;
							}

							@Override
							public void onError() {
								started = false;
								plugin.getMessages().debug("https://bstats.org/ seems to be down");
							}
						});
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}, 100L, 72000L);
	}

	public void startBStatsMetrics() {
		// https://bstats.org/what-is-my-plugin-id
		bStatsMetrics = new Metrics(plugin, 22713);
		bStatsMetrics.addCustomChart(
				new SimplePie("database_used_for_bagofgold", () -> plugin.getConfigManager().databaseType));
		bStatsMetrics
				.addCustomChart(new AdvancedPie("other_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("Citizens", CitizensCompat.isSupported() ? 1 : 0);
						valueMap.put("Essentials", EssentialsCompat.isSupported() ? 1 : 0);
						//TODO: PerWorldInventory is possibly deprecated valueMap.put("PerWorldInventory", PerWorldInventoryCompat.isSupported() ? 1 : 0);
						//TODO: ProtocolLib is possibly deprecated valueMap.put("ProtocolLib", ProtocolLibCompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));
		bStatsMetrics.addCustomChart(new SimplePie("language", () -> plugin.getConfigManager().language));
		
		bStatsMetrics.addCustomChart(new SimplePie("item_type", () -> Core.getConfigManager().rewardItemtype));
		
		bStatsMetrics.addCustomChart(new SimplePie("economy_base", () -> plugin.getEconomyManager().getEconomyAPI()));

		/**bStatsMetrics.addCustomChart(new AdvancedPie("titlemanagers", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				//actionbar
				valueMap.put("TitleAPI", TitleAPICompat.isSupported() ? 1 : 0);
				valueMap.put("TitleManager", TitleManagerCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBar", ActionbarCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBarAPI", ActionBarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("ActionAnnouncer", ActionAnnouncerCompat.isSupported() ? 1 : 0);
				valueMap.put("CMI", CMICompat.isSupported() ? 1 : 0);
				//bossbar
				valueMap.put("BarAPI", BarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("BossBarAPI", BossBarAPICompat.isSupported() ? 1 : 0);
				//Placeholder
				valueMap.put("PlaceholderAPI", PlaceholderAPICompat.isSupported() ? 1 : 0);
				return valueMap;
			}
		}));*/

	}
}
