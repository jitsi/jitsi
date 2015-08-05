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

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.account.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.skin.*;

import org.jitsi.service.neomedia.*;
import org.jitsi.service.neomedia.device.*;
import org.jitsi.util.*;
import org.jitsi.util.swing.TransparentPanel;

import com.explodingpixels.macwidgets.*;

/**
 * The <tt>PreCallDialog</tt> is a dialog allowing to pick-up or hangup a call.
 * This is the parent dialog of the <tt>ReceivedCallDialog</tt> and the
 * <tt>ChooseCallAccountDialog</tt>.
 *
 * @author Yana Stamcheva
 */
public abstract class PreCallDialog
    implements ActionListener,
               Skinnable
{
    /**
     * List of PreCallDialog windows that are currently visible to the user.
     */
    private static final List<Window> visibleWindows = new ArrayList<Window>();

    /**
     * The call button name.
     */
    private static final String CALL_BUTTON = "CallButton";

    /**
     * The conference call button name.
     */
    private static final String CONF_CALL_BUTTON = "ConfCallButton";

    /**
     * The call button name.
     */
    private static final String VIDEO_CALL_BUTTON = "VideoCallButton";

    /**
     * The hangup button name.
     */
    private static final String HANGUP_BUTTON = "HangupButton";

    /**
     * Call button.
     */
    private SIPCommButton callButton;

    /**
     * Conference call button to answer the call in an existing call or
     * conference.
     */
    private SIPCommButton mergeCallButton;

    /**
     * Video call button.
     */
    private SIPCommButton videoCallButton;

    /**
     * HandUp button.
     */
    private SIPCommButton hangupButton;

    /**
     * Call label for display name.
     */
    private JLabel callLabelDisplayName;

    /**
     * Call label for address.
     */
    private JLabel callLabelAddress;

    /**
     * Call label for account.
     */
    private JLabel callLabelAccount;

    /**
     * The label that will contain the peer image.
     */
    private JLabel callLabelImage;

    /**
     * The combo box containing a list of accounts to choose from.
     */
    private JComboBox accountsCombo;

    /**
     * The window handling received calls.
     */
    private final Window preCallWindow;

    /**
     * If it is a video call.
     */
    private final boolean video;

    /**
     * If the call should be answered in an existing call.
     */
    private final boolean mergeCall;

    /**
     * Creates an instance of <tt>PreCallDialog</tt> by specifying the dialog
     * title.
     *
     * @param title the title of the dialog
     * @param video if it is a video call
     * @param existingCall true to answer call in an existing call
     */
    public PreCallDialog(String title, boolean video, boolean existingCall)
    {
        this(title, null, null, video, existingCall);
    }

    /**
     * Creates an instance of <tt>PreCallDialog</tt> by specifying the dialog
     * title and the text to show.
     *
     * @param title the title of the dialog
     * @param text the text to show
     * @param accounts the list of accounts to choose from
     */
    public PreCallDialog(String title, String text, Account[] accounts)
    {
        this(title, text, accounts, false, false);
    }

    /**
     * Creates an instance of <tt>PreCallDialog</tt> by specifying the dialog
     * title and the text to show.
     *
     * @param title the title of the dialog
     * @param text the text to show
     * @param accounts the list of accounts to choose from
     * @param video if it is a video call
     * @param existingCall true to answer call in an existing call
     */
    public PreCallDialog(String title, String text, Account[] accounts,
        boolean video, boolean existingCall)
    {
        preCallWindow = createPreCallWindow(title, text, accounts);

        if (video)
        {
            // Make sure there is a VIDEO MediaDevice and it is capable of
            // capture/sending
            MediaDevice mediaDevice
                = GuiActivator.getMediaService().getDefaultDevice(
                        MediaType.VIDEO,
                        MediaUseCase.CALL);

            if ((mediaDevice == null)
                    || !mediaDevice.getDirection().allowsSending())
                video = false;
        }

        this.video = video;
        this.mergeCall = existingCall;

        initComponents();
    }

    /**
     * Creates this received call window.
     *
     * @param title the title of the created window
     * @param text the text to show
     * @param accounts the list of accounts to choose from
     * @return the created window
     */
    private Window createPreCallWindow( String title,
                                        String text,
                                        Account[] accounts)
    {
        Window receivedCallWindow = null;

        if (OSUtils.IS_MAC)
        {
            HudWindow window = new HudWindow();
            window.hideCloseButton();

            JDialog dialog = window.getJDialog();
            dialog.setUndecorated(true);
            dialog.setTitle(title);

            receivedCallWindow = window.getJDialog();

            callLabelDisplayName = HudWidgetFactory.createHudLabel("");
            callLabelAddress = HudWidgetFactory.createHudLabel("");
            callLabelImage = HudWidgetFactory.createHudLabel("");

            if (accounts != null)
            {
                accountsCombo
                    = HudWidgetFactory.createHudComboBox(
                            new DefaultComboBoxModel(accounts));
            }
            else
                callLabelAccount = HudWidgetFactory.createHudLabel("");
        }
        else
        {
            SIPCommFrame frame = new SIPCommFrame(false);

            frame.setUndecorated(true);

            receivedCallWindow = frame;

            callLabelDisplayName = new JLabel();
            callLabelAddress = new JLabel();
            callLabelImage = new JLabel();

            if (accounts != null)
                accountsCombo = new JComboBox(accounts);
            else
                callLabelAccount = new JLabel();
        }

        if (text != null)
            callLabelDisplayName.setText(text);

        receivedCallWindow.setAlwaysOnTop(true);

        // prevents dialog window to get unwanted key events and when going
        // on top on linux, it steals focus and if we are accidently
        // writing something and pressing enter a call get answered
        receivedCallWindow.setFocusableWindowState(false);

        return receivedCallWindow;
    }

    /**
     * Initializes all components in this panel.
     */
    private void initComponents()
    {
        JPanel mainPanel = new TransparentPanel(new BorderLayout(10, 0));

        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // disable html rendering
        callLabelDisplayName.putClientProperty("html.disable", Boolean.TRUE);
        callLabelAddress.putClientProperty("html.disable", Boolean.TRUE);
        callLabelImage.putClientProperty("html.disable", Boolean.TRUE);

        if(callLabelAccount != null)
            callLabelAccount.putClientProperty("html.disable", Boolean.TRUE);

        JComponent buttonsPanel = new CallToolBar(false, true);

        callButton = new SIPCommButton();

        if(mergeCall)
            mergeCallButton = new SIPCommButton();

        if(video)
            videoCallButton = new SIPCommButton();

        hangupButton = new SIPCommButton();

        callButton.setName(CALL_BUTTON);
        hangupButton.setName(HANGUP_BUTTON);

        callButton.addActionListener(this);

        if(mergeCall)
        {
            mergeCallButton.setName(CONF_CALL_BUTTON);
            mergeCallButton.addActionListener(this);
        }

        hangupButton.addActionListener(this);

        if(video)
        {
            videoCallButton.setName(VIDEO_CALL_BUTTON);
            videoCallButton.addActionListener(this);
        }

        preCallWindow.add(mainPanel);

        mainPanel.add(callLabelImage, BorderLayout.WEST);

        JPanel labelsPanel = new TransparentPanel();
        labelsPanel.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        labelsPanel.setLayout(new BoxLayout(labelsPanel, BoxLayout.Y_AXIS));
        callLabelDisplayName.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        labelsPanel.add(callLabelDisplayName);

        labelsPanel.add(Box.createVerticalStrut(3));
        callLabelAddress.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        labelsPanel.add(callLabelAddress);

        if(callLabelAccount != null)
        {
            labelsPanel.add(Box.createVerticalStrut(3));
            callLabelAccount.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            labelsPanel.add(callLabelAccount);
        }

        if (accountsCombo != null)
        {
            labelsPanel.add(Box.createVerticalStrut(3));
            accountsCombo.setAlignmentX(JLabel.LEFT_ALIGNMENT);
            labelsPanel.add(accountsCombo);
        }

        mainPanel.add(labelsPanel, BorderLayout.CENTER);

        // Loads skin resources.
        loadSkin();

        JPanel rightPanel = new TransparentPanel();
        rightPanel.setLayout(new BoxLayout(rightPanel, BoxLayout.Y_AXIS));

        rightPanel.setBorder(BorderFactory.createEmptyBorder(6, 0, 0, 0));

        buttonsPanel.add(callButton);

        if(mergeCall)
            buttonsPanel.add(mergeCallButton);

        if(video)
            buttonsPanel.add(videoCallButton);

        buttonsPanel.add(hangupButton);

        buttonsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        rightPanel.add(Box.createVerticalGlue());
        rightPanel.add(buttonsPanel);
        rightPanel.add(Box.createVerticalGlue());
        mainPanel.add(rightPanel, BorderLayout.EAST);
    }

    /**
     * Packs the content of this dialog.
     */
    public void pack()
    {
        preCallWindow.pack();
    }

    /**
     * Shows/hides this dialog.
     *
     * Synchronized to ensure that only one window can update the global
     * visibleWindows datastructure at a time.
     *
     * @param isVisible indicates if this dialog should be shown or hidden
     */
    public synchronized void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            preCallWindow.pack();

            // Set the visibility before setting the location. This is the only
            // reliable way of ensure that the location is respected.
            preCallWindow.setVisible(isVisible);

            if (visibleWindows.isEmpty())
            {
                // No existing visible windows displayed. Just center this one.
                preCallWindow.setLocationRelativeTo(null);
            }
            else
            {
                // Get the location of last displayed window and and the
                // height of it, so the this window appears directly underneath.
                Window last = visibleWindows.get(visibleWindows.size() - 1);
                int newX = last.getX();
                int newY = last.getY() + last.getHeight();

                preCallWindow.setLocation(newX, newY);
            }

            visibleWindows.add(preCallWindow);
        }
        else
        {
            // Remove the window from the list of visible windows. This also
            // occurs in dispose()
            visibleWindows.remove(preCallWindow);
            preCallWindow.setVisible(isVisible);
        }
    }

    /**
     * Indicates if this dialog is currently visible.
     *
     * @return <tt>true</tt> if this dialog is currently visible, <tt>false</tt>
     * otherwise
     */
    public boolean isVisible()
    {
        return preCallWindow.isVisible();
    }

    /**
     * Disposes this window.
     */
    public void dispose()
    {
        visibleWindows.remove(preCallWindow);
        preCallWindow.dispose();
    }

    /**
     * Returns the labels contained in this dialog.
     * The first is the label that will contain the image.
     * The second one is the one with the display name(s).
     * The third is the one that will hold the peer address(s).
     *
     * @return the call label contained in this dialog
     */
    public JLabel[] getCallLabels()
    {
        if(callLabelAccount == null)
            return new JLabel[]{
                callLabelImage, callLabelDisplayName, callLabelAddress};
        else
            return new JLabel[]{
                callLabelImage,
                callLabelDisplayName,
                callLabelAddress,
                callLabelAccount};
    }

    /**
     * Returns the accounts combo box contained in this dialog.
     *
     * @return the accounts combo box contained in this dialog
     */
    public JComboBox getAccountsCombo()
    {
        return accountsCombo;
    }

    /**
     * Reloads icons.
     */
    public void loadSkin()
    {
        callButton.setBackgroundImage(
            ImageLoader.getImage(ImageLoader.INCOMING_CALL_BUTTON_BG));
        callButton.setRolloverImage(
            ImageLoader.getImage(ImageLoader.INCOMING_CALL_BUTTON_ROLLOVER));
        callButton.setPressedImage(
            ImageLoader.getImage(ImageLoader.INCOMING_CALL_BUTTON_PRESSED));

        if (videoCallButton != null)
        {
            videoCallButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_BG));
            videoCallButton.setRolloverImage(
                ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_ROLLOVER));
            videoCallButton.setPressedImage(
                ImageLoader.getImage(ImageLoader.CALL_VIDEO_BUTTON_PRESSED));
        }

        if (mergeCallButton != null)
        {
            mergeCallButton.setBackgroundImage(
                ImageLoader.getImage(ImageLoader.MERGE_CALL_BUTTON_BG));
            mergeCallButton.setRolloverImage(
                ImageLoader.getImage(ImageLoader.MERGE_CALL_BUTTON_ROLLOVER));
            mergeCallButton.setPressedImage(
                ImageLoader.getImage(ImageLoader.MERGE_CALL_BUTTON_PRESSED));
        }

        hangupButton.setBackgroundImage(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));
        hangupButton.setRolloverImage(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_ROLLOVER));
        hangupButton.setPressedImage(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_PRESSED));
    }

    /**
     * Handles <tt>ActionEvent</tt>s triggered by pressing the call or the
     * hangup buttons.
     * @param e The <tt>ActionEvent</tt> to handle.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equals(CALL_BUTTON))
        {
            callButtonPressed();
        }
        else if (buttonName.equals(CONF_CALL_BUTTON))
        {
            mergeCallButtonPressed();
        }
        else if (buttonName.equals(VIDEO_CALL_BUTTON))
        {
            videoCallButtonPressed();
        }
        else if (buttonName.equals(HANGUP_BUTTON))
        {
            hangupButtonPressed();
        }

        dispose();
    }

    /**
     * Indicates that the call button has been pressed.
     */
    public abstract void callButtonPressed();

    /**
     * Indicates that the conference call button has been pressed.
     */
    public abstract void mergeCallButtonPressed();

    /**
     * Indicates that the hangup button has been pressed.
     */
    public abstract void hangupButtonPressed();

    /**
     * Indicates that the video call button has been pressed.
     */
    public abstract void videoCallButtonPressed();
}
