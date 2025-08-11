package metadev.digital.MetaMobHunting.mobs;

import metadev.digital.MetaMobHunting.compatibility.addons.CitizensCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.EliteMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MythicMobsCompat;
import metadev.digital.MetaMobHunting.compatibility.addons.MysteriousHalloweenCompat;
import net.citizensnpcs.api.npc.NPC;
import metadev.digital.metacustomitemslib.mobs.MobType;
import metadev.digital.metacustomitemslib.rewards.CoreCustomItems;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.ItemStack;

public class ExtendedMob {

	private Integer mob_id; // The unique mob_id from mh_Mobs
	private MobPlugin mobPlugin; // Plugin_id from mh_Plugins
	private String mobtype; // mobtype NOT unique

	public ExtendedMob(Integer mob_id, MobPlugin mobPlugin, String mobtype) {
		this.mob_id = mob_id;
		this.mobPlugin = mobPlugin;
		this.mobtype = mobtype;
	}

	public ExtendedMob(MobPlugin mobPlugin, String mobtype) {
		this.mobPlugin = mobPlugin;
		this.mobtype = mobtype;
	}

	/**
	 * @return the mob_id
	 */
	public Integer getMob_id() {
		return mob_id;
	}

	/**
	 * @return the plugin_id
	 */
	public MobPlugin getMobPlugin() {
		return mobPlugin;
	}

	/**
	 * @return the mobtype
	 */
	public String getMobtype() {
		return mobtype;
	}

	/**
	 * @param mobtype the mobtype to set
	 */
	public void setMobtype(String mobtype) {
		this.mobtype = mobtype;
	}

	@Override
	public int hashCode() {
		return mob_id.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ExtendedMob))
			return false;

		ExtendedMob other = (ExtendedMob) obj;

		return mob_id.equals(other.mob_id);
	}

	@Override
	public String toString() {
		return String.format("MobStore: {mob_id: %s, plugin_id: %s, mobtype: %s}", this.mob_id, mobPlugin.name(),
				mobtype);
	}

	public String getMobName() {
		switch (mobPlugin) {
		case Minecraft:
			return mobtype;
		case Citizens:
			NPC npc = CitizensCompat.getCitizensPlugin().getNPCRegistry().getById(Integer.valueOf(mobtype));
			if (npc != null)
				return npc.getFullName();
			else
				return "Unknown";
        case EliteMobs:
            if (EliteMobsCompat.getMobRewardData().containsKey(mobtype))
                return EliteMobsCompat.getMobRewardData().get(mobtype).getMobName();
            else
                return mobtype;
		case MythicMobs:
			if (MythicMobsCompat.getMobRewardData().containsKey(mobtype))
				return MythicMobsCompat.getMobRewardData().get(mobtype).getMobName();
			else
				return MythicMobsCompat.getMythicMobName(mobtype);
        case MysteriousHalloween:
            if (MysteriousHalloweenCompat.getMobRewardData().containsKey(mobtype))
                return MysteriousHalloweenCompat.getMobRewardData().get(mobtype).getMobName();
            else
                return mobtype;
		default:
			ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
			console.sendMessage(
					ChatColor.RED + "[MobHunting] Missing pluginType '" + mobPlugin.name() + "' in ExtendeMob.");
		}
		return null;
	}

	public String getLocalizedName() {
		if (mobPlugin == MobPlugin.Minecraft)
			return MobHunting.getInstance().getMessages().getString("mobs." + mobtype + ".name");
		else
			return MobHunting.getInstance().getMessages().getString("mobs." + mobPlugin.name() + "_" + mobtype + ".name");
	}

	/**
	 * Gets a safe and filtered Entity Name out of what is in the localized files
	 * @return
	 */
	public String getEntityName() {
		if (mobPlugin == MobPlugin.Minecraft) {
			return ChatColor.stripColor(MobHunting.getInstance().getMessages().getString("mobs." + mobtype + ".name")).replace(' ', '_');
		}
		else {
			return ChatColor.stripColor(MobHunting.getInstance().getMessages().getString("mobs." + mobPlugin.name() + "_" + mobtype + ".name")).replace(' ', '_');
		}
	}

	public int getProgressAchievementLevel1() {
		switch (mobPlugin) {
		case Minecraft:
			MobType mob = MobType.getMobType(mobtype);
			return MobHunting.getInstance().getConfigManager().getProgressAchievementLevel1(mob);
		case Citizens:
			return CitizensCompat.getProgressAchievementLevel1(mobtype);
		case EliteMobs:
			return EliteMobsCompat.getProgressAchievementLevel1(mobtype);
        case MythicMobs:
            return MythicMobsCompat.getProgressAchievementLevel1(mobtype);
        case MysteriousHalloween:
            return MysteriousHalloweenCompat.getProgressAchievementLevel1(mobtype);
		default:
			ConsoleCommandSender console = Bukkit.getServer().getConsoleSender();
			console.sendMessage(
					ChatColor.RED + "[MobHunting] Missing pluginType '" + mobPlugin.name() + "' in ExtendeMob.");
		}
		return 0;
	}

	public boolean matches(Entity entity) {
		ExtendedMob mob = MobHunting.getInstance().getExtendedMobManager().getExtendedMobFromEntity(entity);
		return mobtype.equalsIgnoreCase(mob.mobtype);
	}

	public ItemStack getInventoryAchivementItem(String name, int amount, int money) {
		switch (mobPlugin) {
		case Minecraft:
			MobType mob = MobType.getMobType(name);
			
			return CoreCustomItems.getCustomHead(mob,name, amount, money, mob.getSkinUUID());
		default:
			return new ItemStack(Material.IRON_INGOT, amount);
		}
	}

}
