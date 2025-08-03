package metadev.digital.MetaMobHunting.compatibility;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import metadev.digital.MetaMobHunting.leaderboard.HologramLeaderboard;
import metadev.digital.metacustomitemslib.Core;
import metadev.digital.metacustomitemslib.compatibility.addons.CMICompat;
import metadev.digital.metacustomitemslib.compatibility.enums.SupportedPluginEntities;
import net.Zrips.CMILib.Container.CMILocation;
import org.bukkit.Bukkit;

public class CMIHelper {

    public static void createHologram(CMIHologram hologram) {
        if(isCMILoaded()){
            CMICompat.getHologramManager().addHologram(hologram);
        }
    }

    public static void createHologramFromLeaderboard(HologramLeaderboard hologramLeaderboard) {
        if(isCMILoaded()){
            CMILocation location = new CMILocation(hologramLeaderboard.getLocation());
            CMIHologram hologram = new CMIHologram(hologramLeaderboard.getHologramName(), location);
            createHologram(hologram);
        }
    }

    public static void deleteHologram(CMIHologram hologram) {
        if(isCMILoaded()){
            CMICompat.getHologramManager().removeHolo(hologram);
        }
    }

    public static void deleteHologramByName(String hologramName) {
        if(isCMILoaded()) {
            CMIHologram hologram = CMICompat.getHologramManager().getByName(hologramName);
            deleteHologram(hologram);
        }
    }

    public static void editTextLine(CMIHologram hologram, String message, int position) {
        if(isCMILoaded()){
            hologram.setLine(position,message);
        }
    }

    public static boolean isCMILoaded() {
        return Core.getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CMI.getName()));
    }

    public static boolean isCMILibLoaded(){
        return Core.getCompatibilityManager().isCompatibilityLoaded(Bukkit.getPluginManager().getPlugin(SupportedPluginEntities.CMILib.getName()));
    }
}
