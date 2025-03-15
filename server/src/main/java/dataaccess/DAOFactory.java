package dataaccess;

/**
 * Factory for creating DAO objects.
 * This allows switching between memory and database implementations.
 */
public class DAOFactory {
    private static boolean useDatabase = false;
    
    /**
     * Set whether to use database implementations
     * @param useDb true to use database, false to use memory
     */
    public static void setUseDatabase(boolean useDb) {
        useDatabase = useDb;
    }
    
    /**
     * Creates a UserDAO implementation
     */
    public static UserDAO createUserDAO() throws DataAccessException {
        if (useDatabase) {
            return new MySQLUserDAO();
        }
        return new MemoryUserDAO();
    }
    
    /**
     * Creates a GameDAO implementation
     */
    public static GameDAO createGameDAO() throws DataAccessException {
        if (useDatabase) {
            return new MySQLGameDAO();
        }
        return new MemoryGameDAO();
    }
    
    /**
     * Creates an AuthDAO implementation
     */
    public static AuthDAO createAuthDAO() throws DataAccessException {
        if (useDatabase) {
            return new MySQLAuthDAO();
        }
        return new MemoryAuthDAO();
    }
}