/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence;

import net.java.sip.communicator.service.protocol.*;

import javax.swing.*;

/**
 * All the entries represented in the global status menu for protocol providers.
 * Currently the following implemented types are available:
 * - menu with option to change several statuses
 * - menu with only online and offline, for protocols that do not support
 * presence operation set
 * - readonly menu item, just showing current status
 *
 * @author Damian Minkov
 */
public interface StatusEntry
{
    /**
     * Returns the protocol provider associated with this status menu.
     * @return the protocol provider associated with this status menu
     */
    public ProtocolProviderService getProtocolProvider();

    /**
     * The component of this entry.
     * @return the component used to add to global status
     */
    public JMenuItem getEntryComponent();

    /**
     * Clears resources.
     */
    public void dispose();

    /**
     * Starts the connecting.
     */
    public void startConnecting();

    /**
     * Stops the connecting.
     */
    public void stopConnecting();

    /**
     * Returns true if the entry is currently selected (highlighted).
     *
     * @return true if the entry is selected, else false
     */
    public boolean isSelected();

    /**
     * To repaint component.
     */
    public void repaint();

    /**
     * Returns the Offline status in this selector box.
     *
     * @return the Offline status in this selector box
     */
    public PresenceStatus getOfflineStatus();

    /**
     * Returns the Online status in this selector box.
     *
     * @return the Online status in this selector box
     */
    public PresenceStatus getOnlineStatus();

    /**
     * Selects a specific <tt>PresenceStatus</tt> in this instance and the
     * <tt>ProtocolProviderService</tt> it depicts. If presence is supported.
     *
     * @param presenceStatus the <tt>PresenceStatus</tt> to be selected in this
     * instance and the <tt>ProtocolProviderService</tt> it depicts
     */
    public void updateStatus(PresenceStatus presenceStatus);
}
