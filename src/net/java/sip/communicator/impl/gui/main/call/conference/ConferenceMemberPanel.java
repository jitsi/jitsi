package net.java.sip.communicator.impl.gui.main.call.conference;

import java.awt.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.swing.*;

public class ConferenceMemberPanel
    extends TransparentPanel
    implements PropertyChangeListener
{
    private final JLabel nameLabel = new JLabel();

    /**
     * The image of the member.
     */
    private ImageIcon memberImage;

    /**
     * The status of the peer
     */
    private final JLabel callStatusLabel = new JLabel();

    private final ConferenceMember member;

    private final Color bgColor = new Color(222, 222, 222);

    /**
     * Creates a <tt><ConferenceMemberPanel</tt> by specifying the corresponding
     * <tt>member</tt> that it represents.
     * @param member the <tt>ConferenceMember</tt> shown in this panel
     */
    public ConferenceMemberPanel(ConferenceMember member)
    {
        this.member = member;

        this.memberImage = new ImageIcon
            (ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)
                .getScaledInstance(50, 50, Image.SCALE_SMOOTH));

        Component nameBar = createNameBar(member.getDisplayName());
        Component soundLevelsIndicator = createSoundLevelIndicator();

        JPanel titlePanel = new CallTitlePanel(
            new Color(GuiActivator.getResources().getColor(
                "service.gui.CALL_MEMBER_NAME_BACKGROUND")));
        titlePanel.setLayout(new GridBagLayout());

        this.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

        TransparentPanel detailPanel = new TransparentPanel();
        detailPanel.setLayout(new GridBagLayout());
        detailPanel.setBackground(new Color(255, 255, 255));

        this.setLayout(new GridBagLayout());

        GridBagConstraints constraints = new GridBagConstraints();

        if (nameBar != null)
        {
            constraints.fill = GridBagConstraints.BOTH;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(2, 0, 2, 0);

            titlePanel.add(nameBar, constraints);
        }
        // Add the image
        if (memberImage != null)
        {
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.LINE_START;
            constraints.gridx = 0;
            constraints.gridy = 0;
            constraints.weightx = 0;
            constraints.weighty = 0;
            constraints.insets = new Insets(5, 10, 5, 0);

            detailPanel.add(new JLabel(memberImage), constraints);
        }
        if (soundLevelsIndicator != null)
        {
            constraints.fill = GridBagConstraints.HORIZONTAL;
            constraints.gridx = 1;
            constraints.gridy = 0;
            constraints.weightx = 1;
            constraints.weighty = 0;
            constraints.insets = new Insets(2, 20, 2, 20);

            detailPanel.add(soundLevelsIndicator, constraints);
        }

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.add(titlePanel, constraints);

        constraints.fill = GridBagConstraints.BOTH;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.weightx = 1;
        constraints.weighty = 0;
        constraints.insets = new Insets(0, 0, 0, 0);

        this.add(detailPanel, constraints);
    }

    /**
     * Create the name bar of the peer
     *
     * @return The name bar component
     */
    private Component createNameBar(String memberName)
    {
        TransparentPanel namePanel = new TransparentPanel(new BorderLayout());
        namePanel.setBorder(BorderFactory.createEmptyBorder(0,10,0,10));

        nameLabel.setText(memberName);

        namePanel.add(nameLabel, BorderLayout.WEST);

        Component statusBar = createStatusBar();
        namePanel.add(statusBar, BorderLayout.EAST);

        return namePanel;
    }

    /**
     * Create the sound level indicator of the peer
     *
     * @return The sound level indicator component
     */
    private Component createSoundLevelIndicator()
    {
        SoundLevelIndicator indicator
            = new SoundLevelIndicator();

        member.addCallPeerSoundLevelListener(indicator);

        return indicator;
    }

    /**
     * Create the status bar of the peer
     *
     * @return The status bar component
     */
    private Component createStatusBar()
    {
        callStatusLabel.setText(member.getState().toString());

        TransparentPanel statusPanel
            = new TransparentPanel(
                new FlowLayout(FlowLayout.RIGHT, 0, 0));

        statusPanel.add(callStatusLabel);

        return statusPanel;
    }

    /**
     * Fired when a change in property happened
     *
     * @param evt fired PropertyChangeEvent
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        String propertyName = evt.getPropertyName();

        if (propertyName.equals(ConferenceMember.DISPLAY_NAME_PROPERTY_NAME))
        {
            String displayName = (String) evt.getNewValue();

            nameLabel.setText(displayName);

            this.revalidate();
            this.repaint();
        }
        else if (propertyName.equals(ConferenceMember.STATE_PROPERTY_NAME))
        {
            ConferenceMemberState state
                = (ConferenceMemberState) evt.getNewValue();

            callStatusLabel.setText(state.toString());

            this.revalidate();
            this.repaint();
        }
    }

    /**
     * Returns the contained in this panel <tt>ConferenceMember</tt>.
     * 
     * @return the contained in this panel <tt>ConferenceMember</tt>.
     */
    public ConferenceMember getConferenceMember()
    {
        return member;
    }

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
}
