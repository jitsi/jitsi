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
import java.util.List;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.plugin.desktoputil.event.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.skin.*;

/**
 *
 * @author Yana Stamcheva
 */
public class CallButton
    extends SIPCommButton
    implements  Skinnable,
                TextFieldChangeListener
{
    /**
     * The history icon.
     */
    private Image image;

    /**
     * The history pressed icon.
     */
    private Image pressedImage;

    /**
     * The rollover image.
     */
    private Image rolloverImage;

    /**
     * The main application window.
     */
    private final MainFrame mainFrame;

    private final String defaultTooltip
        = GuiActivator.getResources().getI18NString(
            "service.gui.CALL_NAME_OR_NUMBER");

    /**
     * Creates the contact list call button.
     *
     * @param mainFrame the main window
     */
    public CallButton(final MainFrame mainFrame)
    {
        this.mainFrame = mainFrame;

        loadSkin();

        mainFrame.addSearchFieldListener(this);

        setToolTipText(defaultTooltip);

        addActionListener(new ActionListener()
        {
            public void actionPerformed(ActionEvent e)
            {
                List<ProtocolProviderService> telephonyProviders
                    = CallManager.getTelephonyProviders();

                String searchText = mainFrame.getCurrentSearchText();
                if (telephonyProviders.size() == 1)
                {
                    CallManager.createCall(
                        telephonyProviders.get(0),
                        searchText);
                }
                else if (telephonyProviders.size() > 1)
                {
                    ChooseCallAccountPopupMenu chooseAccountDialog
                        = new ChooseCallAccountPopupMenu(
                            CallButton.this,
                            searchText,
                            telephonyProviders);

                    chooseAccountDialog
                        .setLocation(CallButton.this.getLocation());
                    chooseAccountDialog.showPopupMenu();
                }
            }
        });
    }

    /**
     * Loads button images.
     */
    public void loadSkin()
    {
        image
            = GuiActivator.getResources()
                .getImage(ImageLoader.CALL_BUTTON_SMALL.getId()).getImage();

        pressedImage
            = GuiActivator.getResources()
                .getImage(ImageLoader.CALL_BUTTON_SMALL_PRESSED.getId())
                    .getImage();

        rolloverImage
            = GuiActivator.getResources()
                .getImage(ImageLoader.CALL_BUTTON_SMALL_ROLLOVER.getId())
                    .getImage();

        setBackgroundImage(image);
        setPressedImage(pressedImage);
        setRolloverImage(rolloverImage);
    }

    /**
     * Invoked when any text is inserted in the search field.
     */
    public void textInserted()
    {
        updateTooltip();
    }

    /**
     * Invoked when any text is removed from the search field.
     */
    public void textRemoved()
    {
        updateTooltip();
    }

    /**
     * Updates the tooltip of this button.
     */
    private void updateTooltip()
    {
        String searchText = mainFrame.getCurrentSearchText();

        if (searchText != null && searchText.length() > 0)
            setToolTipText(
                GuiActivator.getResources().getI18NString("service.gui.CALL")
                    + " " + searchText);
        else
            setToolTipText(defaultTooltip);
    }
}
