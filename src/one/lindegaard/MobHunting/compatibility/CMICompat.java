package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.Zrips.CMI.CMI;
import com.Zrips.CMI.Modules.Holograms.HologramManager;

import net.Zrips.CMILib.CMILib;
import net.Zrips.CMILib.ActionBar.CMIActionBar;
import net.Zrips.CMILib.BossBar.BossBarInfo;
import one.lindegaard.CustomItemsLib.compatibility.CompatPlugin;
import one.lindegaard.MobHunting.MobHunting;

public class CMICompat {

	private static Plugin mPlugin, mPlugin2;
	private static boolean supported = false;

	// https://www.spigotmc.org/resources/cmi-ranks-kits-portals-essentials-mysql-sqlite-bungeecord.3742/
	// https://www.spigotmc.org/resources/cmilib.87610/

	public CMICompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with CMI is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.CMI.getName());

			if (mPlugin.getDescription().getVersion().compareTo("9.0") >= 0) {
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
						+ "Enabling compatibility with CMI (" + mPlugin.getDescription().getVersion() + ").");

				mPlugin2 = Bukkit.getPluginManager().getPlugin(CompatPlugin.CMILib.getName());
				if (mPlugin2.getDescription().getVersion().compareTo("1.0") >= 0) {
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
							+ "Enabling compatibility with CMILib (" + mPlugin2.getDescription().getVersion() + ").");

					Bukkit.getScheduler().runTaskLater(MobHunting.getInstance(), new Runnable() {

						@Override
						public void run() {
							int n = 0;
							while (n < 30) {
								if (CMI.getInstance() != null && CMI.getInstance().isFullyLoaded())
									break;
								n++;
								try {
									Bukkit.getConsoleSender().sendMessage(
											ChatColor.GOLD + "[MobHunting] waiting for CMI to be fully loaded.");
									Thread.sleep(3000L);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							if (n == 30)
								Bukkit.getConsoleSender()
										.sendMessage(ChatColor.GOLD + "[MobHunting] waiting for CMI timed out.");

							if (CMI.getInstance() != null && CMI.getInstance().isFullyLoaded()) {
								supported = true;
								MobHunting.getInstance().getLeaderboardManager().getHologramManager()
										.loadHologramLeaderboards();
								MobHunting.getInstance().getLeaderboardManager().getHologramManager()
										.saveHologramLeaderboards();
							}

						}
					}, 600L);
					//supported = true;

				} else
					Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
							+ "Your current version of CMILib (" + mPlugin2.getDescription().getVersion()
							+ ") is not supported by MobHunting. Mobhunting does only support version 1.0 or newer.");

			} else
				Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RED
						+ "Your current version of CMI (" + mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Mobhunting does only support version 9.0 or newer.");

		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public static CMI getCMIPlugin() {
		return (CMI) mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationCMI;
	}

	public static HologramManager getHologramManager() {
		return getCMIPlugin().getHologramManager();
	}

	public static void sendActionBarMessage(Player player, String text) {
		CMIActionBar.send(player, text);
	}

	public static void sendBossBarMessage(Player player, String text) {
		BossBarInfo bossBar = new BossBarInfo(player, "...");
		bossBar.setSeconds(10);
		bossBar.setTitleOfBar(text);
		bossBar.setKeepForTicks(0);
		CMILib.getInstance().getBossBarManager().addBossBar(player, bossBar);
	}

}
