/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main.presence;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>SimpleStatusSelectorBox</tt> is a <tt>SIPCommMenu</tt> that
 * contains two statuses ONLINE and OFFLINE. It's used to represent the status
 * of a protocol provider which doesn't support presence operation set.
 * 
 * @author Yana Stamcheva
 */
public class SimpleStatusSelectorBox
    extends StatusSelectorBox
    implements ActionListener
{

    private Logger logger = Logger.getLogger(
            PresenceStatusSelectorBox.class.getName());

    private MainFrame mainFrame;

    private BufferedImage[] animatedImageArray;

    private Connecting connecting = new Connecting();

    private ProtocolProviderService protocolProvider;
    
    private ImageIcon onlineIcon 
        = new ImageIcon(ImageLoader.getImage(ImageLoader.SIP_LOGO));
    
    private ImageIcon offlineIcon
        =  new ImageIcon(LightGrayFilter.createDisabledImage(
                ImageLoader.getImage(ImageLoader.SIP_LOGO)));
    
    private JMenuItem onlineItem = new JMenuItem(
            Messages.getString("online"),
            onlineIcon);
    
    private JMenuItem offlineItem = new JMenuItem(
            Messages.getString("offline"),
            offlineIcon);
    
    private int accountIndex;
    
    /**
     * Creates an instance of <tt>SimpleStatusSelectorBox</tt>.
     * 
     * @param mainFrame The main application window.
     * @param protocolProvider The protocol provider.
     * @param accountIndex If we have more than one account for a protocol,
     * each account has an index.
     */
    public SimpleStatusSelectorBox(MainFrame mainFrame,
            ProtocolProviderService protocolProvider,
            int accountIndex)
    {
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
        this.accountIndex = accountIndex;
        
        this.setToolTipText(protocolProvider.getAccountID().getUserID());
                
        onlineItem.setName("online");
        offlineItem.setName("offline");
        
        onlineItem.addActionListener(this);
        offlineItem.addActionListener(this);
        
        this.add(onlineItem);
        this.add(offlineItem);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */    
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();
        
        if(itemName.equals("online")) {
            if(!protocolProvider.isRegistered()) {
                this.mainFrame.getLoginManager().login(protocolProvider);
            }
        }
        else {
            try {
                mainFrame.getLoginManager()
                    .setManuallyDisconnected(true);
                protocolProvider.unregister();
            }
            catch (OperationFailedException e1) {
                logger.error("Unable to unregister the protocol provider: "
                        + protocolProvider
                        + " due to the following exception: " + e1);
            }
        }
    }


    /**
     * Starts the timer that changes the images given by the array, thus
     * creating an animated image that indicates that the user is connecting.
     * 
     * @param images A <tt>BufferedImage</tt> array that contains all images
     * from which to create the animated image indicating the connecting state.
     */
    public void startConnecting(BufferedImage[] images)
    {
        this.animatedImageArray = images;

        this.setIcon(new ImageIcon(images[0]));

        this.connecting.start();
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus()
    {        
        this.connecting.stop();
        
        if(protocolProvider.isRegistered()) {
            setSelected(onlineItem, onlineIcon); 
        }
        else {
            setSelected(offlineItem, offlineIcon);
        }
    }

    /**
     * A <tt>Timer</tt> that creates an animated icon, which indicates the
     * connecting state.
     */
    private class Connecting extends Timer {

        public Connecting() {

            super(100, null);

            this.addActionListener(new TimerActionListener());
        }

        private class TimerActionListener implements ActionListener {

            private int j = 1;

            public void actionPerformed(ActionEvent evt) {

                setIcon(new ImageIcon(animatedImageArray[j]));
                j = (j + 1) % animatedImageArray.length;
            }

        }
    }

    public int getAccountIndex()
    {
        return accountIndex;
    }

    public void setAccountIndex(int accountIndex)
    {
        this.accountIndex = accountIndex;
    }
    
    public void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        
        if(accountIndex > 0) {
            AntialiasingManager.activateAntialiasing(g);
            g.setColor(Color.DARK_GRAY);
            g.setFont(Constants.FONT.deriveFont(Font.BOLD, 9));
            g.drawString(new Integer(accountIndex).toString(), 20, 12);
        }
    }

    public void updateStatus(Object status)
    {}
}
