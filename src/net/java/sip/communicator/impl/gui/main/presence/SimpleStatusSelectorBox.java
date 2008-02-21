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

    private ImageIcon onlineIcon;

    private ImageIcon offlineIcon;

    private JMenuItem onlineItem = new JMenuItem(
            Messages.getI18NString("online").getText(),
            onlineIcon);

    private JMenuItem offlineItem = new JMenuItem(
            Messages.getI18NString("offline").getText(),
            offlineIcon);

    private int accountIndex;

    private JLabel titleLabel;

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

        this.setPreferredSize(new Dimension(28, 24));

        this.onlineIcon = new ImageIcon(
                ImageLoader.getBytesInImage(protocolProvider.getProtocolIcon()
                        .getIcon(ProtocolIcon.ICON_SIZE_16x16)));
        
        this.offlineIcon = new ImageIcon(
            LightGrayFilter.createDisabledImage(
                ImageLoader.getBytesInImage(protocolProvider.getProtocolIcon()
                    .getIcon(ProtocolIcon.ICON_SIZE_16x16))));
        
        String tooltip = "<html><b>"
            + protocolProvider.getAccountID().getUserID()
            + "</b><br>Offline</html>";

        this.setToolTipText(tooltip);

        onlineItem.setName(Constants.ONLINE_STATUS);
        offlineItem.setName(Constants.OFFLINE_STATUS);

        onlineItem.addActionListener(this);
        offlineItem.addActionListener(this);

        titleLabel = new JLabel(protocolProvider.getAccountID().getUserID());

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 0));
        titleLabel.setFont(Constants.FONT.deriveFont(Font.BOLD));

        this.add(titleLabel);
        this.addSeparator();

        this.add(onlineItem);
        this.add(offlineItem);
        
        setSelected(offlineItem, offlineIcon);
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */
    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();
        String itemName = menuItem.getName();

        if(itemName.equals(Constants.ONLINE_STATUS))
        {
            if(!protocolProvider.isRegistered()) {
                this.mainFrame.getLoginManager().login(protocolProvider);
            }
        }
        else
        {
            if(    !protocolProvider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERED)
                && !protocolProvider.getRegistrationState()
                            .equals(RegistrationState.UNREGISTERING))
            {
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

        saveStatusInformation(protocolProvider, itemName);
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
        
        String tooltip = this.getToolTipText();
        
        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));
        
        this.setToolTipText(tooltip.concat("<br>Connecting"));
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void updateStatus()
    {
        this.connecting.stop();

        String tooltip = this.getToolTipText();
        
        tooltip = tooltip.substring(0, tooltip.lastIndexOf("<br>"));
        
        if(protocolProvider.isRegistered()) {
            setSelected(onlineItem, onlineIcon);
        
            this.setToolTipText(tooltip.concat("<br>" + onlineItem.getText()));
        }
        else {
            setSelected(offlineItem, offlineIcon);

            this.setToolTipText(tooltip.concat("<br>" + offlineItem.getText()));
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
            g.drawString(new Integer(accountIndex+1).toString(), 20, 12);
        }
    }

    public void updateStatus(Object status)
    {}
}
