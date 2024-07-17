package metadev.digital.metabagofgold.storage.asynch;

import one.lindegaard.BagOfGold.PlayerBalance;
import one.lindegaard.BagOfGold.storage.IDataStore;
import one.lindegaard.CustomItemsLib.storage.DataStoreException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Top54BalanceRetrieverTask implements IDataStoreTask<List<PlayerBalance>> {
	private int mCount;
	private String mWorldGroup;
	private int mGamemode;
	private HashSet<Object> mWaiting;

	public Top54BalanceRetrieverTask(int count, String worldGroup, int mGamemode, HashSet<Object> waiting) {
		mCount = count;
		mWorldGroup = worldGroup;
		mWaiting = waiting;
	}

	private void updateUsingCache(List<PlayerBalance> stats) {
		for (Object obj : mWaiting) {
			if (obj instanceof PlayerBalance) {
				PlayerBalance cached = (PlayerBalance) obj;

				Iterator<PlayerBalance> it = stats.iterator();
				boolean found = false;

				while (it.hasNext()) {
					PlayerBalance stat = it.next();
					if (cached.getPlayer().getUniqueId().equals(stat.getPlayer().getUniqueId())
							&& cached.getWorldGroup().equals(stat.getWorldGroup())
							&& cached.getGamemode() == stat.getGamemode()) {
						stat.setBalance(cached.getBalance());
						stat.setBalanceChanges(cached.getBalanceChanges());
						stat.setBankBalance(cached.getBankBalance());
						stat.setBankBalanceChanges(cached.getBankBalanceChanges());
						found = true;
					}
				}

				if (!found && cached.getPlayer().getUniqueId().equals(cached.getPlayer().getUniqueId())
						&& cached.getWorldGroup().equals(cached.getWorldGroup())
						&& cached.getGamemode() == cached.getGamemode())
					stats.add(cached);
			}
		}
	}

	@Override
	public List<PlayerBalance> run(IDataStore store) throws DataStoreException {
		synchronized (mWaiting) {
			List<PlayerBalance> stats = store.loadTop54(mCount, mWorldGroup, mGamemode);
			updateUsingCache(stats);
			return stats;
		}
	}

	@Override
	public boolean readOnly() {
		return true;
	}

}
