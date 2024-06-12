package metadev.digital.MetaMobHunting.storage.asynch;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.bukkit.OfflinePlayer;

import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.bounty.Bounty;
import metadev.digital.MetaMobHunting.bounty.BountyStatus;
import metadev.digital.MetaMobHunting.storage.IDataStore;

public class BountyRetrieverTask implements IDataStoreTask<Set<Bounty>> {
	private MobHunting plugin;
	private BountyStatus mMode;
	private OfflinePlayer mPlayer;
	private HashSet<Object> mWaiting;

	public BountyRetrieverTask(MobHunting plugin, BountyStatus mode, OfflinePlayer player, HashSet<Object> waiting) {
		this.plugin=plugin;
		mMode = mode;
		mPlayer = player;
		mWaiting = waiting;
	}

	private void updateUsingCache(Set<Bounty> bounties) {
		for (Object obj : mWaiting) {
			if (obj instanceof Bounty) {
				Bounty cached = (Bounty) obj;
				if (plugin.getBountyManager().hasOpenBounty(cached)) {
					continue;
				}

				switch (mMode) {
				case completed:
					if (cached.isCompleted())
						bounties.add(cached);
					break;
				case open:
					if (cached.isOpen())
						bounties.add(cached);
					break;
				default: { // all
					bounties.add(cached);
					break;
				}
				}
			}
		}
	}

	public Set<Bounty> run(IDataStore store) throws DataStoreException {
		synchronized (mWaiting) {
			Set<Bounty> bounties = store.loadBounties(mPlayer);
			switch (mMode) {
			case completed: {
				Iterator<Bounty> it = bounties.iterator();
				while (it.hasNext()) {
					Bounty bounty = it.next();
					if (!bounty.isCompleted())
						it.remove();
				}
				break;
			}
			case open: {
				Iterator<Bounty> it = bounties.iterator();
				while (it.hasNext()) {
					Bounty bounty = it.next();
					if (!bounty.isOpen())
						it.remove();
				}
				break;
			}
			default:
				break;
			}
			updateUsingCache(bounties);
			return bounties;
		}
	}

	@Override
	public boolean readOnly() {
		return true;
	}
}
