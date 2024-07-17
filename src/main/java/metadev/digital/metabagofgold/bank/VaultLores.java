package metadev.digital.metabagofgold.bank;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.UUID;

public class VaultLores {

	private UUID uniqueId; // Hidden(0)

	public VaultLores(UUID uuid) {
		this.uniqueId = uuid;

	}

	/**
	 * return the hidden Lores which identifies the Item / ItemsStack
	 * 
	 * @return
	 */
	public ArrayList<String> getHiddenLores() {
		return new ArrayList<String>(Arrays.asList("Hidden(0):" + uniqueId.toString())); // uniqueId
	}

	public UUID getUniqueId() {
		return this.uniqueId;
	}

	public String toString() {
		return "{Id=" + uniqueId + "}";
	}

	public boolean equals(VaultLores lores) {
		return uniqueId.equals(lores.uniqueId);
	}

	public void save(ConfigurationSection section) {
		section.set("uniqueId", uniqueId);
	}

	public void read(ConfigurationSection section) throws InvalidConfigurationException {
		uniqueId = UUID.fromString(section.getString("uniqueId"));
	}

}
