/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.swing.*;

/**
 * The basic panel used to render any conference participant. Meant to be
 * extended for <tt>CallPeer</tt>s and <tt>ConferenceMember</tt>s.
 *
 * @author Yana Stamcheva
 */
public class BasicConferenceParticipantPanel
    extends TransparentPanel
{
    private static final Color bgColor = new Color(255, 255, 255);

    /**
     * The label showing the name of the participant.
     */
    private final JLabel nameLabel = new JLabel();

    /**
     * The panel containing the title of the participant.
     */
    private final JPanel titleBar = new CallTitlePanel(new BorderLayout());

    /**
     * The label showing the image of the participant.
     */
    private final JLabel imageLabel = new JLabel();

    /**
     * The status of the peer
     */
    private final JLabel callStatusLabel = new JLabel();

    private final JPanel statusBar
        = new TransparentPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));

    private final JPanel nameBar
        = new TransparentPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

    /**
     * The component showing the sound level of the participant.
     */
    private final SoundLevelIndicator soundIndicator
        = new SoundLevelIndicator(  ConferenceMembersSoundLevelEvent.MIN_LEVEL,
                                    ConferenceMembersSoundLevelEvent.MAX_LEVEL);

    /**
     * Creates an instance of <tt>ConferenceParticipantPanel</tt>.
     */
    public BasicConferenceParticipantPanel()
    {
        ImageIcon avatarIcon = new ImageIcon
            (ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)
                .getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        TransparentPanel detailPanel = new TransparentPanel();
        detailPanel.setLayout(new GridBagLayout());
        detailPanel.setBackground(new Color(255, 255, 255));

        this.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        imageLabel.setIcon(avatarIcon);

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 10, 5, 0);

        detailPanel.add(imageLabel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(2, 20, 2, 20);

        detailPanel.add(soundIndicator, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.initTitleBar();
        this.add(titleBar, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.add(detailPanel, constraints);
    }

    /**
     * Sets the name of the participant.
     * @param participantName the name of the participant
     */
    public void setParticipantName(String participantName)
    {
        nameLabel.setText(participantName);
    }

    /**
     * Sets the state of the participant.
     * @param participantState the state of the participant
     */
    public void setParticipantState(String participantState)
    {
        callStatusLabel.setText(participantState.toLowerCase());
    }

    /**
     * Sets the image of the participant.
     * @param icon the image to set
     */
    public void setParticipantImage(ImageIcon icon)
    {
        imageLabel.setIcon(icon);
    }

    /**
     * Adds the given <tt>component</tt> to the status bar.
     * @param component the component to add
     */
    public void addToStatusBar(Component component)
    {
        this.statusBar.add(component);
    }

    /**
     * Adds the given <tt>component</tt> to the name bar.
     * @param component the component to add
     */
    public void addToNameBar(Component component)
    {
        this.nameBar.add(component);
    }

    /**
     * Updates the sound level bar to reflect the new sound level value.
     * @param soundLevel the new sound level value
     */
    public void updateSoundBar(int soundLevel)
    {
        soundIndicator.updateSoundLevel(soundLevel);
    }

    /**
     * Initializes the title bar.
     */
    private void initTitleBar()
    {
        titleBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        nameBar.add(nameLabel);

        statusBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        statusBar.add(callStatusLabel);

        titleBar.add(nameBar, BorderLayout.WEST);
        titleBar.add(statusBar, BorderLayout.EAST);
    }

    /**
     * Overrides {@link JComponent#paintComponent(Graphics)} in order to
     * customize the background of this panel.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        g = g.create();

        try
        {
            AntialiasingManager.activateAntialiasing(g);

            g.setColor(bgColor);
            g.fillRoundRect(0, 0, this.getWidth(), this.getHeight(), 20, 20);
        }
        finally
        {
            g.dispose();
        }
    }

    /**
     * Sets the background color of the title panel.
     * @param color the background color to set
     */
    protected void setTitleBackground(Color color)
    {
        titleBar.setBackground(color);
    }
}
