package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import metadev.digital.MetaMobHunting.MobHunting;

public class WarCompat implements Listener {
	public WarCompat() {
		Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
		Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling War Compatibility");
	}

}
