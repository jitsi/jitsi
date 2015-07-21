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
import java.beans.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.skin.*;

/**
 * The base class for all toggle buttons which control the call from the UI.
 * Allows extending buttons to focus on performing their toggle actions.
 *
 * @author Dmitri Melnikov
 * @author Adam Netocny
 * @author Yana Stamcheva
 * @author Lyubomir Marinov
 */
public abstract class AbstractCallToggleButton
    extends SIPCommToggleButton
    implements Skinnable
{
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

        private void doRun()
        {
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
    }

    /**
     * The background image.
     */
    protected ImageID bgImageID;

    /**
     * The rollover image
     */
    protected ImageID bgRolloverImageID;

    /**
     * The <tt>Call</tt> that this button controls.
     */
    protected final Call call;

    /**
     * The <tt>CallPanel</tt> which is the current ancestor of this instance and
     * which, for example, represents the full-screen display state of this
     * instance.
     */
    private CallPanel callPanel;

    /**
     * The <tt>PropertyChangeListener</tt> which listens to {@link #callPanel}
     * about changes to the values of its properties such as
     * {@link CallContainer#PROP_FULL_SCREEN}.
     */
    private final PropertyChangeListener callPanelPropertyChangeListener
        = new PropertyChangeListener()
        {
            public void propertyChange(PropertyChangeEvent ev)
            {
                AbstractCallToggleButton.this.callPanelPropertyChange(ev);
            }
        };

    /**
     * The indicator which determines whether this instance is displayed in
     * full-screen or windowed mode.
     */
    private boolean fullScreen = false;

    /**
     * The icon image.
     */
    protected ImageID iconImageID;

    /**
     * The pressed icon image.
     */
    protected ImageID pressedIconImageID;

    /**
     * The pressed image.
     */
    protected ImageID pressedImageID;

    private final boolean settingsPanel;

    /**
     * Whether we should spawn action when clicking the button in new thread.
     * Volume control buttons use this abstract button for its fullscreen view
     * and don't need the new thread. Default is true, create new thread.
     */
    private boolean spawnActionInNewThread = true;

    /**
     * Initializes a new <tt>AbstractCallToggleButton</tt> instance which is to
     * control a toggle action for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to be controlled by the instance
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
    protected AbstractCallToggleButton(
            Call call,
            boolean settingsPanel,
            boolean selected,
            ImageID iconImageID,
            ImageID pressedIconImageID,
            String toolTipTextKey)
    {
        this.call = call;

        this.iconImageID = iconImageID;
        this.pressedIconImageID = pressedIconImageID;
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

        /*
         * AbstractCallToggleButton is dependent on whether it is displayed in
         * full-screen or windowed mode. Because AbstractCallToggleButton is
         * associated with a Call and because we know Calls are depicted by
         * CallPanels, keep track of the CallPanel in which this instance is
         * added and use the full-screen state of the CallPanel.
         */
        addHierarchyListener(
                new HierarchyListener()
                {
                    public void hierarchyChanged(HierarchyEvent ev)
                    {
                        AbstractCallToggleButton.this.hierarchyChanged(ev);
                    }
                });
        hierarchyChanged(null);

        setSelected(selected);

        // All items are now instantiated and could safely load the skin.
        loadSkin();
    }

    /**
     * Initializes a new <tt>AbstractCallToggleButton</tt> instance which is to
     * control a toggle action for a specific <tt>Call</tt>.
     *
     * @param call the <tt>Call</tt> to be controlled by the instance
     * @param selected <tt>true</tt> if the new toggle button is to be initially
     * selected; otherwise, <tt>false</tt>
     * @param iconImageID the <tt>ImageID</tt> of the image to be used as the
     * icon of the new instance
     * @param toolTipTextKey the key in the <tt>ResourceManagementService</tt>
     * of the internationalized string which is to be used as the tool tip text
     * of the new instance
     */
    protected AbstractCallToggleButton(
            Call call,
            boolean selected,
            ImageID iconImageID,
            String toolTipTextKey)
    {
        this(call, true, selected, iconImageID, null, toolTipTextKey);
    }

    /**
     * Notifies this <tt>AbstractCallToggleButton</tt> that its associated
     * action has been performed and that it should execute its very logic.
     */
    public abstract void buttonPressed();

    /**
     * Notifies this instance about a <tt>PropertyChangeEvent</tt> fired by
     * {@link #callPanel}.
     *
     * @param ev the <tt>PropertyChangeEvent</tt> fired by <tt>callPanel</tt> to
     * notify this instance about
     */
    private void callPanelPropertyChange(PropertyChangeEvent ev)
    {
        if ((ev == null)
                || CallContainer.PROP_FULL_SCREEN.equals(ev.getPropertyName()))
        {
            boolean fullScreen
                = (callPanel == null) ? false : callPanel.isFullScreen();

            if (this.fullScreen != fullScreen)
            {
                this.fullScreen = fullScreen;

                loadSkin();
            }
        }
    }

    /**
     * Notifies this instance that the UI hierarchy that it belongs to has been
     * changed.
     *
     * @param ev an <tt>HierarchyEvent</tt> which identifies the specific of the
     * change in the UI hierarchy that this instance belongs
     */
    private void hierarchyChanged(HierarchyEvent ev)
    {
        /*
         * Keep track of the CallPanel which is the current ancestor of this
         * instance and its full-screen display state.
         */

        CallPanel callPanel = null;

        for (Container parent = getParent();
                parent != null;
                parent = parent.getParent())
        {
            if (parent instanceof CallPanel)
            {
                callPanel = (CallPanel) parent;
                break;
            }
        }

        if (this.callPanel != callPanel)
        {
            if (this.callPanel != null)
            {
                this.callPanel.removePropertyChangeListener(
                        CallContainer.PROP_FULL_SCREEN,
                        callPanelPropertyChangeListener);
            }

            this.callPanel = callPanel;

            if (this.callPanel != null)
            {
                this.callPanel.addPropertyChangeListener(
                        CallContainer.PROP_FULL_SCREEN,
                        callPanelPropertyChangeListener);
            }

            /*
             * If the callPanel instance has changed, then the full-screen
             * display state that it represents may have changed from the point
             * of view of this instance.
             */
            callPanelPropertyChange(null);
        }
    }

    /**
     * Determines whether this instance is displayed in full-screen or windowed
     * mode.
     *
     * @return <tt>true</tt> if this instance is displayed in full-screen mode
     * or <tt>false</tt> for windowed mode
     */
    protected boolean isFullScreen()
    {
        return fullScreen;
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

        boolean fullScreen = isFullScreen();

        if (iconImageID != null)
        {
            Image iconImage = ImageLoader.getImage(iconImageID);

            if (!fullScreen && !settingsPanel)
            {
                iconImage
                    = ImageUtils.scaleImageWithinBounds(iconImage, 18, 18);
            }
            setIconImage(iconImage);
        }

        if (pressedIconImageID != null)
        {
            Image iconImage = ImageLoader.getImage(pressedIconImageID);

            if (!fullScreen && !settingsPanel)
            {
                iconImage
                    = ImageUtils.scaleImageWithinBounds(iconImage, 18, 18);
            }
            setPressedIconImage(iconImage);
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

        Image iconImage = ImageLoader.getImage(iconImageID);

        if (!isFullScreen() && !settingsPanel)
            iconImage = ImageUtils.scaleImageWithinBounds(iconImage, 18, 18);
        setIconImage(iconImage);
    }

    /**
     * Changes behavior, whether we should start new thread for executing
     * actions, buttonPressed().
     *
     * @param spawnActionInNewThread
     */
    public void setSpawnActionInNewThread(boolean spawnActionInNewThread)
    {
        this.spawnActionInNewThread = spawnActionInNewThread;
    }
}
