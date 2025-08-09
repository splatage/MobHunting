package metadev.digital.MetaMobHunting.mobs;

import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

public enum MobPlugin {
	Minecraft("Minecraft", 0), MythicMobs("MythicMobs", 1), Citizens("Citizens", 2), InfernalMobs("InfernalMobs", 7), Herobrine("Herobrine",8), EliteMobs("EliteMobs",9), Boss("Boss",10);

	private final String name;
	private final Integer id;

	private MobPlugin(String name, Integer id) {
		this.name = name;
		this.id = id;
	}

	public Integer getId() {
		return id;
	}

	public boolean equalsName(String otherName) {
		return (otherName != null) && name.equals(otherName);
	}

	public String toString() {
		return name;
	}

	public MobPlugin valueOf(int id) {
		return MobPlugin.values()[id];
	}

	public String getName() {
		return name;
	}

	public boolean isSupported() {
		switch (this) {
		case Minecraft:
			return true;
		case Citizens:
			return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.Citizens.getName()));
		case MythicMobs:
			return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName()));
		case EliteMobs:
			return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName()));
		default:
			ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
			console.sendMessage(ChatColor.RED + "[MobHunting] Missing pluginType '" + this.name() + "' in MobPlugin");
		}
		return false;
	}

}
