package metadev.digital.MetaMobHunting.Messages;

import metadev.digital.MetaMobHunting.Messages.constants.Prefixes;
import metadev.digital.MetaMobHunting.MobHunting;
import org.bukkit.Bukkit;

public class MessageHelper {

    /**
     * Show debug information in the Server console log
     *
     * @param message
     * @param args
     */
    public static void debug(String message, Object... args) {
        if (MobHunting.getInstance().getConfigManager().killDebug) {
            Bukkit.getServer().getConsoleSender().sendMessage(Prefixes.PREFIX_DEBUG + String.format(message, args));
        }
    }

    /**
     * Show console message
     *
     * @param message
     * @param args
     */
    public static void notice(String message, Object... args) {
        Bukkit.getServer().getConsoleSender().sendMessage(Prefixes.PREFIX  + " " + String.format(message, args));
    }

    /**
     * Show console warning
     *
     * @param message
     * @param args
     */
    public static void warning(String message, Object... args) {
        Bukkit.getServer().getConsoleSender().sendMessage(Prefixes.PREFIX_WARNING + " " + String.format(message, args));
    }

    /**
     * Show console error
     *
     * @param message
     * @param args
     */
    public static void error(String message, Object... args) {
        Bukkit.getServer().getConsoleSender().sendMessage(Prefixes.PREFIX_ERROR  + " " + String.format(message, args));
    }
}
