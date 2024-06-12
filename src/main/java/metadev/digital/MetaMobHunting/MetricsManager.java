package metadev.digital.MetaMobHunting;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.bstats.bukkit.Metrics;
import org.bstats.charts.AdvancedPie;
import org.bstats.charts.DrilldownPie;
import org.bstats.charts.SimplePie;
import org.bukkit.Bukkit;

import metadev.digital.metacustomitemslib.HttpTools;
import metadev.digital.metacustomitemslib.HttpTools.httpCallback;
import metadev.digital.MetaMobHunting.compatibility.ActionAnnouncerCompat;
import metadev.digital.MetaMobHunting.compatibility.ActionBarAPICompat;
import metadev.digital.MetaMobHunting.compatibility.ActionbarCompat;
import metadev.digital.MetaMobHunting.compatibility.BarAPICompat;
import metadev.digital.MetaMobHunting.compatibility.BattleArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.BossBarAPICompat;
import metadev.digital.MetaMobHunting.compatibility.BossCompat;
import metadev.digital.MetaMobHunting.compatibility.CMICompat;
import metadev.digital.MetaMobHunting.compatibility.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.ConquestiaMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.CrackShotCompat;
import metadev.digital.MetaMobHunting.compatibility.CustomMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.DisguiseCraftCompat;
import metadev.digital.MetaMobHunting.compatibility.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.EssentialsCompat;
import metadev.digital.MetaMobHunting.compatibility.ExtraHardModeCompat;
import metadev.digital.MetaMobHunting.compatibility.FactionsHelperCompat;
import metadev.digital.MetaMobHunting.compatibility.FactionsHelperCompat.FactionsVersion;
import metadev.digital.MetaMobHunting.compatibility.GringottsCompat;
import metadev.digital.MetaMobHunting.compatibility.HerobrineCompat;
import metadev.digital.MetaMobHunting.compatibility.HologramsCompat;
import metadev.digital.MetaMobHunting.compatibility.HolographicDisplaysCompat;
import metadev.digital.MetaMobHunting.compatibility.IDisguiseCompat;
import metadev.digital.MetaMobHunting.compatibility.InfernalMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.LevelledMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.LibsDisguisesCompat;
import metadev.digital.MetaMobHunting.compatibility.LorinthsRpgMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.McMMOCompat;
import metadev.digital.MetaMobHunting.compatibility.McMMOCompat.McMMO_Version;
import metadev.digital.MetaMobHunting.compatibility.McMMOHorses;
import metadev.digital.MetaMobHunting.compatibility.MinigamesCompat;
import metadev.digital.MetaMobHunting.compatibility.MinigamesLibCompat;
import metadev.digital.MetaMobHunting.compatibility.MobArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.MobStackerCompat;
import metadev.digital.MetaMobHunting.compatibility.MyPetCompat;
import metadev.digital.MetaMobHunting.compatibility.MysteriousHalloweenCompat;
import metadev.digital.MetaMobHunting.compatibility.MythicMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.PVPArenaCompat;
import metadev.digital.MetaMobHunting.compatibility.PreciousStonesCompat;
import metadev.digital.MetaMobHunting.compatibility.ProtocolLibCompat;
import metadev.digital.MetaMobHunting.compatibility.ResidenceCompat;
import metadev.digital.MetaMobHunting.compatibility.SmartGiantsCompat;
import metadev.digital.MetaMobHunting.compatibility.StackMobCompat;
import metadev.digital.MetaMobHunting.compatibility.TARDISWeepingAngelsCompat;
import metadev.digital.MetaMobHunting.compatibility.TitleAPICompat;
import metadev.digital.MetaMobHunting.compatibility.TitleManagerCompat;
import metadev.digital.MetaMobHunting.compatibility.TownyCompat;
import metadev.digital.MetaMobHunting.compatibility.VanishNoPacketCompat;
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
						plugin.getMessages().debug("check if home page can be reached");
						HttpTools.isHomePageReachable(url, new httpCallback() {
						
						@Override
						public void onSuccess() {
							startBStatsMetrics();
							plugin.getMessages().debug("Metrics reporting to Https://bstats.org has started.");
							started = true;
						}
						
						@Override
						public void onError() {
							started=false;
							plugin.getMessages().debug("https://bstats.org/ seems to be down");
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
		bStatsMetrics = new Metrics(plugin,173);
		
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
						valueMap.put("Factions", Integer
								.valueOf(FactionsHelperCompat.factionsVersion == FactionsVersion.FACTIONS ? 1 : 0));
						valueMap.put("FactionsUUID", Integer.valueOf(
								FactionsHelperCompat.factionsVersion == FactionsVersion.FACTIONS_UUID ? 1 : 0));
						valueMap.put("Towny", Integer.valueOf(TownyCompat.isSupported() ? 1 : 0));
						valueMap.put("Residence", Integer.valueOf(ResidenceCompat.isSupported() ? 1 : 0));
						valueMap.put("PreciousStones", Integer.valueOf(PreciousStonesCompat.isSupported() ? 1 : 0));
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new AdvancedPie("minigame_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("MobArena", MobArenaCompat.isSupported() ? 1 : 0);
						valueMap.put("Minigames", MinigamesCompat.isSupported() ? 1 : 0);
						valueMap.put("MinigamesLib", MinigamesLibCompat.isSupported() ? 1 : 0);
						valueMap.put("PVPArena", PVPArenaCompat.isSupported() ? 1 : 0);
						valueMap.put("BattleArena", BattleArenaCompat.isSupported() ? 1 : 0);
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
							Class cls = Class.forName("pgDev.bukkit.DisguiseCraft.disguise.DisguiseType");
							valueMap.put("DisguiseCraft", DisguiseCraftCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("de.robingrether.idisguise.disguise.DisguiseType");
							valueMap.put("iDisguise", IDisguiseCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						try {
							@SuppressWarnings({ "rawtypes", "unused" })
							Class cls = Class.forName("me.libraryaddict.disguise.disguisetypes.DisguiseType");
							valueMap.put("LibsDisguises", LibsDisguisesCompat.isSupported() ? 1 : 0);
						} catch (ClassNotFoundException e) {
						}
						valueMap.put("VanishNoPacket", VanishNoPacketCompat.isSupported() ? 1 : 0);
						valueMap.put("Essentials", EssentialsCompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics
				.addCustomChart(new AdvancedPie("other_integrations", new Callable<Map<String, Integer>>() {
					@Override
					public Map<String, Integer> call() throws Exception {
						Map<String, Integer> valueMap = new HashMap<>();
						valueMap.put("Citizens", CitizensCompat.isSupported() ? 1 : 0);
						valueMap.put("Gringotts", GringottsCompat.isSupported() ? 1 : 0);
						valueMap.put("MyPet", MyPetCompat.isSupported() ? 1 : 0);
						valueMap.put("McMMOHorses", McMMOHorses.isSupported() ? 1 : 0);
						valueMap.put("McMMO", McMMOCompat.getMcMMOVersion()==McMMO_Version.McMMO ? 1 : 0);
						valueMap.put("McMMO Classic", McMMOCompat.getMcMMOVersion()==McMMO_Version.McMMO_CLASSIC ? 1 : 0);
						valueMap.put("WorldEdit", WorldEditCompat.isSupported() ? 1 : 0);
						valueMap.put("ProtocolLib", ProtocolLibCompat.isSupported() ? 1 : 0);
						valueMap.put("ExtraHardMode", ExtraHardModeCompat.isSupported() ? 1 : 0);
						valueMap.put("CrackShot", CrackShotCompat.isSupported() ? 1 : 0);
						valueMap.put("CMI", CMICompat.isSupported() ? 1 : 0);
						return valueMap;
					}

				}));

		bStatsMetrics.addCustomChart(new AdvancedPie("special_mobs", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("MythicMobs", MythicMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("TARDISWeepingAngels", TARDISWeepingAngelsCompat.isSupported() ? 1 : 0);
				valueMap.put("MobStacker", MobStackerCompat.isSupported() ? 1 : 0);
				valueMap.put("CustomMobs", CustomMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("ConquestiaMobs", ConquestiaMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("LorinthsRpgMobs", LorinthsRpgMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("Levelled Mobs", LevelledMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("StackMob", StackMobCompat.isSupported() ? 1 : 0);
				valueMap.put("MysteriousHalloween", MysteriousHalloweenCompat.isSupported() ? 1 : 0);
				valueMap.put("SmartGiants", SmartGiantsCompat.isSupported() ? 1 : 0);
				valueMap.put("InfernalMobs", InfernalMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("Herobrine", HerobrineCompat.isSupported() ? 1 : 0);
				valueMap.put("EliteMobs", EliteMobsCompat.isSupported() ? 1 : 0);
				valueMap.put("Boss mobs", BossCompat.isSupported() ? 1 : 0);
				return valueMap;
			}

		}));

		bStatsMetrics.addCustomChart(new AdvancedPie("titlemanagers", new Callable<Map<String, Integer>>() {
			@Override
			public Map<String, Integer> call() throws Exception {
				Map<String, Integer> valueMap = new HashMap<>();
				valueMap.put("BossBarAPI", BossBarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("TitleAPI", TitleAPICompat.isSupported() ? 1 : 0);
				valueMap.put("BarAPI", BarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("TitleManager", TitleManagerCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBar", ActionbarCompat.isSupported() ? 1 : 0);
				valueMap.put("ActionBarAPI", ActionBarAPICompat.isSupported() ? 1 : 0);
				valueMap.put("ActionAnnouncer", ActionAnnouncerCompat.isSupported() ? 1 : 0);
				valueMap.put("Holograms", HologramsCompat.isSupported() ? 1 : 0);
				valueMap.put("Holographic Display", HolographicDisplaysCompat.isSupported() ? 1 : 0);
				valueMap.put("CMIHolograms", CMICompat.isSupported() ? 1 : 0);
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
				if (CitizensCompat.isSupported())
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
