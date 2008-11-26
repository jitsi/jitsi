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
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

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
    implements MouseListener
{
    private Logger logger = Logger.getLogger(DialPanel.class);

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

    /**
     * Handles press and hold zero button action.
     */
    private Timer plusZeroTimer =
        new Timer(1000, new PlusZeroActionListener());

    private boolean isTypedPlus = false;

    private JPanel dialPadPanel = new JPanel(new GridLayout(4, 3, hgap, vgap));

    private LinkedList<CallParticipant> callParticipantsList
        = new LinkedList<CallParticipant>();

    private MainCallPanel parentCallPanel;

    /**
     * Creates an instance of <tt>DialPanel</tt>.
     */
    public DialPanel(MainCallPanel parentCallPanel)
    {
        super(new FlowLayout(FlowLayout.CENTER));

        this.parentCallPanel = parentCallPanel;

        this.init();
    }

    /**
     * Creates an instance of <tt>DialPanel</tt> for a specific call, by
     * specifying the parent <tt>CallManager</tt> and the
     * <tt>CallParticipant</tt>.
     * 
     * @param callManager the parent <tt>CallManager</tt>
     * @param callParticipant the <tt>CallParticipant</tt>, for which the
     * dialpad will be opened.
     */
    public DialPanel(Iterator<CallParticipant> callParticipants)
    {
        // We need to send DTMF tones to all participants each time the user
        // presses a dial button, so we put the iterator into a list.
        while (callParticipants.hasNext())
        {
            this.callParticipantsList.add(callParticipants.next());
        }

        this.init();
    }

    /**
     * Initializes this panel by adding all dial buttons to it.
     */
    public void init()
    {
        this.dialPadPanel.setOpaque(false);

        this.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));

        int width = GuiActivator.getResources().getSettingsInt("dialPadWidth");
        int height = GuiActivator.getResources().getSettingsInt("dialPadHeight");

        this.dialPadPanel.setPreferredSize(new Dimension(width, height));

        this.plusZeroTimer.setRepeats(false);

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

    public void mouseClicked(MouseEvent e)
    {
    }

    public void mouseEntered(MouseEvent e)
    {
    }

    public void mouseExited(MouseEvent e)
    {
    }

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user presses one of the
     * dial buttons.
     */
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

            plusZeroTimer.start();
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

    /**
     * Handles the <tt>MouseEvent</tt> triggered when user releases one of the
     * dial buttons.
     */
    public void mouseReleased(MouseEvent e)
    {
        JButton button = (JButton) e.getSource();
        String buttonName = button.getName();
        String phoneNumber = "";
        if(parentCallPanel != null)
        {
            phoneNumber = parentCallPanel.getPhoneNumberComboText();
        }

        DTMFTone dtmfTone = null;
        if (buttonName.equals("one"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_1;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "1");
        }
        else if (buttonName.equals("two"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_2;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "2");
        }
        else if (buttonName.equals("three"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_3;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "3");
        }
        else if (buttonName.equals("four"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_4;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "4");
        }
        else if (buttonName.equals("five"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_5;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "5");
        }
        else if (buttonName.equals("six"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_6;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "6");
        }
        else if (buttonName.equals("seven"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_7;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "7");
        }
        else if (buttonName.equals("eight"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_8;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "8");
        }
        else if (buttonName.equals("nine"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_9;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "9");
        }
        else if (buttonName.equals("zero"))
        {
            if (isTypedPlus)
            {
                isTypedPlus = false;
                return;
            }
            else
                plusZeroTimer.stop();

            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_0;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "0");
        }
        else if (buttonName.equals("diez"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_SHARP;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "#");
        }
        else if (buttonName.equals("star"))
        {
            if(parentCallPanel == null)
                dtmfTone = DTMFTone.DTMF_STAR;
            else
                this.parentCallPanel.setPhoneNumberComboText(phoneNumber + "*");
        }

        if(dtmfTone != null)
            this.sendDtmfTone(dtmfTone);
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
                    iconImage);
        }
    }

    /**
     * Paints the main background image to the background of this dial panel.
     */
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

    /**
     * Handles press and hold zero button action.
     */
    private class PlusZeroActionListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e)
        {
            isTypedPlus = true;

            plusZeroTimer.stop();

            if(parentCallPanel == null)
            {
                sendDtmfTone(DTMFTone.DTMF_0);
                sendDtmfTone(DTMFTone.DTMF_0);
            }
            else
            {
                parentCallPanel.setPhoneNumberComboText(
                    parentCallPanel.getPhoneNumberComboText() + "+");
            }
        }
    }

    /**
     * Sends a DTMF tone to the current DTMF operation set.
     * 
     * @param dtmfTone The DTMF tone to send.
     */
    private void sendDtmfTone(DTMFTone dtmfTone)
    {
        Iterator<CallParticipant> callParticipants
            = this.callParticipantsList.iterator();

        try
        {
            while (callParticipants.hasNext())
            {
                CallParticipant participant
                    = (CallParticipant) callParticipants.next();

                if (participant.getProtocolProvider()
                    .getOperationSet(OperationSetDTMF.class) != null)
                {
                    OperationSetDTMF dtmfOpSet
                        = (OperationSetDTMF) participant.getProtocolProvider()
                            .getOperationSet(OperationSetDTMF.class);

                    dtmfOpSet.sendDTMF(participant, dtmfTone);
                }
            }
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
    }
}
