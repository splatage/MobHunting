package metadev.digital.MetaMobHunting.compatibility;

import com.Zrips.CMI.Modules.Holograms.CMIHologram;
import metadev.digital.MetaMobHunting.leaderboard.HologramLeaderboard;
import metadev.digital.metacustomitemslib.compatibility.CMICompat;
import net.Zrips.CMILib.Container.CMILocation;

public class CMIHologramsHelper {

    public static void createHologram(CMIHologram hologram) {
        CMICompat.getHologramManager().addHologram(hologram);
    }

    public static void createHologramFromLeaderboard(HologramLeaderboard hologramLeaderboard) {
        CMILocation location = new CMILocation(hologramLeaderboard.getLocation());
        CMIHologram hologram = new CMIHologram(hologramLeaderboard.getHologramName(), location);
        createHologram(hologram);
    }

    public static void deleteHologram(CMIHologram hologram) {
        CMICompat.getHologramManager().removeHolo(hologram);
    }

    public static void deleteHologramByName(String hologramName) {
        CMIHologram hologram = CMICompat.getHologramManager().getByName(hologramName);
        deleteHologram(hologram);
    }

    public static void editTextLine(CMIHologram hologram, String message, int position) {
        hologram.setLine(position,message);
    }
}
