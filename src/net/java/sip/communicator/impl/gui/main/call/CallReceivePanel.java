/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import javax.swing.*;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;

/**
 * 
 * @author Yana Stamcheva
 */
public class CallReceivePanel extends JDialog {

    private Image callButtonBG = ImageLoader
            .getImage(ImageLoader.CALL_BUTTON_BG);

    private Image callButtonRolloverBG = ImageLoader
            .getImage(ImageLoader.CALL_ROLLOVER_BUTTON_BG);

    private Image hangupButtonBG = ImageLoader
            .getImage(ImageLoader.HANGUP_BUTTON_BG);

    private Image hangupButtonRolloverBG = ImageLoader
            .getImage(ImageLoader.HANGUP_ROLLOVER_BUTTON_BG);

    private JLabel personPhoto = new JLabel(new ImageIcon(ImageLoader
            .getImage(ImageLoader.DEFAULT_USER_PHOTO)));

    private JLabel personName = new JLabel("John Smith");

    private JLabel birthDate = new JLabel("11/11/1900");

    private JLabel emptyLabel = new JLabel("  ");

    private JLabel personInfo = new JLabel("additional info");

    private SIPCommButton callButton;

    private SIPCommButton hangupButton;

    private JPanel userInfoPanel = new JPanel();

    private JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
            15, 5));

    public CallReceivePanel(MainFrame parent) {

        super(parent);

        this.setSize(300, 200);

        this.getContentPane().setLayout(new BorderLayout());

        callButton = new SIPCommButton(callButtonBG, callButtonRolloverBG);

        hangupButton = new SIPCommButton(
                hangupButtonBG, hangupButtonRolloverBG);

        this.init();
    }

    public void init() {
        this.personName.setFont(new Font("Sans Serif", Font.BOLD, 12));

        // this.buttonsPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0,
        // 0, Color.GRAY));
        this.buttonsPanel.add(callButton);
        this.buttonsPanel.add(hangupButton);

        this.userInfoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 0,
                0));
        this.userInfoPanel.setLayout(new BoxLayout(userInfoPanel,
                BoxLayout.Y_AXIS));

        this.userInfoPanel.add(personName);
        this.userInfoPanel.add(birthDate);
        this.userInfoPanel.add(emptyLabel);
        this.userInfoPanel.add(personInfo);

        this.getContentPane().add(personPhoto, BorderLayout.WEST);
        this.getContentPane().add(userInfoPanel, BorderLayout.CENTER);
        this.getContentPane().add(buttonsPanel, BorderLayout.SOUTH);
    }
}
