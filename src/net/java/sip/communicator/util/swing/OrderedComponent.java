/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.swing;

/**
 * Components (like buttons) implement this interface to be able to
 * order them in a Ordered Transparent Panels.
 *
 * @author Damian Minkov
 */
public interface OrderedComponent
{
    /**
     * Change component index when we want to order it.
     * @param index the button index.
     */
    public void setIndex(int index);

    /**
     * Returns the current component index we have set, or -1 if none used.
     * @return
     */
    public int getIndex();
}
