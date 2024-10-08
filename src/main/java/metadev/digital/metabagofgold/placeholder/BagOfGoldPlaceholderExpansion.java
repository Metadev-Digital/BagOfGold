package metadev.digital.metabagofgold.placeholder;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import metadev.digital.metabagofgold.BagOfGold;
import metadev.digital.metacustomitemslib.Tools;
import org.bukkit.OfflinePlayer;
import org.bukkit.event.Listener;

public class BagOfGoldPlaceholderExpansion extends PlaceholderExpansion implements Listener {
	
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

		// placeholder: %bagofgold_ping%
		if (identifier.equals("ping")) {
			return "pong";
		}

		// always check if the player is null for placeholders related to the
		// player!
		if (player == null) {
			return "";
		}

		// placeholder: %bagofgold_balance%
		if (identifier.equals("balance")) {
			return Tools.format(BagOfGold.getInstance().getPlayerBalanceManager().getPlayerBalance(player).getBalance());
		}

		// placeholder: %bagofgold_bank_balance%
		if (identifier.equals("bank_balance")) {
			return Tools.format(BagOfGold.getInstance().getPlayerBalanceManager().getPlayerBalance(player).getBankBalance());
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
		return "bagofgold";
	}

	@Override
	public String getVersion() {
		return "1.0.0";
	}

}
