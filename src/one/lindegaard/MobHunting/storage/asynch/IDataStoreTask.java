package one.lindegaard.MobHunting.storage.asynch;

import one.lindegaard.CustomItemsLib.storage.DataStoreException;
import one.lindegaard.MobHunting.storage.IDataStore;

public interface IDataStoreTask<T>
{
	public T run(IDataStore store) throws DataStoreException;
	
	public boolean readOnly();
}
