package dataaccess;

public class DAOFactory {
    private static boolean useDatabase = false;
    
    public static void setUseDatabase(boolean useDb) {
        useDatabase = useDb;
    }
    
    public static UserDAO createUserDAO() throws DataAccessException {
        if (useDatabase) {
            try {
                return new MySQLUserDAO();
            } catch (Exception e) {
                throw new DataAccessException("Error creating MySQL user DAO: " + e.getMessage());
            }
        }
        return new MemoryUserDAO();
    }
    
    public static GameDAO createGameDAO() throws DataAccessException {
        if (useDatabase) {
            try {
                return new MySQLGameDAO();
            } catch (Exception e) {
                throw new DataAccessException("Error creating MySQL game DAO: " + e.getMessage());
            }
        }
        return new MemoryGameDAO();
    }
    
    public static AuthDAO createAuthDAO() throws DataAccessException {
        if (useDatabase) {
            try { 
                return new MySQLAuthDAO();
            } catch (Exception e) {
                throw new DataAccessException("Error creating MySQL auth DAO: " + e.getMessage());
            }
        }
        return new MemoryAuthDAO();
    }
}