/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import javax.swing.*;
import javax.swing.text.*;

import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;
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
     * The avatar icon height.
     */
    private static final int AVATAR_HEIGHT = 50;

    /**
     * The avatar icon width.
     */
    private static final int AVATAR_WIDTH = 50;

    /**
     * The label showing the name of the participant.
     */
    private final JLabel nameLabel = new JLabel();

    /**
     * The panel containing the title of the participant.
     */
    private final JPanel titleBar = new CallTitlePanel(new GridBagLayout());

    /**
     * The label showing the image of the participant.
     */
    private final JLabel imageLabel = new JLabel();

    /**
     * The status of the peer
     */
    private final JLabel callStatusLabel = new JLabel();

    /**
     * The component responsible for displaying an error message.
     */
    private JTextComponent errorMessageComponent;

    private final JPanel statusBar
        = new TransparentPanel(new GridBagLayout());

    private final GridBagConstraints statusBarConstraints
        = new GridBagConstraints();

    private final JPanel nameBar
        = new TransparentPanel(new GridBagLayout());

    private final GridBagConstraints nameBarConstraints
        = new GridBagConstraints();

    private final TransparentPanel peerDetailsPanel = new TransparentPanel();

    private final TransparentPanel rightDetailsPanel
        = new TransparentPanel(new GridLayout(0, 1));

    /**
     * The component showing the sound level of the participant.
     */
    private final SoundLevelIndicator soundIndicator
        = new SoundLevelIndicator(  SoundLevelChangeEvent.MIN_LEVEL,
                                    SoundLevelChangeEvent.MAX_LEVEL);

    private final GridBagConstraints constraints = new GridBagConstraints();

    private boolean isFocusUI;

    private boolean isSingleFocusUI;

    /**
     * Creates an instance of <tt>ConferenceParticipantPanel</tt>.
     */
    public BasicConferenceParticipantPanel()
    {
        this.setLayout(new GridBagLayout());
        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        this.initTitleBar();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        this.add(titleBar, constraints);

        this.initPeerDetailsPanel();

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);
        this.add(peerDetailsPanel, constraints);
    }

    /**
     * Initializes the details panel for the peer.
     */
    private void initPeerDetailsPanel()
    {
        ImageIcon avatarIcon = new ImageIcon
            (ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)
                .getScaledInstance( AVATAR_WIDTH,
                                    AVATAR_HEIGHT,
                                    Image.SCALE_SMOOTH));

        peerDetailsPanel.setLayout(new GridBagLayout());
        peerDetailsPanel.setBackground(new Color(255, 255, 255));

        imageLabel.setIcon(avatarIcon);

        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 0;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 10, 5, 0);

        peerDetailsPanel.add(imageLabel, constraints);

        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(5, 20, 5, 20);

        rightDetailsPanel.add(soundIndicator);
        peerDetailsPanel.add(rightDetailsPanel, constraints);
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
     * @param image the image to set
     */
    public void setParticipantImage(byte[] image)
    {
        ImageIcon icon = ImageUtils.getScaledRoundedIcon(image,
                                                        AVATAR_WIDTH,
                                                        AVATAR_HEIGHT);

        if (icon != null)
            imageLabel.setIcon(icon);
    }

    /**
     * Enables or disables the single conference focus user interface.
     * @param isSingleFocusUI indicates if we should enable or disable the
     * conference focus user interface.
     */
    public void setSingleFocusUI(boolean isSingleFocusUI)
    {
        this.isSingleFocusUI = isSingleFocusUI;

        if (isSingleFocusUI)
        {
            this.setBorder(BorderFactory.createEmptyBorder(0,0,0,0));

            this.remove(titleBar);
            this.remove(peerDetailsPanel);
        }
        else
        {
            this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(0, 0, 0, 0);

            this.add(titleBar, constraints);

            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(0, 0, 0, 0);

            this.add(peerDetailsPanel, constraints);
        }
        this.revalidate();
        this.repaint();
    }

    /**
     * Enables or disables the conference focus user interface.
     * @param isFocusUI indicates if we should enable or disable the
     * conference focus user interface.
     */
    public void setFocusUI(boolean isFocusUI)
    {
        this.isFocusUI = isFocusUI;

        if (isFocusUI)
        {
            this.remove(peerDetailsPanel);
        }
        else
        {
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 1;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(0, 0, 0, 0);

            this.add(peerDetailsPanel, constraints);
        }
        this.revalidate();
        this.repaint();
    }

    /**
     * Returns <tt>true</tt> if the current interface corresponds to a
     * single conference focus interface, otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the current interface corresponds to a
     * single conference focus interface, otherwise returns <tt>false</tt>.
     */
    public boolean isSingleFocusUI()
    {
        return isSingleFocusUI;
    }

    /**
     * Returns <tt>true</tt> if the current interface corresponds to a
     * conference focus interface, otherwise returns <tt>false</tt>.
     * @return <tt>true</tt> if the current interface corresponds to a
     * conference focus interface, otherwise returns <tt>false</tt>.
     */
    public boolean isFocusUI()
    {
        return isFocusUI;
    }

    /**
     * Adds the given <tt>component</tt> to the status bar.
     * @param component the component to add
     */
    public void addToStatusBar(Component component)
    {
        statusBarConstraints.gridx = statusBarConstraints.gridx + 1;
        statusBarConstraints.weightx = 0f;
        this.statusBar.add(component, statusBarConstraints);
    }

    /**
     * Adds the given <tt>component</tt> to the name bar.
     * @param component the component to add
     */
    public void addToNameBar(Component component)
    {
        nameBarConstraints.gridx = nameBarConstraints.gridx + 1;
        nameBarConstraints.weightx = 0f;
        this.nameBar.add(component, nameBarConstraints);
    }

    /**
     * Adds the given <tt>component</tt> to the center below the sound bar.
     * @param component the component to add
     */
    public void addToCenter(Component component)
    {
        rightDetailsPanel.add(component);
    }

    /**
     * Updates the sound level bar to reflect the new sound level value.
     * @param soundLevel the new sound level value
     */
    public void updateSoundBar(int soundLevel)
    {
        if (soundIndicator != null)
            soundIndicator.updateSoundLevel(soundLevel);
    }

    /**
     * Initializes the title bar.
     */
    private void initTitleBar()
    {
        titleBar.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));

        nameBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        nameBarConstraints.gridx = 0;
        nameBarConstraints.gridy = 0;
        nameBarConstraints.weightx = 1f;
        nameBar.add(nameLabel, nameBarConstraints);

        statusBar.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
        statusBarConstraints.gridx = 0;
        statusBarConstraints.gridy = 0;
        statusBarConstraints.weightx = 1f;
        statusBar.add(callStatusLabel, statusBarConstraints);

        GridBagConstraints constraints = new GridBagConstraints();

        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.anchor = GridBagConstraints.WEST;
        titleBar.add(nameBar, constraints);

        constraints.gridx = 1;
        constraints.gridy = 0;
        constraints.weightx = 1f;
        constraints.anchor = GridBagConstraints.EAST;
        titleBar.add(statusBar, constraints);
    }

    /**
     * Overrides {@link JComponent#paintComponent(Graphics)} in order to
     * customize the background of this panel.
     * @param g the <tt>Graphics</tt> object used for painting
     */
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);

        if (!isSingleFocusUI)
        {
            g = g.create();

            try
            {
                AntialiasingManager.activateAntialiasing(g);

                g.setColor(bgColor);
                g.fillRoundRect(
                    0, 0, this.getWidth(), this.getHeight(), 20, 20);
            }
            finally
            {
                g.dispose();
            }
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

    /**
     * Sets the reason of a call failure if one occurs. The renderer should
     * display this reason to the user.
     * @param reason the reason to display
     */
    protected void setErrorReason(String reason)
    {
        if (errorMessageComponent == null)
        {
            errorMessageComponent = new JTextPane();

            JTextPane textPane = (JTextPane) errorMessageComponent;
            textPane.setEditable(false);
            textPane.setOpaque(false);

            StyledDocument doc = textPane.getStyledDocument();

            MutableAttributeSet standard = new SimpleAttributeSet();
            StyleConstants.setFontFamily(standard,
                                        textPane.getFont().getFamily());
            StyleConstants.setFontSize(standard, 12);
            doc.setParagraphAttributes(0, 0, standard, true);

            addToCenter(errorMessageComponent);
            this.revalidate();
        }

        errorMessageComponent.setText(reason);

        if (isVisible())
            errorMessageComponent.repaint();
    }
}
