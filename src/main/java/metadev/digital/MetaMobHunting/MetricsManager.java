package metadev.digital.MetaMobHunting;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;

import metadev.digital.metacustomitemslib.HttpTools;
import metadev.digital.metacustomitemslib.HttpTools.httpCallback;
import metadev.digital.MetaMobHunting.compatibility.addons.CMIHelper;
import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.ResidenceCompat;
import metadev.digital.MetaMobHunting.compatibility.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.WorldEditCompat;
import metadev.digital.MetaMobHunting.compatibility.WorldGuardCompat;

public class MetricsManager {

	private MobHunting plugin;
	private boolean started = false;

	private Metrics bStatsMetrics;

	public MetricsManager(MobHunting plugin) {
		this.plugin = plugin;
	}

	public void start() {
		Bukkit.getScheduler().runTaskTimer(plugin, new Runnable() {
			public void run() {
				try {
					// make a URL to MCStats.org
					URL url = new URL("https://bstats.org/");
					if (!started) {
						MessageHelper.debug("check if home page can be reached");
						HttpTools.isHomePageReachable(url, new httpCallback() {
						
						@Override
						public void onSuccess() {
							startBStatsMetrics();
							MessageHelper.debug("Metrics reporting to Https://bstats.org has started.");
							started = true;
						}
						
						@Override
						public void onError() {
							started=false;
							MessageHelper.debug("https://bstats.org/ seems to be down");
						}
					});
					}
				} catch (IOException e) {
					e.printStackTrace();
				}

			}
		}, 100L, 72000L);
	}

	public void startBStatsMetrics() {
		// https://bstats.org/what-is-my-plugin-id
		bStatsMetrics = new Metrics(plugin,22712);

		bStatsMetrics.addCustomChart(
				new SimplePie("database_used_for_mobhunting", () -> plugin.getConfigManager().databaseType));
		bStatsMetrics.addCustomChart(new SimplePie("language", () -> plugin.getConfigManager().language));	// McMMO 2.1.0 documentation:
		// https://docs.google.com/document/d/1qY6hEyGCO5z1PRup_OvMBxAmumydxxoO_H-pnUrVK8M/edit#heading=h.474ghxburdpp

		bStatsMetrics.addCustomChart(
				new SimplePie("economy_plugin", () -> plugin.getEconomyManager().getName()));

		bStatsMetrics.addCustomChart(
				new AdvancedPie("protection_plugin_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("WorldGuard", Integer.valueOf(WorldGuardCompat.isSupported() ? 1 : 0));
						valueMap.put("Residence", Integer.valueOf(MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Residence.getName())) ? 1 : 0));
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new AdvancedPie("minigame_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("PVPArena", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.PVPArena.getName()))? 1 : 0);
						valueMap.put("BattleArena", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BattleArena.getName())) ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics.addCustomChart(
				new AdvancedPie("disguise_plugin_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();

						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("me.libraryaddict.disguise.disguisetypes.DisguiseType");
							valueMap.put("LibsDisguises", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LibsDisguises.getName()))  ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						valueMap.put("Essentials", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Essentials.getName())) ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new AdvancedPie("other_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("Citizens", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())) ? 1 : 0);
						valueMap.put("Gringotts", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Gringotts.getName())) ? 1 : 0);
						valueMap.put("MyPet", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MyPet.getName())) ? 1 : 0);
						valueMap.put("McMMO", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.mcMMO.getName())) ? 1 : 0);
						valueMap.put("WorldEdit", WorldEditCompat.isSupported() ? 1 : 0);
						valueMap.put("ExtraHardMode", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BagOfGold.getName())) ? 1 : 0);
						valueMap.put("CrackShot", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CrackShot.getName())) ? 1 : 0);
						valueMap.put("CMI", CMIHelper.isCMILoaded() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics.addCustomChart(new AdvancedPie("special_mobs", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("MythicMobs", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName())) ? 1 : 0);
				valueMap.put("Levelled Mobs", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LevelledMobs.getName())) ? 1 : 0);
				valueMap.put("StackMob", StackMobCompat.isSupported() ? 1 : 0);
				valueMap.put("EliteMobs", MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName())) ? 1 : 0);
				return valueMap;
			}

		}));

		bStatsMetrics.addCustomChart(new AdvancedPie("titlemanagers", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("CMIHolograms", CMIHelper.isCMILoaded() ? 1 : 0);
				return valueMap;
			}
		}));

		bStatsMetrics.addCustomChart(new AdvancedPie("mobhunting_usage", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("Leaderboards", plugin.getLeaderboardManager().getWorldLeaderBoards().size());
				valueMap.put("Holographic Leaderboards",
						plugin.getLeaderboardManager().getHologramManager().getHolograms().size());
				if (MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName())))
					valueMap.put("MasterMobHunters", CitizensCompat.getMasterMobHunterManager().getAll().size());
				valueMap.put("PlayerBounties",
						plugin.getConfigManager().enablePlayerBounties
								? plugin.getBountyManager().getAllBounties().size()
								: 0);
				return valueMap;
			}
		}));

		bStatsMetrics.addCustomChart(new DrilldownPie("economy_api", () -> {
	        Map<String, Map<String, Integer>> map = new HashMap<>();
	        String economyAPI = plugin.getEconomyManager().getVersion();
	        Map<String, Integer> entry = new HashMap<>();
	        entry.put(economyAPI, 1);
	        if (plugin.getEconomyManager().getVersion().endsWith("Vault")) {
	            map.put("Vault", entry);
	        } else if (plugin.getEconomyManager().getVersion().endsWith("Reserve")) {
	            map.put("Reserve", entry);
	        } else {
	            map.put("None", entry);
	        } 
	        return map;
	    }));

	}

}
