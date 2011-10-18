/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.skin.*;
import net.java.sip.communicator.util.swing.*;

import com.explodingpixels.macwidgets.*;

/**
 * 
 * @author Yana Stamcheva
 */
public abstract class PreCallDialog
    implements  ActionListener,
                Skinnable
{
    /**
     * The call button name.
     */
    private static final String CALL_BUTTON = "CallButton";

    /**
     * The hangup button name.
     */
    private static final String HANGUP_BUTTON = "HangupButton";

    /**
     * The horizontal gap between buttons.
     */
    private static final int HGAP = 5;

    /**
     * Call button.
     */
    private SIPCommButton callButton;

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
    private Window preCallWindow;

    /**
     * Creates an instanceof <tt>PreCallDialog</tt> by specifying the dialog
     * title.
     *
     * @param title the title of the dialog
     */
    public PreCallDialog(String title)
    {
        this(title, null, null);
    }

    /**
     * Creates an instanceof <tt>PreCallDialog</tt> by specifying the dialog
     * title and the text to show.
     *
     * @param title the title of the dialog
     * @param text the text to show
     * @param accounts the list of accounts to choose from
     */
    public PreCallDialog(String title, String text, Object[] accounts)
    {
        preCallWindow = createPreCallWindow(title, text, accounts);

        this.initComponents();
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
                                        Object[] accounts)
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
                accountsCombo = HudWidgetFactory
                    .createHudComboBox(new DefaultComboBoxModel(accounts));
            }
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
            {
                accountsCombo = new JComboBox(accounts);
            }
        }

        if (text != null)
            callLabelDisplayName.setText(text);

        receivedCallWindow.setAlwaysOnTop(true);

        // prevents dialog window to get unwanted key events and when going
        // on top on linux, it steals focus and if we are accedently
        // writing something and pressing enter a call get answered
        receivedCallWindow.setFocusableWindowState(false);

        return receivedCallWindow;
    }

    /**
     * Initializes all components in this panel.
     */
    private void initComponents()
    {
        JPanel mainPanel = new JPanel(new GridBagLayout());

        // disable html rendering
        callLabelDisplayName.putClientProperty("html.disable", Boolean.TRUE);
        callLabelAddress.putClientProperty("html.disable", Boolean.TRUE);
        callLabelImage.putClientProperty("html.disable", Boolean.TRUE);

        JPanel buttonsPanel = new TransparentPanel(new GridBagLayout());

        callButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.CALL_BUTTON_BG));

        hangupButton = new SIPCommButton(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));

        mainPanel.setPreferredSize(new Dimension(400, 90));
        mainPanel.setOpaque(false);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        callButton.setName(CALL_BUTTON);
        hangupButton.setName(HANGUP_BUTTON);

        callButton.addActionListener(this);
        hangupButton.addActionListener(this);

        preCallWindow.add(mainPanel);

        GridBagConstraints constraints = new GridBagConstraints();
        constraints.anchor = GridBagConstraints.WEST;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 2;
        constraints.insets = new Insets(0, 0, 0, HGAP);
        mainPanel.add(callLabelImage, constraints);

        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.gridheight = 1;
        constraints.gridx = 1;
        constraints.weightx = 1;
        mainPanel.add(callLabelDisplayName, constraints);

        constraints.gridy = 1;
        mainPanel.add(callLabelAddress, constraints);

        if (accountsCombo != null)
        {
            constraints.gridx = 1;
            constraints.weightx = 1;
            mainPanel.add(Box.createVerticalStrut(HGAP), constraints);

            constraints.gridx = 1;
            constraints.gridy = 2;
            constraints.weightx = 1;
            mainPanel.add(accountsCombo, constraints);
        }

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 2;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.gridheight = 2;
        mainPanel.add(Box.createHorizontalStrut(HGAP), constraints);

        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 3;
        constraints.weightx = 0;
        mainPanel.add(buttonsPanel, constraints);

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridheight = 0;
        buttonsPanel.add(callButton, constraints);
        constraints.gridx = 1;
        buttonsPanel.add(Box.createHorizontalStrut(HGAP));
        constraints.gridx = 2;
        buttonsPanel.add(hangupButton, constraints);
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
     * @param isVisible indicates if this dialog should be shown or hidden
     */
    public void setVisible(boolean isVisible)
    {
        if (isVisible)
        {
            preCallWindow.pack();
            preCallWindow.setLocationRelativeTo(null);
        }

        preCallWindow.setVisible(isVisible);
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
        return new JLabel[]{
                callLabelImage, callLabelDisplayName, callLabelAddress};
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
            ImageLoader.getImage(ImageLoader.CALL_BUTTON_BG));

        hangupButton.setBackgroundImage(
            ImageLoader.getImage(ImageLoader.HANGUP_BUTTON_BG));
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
        else if (buttonName.equals(HANGUP_BUTTON))
        {
            hangupButtonPressed();
        }

        preCallWindow.dispose();
    }

    /**
     * Indicates that the call button has been pressed.
     */
    public abstract void callButtonPressed();

    /**
     * Indicates that the hangup button has been pressed.
     */
    public abstract void hangupButtonPressed();
}
