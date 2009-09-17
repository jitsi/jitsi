/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.neomedia.event;

import java.util.*;

/**
 * Defines the notification support informing about changes in the availability
 * of visual <code>Components</code> representing video such as adding and
 * removing.
 *
 * @author Lubomir Marinov
 */
public interface VideoListener
    extends EventListener
{

    /**
     * Notifies that a visual <code>Component</code> representing video has been
     * added to the provider this listener has been added to.
     *
     * @param event a <code>VideoEvent</code> describing the added visual
     *            <code>Component</code> representing video and the provider it
     *            was added into
     */
    void videoAdded(VideoEvent event);

    /**
     * Notifies that a visual <code>Component</code> representing video has been
     * removed from the provider this listener has been added to.
     *
     * @param event a <code>VideoEvent</code> describing the removed visual
     *            <code>Component</code> representing video and the provider it
     *            was removed from
     */
    void videoRemoved(VideoEvent event);
}
