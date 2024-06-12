package metadev.digital.MetaMobHunting.compatibility;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import metadev.digital.MetaMobHunting.MobHunting;

public class MobDungeonMainCompat implements Listener {
	public MobDungeonMainCompat() {
		Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
		Bukkit.getConsoleSender().sendMessage(MobHunting.PREFIX + "Enabling MobDungeon Compatibility");
	}

}
