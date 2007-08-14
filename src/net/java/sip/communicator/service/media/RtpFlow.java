/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

/**
 * RtpFlow Interface.
 *
 * The role of a RtpFlow is simply to handle media data
 * transfert between two end points
 *
 * @author Symphorien Wanko-Tchuente
 */
public interface RtpFlow
{
    /**
     * Start transmitting and receiving data inside this flow.
     */
    public void start();

    /**
     * Stop transmission and reception of data inside this flow.
     */
    public void stop();

    /**
     * Allow to pause or resume media data transmission
     *
     * @param active pause transmission if false. Otherwise, resume.
     */
    public void setTransmit(boolean active);
}
