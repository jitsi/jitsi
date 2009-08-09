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
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.audionotifier.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.resources.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

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
    private final Logger logger = Logger.getLogger(DialPanel.class);

    /**
     * Handles press and hold zero button action.
     */
    private Timer plusZeroTimer =
        new Timer(1000, new PlusZeroActionListener());

    private boolean isTypedPlus = false;

    private final JPanel dialPadPanel =
        new JPanel(new GridLayout(4, 3,
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_HORIZONTAL_GAP"),
            GuiActivator.getResources()
                .getSettingsInt("impl.gui.DIAL_PAD_VERTICAL_GAP")));

    private final java.util.List<CallPeer> callPeersList =
        new LinkedList<CallPeer>();

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
     * @param callParticipants the <tt>CallParticipant</tt>s, for which the
     * dialpad will be opened.
     */
    public DialPanel(Iterator<CallPeer> callParticipants)
    {
        // We need to send DTMF tones to all participants each time the user
        // presses a dial button, so we put the iterator into a list.
        while (callParticipants.hasNext())
        {
            this.callPeersList.add(callParticipants.next());
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

        int width = GuiActivator.getResources()
            .getSettingsInt("impl.gui.DIAL_PAD_WIDTH");

        int height = GuiActivator.getResources()
            .getSettingsInt("impl.gui.DIAL_PAD_HEIGHT");

        this.dialPadPanel.setPreferredSize(new Dimension(width, height));

        this.plusZeroTimer.setRepeats(false);

        ImageID[] images =
            new ImageID[]
            { ImageLoader.ONE_DIAL_BUTTON, ImageLoader.TWO_DIAL_BUTTON,
                ImageLoader.THREE_DIAL_BUTTON, ImageLoader.FOUR_DIAL_BUTTON,
                ImageLoader.FIVE_DIAL_BUTTON, ImageLoader.SIX_DIAL_BUTTON,
                ImageLoader.SEVEN_DIAL_BUTTON, ImageLoader.EIGHT_DIAL_BUTTON,
                ImageLoader.NINE_DIAL_BUTTON, ImageLoader.STAR_DIAL_BUTTON,
                ImageLoader.ZERO_DIAL_BUTTON, ImageLoader.DIEZ_DIAL_BUTTON };
        String[] names =
            new String[]
            { "one", "two", "three", "four", "five", "six", "seven", "eight",
                "nine", "star", "zero", "diez" };
        final int buttonCount = images.length;
        if (buttonCount != names.length)
            throw new IllegalStateException("names");
        Image bgImage = ImageLoader.getImage(ImageLoader.DIAL_BUTTON_BG);

        for (int buttonIndex = 0; buttonIndex < buttonCount; buttonIndex++)
            dialPadPanel.add(createDialButton(bgImage, images[buttonIndex],
                names[buttonIndex]));

        this.add(dialPadPanel, BorderLayout.CENTER);
    }

    private JButton createDialButton(Image bgImage, ImageID iconImage,
        String name)
    {
        JButton button =
            new SIPCommButton(bgImage, ImageLoader.getImage(iconImage));

        button.setAlignmentY(JButton.LEFT_ALIGNMENT);
        button.setName(name);
        button.setOpaque(false);
        button.addMouseListener(this);
        return button;
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

    /**
     * Paints the main background image to the background of this dial panel.
     */
    public void paintComponent(Graphics g)
    {
     // do the superclass behavior first
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        boolean isTextureBackground = Boolean.parseBoolean(GuiActivator.getResources()
            .getSettingsString("impl.gui.IS_CONTACT_LIST_TEXTURE_BG_ENABLED"));

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
        Iterator<CallPeer> callParticipants
            = this.callPeersList.iterator();

        try
        {
            while (callParticipants.hasNext())
            {
                CallPeer participant
                    = callParticipants.next();

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
