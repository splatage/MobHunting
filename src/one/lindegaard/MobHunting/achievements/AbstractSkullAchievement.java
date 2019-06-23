package one.lindegaard.MobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import one.lindegaard.Core.Server.Servers;

public abstract class AbstractSkullAchievement implements Achievement {

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getSymbol() {
		// TODO: best material?
		ItemStack skull = Servers.isMC113OrNewer() ? new ItemStack(Material.CREEPER_HEAD)
				: new ItemStack(Material.matchMaterial("SKULL_ITEM"), 1, (short) 4);
		SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
		skullMeta.setOwner("MHF_Creeper");
		skull.setItemMeta(skullMeta);
		return skull;
	}
}
