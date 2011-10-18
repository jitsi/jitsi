/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol.event;

/**
 * Notifies interested parties in <tt>ConferenceMember</tt>s sound level changes.
 * When a <tt>CallPeer</tt> is participating in the conference also as a
 * <tt>ConferenceMember</tt> its audio level would be included in the map of
 * received levels.
 *
 * @author Yana Stamcheva
 */
public interface ConferenceMembersSoundLevelListener
{
    /**
     * Indicates that a change has occurred in the sound level of some of the
     * <tt>ConferenceMember</tt>s coming from a given <tt>CallPeer</tt>. It's
     * presumed that all <tt>ConferenceMember</tt>s NOT contained in the event
     * have a 0 sound level.
     *
     * @param event the <tt>ConferenceMembersSoundLevelEvent</tt> containing
     * the new level
     */
    public void soundLevelChanged(ConferenceMembersSoundLevelEvent event);
}
