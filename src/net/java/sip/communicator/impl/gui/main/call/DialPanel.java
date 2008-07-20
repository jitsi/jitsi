/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>DialPanel</tt> is the panel that contains the buttons to dial a
 * phone number.
 * 
 * @author Yana Stamcheva
 */

public class DialPanel
    extends JPanel
    implements  ActionListener,
                MouseListener
{
    private Logger logger = Logger.getLogger(DialPanel.class);

    private JComboBox phoneNumberCombo;

    private DialButton oneButton = new DialButton(
        ImageLoader.getImage(ImageLoader.ONE_DIAL_BUTTON));

    private DialButton twoButton = new DialButton(
        ImageLoader.getImage(ImageLoader.TWO_DIAL_BUTTON));

    private DialButton threeButton = new DialButton(
        ImageLoader.getImage(ImageLoader.THREE_DIAL_BUTTON));

    private DialButton fourButton = new DialButton(
        ImageLoader.getImage(ImageLoader.FOUR_DIAL_BUTTON));

    private DialButton fiveButton = new DialButton(
        ImageLoader.getImage(ImageLoader.FIVE_DIAL_BUTTON));

    private DialButton sixButton = new DialButton(
        ImageLoader.getImage(ImageLoader.SIX_DIAL_BUTTON));

    private DialButton sevenButton = new DialButton(
        ImageLoader.getImage(ImageLoader.SEVEN_DIAL_BUTTON));

    private DialButton eightButton = new DialButton(
        ImageLoader.getImage(ImageLoader.EIGHT_DIAL_BUTTON));

    private DialButton nineButton = new DialButton(
        ImageLoader.getImage(ImageLoader.NINE_DIAL_BUTTON));

    private DialButton starButton = new DialButton(
        ImageLoader.getImage(ImageLoader.STAR_DIAL_BUTTON));

    private DialButton zeroButton = new DialButton(
        ImageLoader.getImage(ImageLoader.ZERO_DIAL_BUTTON));

    private DialButton diezButton = new DialButton(
        ImageLoader.getImage(ImageLoader.DIEZ_DIAL_BUTTON));

    private int hgap = GuiActivator.getResources().
        getSettingsInt("dialPadHorizontalGap");

    private int vgap = GuiActivator.getResources().
        getSettingsInt("dialPadVerticalGap");

    private JPanel dialPadPanel = new JPanel(new GridLayout(4, 3, hgap, vgap));

    private CallManager callManager;

    private CallParticipant callParticipant;

    /**
     * Creates an instance of <tt>DialPanel</tt>.
     */
    public DialPanel(CallManager callManager)
    {
        super(new FlowLayout(FlowLayout.CENTER));

        this.callManager = callManager;

        this.dialPadPanel.setOpaque(false);

        this.phoneNumberCombo = callManager.getCallComboBox();

        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        int width = GuiActivator.getResources().getSettingsInt("dialPadWidth");
        int height = GuiActivator.getResources().getSettingsInt("dialPadHeight");

        this.dialPadPanel.setPreferredSize(new Dimension(width, height));

        this.init();
    }

    public DialPanel(CallManager callManager, CallParticipant callParticipant)
    {
        this(callManager);

        this.callParticipant = callParticipant;
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init()
    {
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

        oneButton.addMouseListener(this);
        twoButton.addMouseListener(this);
        threeButton.addMouseListener(this);
        fourButton.addMouseListener(this);
        fiveButton.addMouseListener(this);
        sixButton.addMouseListener(this);
        sevenButton.addMouseListener(this);
        eightButton.addMouseListener(this);
        nineButton.addMouseListener(this);
        zeroButton.addMouseListener(this);
        diezButton.addMouseListener(this);
        starButton.addMouseListener(this);

        oneButton.setOpaque(false);
        twoButton.setOpaque(false);
        threeButton.setOpaque(false);
        fourButton.setOpaque(false);
        fiveButton.setOpaque(false);
        sixButton.setOpaque(false);
        sevenButton.setOpaque(false);
        eightButton.setOpaque(false);
        nineButton.setOpaque(false);
        zeroButton.setOpaque(false);
        diezButton.setOpaque(false);
        starButton.setOpaque(false);

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
     * Handles the <tt>ActionEvent</tt> triggered when user presses one of the
     * dial buttons.
     */
    public void actionPerformed(ActionEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();
        String phoneNumber = "";

        OperationSetDTMF dtmfOpSet = null;
        DTMFTone dtmfTone = null;
        
        if(callParticipant != null
            && callParticipant.getProtocolProvider()
            .getOperationSet(OperationSetDTMF.class) != null)
        {
            dtmfOpSet = (OperationSetDTMF) callParticipant.getProtocolProvider()
                .getOperationSet(OperationSetDTMF.class);
        }   
        
        if (this.phoneNumberCombo.getEditor().getItem() != null)
            phoneNumber = (String) this.phoneNumberCombo.getEditor().getItem();

        if (buttonName.equals("one"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_1;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "1");
        }
        else if (buttonName.equals("two"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_2;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "2");
        }
        else if (buttonName.equals("three"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_3;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "3");
        }
        else if (buttonName.equals("four"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_4;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "4");
        }
        else if (buttonName.equals("five"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_5;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "5");
        }
        else if (buttonName.equals("six"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_6;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "6");
        }
        else if (buttonName.equals("seven"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_7;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "7");
        }
        else if (buttonName.equals("eight"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_8;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "8");
        }
        else if (buttonName.equals("nine"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_9;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "9");
        }
        else if (buttonName.equals("zero"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_0;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "0");
        }
        else if (buttonName.equals("diez"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_SHARP;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "#");
        }
        else if (buttonName.equals("star"))
        {
            if(dtmfOpSet != null)
                dtmfTone = DTMFTone.DTMF_STAR;
            else
                this.phoneNumberCombo.getEditor().setItem(phoneNumber + "*");
        }
        
        if(dtmfTone != null)
            try
            {
                dtmfOpSet.sendDTMF(callParticipant, dtmfTone);
            }
            catch (NullPointerException e1)
            {
                logger.error("Failed to send a DTMF tone.", e1);
            }
            catch (ClassCastException e1)
            {
                logger.error("Failed to send a DTMF tone.", e1);
            }
            catch (OperationFailedException e1)
            {
                logger.error("Failed to send a DTMF tone.", e1);
            }
        else    
            this.phoneNumberCombo.requestFocus();
    }

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    public void mousePressed(MouseEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();

        AudioNotifierService audioNotifier = GuiActivator.getAudioNotifier();

        if (buttonName.equals("one"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_ONE).play();
        }
        else if (buttonName.equals("two"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_TWO).play();
        }
        else if (buttonName.equals("three"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_THREE).play();
        }
        else if (buttonName.equals("four"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_FOUR).play();
        }
        else if (buttonName.equals("five"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_FIVE).play();
        }
        else if (buttonName.equals("six"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_SIX).play();
        }
        else if (buttonName.equals("seven"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_SEVEN).play();
        }
        else if (buttonName.equals("eight"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_EIGHT).play();
        }
        else if (buttonName.equals("nine"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_NINE).play();
        }
        else if (buttonName.equals("zero"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_ZERO).play();
        }
        else if (buttonName.equals("diez"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_DIEZ).play();
        }
        else if (buttonName.equals("star"))
        {
            audioNotifier.createAudio(SoundProperties.DIAL_STAR).play();
        }
    }

    public void mouseReleased(MouseEvent e)
    {
    }
    
    private class DialButton extends SIPCommButton
    {
        /**
         * Creates an instance of <tt>MsgToolbarButton</tt>.
         * @param iconImage The icon to display on this button.
         */
        public DialButton(Image iconImage)
        {
            super(  ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG),
                    ImageLoader.getImage(ImageLoader.DIAL_BUTTON_ROLLOVER_BG),
                    iconImage, null);
        }
    }

    public void paintComponent(Graphics g)
    {
     // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        boolean isTextureBackground = new Boolean(GuiActivator.getResources()
            .getSettingsString("isTextureBackground")).booleanValue();

        BufferedImage bgImage
            = ImageLoader.getImage(ImageLoader.MAIN_WINDOW_BACKGROUND);

        // paint the image
        if (bgImage != null)
        {
            if (isTextureBackground)
            {
                Rectangle rect
                    = new Rectangle(0, 0,
                            bgImage.getWidth(null),
                            bgImage.getHeight(null));

                TexturePaint texture = new TexturePaint(bgImage, rect);

                g2.setPaint(texture);

                g2.fillRect(0, 0, this.getWidth(), this.getHeight());
            }
            else
            {
                g.setColor(new Color(
                    GuiActivator.getResources()
                        .getColor("contactListBackground")));

                // paint the background with the choosen color
                g.fillRect(0, 0, getWidth(), getHeight());

                g2.drawImage(bgImage,
                        this.getWidth() - bgImage.getWidth(),
                        this.getHeight() - bgImage.getHeight(),
                        this);
            }
        }
    }
}
