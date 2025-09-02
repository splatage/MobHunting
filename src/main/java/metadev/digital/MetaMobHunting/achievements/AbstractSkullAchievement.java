package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

public abstract class AbstractSkullAchievement implements Achievement {

	@SuppressWarnings("deprecation")
	@Override
	public ItemStack getSymbol() {
		// TODO: best material?
		ItemStack skull = new ItemStack(Material.CREEPER_HEAD);
		SkullMeta skullMeta = (SkullMeta) skull.getItemMeta();
		skullMeta.setOwner("MHF_Creeper");
		skull.setItemMeta(skullMeta);
		return skull;
	}
}
