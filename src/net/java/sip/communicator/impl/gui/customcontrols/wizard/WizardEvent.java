/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.customcontrols.wizard;

import java.util.*;

public class WizardEvent
    extends EventObject
{
    private final int eventCode;
    
    /**
     * Indicates that the wizard triggering this event has finished
     * successfully.
     */
    public static final int SUCCESS = 1;

    /**
     * Indicates that the wizard was canceled.
     */
    public static final int CANCEL = 2;
    
    /**
     * Indicates that an error occured and the wizard hasn't been able to
     * finish.
     */
    public static final int ERROR = 3;
    
    /**
     * Creates a new WizardEvent according to the given source and event code.
     * 
     * @param source the source where this event occurred
     * @param eventCode the event code : SUCCESS or ERROR
     */
    public WizardEvent(Object source, int eventCode) {
        super(source);
        
        this.eventCode = eventCode;
    }
    
    /**
     * Returns the event code of this event : SUCCESS or ERRROR.
     * @return the event code of this event : SUCCESS or ERRROR
     */
    public int getEventCode()
    {
        return this.eventCode;
    }
}
