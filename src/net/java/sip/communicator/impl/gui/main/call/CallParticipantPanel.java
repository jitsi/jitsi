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
    JPanel contactPanel = new JPanel(new BorderLayout());
    
    JPanel namePanel = new JPanel(new GridLayout(0, 1));
    
    JLabel nameLabel = new JLabel("", JLabel.CENTER);
    
    JLabel timeLabel = new JLabel("00:00:00", JLabel.CENTER);
    
    JLabel photoLabel = new JLabel(new ImageIcon(
            ImageLoader.getImage(ImageLoader.DEFAULT_USER_PHOTO)));
    
    Timer timer = new Timer(1000, new CallTimerListener());
    
    JLabel stateLabel;
    
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
        private int timer = 0;
        
        public void actionPerformed(ActionEvent e)
        {
            timer ++;
            int s = timer%60;
            int m = (timer - s)/60;
            int h = m/60;
            
            timeLabel.setText(processTime(h)
                    + ":" + processTime(m)
                    + ":" + processTime(s));
        }
    }

    /**
     * Adds a 0 in the beginning of one digit numbers.
     *
     * @param time The time parameter could be hours, minutes or seconds.
     * @return The formatted minutes string.
     */
    private String processTime(int time)
    {
        String timeString = new Integer(time).toString();

        String resultString = "";
        if (timeString.length() < 2)
            resultString = resultString.concat("0").concat(timeString);
        else
            resultString = timeString;

        return resultString;
    }
    
    
}
