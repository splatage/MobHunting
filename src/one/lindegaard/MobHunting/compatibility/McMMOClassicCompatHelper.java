package one.lindegaard.MobHunting.compatibility;

import com.gmail.nossr50.datatypes.skills.SkillType;
import one.lindegaard.Core.materials.Materials;
import one.lindegaard.MobHunting.DamageInformation;

public class McMMOClassicCompatHelper {

	//https://www.spigotmc.org/resources/official-mcmmo-classic.2445/

	public McMMOClassicCompatHelper() {
		
	}

	// **************************************************************************
	// OTHER FUNCTIONS
	// **************************************************************************

	public static String getSKillTypeName(DamageInformation info) {
		if (Materials.isAxe(info.getWeapon()))
			return SkillType.AXES.name();
		else if (Materials.isSword(info.getWeapon()))
			return SkillType.SWORDS.name();
		else if (Materials.isBow(info.getWeapon()))
			return SkillType.ARCHERY.name();
		else if (Materials.isUnarmed(info.getWeapon()))
			return SkillType.UNARMED.name();
		else
			return null;
	}
}
