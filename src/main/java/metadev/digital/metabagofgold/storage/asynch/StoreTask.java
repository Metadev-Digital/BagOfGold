package metadev.digital.metabagofgold.storage.asynch;

import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metabagofgold.storage.IDataStore;
import metadev.digital.metacustomitemslib.storage.DataStoreException;

import java.util.LinkedHashSet;
import java.util.Set;

public class StoreTask implements IDataStoreTask<Void> {
	private LinkedHashSet<PlayerBalance> mWaitingPlayerBalances = new LinkedHashSet<PlayerBalance>();

	public StoreTask(Set<Object> waiting) {
		synchronized (waiting) {
			mWaitingPlayerBalances.clear();

			for (Object obj : waiting) {
				if (obj instanceof PlayerBalance)
					mWaitingPlayerBalances.add((PlayerBalance) obj);
			}

			waiting.clear();
		}
	}

	@Override
	public Void run(IDataStore store) throws DataStoreException {
		if (!mWaitingPlayerBalances.isEmpty())
			store.savePlayerBalances(mWaitingPlayerBalances, true);
		
		BagOfGold.getInstance().getMessages().debug("Saving BagOfGold data");
		
		BagOfGold.getInstance().getBankManager().save();
		
		return null;
	}

	@Override
	public boolean readOnly() {
		return false;
	}
}
