/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

import java.util.*;

/**
 * SecureEventListener interface extends EventListener
 * This is the listener interface used to handle an event
 * related with a change in security status. It is required 
 * to be implemented by a CallSession for this purpose. 
 * The change in security status is triggered at the GUI level
 * by the user, who can toggle on or off securing the communication.
 * This modifies the current security status static indicator for
 * the call sessions, action that is the one which actually triggers
 * sending the changed secure event to the current call sessions in 
 * progress. 
 * There are two different cases related to when the user could
 * toggle secure communication on and off at the GUI level.
 *
 * 1. No active call session is in progress and 
 *    subsequently no listeners are registered.
 *    Result: Only the usingSRTP static general status of security
 *            is changed and any following call sessions will use
 *            that as default for going secure or not from the start.
 * 
 * 2. Active call sessions (actually one according to the current 
 *    media service implementation) are in progress and have 
 *    registered their listeners.
 *    Result: The usingSRTP default start status is changed and 
 *            triggers successfully the changed secure status event 
 *            sending to the call sessions, which handle it modifying
 *            the session secure state (going secure or back to normal 
 *            state during the actual session) according to the used key 
 *            management system.  
 *     
 * @author Emanuel Onica (eonica@info.uaic.ro)
 */
public interface SecureEventListener 
    extends EventListener
{
    
    /**
     * The handler for the secure event received.
     * The secure event represents an indication of change in the secure 
     * communication status of the implementor of this interface.
     * The implementor should modify it's internal state if supported.
     * 
     * @param secureEvent the secureEvent received
     */
    public void secureStatusChanged(SecureEvent secureEvent);
}
