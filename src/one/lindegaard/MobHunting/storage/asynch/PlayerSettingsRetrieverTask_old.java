package one.lindegaard.MobHunting.storage.asynch;

import java.sql.SQLException;
import java.util.HashSet;

import org.bukkit.OfflinePlayer;

import one.lindegaard.Core.Core;
import one.lindegaard.Core.PlayerSettings;
import one.lindegaard.Core.storage.async.IDataStoreTask;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.storage.IDataStore;

public abstract class PlayerSettingsRetrieverTask_old implements IDataStoreTask<PlayerSettings> {

	private OfflinePlayer mPlayer;
	private HashSet<Object> mWaiting;

	public PlayerSettingsRetrieverTask_old(OfflinePlayer player, HashSet<Object> waiting) {
		mPlayer = player;
		mWaiting = waiting;
	}

	/**public PlayerSettings run(IDataStore store) throws DataStoreException {
		synchronized (mWaiting) {
			try {
				return null;//store.loadPlayerSettings(mPlayer);
			} catch (UserNotFoundException e) {
				MobHunting.getInstance().getMessages().debug("Saving new PlayerSettings for %s to database.", mPlayer.getName());
				PlayerSettings ps = new PlayerSettings(mPlayer,
						null, Core.getConfigManager().learningMode, false, null, null, 0, 0);
				try {
					store.insertPlayerSettings(ps);
				} catch (DataStoreException e1) {
					e1.printStackTrace();
				}
				return ps;
			} catch (DataStoreException e) {
				e.printStackTrace();
				return null;
			} catch (SQLException e) {
				e.printStackTrace();
				return null;
			}
		}
	}**/

	@Override
	public boolean readOnly() {
		return true;
	}
}
