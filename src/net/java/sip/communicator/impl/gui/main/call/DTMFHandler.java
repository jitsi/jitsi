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
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.call.*;
import net.java.sip.communicator.service.notification.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.protocol.*;

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
     * All available DTMF tones and their properties such as images for buttons
     * and sounds to be played during send.
     */
    public static final DTMFToneInfo[] AVAILABLE_TONES
        = new DTMFToneInfo[]
                {
                    new DTMFToneInfo(
                        DTMFTone.DTMF_1,
                        KeyEvent.VK_1,
                        '1',
                        ImageLoader.ONE_DIAL_BUTTON,
                        ImageLoader.ONE_DIAL_BUTTON_PRESSED,
                        ImageLoader.ONE_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.ONE_DIAL_BUTTON_MAC,
                        ImageLoader.ONE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_ONE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_2,
                        KeyEvent.VK_2,
                        '2',
                        ImageLoader.TWO_DIAL_BUTTON,
                        ImageLoader.TWO_DIAL_BUTTON_PRESSED,
                        ImageLoader.TWO_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.TWO_DIAL_BUTTON_MAC,
                        ImageLoader.TWO_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_TWO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_3,
                        KeyEvent.VK_3,
                        '3',
                        ImageLoader.THREE_DIAL_BUTTON,
                        ImageLoader.THREE_DIAL_BUTTON_PRESSED,
                        ImageLoader.THREE_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.THREE_DIAL_BUTTON_MAC,
                        ImageLoader.THREE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_THREE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_4,
                        KeyEvent.VK_4,
                        '4',
                        ImageLoader.FOUR_DIAL_BUTTON,
                        ImageLoader.FOUR_DIAL_BUTTON_PRESSED,
                        ImageLoader.FOUR_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.FOUR_DIAL_BUTTON_MAC,
                        ImageLoader.FOUR_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_FOUR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_5,
                        KeyEvent.VK_5,
                        '5',
                        ImageLoader.FIVE_DIAL_BUTTON,
                        ImageLoader.FIVE_DIAL_BUTTON_PRESSED,
                        ImageLoader.FIVE_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.FIVE_DIAL_BUTTON_MAC,
                        ImageLoader.FIVE_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_FIVE),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_6,
                        KeyEvent.VK_6,
                        '6',
                        ImageLoader.SIX_DIAL_BUTTON,
                        ImageLoader.SIX_DIAL_BUTTON_PRESSED,
                        ImageLoader.SIX_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.SIX_DIAL_BUTTON_MAC,
                        ImageLoader.SIX_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_SIX),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_7,
                        KeyEvent.VK_7,
                        '7',
                        ImageLoader.SEVEN_DIAL_BUTTON,
                        ImageLoader.SEVEN_DIAL_BUTTON_PRESSED,
                        ImageLoader.SEVEN_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.SEVEN_DIAL_BUTTON_MAC,
                        ImageLoader.SEVEN_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_SEVEN),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_8,
                        KeyEvent.VK_8,
                        '8',
                        ImageLoader.EIGHT_DIAL_BUTTON,
                        ImageLoader.EIGHT_DIAL_BUTTON_PRESSED,
                        ImageLoader.EIGHT_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.EIGHT_DIAL_BUTTON_MAC,
                        ImageLoader.EIGHT_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_EIGHT),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_9,
                        KeyEvent.VK_9,
                        '9',
                        ImageLoader.NINE_DIAL_BUTTON,
                        ImageLoader.NINE_DIAL_BUTTON_PRESSED,
                        ImageLoader.NINE_DIAL_BUTTON_ROLLOVER,
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
                        null,
                        null,
                        null),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_STAR,
                        KeyEvent.VK_ASTERISK,
                        '*',
                        ImageLoader.STAR_DIAL_BUTTON,
                        ImageLoader.STAR_DIAL_BUTTON_PRESSED,
                        ImageLoader.STAR_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.STAR_DIAL_BUTTON_MAC,
                        ImageLoader.STAR_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_STAR),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_0,
                        KeyEvent.VK_0,
                        '0',
                        ImageLoader.ZERO_DIAL_BUTTON,
                        ImageLoader.ZERO_DIAL_BUTTON_PRESSED,
                        ImageLoader.ZERO_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.ZERO_DIAL_BUTTON_MAC,
                        ImageLoader.ZERO_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_ZERO),
                    new DTMFToneInfo(
                        DTMFTone.DTMF_SHARP,
                        KeyEvent.VK_NUMBER_SIGN,
                        '#',
                        ImageLoader.DIEZ_DIAL_BUTTON,
                        ImageLoader.DIEZ_DIAL_BUTTON_PRESSED,
                        ImageLoader.DIEZ_DIAL_BUTTON_ROLLOVER,
                        ImageLoader.DIEZ_DIAL_BUTTON_MAC,
                        ImageLoader.DIEZ_DIAL_BUTTON_MAC_ROLLOVER,
                        SoundProperties.DIAL_DIEZ)
                };

    /**
     * Whether we have already loaded the defaults for DTMF tones.
     */
    private static boolean defaultsLoaded = false;

    /**
     * The maximum number of milliseconds of idleness after which
     * {@link #dtmfToneNotificationThread} should die.
     */
    private static final long DTMF_TONE_NOTIFICATION_THREAD_IDLE_TIMEOUT
        = 15 * 1000;

    /**
     * Default event type for DTMF tone.
     */
    public static final String DTMF_TONE_PREFIX = "DTMFTone.";

    /**
     * The <tt>Logger</tt> used by the <tt>DTMFHandler</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(DTMFHandler.class);

    /**
     * Load the defaults for DTMF tones.
     */
    public static synchronized void loadDefaults()
    {
        if(defaultsLoaded)
            return;

        NotificationService notificationService
            = GuiActivator.getNotificationService();

        for(DTMFToneInfo info : AVAILABLE_TONES)
        {
            notificationService.registerDefaultNotificationForEvent(
                    DTMF_TONE_PREFIX + info.tone.getValue(),
                    new SoundNotificationAction(
                            info.sound,
                            0,
                            false, true, false));
        }

        defaultsLoaded = true;
    }

    /**
     * The call dialog, where this handler is registered.
     */
    private final CallPanel callContainer;

    /**
     * The list of audio DTMF tones to play.
     */
    private final List<DTMFToneInfo> dtmfToneNotifications
        = new LinkedList<DTMFToneInfo>();

    /**
     * The background/daemon <tt>Thread</tt> which plays the audio of
     * {@link #dtmfToneNotifications} as sound notifications.
     */
    private Thread dtmfToneNotificationThread;

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
     * Dispatches a specific <tt>KeyEvent</tt>. If one of the <tt>parents</tt>
     * registered with this <tt>DTMFHandler</tt> is focused, starts or stops
     * sending a respective DTMF tone.
     *
     * @param e the <tt>KeyEvent</tt> to be dispatched
     * @return <tt>true</tt> to stop dispatching the event or <tt>false</tt> to
     * continue dispatching it. <tt>DTMFHandler</tt> always returns
     * <tt>false</tt>
     */
    @Override
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
     * Runs in a background/daemon thread and consecutively plays each of the
     * {@link #dtmfToneNotifications} through the current
     * {@link NotificationService}.
     */
    private void runInDTMFToneNotificationThread()
    {
        long idleStartTime = -1;

        do
        {
            DTMFToneInfo toneToPlay;

            synchronized (dtmfToneNotifications)
            {
                if (dtmfToneNotificationThread != Thread.currentThread())
                    break;

                if (dtmfToneNotifications.isEmpty())
                {
                    toneToPlay = null;

                    long now = System.currentTimeMillis();

                    if (idleStartTime == -1)
                        idleStartTime = now;

                    long timeout
                        = DTMF_TONE_NOTIFICATION_THREAD_IDLE_TIMEOUT
                            - (now - idleStartTime);
                    if (timeout <= 0)
                    {
                        break;
                    }
                    else
                    {
                        try
                        {
                            dtmfToneNotifications.wait(timeout);
                        }
                        catch (InterruptedException ie)
                        {
                        }
                        continue;
                    }
                }
                else
                {
                    toneToPlay = dtmfToneNotifications.remove(0);
                    idleStartTime = -1;
                }
            }

            // Play the DTMF tone as a sound notification.
            if ((toneToPlay != null) && (toneToPlay.sound != null))
            {
                GuiActivator.getNotificationService().fireNotification(
                        DTMF_TONE_PREFIX + toneToPlay.tone.getValue());
            }
        }
        while (true);
    }

    /**
     * Initializes and starts {@link #dtmfToneNotificationThread} if it is
     * <tt>null</tt> and {@link #dtmfToneNotifications} is not empty.
     */
    private void startDTMFToneNotificationThreadIfNecessary()
    {
        synchronized (dtmfToneNotifications)
        {
            if((dtmfToneNotificationThread == null)
                    && !dtmfToneNotifications.isEmpty())
            {
                Thread t
                    = new Thread()
                    {
                        @Override
                        public void run()
                        {
                            try
                            {
                                runInDTMFToneNotificationThread();
                            }
                            finally
                            {
                                synchronized (dtmfToneNotifications)
                                {
                                    if (dtmfToneNotificationThread
                                            == Thread.currentThread())
                                    {
                                        dtmfToneNotificationThread = null;
                                        startDTMFToneNotificationThreadIfNecessary();
                                    }
                                }
                            }
                        }
                    };

                t.setDaemon(true);
                t.setName("DTMFHandler: DTMF tone notification player");

                boolean started = false;

                dtmfToneNotificationThread = t;
                try
                {
                    t.start();
                    started = true;
                }
                finally
                {
                    if (!started && (dtmfToneNotificationThread == t))
                        dtmfToneNotificationThread = null;
                }
            }
            else
            {
                dtmfToneNotifications.notify();
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
            if (t instanceof InterruptedException)
                Thread.currentThread().interrupt();
            else if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     *
     * @param info The DTMF tone to send.
     */
    private synchronized void startSendingDtmfTone(DTMFToneInfo info)
    {
        if(info.sound != null)
        {
            synchronized(dtmfToneNotifications)
            {
                dtmfToneNotifications.add(info);
                startDTMFToneNotificationThreadIfNecessary();
            }
        }

        Collection<Call> calls
            = (callContainer == null)
                ? CallManager.getInProgressCalls()
                : callContainer.getCallConference().getCalls();

        if ((calls != null) && !calls.isEmpty())
        {
            for (Call call : calls)
                startSendingDtmfTone(call, info);
        }
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
     * Stop sending DTMF tone.
     */
    public synchronized void stopSendingDtmfTone()
    {
        Collection<Call> calls
            = (callContainer == null)
                ? CallManager.getInProgressCalls()
                : callContainer.getCallConference().getCalls();

        if ((calls != null) &&  !calls.isEmpty())
        {
            for (Call call : calls)
                stopSendingDtmfTone(call);
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
            if (t instanceof InterruptedException)
                Thread.currentThread().interrupt();
            else if (t instanceof ThreadDeath)
                throw (ThreadDeath) t;
            else
                logger.error("Failed to send a DTMF tone.", t);
        }
    }

    /**
     * DTMF extended information.
     */
    public static class DTMFToneInfo
    {
        /**
         * The image to display in buttons sending DTMFs.
         */
        public final ImageID imageID;

        /**
         * The image to display in buttons sending DTMFs.
         */
        public final ImageID imageIDPressed;

        /**
         * The image to display in buttons sending DTMFs.
         */
        public final ImageID imageIDRollover;

        /**
         * The char associated with this DTMF tone.
         */
        public final char keyChar;

        /**
         * The key code when entered from keyboard.
         */
        public final int keyCode;

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
         * The tone itself
         */
        public final DTMFTone tone;

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
            ImageID imageID, ImageID imageIDPressed,ImageID imageIDRollover,
            ImageID macImageID, ImageID macImageRolloverID,
            String sound)
        {
            this.tone = tone;
            this.keyCode = keyCode;
            this.keyChar = keyChar;
            this.imageID = imageID;
            this.imageIDPressed = imageIDPressed;
            this.imageIDRollover = imageIDRollover;
            this.macImageID = macImageID;
            this.macImageRolloverID = macImageRolloverID;
            this.sound = sound;
        }
    }
}
