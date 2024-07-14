package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.inventory.ItemStack;

import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.ExtendedMob;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;

public class SixthHuntAchievement implements ProgressAchievement {

	private MobHunting plugin;
	private ExtendedMob mExtendedMob;

	public SixthHuntAchievement(MobHunting plugin, ExtendedMob extendedMob) {
		this.plugin = plugin;
		mExtendedMob = extendedMob;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.hunter.6.name", "mob", mExtendedMob.getLocalizedName());
	}

	@Override
	public String getID() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level6-" + mExtendedMob.getMobName().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level6-"
					+ mExtendedMob.getMobtype().toLowerCase();

	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.hunter.6.description", "count", getNextLevel(), "mob",
				mExtendedMob.getLocalizedName());
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialHunter6;
	}

	@Override
	public int getNextLevel() {
		return mExtendedMob.getProgressAchievementLevel1() * 50;
	}

	@Override
	public String inheritFrom() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level5-" + mExtendedMob.getMobtype().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level5-"
					+ mExtendedMob.getMobtype().toLowerCase();
	}

	@Override
	public String nextLevelId() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level7-" + mExtendedMob.getMobtype().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level7-"
					+ mExtendedMob.getMobtype().toLowerCase();
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialHunter6Cmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialHunter6CmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return mExtendedMob.getInventoryAchivementItem(mExtendedMob.getMobName(), 6, 0);
	}

	@Override
	public ExtendedMob getExtendedMob() {
		return mExtendedMob;
	}
}
