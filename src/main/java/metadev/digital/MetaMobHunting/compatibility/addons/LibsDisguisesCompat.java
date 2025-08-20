package metadev.digital.MetaMobHunting.compatibility.addons;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import metadev.digital.metacustomitemslib.compatibility.Feature;
import metadev.digital.metacustomitemslib.compatibility.FeatureList;
import metadev.digital.MetaMobHunting.compatibility.IMobHuntCompat;
import metadev.digital.metacustomitemslib.compatibility.IFeatureHolder;
import metadev.digital.metacustomitemslib.compatibility.enums.BoundIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.enums.VersionSetIdentifierEnum;
import metadev.digital.metacustomitemslib.compatibility.exceptions.FeatureNotFoundException;
import metadev.digital.metacustomitemslib.compatibility.exceptions.SpinupShutdownException;
import metadev.digital.metacustomitemslib.server.Server;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import metadev.digital.MetaMobHunting.MobHunting;

import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.events.DisguiseEvent;
import me.libraryaddict.disguise.events.UndisguiseEvent;

public class LibsDisguisesCompat implements Listener, IMobHuntCompat, IFeatureHolder {

	// API
	// https://www.spigotmc.org/wiki/lib-s-disguises/

    // ****** Standard ******
    private static Plugin compatPlugin;
    private static boolean enabled = false, supported = false, loaded = false;
    private static String sMin, sMax, pMin = "11.0.7", pMax;
    private static FeatureList features;

    // ****** Plugin Specific ******
	private static DisguiseType[] aggresiveList = new DisguiseType[30];
	private static DisguiseType[] passiveList = new DisguiseType[20];
	private static DisguiseType[] otherList = new DisguiseType[40];

	public LibsDisguisesCompat() {
        compatPlugin = Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.LibsDisguises.getName());

