package one.lindegaard.MobHunting.compatibility;

import one.lindegaard.MobHunting.DamageInformation;

public class McMMOCompatHelper {

	public McMMOCompatHelper() {
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

}
