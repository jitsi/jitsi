/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.media;

import net.java.sip.communicator.service.media.event.*;

/**
 * RtpFlow Interface.
 *
 * The role of a RtpFlow is simply to handle media data transfer between two 
 * end points as well as playback and capture.
 *
 * @author Symphorien Wanko
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
     * Gives the local port used by this flow
     *
     * @return the local port used by this flow
     */
    public int getLocalPort();

    /**
     * Gives the local address used by this flow
     *
     * @return the local address used by this flow
     */
    public String getLocalAddress();

    /**
     * Gives the remote port used by this flow
     *
     * @return the remote port used by this flow
     */
    public int getRemotePort();

    /**
     * Gives the remote address used by this flow
     *
     * @return the remote address used by this flow
     */
    public String getRemoteAddress();

    /**
     * Pause transmission on this flow
     */
    public void pause();

    /**
     * Resume transmission on this flow
     */
    public void resume();
    
    /**
     * Add a listener to be informed when there of media events.
     */
    public void addMediaListener(MediaListener listener);
}
