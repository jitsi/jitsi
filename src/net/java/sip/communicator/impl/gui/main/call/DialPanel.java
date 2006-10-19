/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.utils.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to
 * dial a phone number.
 * 
 * @author Yana Stamcheva
 */

public class DialPanel extends JPanel implements ActionListener {

    private JComboBox phoneNumberCombo;

    private JButton oneButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ONE_DIAL_BUTTON)));

    private JButton twoButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.TWO_DIAL_BUTTON)));

    private JButton threeButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.THREE_DIAL_BUTTON)));

    private JButton fourButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.FOUR_DIAL_BUTTON)));

    private JButton fiveButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.FIVE_DIAL_BUTTON)));

    private JButton sixButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SIX_DIAL_BUTTON)));

    private JButton sevenButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.SEVEN_DIAL_BUTTON)));

    private JButton eightButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.EIGHT_DIAL_BUTTON)));

    private JButton nineButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.NINE_DIAL_BUTTON)));

    private JButton starButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.STAR_DIAL_BUTTON)));

    private JButton zeroButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.ZERO_DIAL_BUTTON)));

    private JButton diezButton = new JButton(new ImageIcon(ImageLoader
            .getImage(ImageLoader.DIEZ_DIAL_BUTTON)));

    private JPanel dialPadPanel = new JPanel(new GridLayout(4, 3, 5, 5));

    /**
     * Creates an instance of <tt>DialPanel</tt>.
     */
    public DialPanel() {
        super(new FlowLayout(FlowLayout.CENTER));

        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        this.init();
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init() {
        oneButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        twoButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        threeButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        fourButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        fiveButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        sixButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        sevenButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        eightButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        nineButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        zeroButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        diezButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        starButton.setAlignmentY(JButton.LEFT_ALIGNMENT);
        
        oneButton.setName("one");
        twoButton.setName("two");
        threeButton.setName("three");
        fourButton.setName("four");
        fiveButton.setName("five");
        sixButton.setName("six");
        sevenButton.setName("seven");
        eightButton.setName("eight");
        nineButton.setName("nine");
        zeroButton.setName("zero");
        diezButton.setName("diez");
        starButton.setName("star");

        oneButton.addActionListener(this);
        twoButton.addActionListener(this);
        threeButton.addActionListener(this);
        fourButton.addActionListener(this);
        fiveButton.addActionListener(this);
        sixButton.addActionListener(this);
        sevenButton.addActionListener(this);
        eightButton.addActionListener(this);
        nineButton.addActionListener(this);
        zeroButton.addActionListener(this);
        diezButton.addActionListener(this);
        starButton.addActionListener(this);

        dialPadPanel.add(oneButton);
        dialPadPanel.add(twoButton);
        dialPadPanel.add(threeButton);
        dialPadPanel.add(fourButton);
        dialPadPanel.add(fiveButton);
        dialPadPanel.add(sixButton);
        dialPadPanel.add(sevenButton);
        dialPadPanel.add(eightButton);
        dialPadPanel.add(nineButton);
        dialPadPanel.add(starButton);
        dialPadPanel.add(zeroButton);
        dialPadPanel.add(diezButton);

        this.add(dialPadPanel, BorderLayout.CENTER);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when user presses one of
     * the dial buttons.
     */
    public void actionPerformed(ActionEvent e) {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();
        String phoneNumber = "";

        if (this.phoneNumberCombo.getEditor().getItem() != null)
            phoneNumber = (String) this.phoneNumberCombo.getEditor().getItem();

        if (buttonName.equals("one")) {
            SoundLoader.getSound(SoundLoader.DIAL_ONE).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "1");
        }
        else if (buttonName.equals("two")) {
            SoundLoader.getSound(SoundLoader.DIAL_TWO).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "2");
        }
        else if (buttonName.equals("three")) {
            SoundLoader.getSound(SoundLoader.DIAL_THREE).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "3");
        }
        else if (buttonName.equals("four")) {
            SoundLoader.getSound(SoundLoader.DIAL_FOUR).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "4");
        }
        else if (buttonName.equals("five")) {
            SoundLoader.getSound(SoundLoader.DIAL_FIVE).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "5");
        }
        else if (buttonName.equals("six")) {
            SoundLoader.getSound(SoundLoader.DIAL_SIX).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "6");
        }
        else if (buttonName.equals("seven")) {
            SoundLoader.getSound(SoundLoader.DIAL_SEVEN).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "7");
        }
        else if (buttonName.equals("eight")) {
            SoundLoader.getSound(SoundLoader.DIAL_EIGHT).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "8");
        }
        else if (buttonName.equals("nine")) {
            SoundLoader.getSound(SoundLoader.DIAL_NINE).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "9");
        }
        else if (buttonName.equals("zero")) {
            SoundLoader.getSound(SoundLoader.DIAL_ZERO).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "0");
        }
        else if (buttonName.equals("diez")) {
            SoundLoader.getSound(SoundLoader.DIAL_DIEZ).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "#");
        }
        else if (buttonName.equals("star")) {
            SoundLoader.getSound(SoundLoader.DIAL_STAR).play();
            this.phoneNumberCombo.getEditor().setItem(phoneNumber + "*");
        }
        this.phoneNumberCombo.requestFocus();
    }

    /**
     * Sets the combo box, where user enters the phone number to call to.
     * @param combo The combo box, where user enters the phone number to
     * call to.
     */
    public void setPhoneNumberCombo(JComboBox combo) {
        this.phoneNumberCombo = combo;
    }
}
