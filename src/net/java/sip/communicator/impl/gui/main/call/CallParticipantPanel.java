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
    
    private Date startDate;
    
    private Date endDate;
    
    private Timer timer;
    
    private JLabel stateLabel;
    
    private String callType;
    
    /**
     * Creates a <tt>CallParticipantPanel</tt> for the given call participant.
     * 
     * @param participant the call participant
     * @param callType the call type. INCOMING_CALL or OUTGOING_CALL
     */
    public CallParticipantPanel(CallParticipant participant)
    {
        super(new BorderLayout());
        
        this.stateLabel = new JLabel(participant.getState().getStateString(),
                JLabel.CENTER);
        
        this.startDate = new Date(System.currentTimeMillis());
        this.timer = new Timer(1000, new CallTimerListener());
        this.timer.setRepeats(true);
        
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
                    startDate);
            
            endDate = time;
            timeLabel.setText(GuiUtils.formatTime(time));
        }
    }

    /**
     * Returns the start time of the contained participant call.
     * 
     * @return the start time of the contained participant call
     */
    public Date getStartTime()
    {        
        return startDate;
    }
    
    /**
     * Returns the start time of the contained participant call.
     * 
     * @return the start time of the contained participant call
     */
    public Date getEndTime()
    {
        if(endDate == null)
            endDate = startDate;
        
        return endDate;
    }
    
    /**
     * Returns this call type : INCOMING_CALL or OUTGOING_CALL
     * @return Returns this call type : INCOMING_CALL or OUTGOING_CALL
     */
    public String getCallType()
    {
        return GuiCallParticipantRecord.INCOMING_CALL;
    }
    
    public void setCallType(String callType)
    {
        this.callType = callType;
    }
}
