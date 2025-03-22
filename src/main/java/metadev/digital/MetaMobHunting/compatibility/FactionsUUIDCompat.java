package metadev.digital.MetaMobHunting.compatibility;
/**
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.massivecraft.factions.Board;
import com.massivecraft.factions.FLocation;
import com.massivecraft.factions.FPlayer;
import com.massivecraft.factions.FPlayers;
import com.massivecraft.factions.Faction;

import metadev.digital.metacustomitemslib.compatibility.CompatPlugin;
import metadev.digital.MetaMobHunting.MobHunting;

public class FactionsUUIDCompat {

	private static Plugin mPlugin;

	// https://www.spigotmc.org/resources/factionsuuid.1035/

	public FactionsUUIDCompat() {
		mPlugin = Bukkit.getPluginManager().getPlugin(CompatPlugin.Factions.getName());
	}

	// **************************************************************************
	// OTHER
	// **************************************************************************

	public Plugin getPlugin() {
		return mPlugin;
	}

	public static boolean isEnabledInConfig() {
		return MobHunting.getInstance().getConfigManager().enableIntegrationFactions;
	}

	public static boolean isInSafeZoneAndpeaceful(Player player) {
		FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
		Faction faction_home = fplayer.getFaction();
		FLocation flocation = fplayer.getLastStoodAt();
		Faction faction_here = Board.getInstance().getFactionAt(flocation);
		if (faction_here != null && faction_here.isSafeZone() && !faction_here.isPeaceful()) {
			MobHunting.getInstance().getMessages().debug("player is in a safe zone: %s", faction_home.getDescription());
			if (faction_here.isPeaceful()) {
				MobHunting.getInstance().getMessages().debug("The safe zone is peacefull - no reward.");
				return true;
			}
			return false;
		} else
			return false;
	}

	public static boolean isInWilderness(Player player) {
		FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
		FLocation flocation = fplayer.getLastStoodAt();
		Faction faction_here = Board.getInstance().getFactionAt(flocation);
		if (faction_here != null && faction_here.isWilderness()) {
			MobHunting.getInstance().getMessages().debug("%s is in Wilderness", player.getName());
			return true;
		} else
			return false;
	}

	public static boolean isInWarZone(Player player) {
		FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
		FLocation flocation = fplayer.getLastStoodAt();
		Faction faction_here = Board.getInstance().getFactionAt(flocation);
		if (faction_here != null && faction_here.isWarZone()) {
			MobHunting.getInstance().getMessages().debug("%s is in a War zone", player.getName());
			return true;
		} else
			return false;
	}

	public static boolean isInHomeZoneAndPeaceful(Player player) {
		FPlayer fplayer = FPlayers.getInstance().getByPlayer(player);
		Faction faction_home = fplayer.getFaction();
		FLocation flocation = fplayer.getLastStoodAt();
		Faction faction_here = Board.getInstance().getFactionAt(flocation);
		if (faction_here != null && faction_here.equals(faction_home)) {
			MobHunting.getInstance().getMessages().debug("player is in home zone: %s (peaceful=%s, normal=%s) ",
					faction_home.getDescription(), faction_here.isPeaceful(), faction_here.isNormal());
			if (faction_here.isPeaceful()) {
				MobHunting.getInstance().getMessages().debug("The home zone is peacefull - no reward.");
				return true;
			}
			return false;
		} else
			return false;
	}

}*/
