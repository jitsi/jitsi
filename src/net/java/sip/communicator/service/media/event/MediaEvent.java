/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media.event;

import net.java.sip.communicator.service.media.*;

/**
 *
 * @author Symphorien Wanko
 */
public class MediaEvent
    extends java.util.EventObject
{
    /**
     * Remote user involved in the event.
     */
    String from;

    public MediaEvent(Object source)
    {
        super(source);
    }

    /**
     * Constructor for a new media event which occurs
     * inside a <tt>RtpFlow</tt>.
     *
     * @param flow the <tt>RtpFlow</tt> where the event occured.
     * @param from the origi of the event.
     */
    public MediaEvent(RtpFlow flow, String from)
    {
        super(flow);
        this.from = from;
    }

    /**
     * Return the remote name of the user which has caused this
     * event.
     */
    public String getFrom()
    {
        return from;
    }
}
