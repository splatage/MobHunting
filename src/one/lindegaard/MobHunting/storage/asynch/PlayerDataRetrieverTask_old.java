package one.lindegaard.MobHunting.storage.asynch;

import java.util.HashSet;

import org.bukkit.OfflinePlayer;

import one.lindegaard.Core.Core;
import one.lindegaard.Core.PlayerSettings;
import one.lindegaard.Core.storage.DataStoreException;
import one.lindegaard.Core.storage.IDataStore;
import one.lindegaard.Core.storage.UserNotFoundException;
import one.lindegaard.MobHunting.MobHunting;

public abstract class PlayerDataRetrieverTask_old implements IDataStoreTask<PlayerSettings> {

	private OfflinePlayer mPlayer;
	private HashSet<Object> mWaiting;

	public PlayerDataRetrieverTask_old(OfflinePlayer player, HashSet<Object> waiting) {
		mPlayer = player;
		mWaiting = waiting;
	}

	public PlayerSettings run(IDataStore store) throws DataStoreException {
		synchronized (mWaiting) {
			try {
				return store.loadPlayerSettings(mPlayer);
			} catch (UserNotFoundException e) {
				MobHunting.getInstance().getMessages().debug("Saving new PlayerSettings for %s to database.", mPlayer.getName());
				PlayerSettings ps = new PlayerSettings(mPlayer,
						"default", Core.getConfigManager().learningMode, false, null, null, 0, 0);
				Core.getDataStoreManager().insertPlayerSettings(ps);
				return ps;
			} catch (DataStoreException e) {
				e.printStackTrace();
				return null;
			}
		}
	}

	@Override
	public boolean readOnly() {
		return true;
	}
}
