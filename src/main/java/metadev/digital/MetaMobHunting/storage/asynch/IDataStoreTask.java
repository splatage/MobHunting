package metadev.digital.MetaMobHunting.storage.asynch;

import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.MetaMobHunting.storage.IDataStore;

public interface IDataStoreTask<T>
{
	public T run(IDataStore store) throws DataStoreException;
	
	public boolean readOnly();
}
