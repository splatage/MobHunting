package metadev.digital.MetaMobHunting.storage.asynch;

import java.util.LinkedHashSet;
import java.util.Set;

import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.MetaMobHunting.bounty.Bounty;
import metadev.digital.MetaMobHunting.storage.AchievementStore;
import metadev.digital.MetaMobHunting.storage.IDataStore;
import metadev.digital.MetaMobHunting.storage.StatStore;

public class StoreTask implements IDataStoreTask<Void> {
	private LinkedHashSet<StatStore> mWaitingPlayerStats = new LinkedHashSet<StatStore>();
	private LinkedHashSet<AchievementStore> mWaitingAchievements = new LinkedHashSet<AchievementStore>();
	private LinkedHashSet<Bounty> mWaitingBounties = new LinkedHashSet<Bounty>();

	public StoreTask(Set<Object> waiting) {
		synchronized (waiting) {
			mWaitingPlayerStats.clear();
			mWaitingAchievements.clear();
			mWaitingBounties.clear();

			for (Object obj : waiting) {
				if (obj instanceof AchievementStore)
					mWaitingAchievements.add((AchievementStore) obj);
				else if (obj instanceof StatStore)
					mWaitingPlayerStats.add((StatStore) obj);
				else if (obj instanceof Bounty)
					mWaitingBounties.add((Bounty) obj);
			}

			waiting.clear();
		}
	}

	@Override
	public Void run(IDataStore store) throws DataStoreException {
		if (!mWaitingPlayerStats.isEmpty())
			store.savePlayerStats(mWaitingPlayerStats);

		if (!mWaitingAchievements.isEmpty())
			store.saveAchievements(mWaitingAchievements);

		if (!mWaitingBounties.isEmpty())
			store.saveBounties(mWaitingBounties);

		return null;
	}

	@Override
	public boolean readOnly() {
		return false;
	}

}