        if(compatPlugin != null) {
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
            Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
            successfullyLoadedMessage();
            loaded = true;
        } else if (enabled && !supported) {
            Feature base = getFeature("base");
            if(base != null) unsupportedMessage(base);
            else pluginError("Plugin is enabled but not supported, and failed to understand the reasoning out of the base " +
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
        enabled = MobHunting.getInstance().getConfigManager().enableIntegrationLibsDisguises;
        features.addFeature("base", pMin, BoundIdentifierEnum.FLOOR, VersionSetIdentifierEnum.PLUGIN, enabled);
        supported = isFeatureSupported("base");

        // Other features
    }

    @Override
    public boolean isFeatureEnabled(String name) {
        boolean featureEnabled = false;
        try {
            featureEnabled = features.isFeatureEnabled(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return enable flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }

        return featureEnabled;
    }

    @Override
    public boolean isFeatureSupported(String name) {
        boolean featureSupported = false;
        try {
            featureSupported = features.isFeatureSupported(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return supported flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }

        return featureSupported;
    }

    @Override
    public boolean isFeatureActive(String name) {
        boolean featureActive = false;
        try {
            featureActive = features.isFeatureActive(name);
        } catch (FeatureNotFoundException e) {
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return active flag of the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
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
            MessageHelper.debug("Triggered a FeatureNotFoundException when trying to return the feature " + name + " in the " + compatPlugin.getName() +" compat class." );
        }
        return null;
    }

    // ****** Listener ******
    @EventHandler(priority = EventPriority.NORMAL)
    public void onDisguiseEvent(final DisguiseEvent event) {

    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onUndisguiseEvent(final UndisguiseEvent event) {

    }

    // ****** Plugin Specific ******
    public static me.libraryaddict.disguise.disguisetypes.Disguise getDisguise(Entity entity) {
        return DisguiseAPI.getDisguise(entity);
    }

    public static void disguisePlayer(Entity entity, me.libraryaddict.disguise.disguisetypes.Disguise disguise) {
        DisguiseAPI.disguiseToAll(entity, disguise);
    }

    public static void undisguiseEntity(Entity entity) {
        DisguiseAPI.undisguiseToAll(entity);
    }

    public static boolean isDisguised(Entity entity) {
        return DisguiseAPI.isDisguised(entity);
    }

    public static boolean isAggresiveDisguise(Entity entity) {
        return aggresiveMobs.contains(DisguiseAPI.getDisguise(entity).getType());
    }

    public static boolean isPassiveDisguise(Entity entity) {
        return passiveMobs.contains(DisguiseAPI.getDisguise(entity).getType());
    }

    public static boolean isOtherDisguise(Entity entity) {
        return otherDisguiseTypes.contains(DisguiseAPI.getDisguise(entity).getType());
    }

    public static boolean isPlayerDisguise(Entity entity) {

        if (DisguiseAPI.getDisguise(entity).getType().equals(DisguiseType.PLAYER))
            return true;
        else
            return false;
    }

    static {
        int n = 0;
        if (Server.isMC111OrNewer()) {
            aggresiveList[n++] = DisguiseType.VEX;
            aggresiveList[n++] = DisguiseType.EVOKER;
            aggresiveList[n++] = DisguiseType.VINDICATOR;
        }
        if (Server.isMC111OrNewer()) {
            aggresiveList[n++] = DisguiseType.HUSK;
            aggresiveList[n++] = DisguiseType.STRAY;
        }
        if (Server.isMC19OrNewer()) {
            aggresiveList[n++] = DisguiseType.SHULKER;
        }
        aggresiveList[n++] = DisguiseType.GUARDIAN;
        aggresiveList[n++] = DisguiseType.ENDERMITE;
        aggresiveList[n++] = DisguiseType.ELDER_GUARDIAN;
        aggresiveList[n++] = DisguiseType.ZOMBIE;
        aggresiveList[n++] = DisguiseType.BLAZE;
        aggresiveList[n++] = DisguiseType.CAVE_SPIDER;
        aggresiveList[n++] = DisguiseType.CREEPER;
        aggresiveList[n++] = DisguiseType.ENDER_DRAGON;
        aggresiveList[n++] = DisguiseType.ENDERMAN;
        aggresiveList[n++] = DisguiseType.GHAST;
        aggresiveList[n++] = DisguiseType.GIANT;
        aggresiveList[n++] = DisguiseType.PIG_ZOMBIE;
        aggresiveList[n++] = DisguiseType.SKELETON;
        aggresiveList[n++] = DisguiseType.SLIME;
        aggresiveList[n++] = DisguiseType.SPIDER;
        aggresiveList[n++] = DisguiseType.WITCH;
        aggresiveList[n++] = DisguiseType.WITHER;
        aggresiveList[n++] = DisguiseType.WITHER_SKELETON;
        aggresiveList[n++] = DisguiseType.WITHER_SKULL;
        aggresiveList[n++] = DisguiseType.ZOMBIE_VILLAGER;
    }
    private static Set<DisguiseType> aggresiveMobs = new HashSet<DisguiseType>(Arrays.asList(aggresiveList));

    static {
        int n2 = 0;
        if (Server.isMC111OrNewer()) {
            passiveList[n2++] = DisguiseType.LLAMA;
        }
        if (Server.isMC110OrNewer()) {
            passiveList[n2++] = DisguiseType.POLAR_BEAR;
        }
        passiveList[n2++] = DisguiseType.BAT;
        passiveList[n2++] = DisguiseType.CHICKEN;
        passiveList[n2++] = DisguiseType.COW;
        passiveList[n2++] = DisguiseType.DONKEY;
        passiveList[n2++] = DisguiseType.HORSE;
        passiveList[n2++] = DisguiseType.IRON_GOLEM;
        passiveList[n2++] = DisguiseType.MAGMA_CUBE;
        passiveList[n2++] = DisguiseType.MULE;
        passiveList[n2++] = DisguiseType.MUSHROOM_COW;
        passiveList[n2++] = DisguiseType.OCELOT;
        passiveList[n2++] = DisguiseType.PIG;
        passiveList[n2++] = DisguiseType.RABBIT;
        passiveList[n2++] = DisguiseType.SHEEP;
        passiveList[n2++] = DisguiseType.SILVERFISH;
        passiveList[n2++] = DisguiseType.SKELETON_HORSE;
        passiveList[n2++] = DisguiseType.SNOWMAN;
        passiveList[n2++] = DisguiseType.SQUID;
    }
    private static Set<DisguiseType> passiveMobs = new HashSet<DisguiseType>(Arrays.asList(passiveList));

    static {
        int n3 = 0;
        otherList[n3++] = DisguiseType.ARMOR_STAND;
        otherList[n3++] = DisguiseType.ARROW;
        otherList[n3++] = DisguiseType.BOAT;
        otherList[n3++] = DisguiseType.DROPPED_ITEM;
        otherList[n3++] = DisguiseType.EGG;
        otherList[n3++] = DisguiseType.ENDER_CRYSTAL;
        otherList[n3++] = DisguiseType.ENDER_PEARL;
        otherList[n3++] = DisguiseType.ENDER_SIGNAL;
        otherList[n3++] = DisguiseType.EXPERIENCE_ORB;
        otherList[n3++] = DisguiseType.FALLING_BLOCK;
        otherList[n3++] = DisguiseType.FIREBALL;
        otherList[n3++] = DisguiseType.FIREWORK;
        otherList[n3++] = DisguiseType.FISHING_HOOK;
        otherList[n3++] = DisguiseType.ITEM_FRAME;
        otherList[n3++] = DisguiseType.LEASH_HITCH;
        otherList[n3++] = DisguiseType.MINECART;
        otherList[n3++] = DisguiseType.MINECART_CHEST;
        otherList[n3++] = DisguiseType.MINECART_COMMAND;
        otherList[n3++] = DisguiseType.MINECART_FURNACE;
        otherList[n3++] = DisguiseType.MINECART_HOPPER;
        otherList[n3++] = DisguiseType.MINECART_MOB_SPAWNER;
        otherList[n3++] = DisguiseType.MINECART_TNT;
        otherList[n3++] = DisguiseType.PAINTING;
        otherList[n3++] = DisguiseType.PLAYER;
        otherList[n3++] = DisguiseType.PRIMED_TNT;
        otherList[n3++] = DisguiseType.SMALL_FIREBALL;
        otherList[n3++] = DisguiseType.SNOWBALL;
        otherList[n3++] = DisguiseType.SPLASH_POTION;
        otherList[n3++] = DisguiseType.THROWN_EXP_BOTTLE;
        otherList[n3++] = DisguiseType.VILLAGER;
        otherList[n3++] = DisguiseType.WOLF;
    }
    private static Set<DisguiseType> otherDisguiseTypes = new HashSet<DisguiseType>(Arrays.asList(otherList));
}
