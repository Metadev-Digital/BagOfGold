package metadev.digital.metabagofgold.storage.asynch;

import metadev.digital.metabagofgold.storage.IDataStore;
import metadev.digital.metacustomitemslib.storage.DataStoreException;

public interface IDataStoreTask<T>
{
	public T run(IDataStore store) throws DataStoreException;
	
	public boolean readOnly();
}
