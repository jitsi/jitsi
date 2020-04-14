/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.impl.protocol.gibberish;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

/**
 * A Gibberish implementation of the <tt>CallPeer</tt> interface.
 *
 * @author Yana Stamcheva
 */
public class CallPeerGibberishImpl
    extends AbstractCallPeer<CallGibberishImpl, ProtocolProviderServiceGibberishImpl>
{
    /**
     * The sip address of this peer
     */
    private String peerAddress = null;

    /**
     * The call peer belongs to.
     */
    private CallGibberishImpl call;

    /**
     * A string uniquely identifying the peer.
     */
    private String peerID;

    /**
     * A list of listeners registered for stream user sound level events.
     */
    private final List<SoundLevelListener> soundLevelListeners
        = new Vector<SoundLevelListener>();

    /**
     * A list of listeners registered for stream user sound level events.
     */
    private final List<ConferenceMembersSoundLevelListener>
        confMemebrSoundLevelListeners
            = new Vector<ConferenceMembersSoundLevelListener>();

    /**
     * Creates an instance of <tt>CallPeerGibberishImpl</tt> by specifying the
     * call peer <tt>address</tt> and the parent <tt>owningCall</tt>.
     * @param address the address of the peer
     * @param owningCall the parent call
     */
    public CallPeerGibberishImpl(String address, CallGibberishImpl owningCall)
    {
        this.peerAddress = address;
        this.call = owningCall;

        //create the uid
        this.peerID = String.valueOf( System.currentTimeMillis())
                             + String.valueOf(hashCode());

        final Random random = new Random();

        // Make this peer a conference focus.
        if (owningCall.getCallPeerCount() > 1)
        {
            this.setConferenceFocus(true);
            final ConferenceMemberGibberishImpl member1
                = new ConferenceMemberGibberishImpl(this, "Dragancho@gibberish");
            member1.setDisplayName("Dragancho");
            member1.setState(ConferenceMemberState.CONNECTED);
            this.addConferenceMember(member1);

            final ConferenceMemberGibberishImpl member2
                = new ConferenceMemberGibberishImpl(this, "Ivancho@gibberish");
            member2.setDisplayName("Ivancho");
            member2.setState(ConferenceMemberState.CONNECTED);
            this.addConferenceMember(member2);

            Timer timer1 = new Timer(false);
            timer1.scheduleAtFixedRate(new TimerTask()
            {
                @Override
                public void run()
                {
                    fireConferenceMembersSoundLevelEvent(
                            new HashMap<ConferenceMember, Integer>()
                            {
                                {
                                    put(member1, new Integer(random.nextInt(255)));
                                    put(member2, new Integer(random.nextInt(255)));
                                }
                            });
                }
            }, 500, 100);
        }
    }

    /**
     * Returns a String locator for that peer.
     *
     * @return the peer's address or phone number.
     */
    public String getAddress()
    {
        return peerAddress;
    }

    /**
     * Returns full URI of the address.
     *
     * @return full URI of the address
     */
    public String getURI()
    {
        return "gibberish:" + peerAddress;
    }

    /**
     * Returns a reference to the call that this peer belongs to.
     *
     * @return a reference to the call containing this peer.
     */
    @Override
    public CallGibberishImpl getCall()
    {
        return call;
    }

    /**
     * Returns a human readable name representing this peer.
     *
     * @return a String containing a name for that peer.
     */
    public String getDisplayName()
    {
        return peerAddress;
    }

    /**
     * The method returns an image representation of the call peer
     * (e.g.
     *
     * @return byte[] a byte array containing the image or null if no image
     *   is available.
     */
    public byte[] getImage()
    {
        return null;
    }

    /**
     * Returns a unique identifier representing this peer.
     *
     * @return an identifier representing this call peer.
     */
    public String getPeerID()
    {
        return peerID;
    }

    /**
     * Returns the contact corresponding to this peer or null if no
     * particular contact has been associated.
     * <p>
     * @return the <tt>Contact</tt> corresponding to this peer or null
     * if no particular contact has been associated.
     */
    public Contact getContact()
    {
        /** @todo implement getContact() */
        return null;
    }

    /**
     * Returns the protocol provider that this peer belongs to.
     *
     * @return a reference to the ProtocolProviderService that this peer
     * belongs to.
     */
    @Override
    public ProtocolProviderServiceGibberishImpl getProtocolProvider()
    {
        return call.getProtocolProvider();
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addStreamSoundLevelListener(
        SoundLevelListener listener)
    {
        synchronized(soundLevelListeners)
        {
            if (!soundLevelListeners.contains(listener))
                soundLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the list of
     * listeners interested in and notified about changes in stream sound level
     * related information.
     *
     * @param listener the <tt>SoundLevelListener</tt> to remove
     */
    public void removeStreamSoundLevelListener(
        SoundLevelListener listener)
    {
        synchronized(soundLevelListeners)
        {
            soundLevelListeners.remove(listener);
        }
    }

    /**
     * Adds a specific <tt>SoundLevelListener</tt> to the list
     * of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to add
     */
    public void addConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
        synchronized(confMemebrSoundLevelListeners)
        {
            if (!confMemebrSoundLevelListeners.contains(listener))
                confMemebrSoundLevelListeners.add(listener);
        }
    }

    /**
     * Removes a specific <tt>SoundLevelListener</tt> of the
     * list of listeners interested in and notified about changes in conference
     * members sound level.
     *
     * @param listener the <tt>SoundLevelListener</tt> to
     * remove
     */
    public void removeConferenceMembersSoundLevelListener(
        ConferenceMembersSoundLevelListener listener)
    {
        synchronized(confMemebrSoundLevelListeners)
        {
            confMemebrSoundLevelListeners.remove(listener);
        }
    }

    /**
     * Fires a <tt>StreamSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param level the new sound level
     */
    void fireStreamSoundLevelEvent(int level)
    {
        SoundLevelListener[] listeners;

        synchronized(soundLevelListeners)
        {
            listeners
                = soundLevelListeners.toArray(
                        new SoundLevelListener[soundLevelListeners.size()]);
        }

        for (SoundLevelListener listener : listeners)
            listener.soundLevelChanged(this, level);
    }

    /**
     * Fires a <tt>StreamSoundLevelEvent</tt> and notifies all registered
     * listeners.
     *
     * @param levels the new sound levels
     */
    void fireConferenceMembersSoundLevelEvent(
        Map<ConferenceMember,Integer> levels)
    {
        ConferenceMembersSoundLevelEvent event
            = new ConferenceMembersSoundLevelEvent(this, levels);

        ConferenceMembersSoundLevelListener[] ls;

        synchronized(confMemebrSoundLevelListeners)
        {
            ls = confMemebrSoundLevelListeners.toArray(
                new ConferenceMembersSoundLevelListener[
                        confMemebrSoundLevelListeners.size()]);
        }

        for (ConferenceMembersSoundLevelListener listener : ls)
        {
            listener.soundLevelChanged(event);
        }
    }
}
