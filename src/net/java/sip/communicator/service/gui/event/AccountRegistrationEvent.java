/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.gui.event;

import java.util.*;

/**
 * The <tt>AccountRegistrationEvent</tt> indicates that an
 * <tt>AccountRegistrationWizard</tt> is added or removed from an
 * <tt>AccountRegistrationWizardContainer</tt>.
 * 
 * @author Yana Stamcheva
 */
public class AccountRegistrationEvent
    extends EventObject{

    private int eventID = -1;

    /**
     * Indicates that the AccountRegistrationEvent instance was triggered by
     * adding a new <tt>AccountRegistrationWizard</tt> to the 
     * <tt>AccountRegistrationWizardContainer</tt>.
     */
    public static final int REGISTRATION_ADDED = 1;

    /**
     * Indicates that the AccountRegistrationEvent instance was triggered by
     * the removal of an existing <tt>AccountRegistrationWizard</tt> from the 
     * <tt>AccountRegistrationWizardContainer</tt>.
     */
    public static final int REGISTRATION_REMOVED = 2;

    /**
     * Creates a new <tt>AccountRegistrationEvent</tt> according to the
     * specified parameters.
     * @param source The <tt>AccountRegistrationWizard</tt> that is added to
     * supported containers.
     * @param eventID one of the REGISTRATION_XXX static fields indicating the
     * nature of the event.
     */
    public AccountRegistrationEvent(Object source, int eventID) {
        super(source);
        this.eventID = eventID;
    }
    
    /**
     * Returns an event id specifying whether the type of this event 
     * (REGISTRATION_ADDED or REGISTRATION_REMOVED)
     * @return one of the REGISTRATION_XXX int fields of this class.
     */
    public int getEventID(){
        return eventID;
    }
}
