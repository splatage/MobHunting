package one.lindegaard.MobHunting.compatibility;

import java.util.UUID;

import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.api.BossShopAPI;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.BSShop;
import org.black_ixx.bossshop.core.prices.BSPriceType;
import org.black_ixx.bossshop.core.rewards.BSRewardType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import one.lindegaard.Core.compatibility.CompatPlugin;
import one.lindegaard.Core.rewards.CoreCustomItems;
import one.lindegaard.Core.rewards.Reward;
import one.lindegaard.Core.rewards.RewardType;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.rewards.CustomItems;

public class BossShopCompat {

	private Plugin mPlugin;
	private static boolean supported = false;
	private static BossShop bs;

	// https://www.spigotmc.org/resources/bossshop-powerful-and-playerfriendly-chest-gui-shop-menu-plugin.222/

	public BossShopCompat() {
		if (!isEnabledInConfig()) {
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Compatibility with BossShop is disabled in config.yml");
		} else {
			mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.BossShop.getName());
			bs = (BossShop) mPlugin;
			Bukkit.getConsoleSender().sendMessage(ChatColor.GOLD + "[MobHunting] " + ChatColor.RESET
					+ "Enabling compatibility with BossShopPro (" + bs.getDescription().getVersion() + ").");
			supported = true;
		}
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public BossShop getBossShop() {
		return bs;
	}

	public static BossShopAPI getAPI() {
		return bs.getAPI();
	}

	public static boolean isSupported() {
		return supported;
	}

	public boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationBossShop;
	}

	public static void openShop(MobHunting plugin, Player p) {

		plugin.getMessages().debug("test1");
		BSShop shop = bs.getAPI().getShop("mobhunting");

		plugin.getMessages().debug("test2");
		BSBuy buy = getAPI().createBSBuy(BSRewardType.Shop, BSPriceType.Nothing, "item_shop", null, null, 1,
				"OpenShop.Item_Shop");

		plugin.getMessages().debug("test3");
		BSBuy sell = getAPI().createBSBuy(BSRewardType.Shop, BSPriceType.Nothing, 1, 10, "bought bag of gold", 4, null);

		ItemStack is = new CoreCustomItems().getCustomtexture(
				plugin.getConfigManager().dropMoneyOnGroundSkullRewardName.trim(), 10, RewardType.BAGOFGOLD,
				UUID.fromString(RewardType.BAGOFGOLD.getUUID()),
				plugin.getConfigManager().dropMoneyOnGroundSkullTextureValue,
				plugin.getConfigManager().dropMoneyOnGroundSkullTextureSignature);

		plugin.getMessages().debug("test4");
		getAPI().addItemToShop(is, buy, shop);

		plugin.getMessages().debug("test5");
		getAPI().addItemToShop(is, sell, shop);

		plugin.getMessages().debug("test6");
		getAPI().finishedAddingItemsToShop(shop);

		plugin.getMessages().debug("test7");
		getAPI().openShop(p, "mobhunting");
	}

}
