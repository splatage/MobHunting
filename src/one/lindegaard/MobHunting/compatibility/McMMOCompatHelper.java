package one.lindegaard.MobHunting.compatibility;

import com.gmail.nossr50.datatypes.skills.PrimarySkillType;

import one.lindegaard.Core.materials.Materials;
import one.lindegaard.MobHunting.DamageInformation;

public class McMMOCompatHelper {

	// McMMO 2.1.0 documentation:
	// https://docs.google.com/document/d/1qY6hEyGCO5z1PRup_OvMBxAmumydxxoO_H-pnUrVK8M/edit#heading=h.474ghxburdpp

	public McMMOCompatHelper() {
	}

	public static String getSKillTypeName(DamageInformation info) {
		if (Materials.isAxe(info.getWeapon()))
			return PrimarySkillType.AXES.name();
		else if (Materials.isSword(info.getWeapon()))
			return PrimarySkillType.SWORDS.name();
		else if (Materials.isBow(info.getWeapon()))
			return PrimarySkillType.ARCHERY.name();
		else if (Materials.isUnarmed(info.getWeapon()))
			return PrimarySkillType.UNARMED.name();
		else
			return null;
	}
}
