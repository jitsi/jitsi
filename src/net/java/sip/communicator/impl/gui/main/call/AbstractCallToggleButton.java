/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The base class for all toggle buttons which control the call from the UI.
 * Allows extending buttons to focus on performing their toggle actions.
 *
 * @author Dmitri Melnikov
 * @author Adam Netocny
 * @author Yana Stamcheva
 */
public abstract class AbstractCallToggleButton
    extends SIPCommToggleButton
    implements Skinnable
{
    /**
     * The <tt>Call</tt> that this button controls.
     */
    protected final Call call;

    /**
     * The background image.
     */
    protected ImageID bgImageID;

    /**
     * The rollover image
     */
    protected ImageID bgRolloverImageID;

    /**
     * The pressed image.
     */
    protected ImageID pressedImageID;

    /**
     * The icon image.
     */
    protected ImageID iconImageID;

    /**
     * The pressed icon image.
     */
    protected ImageID pressedIconImageID;

    /**
     * Whether we should spawn action when clicking the button in new thread.
     * Volume control buttons use this abstract button for its fullscreen view
     * and don't need the new thread. Default is true, create new thread.
     */
    private boolean spawnActionInNewThread = true;

    private final boolean fullScreen;

    private final boolean settingsPanel;

    /**
     * Initializes a new <tt>AbstractCallToggleButton</tt> instance which is to
     * control a toggle action for a specific <tt>Call</tt>.
     * 
     * @param call the <tt>Call</tt> to be controlled by the instance
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param iconImageID the <tt>ImageID</tt> of the image to be used as the
     * icon of the new instance
     * @param toolTipTextKey the key in the <tt>ResourceManagementService</tt>
     * of the internationalized string which is to be used as the tool tip text
     * of the new instance
     */
    public AbstractCallToggleButton(
            Call call,
            boolean fullScreen,
            boolean selected,
            ImageID iconImageID,
            String toolTipTextKey)
    {
        this(   call,
                fullScreen,
                true,
                selected,
                iconImageID,
                null,
                toolTipTextKey);
    }

    /**
     * Initializes a new <tt>AbstractCallToggleButton</tt> instance which is to
     * control a toggle action for a specific <tt>Call</tt>.
     * 
     * @param call the <tt>Call</tt> to be controlled by the instance
     * @param fullScreen <tt>true</tt> if the new instance is to be used in
     * full-screen UI; otherwise, <tt>false</tt>
     * @param settingsPanel indicates if this button is added in the settings
     * panel on the bottom of the call window
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param iconImageID the <tt>ImageID</tt> of the image to be used as the
     * icon of the new instance
     * @param pressedIconImageID the <tt>ImageID</tt> of the image to be used
     * as the icon in the pressed button state of the new instance
     * @param toolTipTextKey the key in the <tt>ResourceManagementService</tt>
     * of the internationalized string which is to be used as the tool tip text
     * of the new instance
     */
    public AbstractCallToggleButton(
            Call call,
            boolean fullScreen,
            boolean settingsPanel,
            boolean selected,
            ImageID iconImageID,
            ImageID pressedIconImageID,
            String toolTipTextKey)
    {
        this.call = call;

        this.iconImageID = iconImageID;
        this.pressedIconImageID = pressedIconImageID;
        this.fullScreen = fullScreen;
        this.settingsPanel = settingsPanel;

        if(settingsPanel)
        {
            bgRolloverImageID = ImageLoader.CALL_SETTING_BUTTON_BG;
            pressedImageID = ImageLoader.CALL_SETTING_BUTTON_PRESSED_BG;
        }
        else
        {
            bgImageID = ImageLoader.SOUND_SETTING_BUTTON_BG;
            bgRolloverImageID = ImageLoader.SOUND_SETTING_BUTTON_BG;
            pressedImageID = ImageLoader.SOUND_SETTING_BUTTON_PRESSED;

        }

        if (toolTipTextKey != null)
        {
            setToolTipText(
                GuiActivator.getResources().getI18NString(toolTipTextKey));
        }

        setModel(new CallToggleButtonModel(call));

        setSelected(selected);

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * Changes behaviour, whether we should start new thread for executing
     * actions, buttonPressed().
     * @param spawnActionInNewThread
     */
    public void setSpawnActionInNewThread(boolean spawnActionInNewThread)
    {
        this.spawnActionInNewThread = spawnActionInNewThread;
    }

    /**
     * The button model of this call toggle button.
     */
    private class CallToggleButtonModel
        extends ToggleButtonModel
        implements ActionListener,
                   Runnable
    {
        private Thread runner;

        public CallToggleButtonModel(Call call)
        {
            addActionListener(this);
        }

        public synchronized void actionPerformed(ActionEvent event)
        {
            if(spawnActionInNewThread)
            {
                if (runner == null)
                {
                    runner = new Thread(this, LocalVideoButton.class.getName());
                    runner.setDaemon(true);

                    setEnabled(false);
                    runner.start();
                }
            }
            else
                buttonPressed();
        }

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
                    if (Thread.currentThread().equals(runner))
                    {
                        runner = null;
                        setEnabled(true);
                    }
                }
            }
        }

        private void doRun()
        {
            buttonPressed();
        }
    }

    /**
     * Loads images.
     */
    public void loadSkin()
    {
        int width = CallToolBarButton.DEFAULT_WIDTH;
        int height = CallToolBarButton.DEFAULT_HEIGHT;

        if (bgImageID != null)
        {
            Image bgImage = ImageLoader.getImage(bgImageID);
            setBgImage(bgImage);

            width = bgImage.getWidth(this);
            height = bgImage.getHeight(this);
        }

        setPreferredSize(new Dimension(width, height));
        setMaximumSize(new Dimension(width, height));
        setMinimumSize(new Dimension(width, height));

        setBgRolloverImage(ImageLoader.getImage(bgRolloverImageID));
        setPressedImage(ImageLoader.getImage(pressedImageID));

        if (iconImageID != null)
        {
            if (!fullScreen && !settingsPanel)
                setIconImage(ImageUtils.scaleImageWithinBounds(
                    ImageLoader.getImage(iconImageID), 18, 18));
            else
                setIconImage(ImageLoader.getImage(iconImageID));
        }

        if (pressedIconImageID != null)
        {
            if (!fullScreen && !settingsPanel)
                setPressedIconImage(ImageUtils.scaleImageWithinBounds(
                    ImageLoader.getImage(pressedIconImageID), 18, 18));
            else
                setPressedIconImage(ImageLoader.getImage(pressedIconImageID));
        }
    }

    /**
     * Sets the icon image of this button.
     *
     * @param iconImageID the identifier of the icon image
     */
    public void setIconImageID(ImageID iconImageID)
    {
        this.iconImageID = iconImageID;

        if (!fullScreen && !settingsPanel)
            setIconImage(ImageUtils.scaleImageWithinBounds(
                ImageLoader.getImage(iconImageID), 18, 18));
        else
            setIconImage(ImageLoader.getImage(iconImageID));
    }

    /**
     * Notifies this <tt>AbstractCallToggleButton</tt> that its associated
     * action has been performed and that it should execute its very logic.
     */
    public abstract void buttonPressed();
}
