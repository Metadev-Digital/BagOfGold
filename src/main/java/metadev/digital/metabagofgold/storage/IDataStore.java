package metadev.digital.metabagofgold.storage;

import metadev.digital.metabagofgold.PlayerBalance;
import metadev.digital.metabagofgold.PlayerBalances;
import metadev.digital.metacustomitemslib.storage.DataStoreException;
import metadev.digital.metacustomitemslib.storage.UserNotFoundException;
import org.bukkit.OfflinePlayer;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;

public interface IDataStore {
	/**
	 * Initialize - opening a connection to the Database and initialize the
	 * connection.
	 * 
	 * @throws DataStoreException
	 */
	void initialize() throws DataStoreException;

	/**
	 * Closing all connections to the Database
	 * 
	 * @throws DataStoreException
	 */
	void shutdown() throws DataStoreException;

	/**
	 * Get the players Balances from the Database
	 * 
	 * @param player
	 * @return
	 * @throws DataStoreException
	 * @throws SQLException
	 */
	PlayerBalances loadPlayerBalances(OfflinePlayer player) throws UserNotFoundException, DataStoreException;

	/**
	 * Save the players Balances in the Database
	 * 
	 * @param playerDataSet
	 * @throws DataStoreException
	 */
	void savePlayerBalances(Set<PlayerBalance> ps, boolean cleanCache) throws DataStoreException;

	/**
	 * Insert PlayerBalance one player into the Database
	 * 
	 * @param ps
	 * @throws DataStoreException
	 */
	void insertPlayerBalance(PlayerBalance ps) throws DataStoreException;

	/**
	 * Convert all tables to use UTF-8 character set.
	 * 
	 * @param database_name
	 * @throws DataStoreException
	 */
	void databaseConvertToUtf8(String database_name) throws DataStoreException;

	void migrateDatabaseLayoutFromV2ToV3(Connection connection) throws SQLException;
	
	List<PlayerBalance> loadTop54(int i, String worldGroup, int gamemode);

	/**
	 * Delete all players which is not known on the server.
	 */
	void databaseDeleteOldPlayers() throws DataStoreException;
	
}
