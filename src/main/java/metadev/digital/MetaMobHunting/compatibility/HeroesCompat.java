package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import metadev.digital.MetaMobHunting.MobHunting;

public class HeroesCompat implements Listener {
	public HeroesCompat() {
		Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling Heroes Compatibility");
	}

}
