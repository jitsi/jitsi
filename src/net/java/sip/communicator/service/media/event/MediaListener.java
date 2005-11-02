/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media.event;

/**
 * Allows you to register for media events.
 *
 * @author Emil Ivov
 */
public interface MediaListener
{
    public void receivedMediaStream(MediaEvent evt);

    public void mediaServiceStatusChanged();
}
