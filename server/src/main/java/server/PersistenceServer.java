package server;

import dataaccess.*;
import service.*;

/**
 * A server implementation that uses MySQL database instead of memory
 * for persistent storage tests.
 */
public class PersistenceServer extends Server {
    
    public PersistenceServer() {
        super(true); // Call the parent constructor with persistence flag
    }
}