package dataaccess;

import model.UserData;
import java.util.HashMap;
import java.util.Map;

import org.mindrot.jbcrypt.BCrypt;

public class MemoryUserDAO implements UserDAO {
    private final Map<String, UserData> users = new HashMap<>();

    @Override
    public void clear() {
        users.clear();
    }

    @Override
    public void createUser(UserData user) throws DataAccessException {
        if (user == null || user.username() == null || user.password() == null || user.email() == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        if (users.containsKey(user.username())) {
            throw new DataAccessException("Error: already taken");
        }
        
        // Hash the password before storing
        String hashedPassword = BCrypt.hashpw(user.password(), BCrypt.gensalt());
        UserData userWithHashedPassword = new UserData(user.username(), hashedPassword, user.email());
        
        users.put(user.username(), userWithHashedPassword);
    }

    @Override
    public UserData getUser(String username) throws DataAccessException {
        if (username == null) {
            throw new DataAccessException("Error: bad request");
        }
        
        // Simply return the user from the map - no password checking here
        return users.get(username);
    }
}