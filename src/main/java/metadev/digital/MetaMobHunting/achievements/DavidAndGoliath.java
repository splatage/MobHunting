package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.SmartGiantsCompat;
import metadev.digital.MetaMobHunting.events.MobHuntKillEvent;

public class DavidAndGoliath implements Achievement, Listener {

	private MobHunting plugin;

	public DavidAndGoliath(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.davidandgoliath.name");
	}

	@Override
	public String getID() {
		return "davidandgoliath";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.davidandgoliath.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().davidAndGoliat;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (SmartGiantsCompat.isSmartGiants(event.getKilledEntity())
				&& event.getDamageInfo().getWeapon().getType() == Material.STONE_BUTTON
				&& !(plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) == 0
						&& plugin.getRewardManager().getKillCommands(event.getKilledEntity()).isEmpty()))
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().davidAndGoliatCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().davidAndGoliatCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		if (Servers.isMC113OrNewer())
			return new ItemStack(Material.PLAYER_HEAD);
		else
			return new ItemStack(Material.matchMaterial("SKULL_ITEM"), 1, (short) 3);
	}

}
