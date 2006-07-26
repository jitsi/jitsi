/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.java.sip.communicator.impl.gui.customcontrols.SIPCommButton;
import net.java.sip.communicator.impl.gui.utils.ImageLoader;

/**
 * The <tt>CallPanel</tt> is the panel that contains the "Call" and "Hangup"
 * buttons, as well as the field, where user could enter the phone number or
 * the contact name of the person, to which he would like to call.
 * 
 * @author Yana Stamcheva
 */

public class CallPanel extends JPanel implements ActionListener {

    private JComboBox phoneNumberCombo = new JComboBox();

    private JPanel comboPanel = new JPanel(new BorderLayout());

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
            10, 0));

    private SIPCommButton callButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_BUTTON_BG), ImageLoader
            .getImage(ImageLoader.CALL_ROLLOVER_BUTTON_BG));

    private SIPCommButton hangupButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.HANGUP_BUTTON_BG), ImageLoader
            .getImage(ImageLoader.HANGUP_ROLLOVER_BUTTON_BG));

    private SIPCommButton minimizeButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_PANEL_MINIMIZE_BUTTON), ImageLoader
            .getImage(ImageLoader.CALL_PANEL_MINIMIZE_ROLLOVER_BUTTON));

    private SIPCommButton restoreButton = new SIPCommButton(ImageLoader
            .getImage(ImageLoader.CALL_PANEL_RESTORE_BUTTON), ImageLoader
            .getImage(ImageLoader.CALL_PANEL_RESTORE_ROLLOVER_BUTTON));

    private JPanel minimizeButtonPanel = new JPanel(new FlowLayout(
            FlowLayout.RIGHT));

    private MainFrame parentWindow;

    /**
     * Creates an instance of <tt>CallPanel</tt>.
     * @param parentWindow The main application window.
     */
    public CallPanel(MainFrame parentWindow) {

        super(new BorderLayout());

        this.parentWindow = parentWindow;

        this.buttonsPanel
                .setBorder(BorderFactory.createEmptyBorder(5, 0, 0, 0));

        this.comboPanel.setBorder(BorderFactory.createEmptyBorder(10, 5, 0, 5));

        this.init();
    }

    /**
     * Initializes and constructs this panel.
     */
    private void init() {

        this.phoneNumberCombo.setEditable(true);

        this.comboPanel.add(phoneNumberCombo, BorderLayout.CENTER);
        // this.add(comboPanel, BorderLayout.NORTH);

        this.callButton.setName("call");
        this.hangupButton.setName("hangup");
        this.minimizeButton.setName("minimize");
        this.restoreButton.setName("restore");

        this.callButton.addActionListener(this);
        this.hangupButton.addActionListener(this);
        this.minimizeButton.addActionListener(this);
        this.restoreButton.addActionListener(this);

        this.buttonsPanel.add(callButton);
        this.buttonsPanel.add(hangupButton);

        // this.add(buttonsPanel, BorderLayout.CENTER);

        this.minimizeButtonPanel.add(restoreButton);

        this.add(minimizeButtonPanel, BorderLayout.SOUTH);

        // Disable all unused buttons.
        this.callButton.setEnabled(false);
        this.hangupButton.setEnabled(false);
    }

    /**
     * Returns the combo box, where user enters the phone number to call to.
     * @return the combo box, where user enters the phone number to call to.
     */
    public JComboBox getPhoneNumberCombo() {
        return phoneNumberCombo;
    }

    /**
     * Handles the <tt>ActionEvent</tt> generated when user presses one of the
     * buttons in this panel.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        if (buttonName.equalsIgnoreCase("call")) {
            CallReceivePanel cr = new CallReceivePanel(this.parentWindow);

            cr.setVisible(true);
        } else if (buttonName.equalsIgnoreCase("hangup")) {

        } else if (buttonName.equalsIgnoreCase("minimize")) {

            this.remove(comboPanel);
            this.remove(buttonsPanel);

            this.minimizeButtonPanel.removeAll();
            this.minimizeButtonPanel.add(restoreButton);

            this.parentWindow.validate();
        } else if (buttonName.equalsIgnoreCase("restore")) {

            this.add(comboPanel, BorderLayout.NORTH);
            this.add(buttonsPanel, BorderLayout.CENTER);

            this.minimizeButtonPanel.removeAll();
            this.minimizeButtonPanel.add(minimizeButton);

            this.parentWindow.validate();
        }
    }
}
