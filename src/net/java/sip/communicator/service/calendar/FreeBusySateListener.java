/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.calendar;

/**
 * A interface for listener that listens for calendar free busy status changes.
 * @author Hristo Terezov
 */
public interface FreeBusySateListener
{
    /**
     * A method that is called when the free busy status is changed.
     * @param oldStatus the old value of the status.
     * @param newStatus the new value of the status.
     */
    public void onStatusChanged(CalendarService.BusyStatusEnum oldStatus, 
        CalendarService.BusyStatusEnum newStatus);
}
