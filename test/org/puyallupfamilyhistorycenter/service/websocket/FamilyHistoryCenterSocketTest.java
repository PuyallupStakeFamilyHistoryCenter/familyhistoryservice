package org.puyallupfamilyhistorycenter.service.websocket;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 *
 * @author tibbitts
 */


public class FamilyHistoryCenterSocketTest {

    /**
     * Test of handleConnection method, of class FamilyHistoryCenterSocket.
     */
    @Test
    public void testReflection() throws Exception {
        FamilyHistoryCenterSocket.class.newInstance();
    }
    
    @Test
    public void testHashPin() {
        String pin = "1234";
        String salt = FamilyHistoryCenterSocket.newSalt();
        String hashedPin = FamilyHistoryCenterSocket.hashPin(pin, salt);
        
        System.out.println("Hashed pin: " + hashedPin);
        assertTrue("Failed to validate hashed pin", FamilyHistoryCenterSocket.validatePin(pin, hashedPin));
    }
    
    @Test
    public void testHashPinWithIterations() {
        String pin = "1234";
        String salt = FamilyHistoryCenterSocket.newSalt();
        String hashedPin = FamilyHistoryCenterSocket.hashPin(pin, salt, 10000);
        
        System.out.println("Hashed pin: " + hashedPin);
        assertTrue("Failed to validate hashed pin", FamilyHistoryCenterSocket.validatePin(pin, hashedPin));
    }
}
