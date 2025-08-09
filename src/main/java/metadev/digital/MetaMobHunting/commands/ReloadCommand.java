package metadev.digital.MetaMobHunting.commands;

import java.util.List;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.Tools;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MythicMobsCompat;


public class ReloadCommand implements ICommand {

	private MobHunting plugin;

	public ReloadCommand(MobHunting plugin) {
		this.plugin = plugin;
	}

	@Override
	public String getName() {
		return "reload";
	}

	@Override
	public String[] getAliases() {
		return null;
	}

	@Override
	public String getPermission() {
		return "mobhunting.reload";
	}

	@Override
	public String[] getUsageString(String label, CommandSender sender) {
		return new String[] { ChatColor.GOLD + label + ChatColor.WHITE + " - to reload MobHunting configuration." };
	}

	@Override
	public String getDescription() {
		return plugin.getMessages().getString("mobhunting.commands.reload.description");
	}

	@Override
	public boolean canBeConsole() {
		return true;
	}

	@Override
	public boolean canBeCommandBlock() {
		return false;
	}

	@Override
	public boolean onCommand(CommandSender sender, String label, String[] args) {

		plugin.getGrindingManager().saveData();

		long starttime = System.currentTimeMillis();
		int i = 1;
		while (plugin.getDataStoreManager().isRunning() && (starttime + 10000 > System.currentTimeMillis())) {
			if (((int) (System.currentTimeMillis() - starttime)) / 1000 == i) {
				MessageHelper.debug("saving data (%s)");
				i++;
			}
		}

		if (Core.getConfigManager().loadConfig() || plugin.getConfigManager().loadConfig()) {
			int n = Tools.getOnlinePlayersAmount();
			if (n > 0) {
				MessageHelper.debug("Reloading %s online playerSettings from the database", n);
				// reload player settings
				for (Player player : Tools.getOnlinePlayers())
					Core.getPlayerSettingsManager().load(player);
				// reload bounties
				if (plugin.getConfigManager().enablePlayerBounties)
					for (Player player : Tools.getOnlinePlayers())
						plugin.getBountyManager().load(player);
				// reload achievements
				for (Player player : Tools.getOnlinePlayers())
					MobHunting.getInstance().getAchievementManager().load(player);
			}

			if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName())))
				MythicMobsCompat.loadMythicMobsData();
			if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())))
				CitizensCompat.loadCitizensData();

			plugin.getMessages().senderSendMessage(sender,
					ChatColor.GREEN + plugin.getMessages().getString("mobhunting.commands.reload.reload-complete"));

		} else
			plugin.getMessages().senderSendMessage(sender,
					ChatColor.RED + plugin.getMessages().getString("mobhunting.commands.reload.reload-error"));

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, String label, String[] args) {
		return null;
	}

}
