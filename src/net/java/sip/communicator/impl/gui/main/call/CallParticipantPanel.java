/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;

/**
 * The <tt>CallParticipantPanel</tt> is the panel containing data for a call
 * participant in a given call. It contains information like call participant
 * name, photo, call duration, etc. 
 * 
 * @author Yana Stamcheva
 */
public class CallParticipantPanel extends JPanel
{   
    private JPanel contactPanel = new JPanel(new BorderLayout());
    
    private JPanel namePanel = new JPanel(new GridLayout(0, 1));
    
    private JLabel nameLabel = new JLabel("", JLabel.CENTER);
     
    private JLabel timeLabel = new JLabel("00:00:00", JLabel.CENTER);
    
    private JLabel photoLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)));
    
    private Date initDate;
    private Timer timer;
    
    private JLabel stateLabel;
    
    /**
     * Creates a <tt>CallParticipantPanel</tt> for the given call participant.
     * 
     * @param participant the call participant
     */
    public CallParticipantPanel(CallParticipant participant)
    {
        super(new BorderLayout());
        
        stateLabel = new JLabel(participant.getState().getStateString(),
                JLabel.CENTER);
        
        initDate = new Date(System.currentTimeMillis());
        timer = new Timer(1000, new CallTimerListener());
        timer.setRepeats(true);
        
        if(participant.getDisplayName() != null)
            nameLabel.setText(participant.getDisplayName());
        else
            nameLabel.setText(participant.getAddress());
        
        namePanel.add(nameLabel);
        namePanel.add(stateLabel);
        namePanel.add(timeLabel);
        
        contactPanel.add(photoLabel, BorderLayout.CENTER);
        contactPanel.add(namePanel, BorderLayout.SOUTH);
        
        this.add(contactPanel, BorderLayout.NORTH);
        this.setPreferredSize(new Dimension(100, 200));
    }

    /**
     * Sets the state of the contained call participant.
     * @param state the state of the contained call participant
     */
    public void setState(String state)
    {
        this.stateLabel.setText(state);
    }
    
    /**
     * Starts the timer that counts call duration.
     */
    public void startCallTimer()
    {
        timer.start();
    }
    
    /**
     * Stops the timer that counts call duration.
     */
    public void stopCallTimer()
    {
        timer.stop();
    }

    /**
     * Each second refreshes the time label to show to the user the exact
     * duration of the call.
     */
    private class CallTimerListener implements ActionListener
    {   
        public void actionPerformed(ActionEvent e)
        {
            Date time = GuiUtils.substractDates(
                    new Date(System.currentTimeMillis()),
                    initDate);
            
            timeLabel.setText(GuiUtils.formatTime(time));
        }
    }    
}
