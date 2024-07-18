package metadev.digital.metabagofgold.storage.asynch;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metabagofgold.PlayerBalances;
import metadev.digital.metabagofgold.storage.IDataStore;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.PlayerSettings;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.storage.UserNotFoundException;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.util.HashSet;

public class PlayerBalanceRetrieverTask implements IDataStoreTask<PlayerBalances> {

	private OfflinePlayer mPlayer;
	private HashSet<Object> mWaiting;

	public PlayerBalanceRetrieverTask(OfflinePlayer player, HashSet<Object> waiting) {
		mPlayer = player;
		mWaiting = waiting;
	}

	public PlayerBalances run(IDataStore store) throws DataStoreException {
		synchronized (mWaiting) {
			PlayerBalances ps = new PlayerBalances();
			try {
				ps = store.loadPlayerBalances(mPlayer);
			} catch (UserNotFoundException e) {
				String worldGroup;
				GameMode gamemode;
				if (mPlayer.isOnline()) {
					Player player = (Player) mPlayer;
					worldGroup = Core.getWorldGroupManager().getCurrentWorldGroup(player);
					gamemode = player.getGameMode();
				} else {
					worldGroup = Core.getWorldGroupManager().getDefaultWorldgroup();
					gamemode = Core.getWorldGroupManager().getDefaultGameMode();
				}
				if (!ps.has(worldGroup, gamemode)) {
					BagOfGold.getInstance().getMessages().debug("PlayerBalanceRetriver - %s%s does not exist -creating",
							worldGroup, gamemode);
					PlayerBalance pb = new PlayerBalance(mPlayer, worldGroup, gamemode);
					ps.putPlayerBalance(pb);
					BagOfGold.getInstance().getPlayerBalanceManager().setPlayerBalance(mPlayer, pb);
					BagOfGold.getInstance().getDataStoreManager().updatePlayerBalance(mPlayer, pb);
				}
				if (mPlayer.isOnline()) {
					PlayerSettings playersettings = Core.getPlayerSettingsManager()
							.getPlayerSettings(mPlayer);
					if (!playersettings.getLastKnownWorldGrp().equals(worldGroup)) {
						playersettings.setLastKnownWorldGrp(worldGroup);
						Core.getDataStoreManager().insertPlayerSettings(playersettings);
					}
				}
			}
			return ps;
		}
	}

	@Override
	public boolean readOnly() {
		return true;
	}
}
