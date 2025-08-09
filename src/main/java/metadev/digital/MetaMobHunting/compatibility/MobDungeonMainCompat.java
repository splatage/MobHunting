package metadev.digital.MetaMobHunting.compatibility;

import metadev.digital.MetaMobHunting.Messages.MessageHelper;
import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

import metadev.digital.MetaMobHunting.MobHunting;

public class MobDungeonMainCompat implements Listener {
	public MobDungeonMainCompat() {
		Bukkit.getPluginManager().registerEvents(this, MobHunting.getInstance());
		MessageHelper.notice("Enabling MobDungeon Compatibility");
	}

}
