package metadev.digital.MetaMobHunting.storage;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.storage.IDataCallback;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.StatType;
import metadev.digital.MetaMobHunting.achievements.Achievement;
import metadev.digital.MetaMobHunting.achievements.ProgressAchievement;
import metadev.digital.MetaMobHunting.bounty.Bounty;
import metadev.digital.MetaMobHunting.bounty.BountyStatus;
import metadev.digital.MetaMobHunting.mobs.ExtendedMob;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;
import metadev.digital.MetaMobHunting.storage.asynch.AchievementRetrieverTask;
import metadev.digital.MetaMobHunting.storage.asynch.AchievementRetrieverTask.Mode;
import metadev.digital.MetaMobHunting.storage.asynch.BountyRetrieverTask;
import metadev.digital.MetaMobHunting.storage.asynch.IDataStoreTask;
import metadev.digital.MetaMobHunting.storage.asynch.StatRetrieverTask;
import metadev.digital.MetaMobHunting.storage.asynch.StoreTask;

public class DataStoreManager {

	private MobHunting plugin;

	// Accessed on multiple threads
	private final LinkedHashSet<Object> mWaiting = new LinkedHashSet<Object>();

	// Accessed only from these threads
	private IDataStore mStore;
	private boolean mExit = false;

	// Accessed only from store thread
	private StoreThread mStoreThread;

	// Accessed only from retrieve thread
	private TaskThread mTaskThread;

