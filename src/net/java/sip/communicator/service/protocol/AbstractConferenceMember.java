/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * Provides the default implementation of the <code>ConferenceMember</code>
 * interface.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 */
public class AbstractConferenceMember
    extends PropertyChangeNotifier
    implements ConferenceMember
{

    /**
     * The <code>CallPeer</code> which is the conference focus of this
     * <code>ConferenceMember</code>.
     */
    private final CallPeer conferenceFocusCallPeer;

    /**
     * The user-friendly display name of this <code>ConferenceMember</code> in
     * the conference.
     */
    private String displayName;

    /**
     * The state of the device and signaling session of this
     * <code>ConferenceMember</code> in the conference.
     */
    private ConferenceMemberState state = ConferenceMemberState.UNKNOWN;

    /**
     * The <tt>CallPeerSoundLevelListener</tt>-s registered to get
     * <tt>CallPeerSoundLevelEvent</tt>-s
     */
    private final List<CallPeerSoundLevelListener> soundLevelListeners
        = new ArrayList<CallPeerSoundLevelListener>();

    public AbstractConferenceMember(
        CallPeer conferenceFocusCallPeer)
    {
        this.conferenceFocusCallPeer = conferenceFocusCallPeer;
    }

    /*
     * Implements ConferenceMember#getConferenceFocusCallPeer().
     */
    public CallPeer getConferenceFocusCallPeer()
    {
        return conferenceFocusCallPeer;
    }

    /*
     * Implement ConferenceMember#getDisplayName().
     */
    public String getDisplayName()
    {
        return displayName;
    }

    /*
     * Implements ConferenceMember#getState().
     */
    public ConferenceMemberState getState()
    {
        return state;
    }

    /**
     * Sets the user-friendly display name of this <code>ConferenceMember</code>
     * in the conference and fires a new <code>PropertyChangeEvent</code> for
     * the property <code>#DISPLAY_NAME_PROPERTY_NAME</code>.
     *
     * @param displayName
     *            the user-friendly display name of this
     *            <code>ConferenceMember</code> in the conference
     */
    public void setDisplayName(String displayName)
    {
        if (((this.displayName == null) && (displayName != null))
                || ((this.displayName != null)
                        && !this.displayName.equals(displayName)))
        {
            String oldValue = this.displayName;

            this.displayName = displayName;

            firePropertyChange(
                DISPLAY_NAME_PROPERTY_NAME,
                oldValue,
                this.displayName);
        }
    }

    /**
     * Sets the state of the device and signaling session of this
     * <code>ConferenceMember</code> in the conference and fires a new
     * <code>PropertyChangeEvent</code> for the property
     * <code>#STATE_PROPERTY_NAME</code>.
     *
     * @param state
     *            the state of the device and signaling session of this
     *            <code>ConferenceMember</code> in the conference
     */
    public void setState(ConferenceMemberState state)
    {
        if (this.state != state)
        {
            ConferenceMemberState oldValue = this.state;

            this.state = state;

            firePropertyChange(STATE_PROPERTY_NAME, oldValue, this.state);
        }
    }

    /**
     * Adds a specific <tt>CallPeerSoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     * 
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to add
     */
    public void addCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
        synchronized (soundLevelListeners)
        {
            soundLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>CallPeerSoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in sound level related
     * information.
     * 
     * @param listener the <tt>CallPeerSoundLevelListener</tt> to remove
     */
    public void removeCallPeerSoundLevelListener(
        CallPeerSoundLevelListener listener)
    {
        synchronized (soundLevelListeners)
        {
            soundLevelListeners.remove(listener);
        }
    }

    /**
     * Fires a <tt>CallPeerSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param level The new sound level
     */
    public void fireCallPeerSoundLevelEvent(int level)
    {
        CallPeerSoundLevelEvent event
            = new CallPeerSoundLevelEvent(this, level);

        CallPeerSoundLevelListener[] listeners;

        synchronized(soundLevelListeners)
        {
            listeners =
                soundLevelListeners.toArray(
                    new CallPeerSoundLevelListener[soundLevelListeners.size()]);
        }

        for (CallPeerSoundLevelListener listener : listeners)
        {
            listener.peerSoundLevelChanged(event);
        }
    }
}
