package metadev.digital.metabagofgold.storage.asynch;

import one.lindegaard.BagOfGold.storage.IDataStore;
import one.lindegaard.CustomItemsLib.storage.DataStoreException;

public interface IDataStoreTask<T>
{
	public T run(IDataStore store) throws DataStoreException;
	
	public boolean readOnly();
}