	public DataStoreManager(MobHunting plugin, IDataStore store) {
		this.plugin = plugin;
		mStore = store;
		mTaskThread = new TaskThread();
		int savePeriod = Core.getConfigManager().savePeriod;
		if (savePeriod < 1200) {
			savePeriod = 1200;
			Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX_WARNING	+ "save-period in your config.yml is too low. Please raise it to 1200 or higher");
		}
		mStoreThread = new StoreThread(savePeriod);
	}

	public boolean isRunning() {
		return mTaskThread.getState() != Thread.State.WAITING && mTaskThread.getState() != Thread.State.TERMINATED
				&& mStoreThread.getState() != Thread.State.WAITING
				&& mStoreThread.getState() != Thread.State.TERMINATED;
	}

	// **************************************************************************************
	// PlayerStats
	// **************************************************************************************
	public void recordKill(OfflinePlayer player, ExtendedMob mob, boolean bonusMob, double cash) {
		synchronized (mWaiting) {
			mWaiting.add(new StatStore(StatType.fromMobType(mob, true), mob, player, 1, cash));

			if (bonusMob)
				mWaiting.add(new StatStore(
						StatType.fromMobType(
								new ExtendedMob(MobType.BonusMob.ordinal(), MobPlugin.Minecraft, "BonusMob"), true),
						mob, player, 1, cash));
		}
	}

	public void recordAssist(OfflinePlayer player, OfflinePlayer killer, ExtendedMob mob, boolean bonusMob,
			double cash) {
		synchronized (mWaiting) {
			mWaiting.add(new StatStore(StatType.fromMobType(mob, false), mob, player, 1, cash));

			if (bonusMob)
				mWaiting.add(new StatStore(
						StatType.fromMobType(
								new ExtendedMob(MobType.BonusMob.ordinal(), MobPlugin.Minecraft, "BonusMob"), false),
						mob, player, 1, cash));
		}
	}

	public void recordCash(OfflinePlayer player, ExtendedMob mob, boolean bonusMob, double cash) {
		synchronized (mWaiting) {
			mWaiting.add(new StatStore(StatType.fromMobType(mob, true), mob, player, 0, cash));

			if (bonusMob)
				mWaiting.add(new StatStore(
						StatType.fromMobType(
								new ExtendedMob(MobType.BonusMob.ordinal(), MobPlugin.Minecraft, "BonusMob"), true),
						mob, player, 0, cash));
		}
	}

	public void requestStats(StatType type, TimePeriod period, int count, IDataCallback<List<StatStore>> callback) {
		mTaskThread.addTask(new StatRetrieverTask(type, period, count, mWaiting), callback);
	}

	// **************************************************************************************
	// Achievements
	// **************************************************************************************
	public void recordAchievement(OfflinePlayer player, Achievement achievement, ExtendedMob mob) {
		synchronized (mWaiting) {
			mWaiting.add(new AchievementStore(achievement.getID(), player, -1));
			mWaiting.add(new StatStore(StatType.AchievementCount, mob, player));
		}
	}

	public void recordAchievementProgress(OfflinePlayer player, ProgressAchievement achievement, int progress) {
		synchronized (mWaiting) {
			mWaiting.add(new AchievementStore(achievement.getID(), player, progress));
		}
	}

	public void requestAllAchievements(OfflinePlayer player, IDataCallback<Set<AchievementStore>> callback) {
		mTaskThread.addTask(new AchievementRetrieverTask(Mode.All, player, mWaiting), callback);
	}

	public void requestCompletedAchievements(OfflinePlayer player, IDataCallback<Set<AchievementStore>> callback) {
		mTaskThread.addTask(new AchievementRetrieverTask(Mode.Completed, player, mWaiting), callback);
	}

	public void requestInProgressAchievements(OfflinePlayer player, IDataCallback<Set<AchievementStore>> callback) {
		mTaskThread.addTask(new AchievementRetrieverTask(Mode.InProgress, player, mWaiting), callback);
	}

	// *****************************************************************************
	// Bounties
	// *****************************************************************************
	public void updateBounty(Bounty bounty) {
		synchronized (mWaiting) {
			mWaiting.add(new Bounty(plugin, bounty));
		}
	}

	public void requestBounties(BountyStatus mode, OfflinePlayer player, IDataCallback<Set<Bounty>> callback) {
		mTaskThread.addTask(new BountyRetrieverTask(plugin, mode, player, mWaiting), callback);
	}

	// *****************************************************************************
	// Common
	// *****************************************************************************
	/**
	 * Flush all waiting data to the database
	 */
	public void flush() {
		if (mWaiting.size() != 0) {
			plugin.getMessages().debug("Force saving waiting %s data to database...", mWaiting.size());
			mTaskThread.addTask(new StoreTask(mWaiting), null);
		}
	}

	/**
	 * Shutdown the DataStoreManager
	 */
	public void shutdown() {
		mExit = true;
		flush();
		mTaskThread.setWriteOnlyMode(true);
		int n = 0;
		try {
			while (mTaskThread.getState() != Thread.State.WAITING && mTaskThread.getState() != Thread.State.TERMINATED
					&& n < 40) {
				Thread.sleep(500);
				n++;
			}
			if (mTaskThread.getState() == Thread.State.RUNNABLE) {
				mTaskThread.interrupt();
			}
			if (mTaskThread.getState() != Thread.State.WAITING) {
				mTaskThread.waitForEmptyQueue();
			}

		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Wait until all data has been updated
	 */
	public void waitForUpdates() {
		flush();
		try {
			mTaskThread.waitForEmptyQueue();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Constructor for the StoreThread
	 * 
	 * @author Rocologo
	 *
	 */
	private class StoreThread extends Thread {
		private int mSaveInterval;

		public StoreThread(int interval) {
			super("MH StoreThread");
			start();
			mSaveInterval = interval;
		}

		@Override
		public void run() {

			plugin.getMessages().debug("Saving MobHunting data");
			
			MobHunting.getInstance().getGrindingManager().saveData();

			try {
				while (true) {
					synchronized (this) {
						if (mExit && mWaiting.isEmpty()) {
							break;
						}
					}
					mTaskThread.addTask(new StoreTask(mWaiting), null);

					Thread.sleep(mSaveInterval * 50);
				}
			} catch (InterruptedException e) {
				plugin.getMessages().debug("StoreThread was interrupted");
			}
		}
	}

	private class Task {
		public Task(IDataStoreTask<?> task, IDataCallback<?> callback) {
			this.task = task;
			this.callback = callback;
		}

		public IDataStoreTask<?> task;

		public IDataCallback<?> callback;
	}

	private class CallbackCaller implements Runnable {
		private IDataCallback<Object> mCallback;
		private Object mObj;
		private boolean mSuccess;

		public CallbackCaller(IDataCallback<Object> callback, Object obj, boolean success) {
			mCallback = callback;
			mObj = obj;
			mSuccess = success;
		}

		@Override
		public void run() {
			if (mSuccess)
				mCallback.onCompleted(mObj);
			else
				mCallback.onError((Throwable) mObj);
		}

	}

	private class TaskThread extends Thread {
		private BlockingQueue<Task> mQueue;
		private boolean mWritesOnly = false;

		private Object mSignal = new Object();

		public TaskThread() {
			super("MH TaskThread");

			mQueue = new LinkedBlockingQueue<Task>();

			start();
		}

		public void waitForEmptyQueue() throws InterruptedException {
			if (mQueue.isEmpty())
				return;

			synchronized (mSignal) {
				plugin.getMessages().debug(
						"waitForEmptyQueue: Waiting for %s+%s tasks to finish before closing connections.",
						mQueue.size(), mWaiting.size());
				while (!mQueue.isEmpty())
					mSignal.wait();
			}
		}

		public void setWriteOnlyMode(boolean writes) {
			mWritesOnly = writes;
		}

		public <T> void addTask(IDataStoreTask<T> storeTask, IDataCallback<T> callback) {
			try {
				mQueue.put(new Task(storeTask, callback));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		@SuppressWarnings("unchecked")
		@Override
		public void run() {
			try {
				while (true) {
					if (mQueue.isEmpty())
						synchronized (mSignal) {
							mSignal.notifyAll();
						}
					// } else { //DONT ENABLE THIS CAUSES 100 CPU USAGE

					Task task = mQueue.take();

					if (mWritesOnly && task.task.readOnly())
						continue;

					try {

						Object result = task.task.run(mStore);

						if (task.callback != null && !mExit)
							Bukkit.getScheduler().runTask(MobHunting.getInstance(),
									new CallbackCaller((IDataCallback<Object>) task.callback, result, true));

					} catch (DataStoreException e) {
						plugin.getMessages().debug("DataStoreManager: TaskThread.run() failed!!!!!!!");
						if (task.callback != null)
							Bukkit.getScheduler().runTask(MobHunting.getInstance(),
									new CallbackCaller((IDataCallback<Object>) task.callback, e, false));
						else
							e.printStackTrace();
					}
				}

			} catch (InterruptedException e) {
				plugin.getMessages().debug(" TaskThread was interrupted");
			}
		}
	}

}
