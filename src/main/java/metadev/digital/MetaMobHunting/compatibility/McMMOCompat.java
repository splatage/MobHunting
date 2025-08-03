package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerFishingEvent;
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerFishingTreasureEvent;
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerMagicHunterEvent;
import com.gmail.nossr50.events.skills.fishing.McMMOPlayerShakeEvent;

import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.DamageInformation;
import metadev.digital.MetaMobHunting.MobHunting;

public class McMMOCompat implements Listener {

	private static boolean supported = false;
	private static Plugin mPlugin;
	public static final String MH_MCMMO = "MH:MCMMO";
	private final String latestSupported = "2.0";

	public enum McMMO_Version {
		NOT_DETECTED, McMMO, McMMO_CLASSIC
	};

	public static McMMO_Version mMcMMOVersion = McMMO_Version.NOT_DETECTED;

	public McMMOCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(
					MobHunting.PREFIX_WARNING + "Compatibility with McMMO / McMMO Classic is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.mcMMO.getName());
			if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with McMMO ("
						+ getMcMmoAPI().getDescription().getVersion() + ")");
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX + "McMMO Level rewards is "
								+ (MobHunting.getInstance().getConfigManager().enableMcMMOLevelRewards ? "enabled"
										: "disabled"));
				mMcMMOVersion = McMMO_Version.McMMO;
				supported = true;
			} else if (mPlugin.getDescription().getVersion().compareTo(latestSupported) >= 0) {
				Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
				Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling compatibility with McMMO Classic ("
						+ getMcMmoAPI().getDescription().getVersion() + ")");
				Bukkit.getConsoleSender()
						.sendMessage(MobHunting.PREFIX + "McMMO Classic Level rewards is "
								+ (MobHunting.getInstance().getConfigManager().enableMcMMOLevelRewards ? "enabled"
										: "disabled"));
				mMcMMOVersion = McMMO_Version.McMMO_CLASSIC;
				supported = true;
			} else {
				ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
				console.sendMessage(MobHunting.PREFIX_WARNING + "Your current version of McMMO ("
						+ mPlugin.getDescription().getVersion()
						+ ") is not supported by MobHunting. Please upgrade to " + latestSupported + " or newer.");
			}
		}

	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************
	public static Plugin getMcMmoAPI() {
		return mPlugin;
	}

	public static boolean isSupported() {
		return supported;
	}

	public static McMMO_Version getMcMMOVersion() {
		return mMcMMOVersion;
	}

	public static boolean isMcMMO(Entity entity) {
		if (isSupported())
			return entity.hasMetadata(MH_MCMMO);
		return false;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationMcMMO;
	}

	//TODO: Remove calls to MCMMOClassic
	public static String getSkilltypeName(DamageInformation info) {
		switch (mMcMMOVersion) {
		case McMMO:
			return McMMOCompatHelper.getSKillTypeName(info);

		default:
			return "";
		}
	}

	public static void addXP_NOT_USED(Player player, String skillType, int XP, String xpGainReason) {
		ExperienceAPI.addXP(player, skillType, XP, xpGainReason);
	}

	public static void addLevel(Player player, String skillType, int levels) {
		ExperienceAPI.addLevel(player, skillType, levels);
	}

	// **************************************************************************
	// EVENTS
	// **************************************************************************

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void Fish2(McMMOPlayerFishingTreasureEvent event) {
		Player p = event.getPlayer();
		ItemStack s = event.getTreasure();
		MobHunting.getInstance().getMessages().debug("McMMO-FishingEvent1: %s caught a %s", p.getName(), s.getType());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void Fish3(McMMOPlayerFishingEvent event) {
		Player p = event.getPlayer();
		MobHunting.getInstance().getMessages().debug("McMMO-FishingEvent2: %s is fishing", p.getName());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void Fish4(McMMOPlayerMagicHunterEvent event) {
		Player p = event.getPlayer();
		ItemStack is = event.getTreasure();
		MobHunting.getInstance().getMessages().debug("McMMO-FishingEvent3: %s, Treasure = %s", p.getName(),
				is.getType());
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = false)
	public void Fish5(McMMOPlayerShakeEvent event) {
		Player p = event.getPlayer();
		ItemStack is = event.getDrop();
		MobHunting.getInstance().getMessages().debug("McMMO-FishingEvent4: %s, Drop = %s", p.getName(), is.getType());
	}

}
