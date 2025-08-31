package metadev.digital.MetaMobHunting.mobs;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.MetaMobHunting.MobHunting;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import org.bukkit.Bukkit;

public enum MobPlugin {
    Minecraft("Minecraft", 0),
    MythicMobs("MythicMobs", 1),
    Citizens("Citizens", 2),
    TARDISWeepingAngels("TARDISWeepingAngels", 3),
    CustomMobs("CustomMobs", 4),
    MysteriousHalloween("MysteriousHalloween", 5),
    SmartGiants("SmartGiants", 6),
    InfernalMobs("InfernalMobs", 7),
    Herobrine("Herobrine",8),
    EliteMobs("EliteMobs",9),
    Boss("Boss",10);

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
        case MysteriousHalloween:
            return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MysteriousHalloween.getName()));
		case MythicMobs:
			return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.MythicMobs.getName()));
		case EliteMobs:
			return MobHunting.getInstance().getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.EliteMobs.getName()));
		default:
			MessageHelper.error("Missing pluginType '" + this.name() + "' in MobPlugin");
		}
		return false;
	}

}
