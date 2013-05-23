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
import java.util.Timer;

import javax.swing.*;
import javax.swing.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.event.*;

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
    extends AbstractCallToggleButton
    implements VolumeChangeListener,
               Runnable
{
    /**
     * Mutes the call in other thread.
     */
    private Thread muteRunner;

    /**
     * Our volume control.
     */
    private final VolumeControl volumeControl;

    /**
     * The slider popup menu.
     */
    private final JPopupMenu sliderMenu;

    /**
     * Current mute state.
     */
    private boolean mute = false;

    private boolean sliderMenuIsVisible = false;

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>CallPeer</tt>.
     *
     * @param call the <tt>Call</tt> to be associated with the new instance and
     * to have the audio stream sent to muted
     */
    public InputVolumeControlButton(Call call)
    {
        this(call, false);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>Call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * whose audio stream is to be muted upon performing its action
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     */
    public InputVolumeControlButton(Call call,
                                    boolean selected)
    {
        this(   call,
                ImageLoader.MICROPHONE,
                ImageLoader.MUTE_BUTTON,
                true,
                selected);
    }

    /**
     * Initializes a new <tt>MuteButton</tt> instance which is to mute the audio
     * stream to a specific <tt>Call</tt>.
     *
     * @param call  the <tt>Call</tt> to be associated with the new instance and
     * whose audio stream is to be muted upon performing its action
     * @param iconImageID the icon image
     * @param pressedIconImageID the <tt>ImageID</tt> of the image to be used
     * as the icon in the pressed button state of the new instance
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param inSettingsPanel <tt>true</tt> when the button is used in a menu,
     * to use different background.
     */
    public InputVolumeControlButton(Call call,
                                    ImageID iconImageID,
                                    ImageID pressedIconImageID,
                                    boolean inSettingsPanel,
                                    boolean selected)
    {
        super(  call,
                inSettingsPanel,
                selected,
                iconImageID,
                pressedIconImageID,
                "service.gui.MUTE_BUTTON_TOOL_TIP");

        this.mute = selected;

        volumeControl = getVolumeControl();

        // Creates the menu that would contain the volume control component.
        sliderMenu
            = new VolumeControlSlider(volumeControl, JSlider.VERTICAL)
                .getPopupMenu();
        sliderMenu.setInvoker(this);

        addMouseListener(new MouseAdapter()
        {
            TimerTask timerTask;
            @Override
            public void mousePressed(MouseEvent mouseevent)
            {
                Timer timer = new Timer();
                timerTask = new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        showSliderMenu();
                    }
                };

                timer.schedule(timerTask, 1000);
            }

            @Override
            public void mouseReleased(MouseEvent mouseevent)
            {
                if (!sliderMenuIsVisible)
                    timerTask.cancel();
                else
                    setSelected(!isSelected());
            }
        });
    }

    /**
     * Volume control used by the button.
     *
     * @return volume control used by the button.
     */
    private VolumeControl getVolumeControl()
    {
        VolumeControl volumeControl
            = GuiActivator.getMediaService().getInputVolumeControl();

        volumeControl.addVolumeChangeListener(this);
        return volumeControl;
    }

    private void showSliderMenu()
    {
        Point location = new Point(getX(), getY() + getHeight());

        SwingUtilities.convertPointToScreen(location,
                InputVolumeControlButton.this.getParent());

        if(isFullScreen())
        {
            location.setLocation(
                    location.getX(),
                    location.getY()
                        - sliderMenu.getPreferredSize().getHeight()
                        - getHeight());
        }

        sliderMenu.setLocation(location);

        sliderMenu.addPopupMenuListener(
                new PopupMenuListener()
                {
                    public void popupMenuWillBecomeVisible(PopupMenuEvent ev)
                    {
                        sliderMenuIsVisible = true;
                    }

                    public void popupMenuWillBecomeInvisible(PopupMenuEvent ev)
                    {
                        sliderMenuIsVisible = false;
                    }

                    public void popupMenuCanceled(PopupMenuEvent ev)
                    {
                    }
                });

        sliderMenu.setVisible(!sliderMenu.isVisible());
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

            // We make sure that the button state corresponds to the mute state.
            setSelected(mute);

            // If we unmute the microphone and the volume control is set to min,
            // make sure that the volume control is restored to the initial
            // state.
            if (!mute
                && volumeControl.getVolume() == volumeControl.getMinValue())
            {
                volumeControl.setVolume(
                    (volumeControl.getMaxValue()
                        - volumeControl.getMinValue())/2);
            }
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

    /**
     * Notifies this <tt>AbstractCallToggleButton</tt> that its associated
     * action has been performed and that it should execute its very logic.
     */
    @Override
    public void buttonPressed()
    {
        if (!sliderMenuIsVisible)
            toggleMute();
    }
}
