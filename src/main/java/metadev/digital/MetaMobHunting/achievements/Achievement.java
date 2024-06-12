package metadev.digital.MetaMobHunting.achievements;

import org.bukkit.inventory.ItemStack;

public interface Achievement
{
	String getName();
	String getID();
	
	String getDescription();
	
	double getPrize();
	
	String getPrizeCmd();
	String getPrizeCmdDescription();
	
	ItemStack getSymbol();
}
