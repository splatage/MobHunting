package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.inventory.ItemStack;

import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.mobs.ExtendedMob;
import metadev.digital.MetaMobHunting.mobs.MobPlugin;

public class SecondHuntAchievement implements ProgressAchievement {
	private MobHunting plugin;
	private ExtendedMob mExtendedMob;

	public SecondHuntAchievement(MobHunting plugin, ExtendedMob extendedMob) {
		this.plugin=plugin;
		mExtendedMob = extendedMob;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.hunter.2.name", "mob", mExtendedMob.getLocalizedName());
	}

	@Override
	public String getID() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level2-" + mExtendedMob.getMobName().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level2-" + mExtendedMob.getMobtype().toLowerCase();

	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.hunter.2.description", "count", getNextLevel(), "mob",
				mExtendedMob.getLocalizedName());
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialHunter2;
	}

	@Override
	public int getNextLevel() {
		return (int) Math.round(mExtendedMob.getProgressAchievementLevel1() * 2.5);
	}

	@Override
	public String inheritFrom() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level1-" + mExtendedMob.getMobtype().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level1-" + mExtendedMob.getMobtype().toLowerCase();
	}

	@Override
	public String nextLevelId() {
		if (mExtendedMob.getMobPlugin() == MobPlugin.Minecraft)
			return "hunting-level3-" + mExtendedMob.getMobtype().toLowerCase();
		else
			return mExtendedMob.getMobPlugin().name().toLowerCase() + "-hunting-level3-" + mExtendedMob.getMobtype().toLowerCase();
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialHunter2Cmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialHunter2CmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		return mExtendedMob.getInventoryAchivementItem(mExtendedMob.getMobName(), 2, 0);
	}

	@Override
	public ExtendedMob getExtendedMob() {
		return mExtendedMob;
	}
}
