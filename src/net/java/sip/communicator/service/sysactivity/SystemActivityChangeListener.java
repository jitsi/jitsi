/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.sysactivity;

import net.java.sip.communicator.service.sysactivity.event.*;

import java.util.*;

/**
 * The <tt>SystemActivityChangeListener</tt> is notified any time an event
 * in the operating system occurs.
 *
 * @author Damian Minkov
 */
public interface SystemActivityChangeListener
    extends EventListener
{
    /**
     * This method gets called when a notification action for a particular event
     * type has been changed (for example the corresponding descriptor has
     * changed).
     *
     * @param event the <tt>NotificationActionTypeEvent</tt>, which is
     * dispatched when an action has been changed.
     */
    public void activityChanged(SystemActivityEvent event);
}
