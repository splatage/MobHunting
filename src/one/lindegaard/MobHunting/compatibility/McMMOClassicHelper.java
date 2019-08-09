package one.lindegaard.MobHunting.compatibility;

import com.gmail.nossr50.datatypes.skills.SkillType;
import one.lindegaard.Core.Materials.Materials;
import one.lindegaard.MobHunting.DamageInformation;

public class McMMOClassicHelper {

	//https://www.spigotmc.org/resources/official-mcmmo-classic.2445/

	public McMMOClassicHelper() {
		
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************

	public static String getSKillTypeName(DamageInformation info) {
		if (Materials.isAxe(info.getWeapon()))
			return SkillType.AXES.getName();
		else if (Materials.isSword(info.getWeapon()))
			return SkillType.SWORDS.getName();
		else if (Materials.isBow(info.getWeapon()))
			return SkillType.ARCHERY.getName();
		else if (Materials.isUnarmed(info.getWeapon()))
			return SkillType.UNARMED.getName();
		else
			return "";
	}
/**
	public static void addXP2(Player player, String skillType, int XP, String xpGainReason) {
		ExperienceAPI.addXP(player, skillType, XP, xpGainReason);
	}

	public static void addLevel(Player player, String skillType, int levels) {
		ExperienceAPI.addLevel(player, skillType, levels);
	}
**/
}
