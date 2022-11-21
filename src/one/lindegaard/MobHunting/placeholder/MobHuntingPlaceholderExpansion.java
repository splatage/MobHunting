package one.lindegaard.MobHunting.placeholder;

import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import one.lindegaard.CustomItemsLib.Core;
import one.lindegaard.MobHunting.MobHunting;
import one.lindegaard.MobHunting.compatibility.PlaceholderAPICompat;

public class MobHuntingPlaceholderExpansion extends PlaceholderExpansion implements Listener {
	
	 /**
     * This method should always return true unless we
     * have a dependency we need to make sure is on the server
     * for our placeholders to work!
     *
     * @return always true since we do not have any dependencies.
     */
    @Override
    public boolean canRegister(){
        return true;
    }
    
	@Override
	public String onRequest(OfflinePlayer player, String identifier) {

		// Remember to update the documentation when adding new placeholders
		// https://www.spigotmc.org/wiki/mobhunting-placeholders/

		// placeholder: %mobhunting_ping%
		if (identifier.equals("ping")) {
			return "pong";
		}

		// placeholder: %mobhunting_dropped_rewards%
		if (identifier.equals("dropped_rewards")) {
			return String.valueOf(Core.getCoreRewardManager().getDroppedMoney().size());
		}

		// placeholder: %mobhunting_dropped_rewards%
		if (identifier.equals("dropped_money")) {
			double amt = 0;
			for (double d : Core.getCoreRewardManager().getDroppedMoney().values())
				amt = amt + d;
			return MobHunting.getInstance().getRewardManager().format(amt);
		}

		// always check if the player is null for placeholders related to the
		// player!
		if (player == null) {
			return "";
		}

		// placeholder: %mobhunting_total_kills%
		if (identifier.equals("total_kills")) {
			if (PlaceholderAPICompat.getPlaceHolders().containsKey(player.getUniqueId()))
			return String.valueOf(PlaceholderAPICompat.getPlaceHolders().get(player.getUniqueId()).getTotal_kills());
			else
				return "0";
		}

		// placeholder: %mobhunting_total_cash%
		if (identifier.equals("total_cash")) {
			return MobHunting.getInstance().getRewardManager()
					.format(PlaceholderAPICompat.getPlaceHolders().get(player.getUniqueId()).getTotal_cash());
		}

		// placeholder: %mobhunting_rank%
		if (identifier.equals("rank")) {
			return String.valueOf(PlaceholderAPICompat.getPlaceHolders().get(player.getUniqueId()).getRank());
		}

		// placeholder: %mobhunting_balance%
		if (identifier.equals("balance")) {
			return String.valueOf(MobHunting.getInstance().getRewardManager().getBalance(player));
		}

		// anything else someone types is invalid because we never defined
		// %customplaceholder_<what they want a value for>%
		// we can just return null so the placeholder they specified is not
		// replaced.
		return null;
	}

	@Override
	public String getAuthor() {
		return "Rocologo";
	}

	@Override
	public String getIdentifier() {
		return "mobhunting";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

}
