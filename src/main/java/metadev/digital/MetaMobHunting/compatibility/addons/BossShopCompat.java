package metadev.digital.MetaMobHunting.compatibility.addons;

import java.util.UUID;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.ICompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import org.black_ixx.bossshop.BossShop;
import org.black_ixx.bossshop.api.BossShopAPI;
import org.black_ixx.bossshop.core.BSBuy;
import org.black_ixx.bossshop.core.BSShop;
import org.black_ixx.bossshop.core.prices.BSPriceType;
import org.black_ixx.bossshop.core.rewards.BSRewardType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.metacustomitemslib.rewards.CoreCustomItems;
import metadev.digital.metacustomitemslib.rewards.Reward;
import metadev.digital.metacustomitemslib.rewards.RewardType;
import metadev.digital.MetaMobHunting.MobHunting;

public class BossShopCompat implements ICompat, IFeatureHolder {

    // ****** Standard ******
    private Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin, pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
    private static BossShop bs;

    // https://www.spigotmc.org/resources/bossshop-powerful-and-playerfriendly-chest-gui-shop-menu-plugin.222/

    public BossShopCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.BossShop.getName());

        if (compatPlugin != null) {
            try {
                start();
            } catch (SpinupShutdownException e) {
                Bukkit.getPluginManager().disablePlugin(compatPlugin);
            }
        }
    }

    // ****** ICompat ******

    @Override
    public void start() throws SpinupShutdownException {
        detectedMessage();
        registerFeatures();

        if (isActive()) {
            bs = (BossShop) compatPlugin;
            successfullyLoadedMessage();
            loaded = true;
        } else if (enabled && !supported) {
            Feature base = getFeature("base");
            if (base != null) unsupportedMessage(base);
            else
                pluginError("Plugin is enabled but not supported, and failed to understand the reasoning out of the base " +
                        "feature. Likely caused by a corrupt / incorrect construction of the base feature.");
            throw new SpinupShutdownException();
        }
    }

    @Override
    public void shutdown() throws SpinupShutdownException {
        if (isActive() && loaded) {
            successfullyShutdownMessage();
            loaded = false;
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public boolean isSupported() {
        return supported;
    }

    @Override
    public boolean isActive() {
        return enabled && supported;
    }

    @Override
    public boolean isLoaded() {
        return loaded;
    }

    @Override
    public Plugin getPluginInstance() {
        return compatPlugin;
    }

    @Override
    public String getPluginName() {
        return compatPlugin.getName();
    }

    @Override
    public String getPluginVersion() {
        return compatPlugin.getDescription().getVersion();
    }

    // ****** IFeatureHolder ******

    @Override
    public void registerFeatures() {
        features = new FeatureList(getPluginVersion());

        // Base plugin
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationBossShop;
        features.addFeature("base", enabled);
        supported = isFeatureSupported("base");

        // Other features
    }

    @Override
    public boolean isFeatureEnabled(String name) {
        boolean featureEnabled = false;
        try {
            featureEnabled = features.isFeatureEnabled(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return enable flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }

        return featureEnabled;
    }

    @Override
    public boolean isFeatureSupported(String name) {
        boolean featureSupported = false;
        try {
            featureSupported = features.isFeatureSupported(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return supported flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }

        return featureSupported;
    }

    @Override
    public boolean isFeatureActive(String name) {
        boolean featureActive = false;
        try {
            featureActive = features.isFeatureActive(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return active flag of the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }

        return featureActive;
    }

    @Override
    public Feature getFeature(String name) {
        Feature feature;
        try {
            feature = features.getFeature(name);
            return feature;
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return the feature " + name + " in the " + compatPlugin.getName() + " compat class.");
        }
        return null;
    }

    // ****** Plugin Specific ******
    public BossShop getBossShop() {
        return bs;
    }

    public static BossShopAPI getAPI() {
        return bs.getAPI();
    }

    public static void openShop(MobHunting plugin, Player p) {

        BSShop shop = bs.getAPI().getShop("mobhunting");

        BSBuy buy = getAPI().createBSBuy(BSRewardType.Shop, BSPriceType.Nothing, "item_shop", null, null, 1,
                "OpenShop.Item_Shop");

        BSBuy sell = getAPI().createBSBuy(BSRewardType.Shop, BSPriceType.Nothing, 1, 10, "bought bag of gold", 4, null);

        ItemStack is = CoreCustomItems.getCustomTexture(
                new Reward(Core.getConfigManager().bagOfGoldName.trim(), 10, RewardType.BAGOFGOLD,
                        UUID.fromString(RewardType.BAGOFGOLD.getUUID())),
                Core.getConfigManager().skullTextureURL);

        getAPI().addItemToShop(is, buy, shop);

        getAPI().addItemToShop(is, sell, shop);

        getAPI().finishedAddingItemsToShop(shop);

        getAPI().openShop(p, "mobhunting");
    }
}
