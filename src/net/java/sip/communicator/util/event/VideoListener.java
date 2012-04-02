/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.event;

import java.util.*;

/**
 * Defines the notification support informing about changes in the availability
 * of visual <tt>Component</tt>s representing video such as adding and
 * removing.
 *
 * @author Lyubomir Marinov
 */
public interface VideoListener
    extends EventListener
{

    /**
     * Notifies that a visual <tt>Component</tt> representing video has been
     * added to the provider this listener has been added to.
     *
     * @param event a <tt>VideoEvent</tt> describing the added visual
     * <tt>Component</tt> representing video and the provider it was added into
     */
    void videoAdded(VideoEvent event);

    /**
     * Notifies that a visual <tt>Component</tt> representing video has been
     * removed from the provider this listener has been added to.
     *
     * @param event a <tt>VideoEvent</tt> describing the removed visual
     * <tt>Component</tt> representing video and the provider it was removed
     * from
     */
    void videoRemoved(VideoEvent event);

    /**
     * Notifies about an update to a visual <tt>Component</tt> representing
     * video.
     *
     * @param event a <tt>VideoEvent</tt> describing the visual
     * <tt>Component</tt> related to the update and the details of the specific
     * update
     */
    void videoUpdate(VideoEvent event);
}
