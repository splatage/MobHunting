package one.lindegaard.MobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import one.lindegaard.MobHunting.MobHunting;

public class HeroesCompat implements Listener {
	public HeroesCompat() {
		Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling Heroes Compatibility");
	}

}
