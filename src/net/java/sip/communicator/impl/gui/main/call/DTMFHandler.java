/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;

import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

/**
 * Handles DTMF sending and playing sound notifications for that.
 *
 * @author Damian Minkov
 * @author Lyubomir Marinov
 */
public class DTMFHandler
    implements KeyEventDispatcher
{
    /**
     * The <tt>Logger</tt> used by the <tt>DTMFHandler</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DTMFHandler.class);

    /**
     * The call dialog, where this handler is registered.
     */
    private final CallPanel callContainer;

    /**
     * The <tt>KeyboadFocusManager</tt> to which this instance is added as a
     * <tt>KeyEventDispatcher</tt>.
     */
    private KeyboardFocusManager keyboardFocusManager;

    /**
     * The <tt>Window</tt>s which this instance listens to for key presses and
     * releases.
     */
    private final List<Window> parents = new ArrayList<Window>();

    /**
     * If we are currently playing an audio for a DTMF tone. Used
     * to play in Loop and stop it if forced to do or new tone has come.
     */
    private SCAudioClip currentlyPlayingAudio;

    /**
     * All available tones and its properties like images for buttons, and
     * sounds to be played during send.
     */
    public static final DTMFToneInfo[] AVAILABLE_TONES
        = new DTMFToneInfo[]
                {
                    new DTMFToneInfo(
                        DTMFTone.DTMF_1,
                        KeyEvent.VK_1,
                        '1',
                        ImageLoader.ONE_DIAL_BUTTON,
                        ImageLoader.ONE_DIAL_BUTTON_MAC,
                        ImageLoader.ONE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_ONE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_2,
                        KeyEvent.VK_2,
                        '2',
                        ImageLoader.TWO_DIAL_BUTTON,
                        ImageLoader.TWO_DIAL_BUTTON_MAC,
                        ImageLoader.TWO_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_TWO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_3,
                        KeyEvent.VK_3,
                        '3',
                        ImageLoader.THREE_DIAL_BUTTON,
                        ImageLoader.THREE_DIAL_BUTTON_MAC,
                        ImageLoader.THREE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_THREE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_4,
                        KeyEvent.VK_4,
                        '4',
                        ImageLoader.FOUR_DIAL_BUTTON,
                        ImageLoader.FOUR_DIAL_BUTTON_MAC,
                        ImageLoader.FOUR_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_FOUR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_5,
                        KeyEvent.VK_5,
                        '5',
                        ImageLoader.FIVE_DIAL_BUTTON,
                        ImageLoader.FIVE_DIAL_BUTTON_MAC,
                        ImageLoader.FIVE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_FIVE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_6,
                        KeyEvent.VK_6,
                        '6',
                        ImageLoader.SIX_DIAL_BUTTON,
                        ImageLoader.SIX_DIAL_BUTTON_MAC,
                        ImageLoader.SIX_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_SIX),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_7,
                        KeyEvent.VK_7,
                        '7',
                        ImageLoader.SEVEN_DIAL_BUTTON,
                        ImageLoader.SEVEN_DIAL_BUTTON_MAC,
                        ImageLoader.SEVEN_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_SEVEN),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_8,
                        KeyEvent.VK_8,
                        '8',
                        ImageLoader.EIGHT_DIAL_BUTTON,
                        ImageLoader.EIGHT_DIAL_BUTTON_MAC,
                        ImageLoader.EIGHT_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_EIGHT),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_9,
                        KeyEvent.VK_9,
                        '9',
                        ImageLoader.NINE_DIAL_BUTTON,
                        ImageLoader.NINE_DIAL_BUTTON_MAC,
                        ImageLoader.NINE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_NINE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_A,
                        KeyEvent.VK_A,
                        'a',
                        null,
                        null,
                        null,
                        null),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_B,
                        KeyEvent.VK_B,
                        'b',
                        null,
                        null,
                        null,
                        null),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_C,
                        KeyEvent.VK_C,
                        'c',
                        null,
                        null,
                        null,
                        null),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_D,
                        KeyEvent.VK_D,
                        'd',
                        null,
                        null,
                        null,
                        null),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_STAR,
                        KeyEvent.VK_ASTERISK,
                        '*',
                        ImageLoader.STAR_DIAL_BUTTON,
                        ImageLoader.STAR_DIAL_BUTTON_MAC,
                        ImageLoader.STAR_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_STAR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_0,
                        KeyEvent.VK_0,
                        '0',
                        ImageLoader.ZERO_DIAL_BUTTON,
                        ImageLoader.ZERO_DIAL_BUTTON_MAC,
                        ImageLoader.ZERO_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_ZERO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_SHARP,
                        KeyEvent.VK_NUMBER_SIGN,
                        '#',
                        ImageLoader.DIEZ_DIAL_BUTTON,
                        ImageLoader.DIEZ_DIAL_BUTTON_MAC,
                        ImageLoader.DIEZ_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_DIEZ)
                };

    /**
     * Creates DTMF handler for a call.
     */
    public DTMFHandler()
    {
        this(null);
    }

    /**
     * Creates DTMF handler for a call.
     *
     * @param callContainer the <tt>CallContainer</tt> where this handler is
     * registered
     */
    public DTMFHandler(CallPanel callContainer)
    {
        this.callContainer = callContainer;

        if (this.callContainer != null)
        {
            final Window parent = callContainer.getCallWindow().getFrame();

            if (parent != null)
            {
                parent.addWindowListener(
                        new WindowAdapter()
                        {
                            @Override
                            public void windowClosed(WindowEvent e)
                            {
                                removeParent(parent);
                            }

                            @Override
                            public void windowOpened(WindowEvent e)
                            {
                                addParent(parent);
                            }
                        });
                if (parent.isVisible())
                    addParent(parent);
            }
        }
    }

    /**
     * Adds a <tt>Window</tt> on which key presses and releases are to be
     * monitored for the purposes of this <tt>DTMFHandler</tt>.
     *
     * @param parent the <tt>Window</tt> on which key presses and releases are
     * to be monitored for the purposes of this <tt>DTMFHandler</tt>
     */
    public void addParent(Window parent)
    {
        synchronized (parents)
        {
            if (!parents.contains(parent)
                    && parents.add(parent)
                    && (keyboardFocusManager == null))
            {
                keyboardFocusManager
                    = KeyboardFocusManager.getCurrentKeyboardFocusManager();
                keyboardFocusManager.addKeyEventDispatcher(this);
            }
        }
    }

    /**
     * Removes a <tt>Window</tt> on which key presses and releases are to no
     * longer be monitored for the purposes of this <tt>DTMFHandler</tt>.
     *
     * @param parent the <tt>Window</tt> on which key presses and releases are
     * to no longer be monitored for the purposes of this <tt>DTMFHandler</tt>
     */
    public void removeParent(Window parent)
    {
        synchronized (parents)
        {
            if (parents.remove(parent)
                    && parents.isEmpty()
                    && (keyboardFocusManager != null))
            {
                keyboardFocusManager.removeKeyEventDispatcher(this);
                keyboardFocusManager = null;
            }
        }
    }

    /**
     * Dispatches a specific <tt>KeyEvent</tt>. If one of the <tt>parents</tt>
     * registered with this <tt>DTMFHandler</tt> is focused, starts or stops
     * sending a respective DTMF tone.
     *
     * @param e the <tt>KeyEvent</tt> to be dispatched
     * @return <tt>true</tt> to stop dispatching the event or <tt>false</tt> to
     * continue dispatching it. <tt>DTMFHandler</tt> always returns
     * <tt>false</tt>
     */
    public boolean dispatchKeyEvent(KeyEvent e)
    {
        if (e.getID() == KeyEvent.KEY_TYPED)
            return false;

        /*
         * When the UI uses a single window and we do not have a callContainer,
         * we do not seem to be able to deal with the situation.
         */
        if ((GuiActivator.getUIService().getSingleWindowContainer() != null)
                && ((callContainer == null) || !callContainer.isFocusOwner()))
            return false;

        boolean dispatch = false;

        synchronized (parents)
        {
            for (int i = 0, count = parents.size(); i < count; i++)
            {
                if (parents.get(i).isFocused())
                {
                    dispatch = true;
                    break;
                }
            }
        }

        // If we are not focused, the KeyEvent was not meant for us.
        if (dispatch)
        {
            for (int i = 0; i < AVAILABLE_TONES.length; i++)
            {
                DTMFToneInfo info = AVAILABLE_TONES[i];

                if (info.keyChar == e.getKeyChar())
                {
                    switch (e.getID())
                    {
                    case KeyEvent.KEY_PRESSED:
                        startSendingDtmfTone(info);
                        break;
                    case KeyEvent.KEY_RELEASED:
                        stopSendingDtmfTone();
                        break;
                    }
                    break;
                }
            }
        }

        return false;
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param toneValue the value of the DTMF tone to send.
     */
    public void startSendingDtmfTone(String toneValue)
    {
        for (int i = 0; i < AVAILABLE_TONES.length; i++)
        {
            DTMFToneInfo info = AVAILABLE_TONES[i];

            if (info.tone.getValue().equals(toneValue))
            {
                startSendingDtmfTone(info);
                return;
            }
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param info The DTMF tone to send.
     */
    private synchronized void startSendingDtmfTone(DTMFToneInfo info)
    {
        AudioNotifierService audioNotifier = GuiActivator.getAudioNotifier();

        if(info.sound != null)
        {
            if(currentlyPlayingAudio != null)
                currentlyPlayingAudio.stop();

            currentlyPlayingAudio = audioNotifier.createAudio(info.sound);

            // some little silence, must have a non-zero or it won't loop
            if(currentlyPlayingAudio != null)
                currentlyPlayingAudio.playInLoop(10);
        }

        if (callContainer != null)
        {
            startSendingDtmfTone(
                callContainer.getCurrentCallRenderer().getCall(),
                info);
        }
        else
        {
            Collection<Call> activeCalls = CallManager.getActiveCalls();

            if (activeCalls != null)
            {
                for (Call activeCall : activeCalls)
                    startSendingDtmfTone(activeCall, info);
            }
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set of the given call.
     *
     * @param call The call to which we send DTMF-s.
     * @param info The DTMF tone to send.
     */
    private void startSendingDtmfTone(Call call, DTMFToneInfo info)
    {
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        try
        {
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                OperationSetDTMF dtmfOpSet
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetDTMF.class);

                if (dtmfOpSet != null)
                {
                    dtmfOpSet.startSendingDTMF(peer, info.tone);

                    CallPeerRenderer peerRenderer
                        = callContainer
                            .getCurrentCallRenderer()
                                .getCallPeerRenderer(peer);

                    if (peerRenderer != null)
                        peerRenderer.printDTMFTone(info.keyChar);
                }
            }
        }
        catch (Throwable t)
        {
            logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * Stop sending DTMF tone.
     */
    public synchronized void stopSendingDtmfTone()
    {
        if (currentlyPlayingAudio != null)
            currentlyPlayingAudio.stop();
        currentlyPlayingAudio = null;

        if (callContainer != null)
        {
            stopSendingDtmfTone(
                    callContainer.getCurrentCallRenderer().getCall());
        }
        else
        {
            Collection<Call> activeCalls = CallManager.getActiveCalls();

            if (activeCalls != null)
            {
                for (Call activeCall : activeCalls)
                    stopSendingDtmfTone(activeCall);
            }
        }
    }

    /**
     * Stops sending DTMF tone to the given call.
     *
     * @param call The call to which we send DTMF-s.
     */
    private void stopSendingDtmfTone(Call call)
    {
        Iterator<? extends CallPeer> callPeers = call.getCallPeers();

        try
        {
            while (callPeers.hasNext())
            {
                CallPeer peer = callPeers.next();
                OperationSetDTMF dtmfOpSet
                    = peer.getProtocolProvider().getOperationSet(
                            OperationSetDTMF.class);

                if (dtmfOpSet != null)
                    dtmfOpSet.stopSendingDTMF(peer);
            }
        }
        catch (Throwable t)
        {
            logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * DTMF extended information.
     */
    public static class DTMFToneInfo
    {
        /**
         * The tone itself
         */
        public final DTMFTone tone;

        /**
         * The key code when entered from keyboard.
         */
        public final int keyCode;

        /**
         * The char associated with this DTMF tone.
         */
        public final char keyChar;

        /**
         * The image to display in buttons sending DTMFs.
         */
        public final ImageID imageID;

        /**
         * The image to display on Mac buttons.
         */
        public final ImageID macImageID;

        /**
         * The id of the image to display on Mac buttons on rollover.
         */
        public final ImageID macImageRolloverID;

        /**
         * The sound to play during send of this tone.
         */
        public final String sound;

        /**
         * Creates DTMF extended info.
         * @param tone the tone.
         * @param keyCode its key code.
         * @param keyChar the char associated with the DTMF
         * @param imageID the image if any.
         * @param macImageID the Mac OS X-specific image if any.
         * @param macImageRolloverID the Mac OS X-specific rollover image if any
         * @param sound the sound if any.
         */
        public DTMFToneInfo(
            DTMFTone tone,
            int keyCode, char keyChar,
            ImageID imageID, ImageID macImageID, ImageID macImageRolloverID,
            String sound)
        {
            this.tone = tone;
            this.keyCode = keyCode;
            this.keyChar = keyChar;
            this.imageID = imageID;
            this.macImageID = macImageID;
            this.macImageRolloverID = macImageRolloverID;
            this.sound = sound;
        }
    }
}
