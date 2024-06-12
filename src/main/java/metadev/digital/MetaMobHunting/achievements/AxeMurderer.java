package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import metadev.digital.metacustomitemslib.materials.Materials;
import metadev.digital.metacustomitemslib.server.Servers;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.events.MobHuntKillEvent;

public class AxeMurderer implements Achievement, Listener {

	private MobHunting plugin;

	public AxeMurderer(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return plugin.getMessages().getString("achievements.axemurderer.name");
	}

	@Override
	public String getID() {
		return "axemurderer";
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("achievements.axemurderer.description");
	}

	@Override
	public double getPrize() {
		return plugin.getConfigManager().specialAxeMurderer;
	}

	@EventHandler
	public void onKill(MobHuntKillEvent event) {
		if (Materials.isAxe(event.getDamageInfo().getWeapon())
				&& plugin.getRewardManager().getBaseKillPrize(event.getKilledEntity()) > 0)
			plugin.getAchievementManager().awardAchievement(this, event.getPlayer(),
					plugin.getExtendedMobManager().getExtendedMobFromEntity(event.getKilledEntity()));
	}

	@Override
	public String getPrizeCmd() {
		return plugin.getConfigManager().specialAxeMurdererCmd;
	}

	@Override
	public String getPrizeCmdDescription() {
		return plugin.getConfigManager().specialAxeMurdererCmdDesc;
	}

	@Override
	public ItemStack getSymbol() {
		if (Servers.isMC113OrNewer())
			return new ItemStack(Material.WOODEN_AXE);
		else
			return new ItemStack(Material.matchMaterial("WOOD_AXE"));
	}

}
