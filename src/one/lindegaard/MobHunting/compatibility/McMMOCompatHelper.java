package one.lindegaard.MobHunting.compatibility;

import org.bukkit.entity.Player;

import one.lindegaard.MobHunting.DamageInformation;

public class McMMOCompatHelper {

	public McMMOCompatHelper() {
	}

	public static boolean isSupported() {
		return McMMOCompat.isSupported() || McMMOClassicCompat.isSupported();
	}

	public static String getSkilltypeName(DamageInformation info) {
		if (info != null) {
			if (McMMOClassicCompat.isSupported())
				return McMMOClassicCompat.getSKillTypeName(info);
			else if (McMMOCompat.isSupported())
				return McMMOCompat.getSKillTypeName(info);
		}
		return "";
	}

	public static void addLevel(Player killer, String skilltypename, int level) {
		if (McMMOClassicCompat.isSupported())
			McMMOClassicCompat.addLevel(killer, skilltypename, level);
		else if (McMMOCompat.isSupported())
			McMMOCompat.addLevel(killer, skilltypename, level);

	}

}
