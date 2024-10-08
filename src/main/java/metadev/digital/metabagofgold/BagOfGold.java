package metadev.digital.metabagofgold;

import metadev.digital.metabagofgold.api.BagOfGoldAPI;
import metadev.digital.metabagofgold.bank.BankManager;
import metadev.digital.metabagofgold.bank.BankSign;
import metadev.digital.metabagofgold.commands.*;
import metadev.digital.metabagofgold.compatibility.*;
import metadev.digital.metabagofgold.config.ConfigManager;
import metadev.digital.metabagofgold.rewards.BagOfGoldItems;
import metadev.digital.metabagofgold.rewards.GringottsItems;
import metadev.digital.metabagofgold.rewards.RewardManager;
import metadev.digital.metabagofgold.storage.DataStoreManager;
import metadev.digital.metabagofgold.storage.IDataStore;
import metadev.digital.metabagofgold.storage.MySQLDataStore;
import metadev.digital.metabagofgold.storage.SQLiteDataStore;
import metadev.digital.metabagofgold.update.UpdateManager;
import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class BagOfGold extends JavaPlugin {

	private static BagOfGold plugin;
	private File mFile = new File(getDataFolder(), "config.yml");

	private Messages mMessages;
	private EconomyManager mEconomyManager;
	private MetricsManager mMetricsManager;
	private ConfigManager mConfig;
	private CommandDispatcher mCommandDispatcher;
	private IDataStore mStore;
	private DataStoreManager mStoreManager;
	private RewardManager mRewardManager;
	private CompatibilityManager mCompatibilityManager;
	private BankManager mBankManager;
	private static UpdateManager mUpdateManager;
	private PlayerBalanceManager mPlayerBalanceManager;
	private GringottsItems mGringottsItems;
	private BagOfGoldItems mBagOfGoldItems;

	private boolean mInitialized = false;
	public boolean disabling = false;

	public static final String PREFIX = ChatColor.GOLD + "[BagOfGold] " + ChatColor.RESET;
	public static final String PREFIX_DEBUG = ChatColor.GOLD + "[BagOfGold][Debug] " + ChatColor.RESET;
	public static final String PREFIX_WARNING = ChatColor.GOLD + "[BagOfGold][Warning] " + ChatColor.RED;
	public static final String PREFIX_ERROR = ChatColor.GOLD + "[BagOfGold][Error] " + ChatColor.RED;

	@Override
	public void onLoad() {
	}

	@Override
	public void onEnable() {

		disabling = false;
		plugin = this;

		mMessages = new Messages(this);
		mConfig = new ConfigManager(this, mFile);

		if (mConfig.loadConfig()) {
			if (mConfig.backup)
				mConfig.backupConfig(mFile);
			mConfig.saveConfig();
		} else
			throw new RuntimeException(plugin.getMessages().getString("bagofgold.config.fail"));

		if (isbStatsEnabled())
			plugin.getMessages().debug("bStat is enabled");
		else {
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "=====================WARNING=============================");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "The statistics collection is disabled. As developer I need the");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "statistics from bStats.org. The statistics is 100% anonymous.");
			Bukkit.getConsoleSender().sendMessage(PREFIX_WARNING + "https://bstats.org/plugin/bukkit/bagofgold");
			Bukkit.getConsoleSender().sendMessage(
					PREFIX_WARNING + "Please enable this in /plugins/bStats/config.yml and get rid of this");
			Bukkit.getConsoleSender().sendMessage(PREFIX_WARNING + "message. Loading will continue in 15 sec.");
			Bukkit.getConsoleSender()
					.sendMessage(PREFIX_WARNING + "=========================================================");
			long now = System.currentTimeMillis();
			while (System.currentTimeMillis() < now + 15000L) {
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}
			}
		}


		// Register commands
		mCommandDispatcher = new CommandDispatcher(this, "bagofgold",
				plugin.getMessages().getString("bagofgold.command.base.description") + getDescription().getVersion());
		getCommand("bagofgold").setExecutor(mCommandDispatcher);
		getCommand("bagofgold").setTabCompleter(mCommandDispatcher);
		mCommandDispatcher.registerCommand(new ReloadCommand(this));
		mCommandDispatcher.registerCommand(new NpcCommand(this));
		mCommandDispatcher.registerCommand(new UpdateCommand(this));
		mCommandDispatcher.registerCommand(new VersionCommand(this));
		mCommandDispatcher.registerCommand(new DebugCommand(this));
		mCommandDispatcher.registerCommand(new ConvertCommand(this));
		mCommandDispatcher.registerCommand(new MoneyCommand(this));
		mCommandDispatcher.registerCommand(new BankCommand(this));
		mCommandDispatcher.registerCommand(new MuteCommand(this));
		mCommandDispatcher.registerCommand(new DatabaseCommand(this));

		// Check for new updates
		mUpdateManager = new UpdateManager(this);
		mUpdateManager.processCheckResultInConsole();

		if (mConfig.databaseType.equalsIgnoreCase("mysql"))
			mStore = new MySQLDataStore(this);
		else
			mStore = new SQLiteDataStore(this);

		try {
			mStore.initialize();
		} catch (DataStoreException e) {
			e.printStackTrace();
			try {
				mStore.shutdown();
			} catch (DataStoreException e1) {
				e1.printStackTrace();
			}
			setEnabled(false);
			return;
		}

		mBankManager = new BankManager(this);

		mStoreManager = new DataStoreManager(this, mStore);

		mPlayerBalanceManager = new PlayerBalanceManager(this);

		mRewardManager = new RewardManager(this);

		mCompatibilityManager = new CompatibilityManager(this);

		//TODO: PerWorldInventory is possibly deprecated mCompatibilityManager.registerPlugin(PerWorldInventoryCompat.class, CompatPlugin.PerWorldInventory);
		if (!Servers.isSpigotServer() && !Servers.isPaperServer() && !Servers.isPurpurServer())
			Bukkit.getConsoleSender().sendMessage(PREFIX_WARNING + "This is server (" + Bukkit.getServer().getName()
					+ ") is not tested with the BagOfGold-Citizens integration");
		mCompatibilityManager.registerPlugin(CitizensCompat.class, CompatPlugin.Citizens);
		mCompatibilityManager.registerPlugin(EssentialsCompat.class, CompatPlugin.Essentials);

		mCompatibilityManager.registerPlugin(WorldEditCompat.class, CompatPlugin.WorldEdit);
		mCompatibilityManager.registerPlugin(WorldGuardCompat.class, CompatPlugin.WorldGuard);

		mCompatibilityManager.registerPlugin(PlaceholderAPICompat.class, CompatPlugin.PlaceholderAPI);

		mCompatibilityManager.registerPlugin(ShopkeepersCompat.class, CompatPlugin.Shopkeepers);

		if (!Servers.isGlowstoneServer()) {
			mMetricsManager = new MetricsManager(this);
			mMetricsManager.startBStatsMetrics();
		}

		// Initialize BagOfGold Bank Signs
		new BankSign(this);

		// start the Economy Service Provider using Vault or Reserve
		mEconomyManager = new EconomyManager(this);

		/** //TODO: PerWorldInventory is possibly deprecated if (PerWorldInventoryCompat.isSupported() && PerWorldInventoryCompat.pwi_sync_economy())
			PerWorldInventoryCompat.pwi_sync_economy_warning();*/

		mGringottsItems = new GringottsItems(this);
		mBagOfGoldItems = new BagOfGoldItems(this);

		mInitialized = true;

	}

	@Override
	public void onDisable() {
		disabling = true;

		if (!mInitialized)
			return;

		mBankManager.shutdown();

		try {
			getMessages().debug("Shutdown StoreManager");
			mStoreManager.shutdown();
			getMessages().debug("Shutdown Store");
			mStore.shutdown();
		} catch (DataStoreException e) {
			e.printStackTrace();
		}

		Bukkit.getConsoleSender().sendMessage(PREFIX + "BagOfGold was disabled.");
	}

	private boolean isbStatsEnabled() {
		File bStatsFolder = new File(plugin.getDataFolder().getParentFile(), "bStats");
		File configFile = new File(bStatsFolder, "config.yml");
		YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
		return config.getBoolean("enabled", true);
	}

	// ************************************************************************************
	// Managers and handlers
	// ************************************************************************************
	public static BagOfGold getInstance() {
		return plugin;
	}

	public static BagOfGoldAPI getAPI() {
		return BagOfGold.getAPI();
	}

	public ConfigManager getConfigManager() {
		return mConfig;
	}

	/**
	 * Get the MessagesManager
	 * 
	 * @return
	 */
	public Messages getMessages() {
		return mMessages;
	}

	/**
	 * setMessages
	 * 
	 * @param messages
	 */
	public void setMessages(Messages messages) {
		mMessages = messages;
	}

	public CommandDispatcher getCommandDispatcher() {
		return mCommandDispatcher;
	}

	/**
	 * Gets the Store Manager
	 * 
	 * @return
	 */
	public IDataStore getStoreManager() {
		return mStore;
	}

	/**
	 * Gets the Database Store Manager
	 * 
	 * @return
	 */
	public DataStoreManager getDataStoreManager() {
		return mStoreManager;
	}

	/**
	 * Get the EconomyManager
	 * 
	 * @return
	 */
	public RewardManager getRewardManager() {
		return mRewardManager;
	}

	public BankManager getBankManager() {
		return mBankManager;
	}

	public CompatibilityManager getCompatibilityManager() {
		return mCompatibilityManager;
	}

	public static UpdateManager getUpdater() {	return mUpdateManager;	}

	public PlayerBalanceManager getPlayerBalanceManager() {
		return mPlayerBalanceManager;
	}

	public GringottsItems getGringottsItems() {
		return mGringottsItems;
	}

	public BagOfGoldItems getBagOfGoldItems() {
		return mBagOfGoldItems;
	}

	public EconomyManager getEconomyManager() {
		return mEconomyManager;
	}

}
