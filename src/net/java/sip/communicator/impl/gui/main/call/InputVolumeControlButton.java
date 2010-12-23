/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.neomedia.*;
import net.java.sip.communicator.service.neomedia.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

/**
 * Represents an UI means to mute the audio stream sent in an associated
 * <tt>Call</tt>.
 *
 * @author Lubomir Marinov
 * @author Yana Stamcheva
 * @author Dmitri Melnikov
 * @author Damian Minkov
 */
public class InputVolumeControlButton
    extends AbstractVolumeControlButton
    implements VolumeChangeListener,
               Runnable
{
    /**
     * The <tt>Call</tt> that this button controls.
     */
    private final Call call;

    /**
     * Mutes the call in other thread.
     */
    private Thread muteRunner;

    /**
     * Our volume control.
     */
    private VolumeControl volumeControl;

    /**
     * Current mute state.
     */
    private boolean mute = false;

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallPeer</tt>.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to have the audio stream sent to muted
     */
    public InputVolumeControlButton(Call call)
    {
        this(call, false, false);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>Call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * whose audio stream is to be muted upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public InputVolumeControlButton(Call call,
                                    boolean fullScreen,
                                    boolean selected)
    {
        this(call, ImageLoader.MUTE_BUTTON, true, fullScreen, selected);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>Call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * whose audio stream is to be muted upon performing its action
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param inSettingsPanel <tt>true</tt> when the button is used in a menu,
     * to use different background.
     */
    public InputVolumeControlButton(Call call,
                                    ImageID iconImageID,
                                    boolean fullScreen,
                                    boolean inSettingsPanel,
                                    boolean selected)
    {
        super(fullScreen, inSettingsPanel, iconImageID,
                "service.gui.MUTE_BUTTON_TOOL_TIP");

        this.call = call;

        this.mute = selected;
    }

    /**
     * Volume control used by the button.
     *
     * @return volume control used by the button.
     */
    @Override
    public VolumeControl getVolumeControl()
    {
        if(volumeControl == null)
        {
            volumeControl = GuiActivator.getMediaService()
                    .getInputVolumeControl();

            volumeControl.addVolumeChangeListener(this);
        }

        return volumeControl;
    }

    /**
     * Mutes or unmutes the associated <tt>Call</tt> upon clicking this button.
     */
    public void toggleMute()
    {
        if (muteRunner == null)
        {
            muteRunner = new Thread(this, getToolTipText());
            muteRunner.setDaemon(true);

            setEnabled(false);
            muteRunner.start();
        }
    }

    /**
     * Toggles state on call in different thread.
     */
    public void run()
    {
        try
        {
            doRun();
        }
        finally
        {
            synchronized (this)
            {
                if (Thread.currentThread().equals(muteRunner))
                {
                    muteRunner = null;
                    setEnabled(true);
                }
            }
        }
    }

    /**
     * Do the actual muting/unmuting.
     */
    private void doRun()
    {
        if (call != null)
        {
            OperationSetBasicTelephony<?> telephony
                = call.getProtocolProvider().getOperationSet(
                        OperationSetBasicTelephony.class);

            mute = !mute;
            telephony.setMute(call, mute);
        }
    }

    /**
     * Event fired when volume has changed.
     *
     * @param volumeChangeEvent the volume change event.
     */
    public void volumeChange(VolumeChangeEvent volumeChangeEvent)
    {
        if(volumeChangeEvent.getLevel() == 0)
            toggleMute();
        else if(mute)
            toggleMute();
    }
}
