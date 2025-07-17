package service;

import dataaccess.DataAccess;
import dataaccess.MemoryDataAccess;
import dataaccess.DataAccessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ClearService
 * Tests the service directly without HTTP server
 */
public class ClearServiceTest {

    private DataAccess dataAccess;
    private ClearService clearService;

    @BeforeEach
    public void setUp() {
        dataAccess = new MemoryDataAccess();
        clearService = new ClearService(dataAccess);
    }

    /**
     * Positive test case: Clear should succeed and not throw exceptions
     */
    @Test
    public void testClearSuccess() {
        // This should not throw an exception
        assertDoesNotThrow(() -> {
            clearService.clear();
        });
    }

    /**
     * Positive test case: Clear should work multiple times
     */
    @Test
    public void testClearMultipleTimes() {
        // Should be able to clear multiple times without issue
        assertDoesNotThrow(() -> {
            clearService.clear();
            clearService.clear();
            clearService.clear();
        });
    }

    /**
     * Positive test case: Clear should work with fresh service instance
     */
    @Test
    public void testClearWithFreshService() {
        // Test with a new service instance
        ClearService newService = new ClearService(dataAccess);

        assertDoesNotThrow(() -> {
            newService.clear();
        });
    }
}