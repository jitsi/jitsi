/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.util.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.icqconstants.*;
import net.java.sip.communicator.util.*;

/**
 * The <tt>StatusSelectorBox</tt> is a <tt>SIPCommSelectorBox</tt> that contains
 * the list of statuses for a protocol provider. This is where the user could
 * select its status.
 * 
 * @author Yana Stamcheva
 */
public class StatusSelectorBox extends SIPCommSelectorBox {

    private Logger logger = Logger.getLogger(StatusSelectorBox.class.getName());

    private MainFrame mainFrame;

    private BufferedImage[] animatedImageArray;

    private Connecting connecting = new Connecting();

    private ProtocolProviderService protocolProvider;

    private Map itemsMap;

    /**
     * Creates an instance of <tt>StatusSelectorBox</tt>.
     * 
     * @param mainFrame The main application window.
     * @param protocolProvider The protocol provider.
     */
    public StatusSelectorBox(MainFrame mainFrame,
            ProtocolProviderService protocolProvider) {
        super();

        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
    }

    /**
     * Creates an instance of <tt>StatusSelectorBox</tt> and initializes
     * the selector box with data.
     * 
     * @param mainFrame The main application window.
     * @param protocolProvider The protocol provider.
     * @param itemsMap The list from which to initialize the selector box.
     * @param selectedItem The initially selected item.
     */
    public StatusSelectorBox(MainFrame mainFrame,
            ProtocolProviderService protocolProvider,
            Map itemsMap,
            Image selectedItem) {
        super(selectedItem);

        this.itemsMap = itemsMap;
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;

        this.setToolTipText(protocolProvider.getAccountID().getUserID());
        
        this.init();
    }

    /**
     * Constructs the list of choices of the selector box.
     */
    public void init() {
        Iterator iter = itemsMap.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            this.addItem(((IcqStatusEnum) entry.getKey()).getStatusName(),
                    new ImageIcon((Image) entry.getValue()),
                    new ItemActionListener());
        }
    }

    /**
     * Handles the <tt>ActionEvent</tt> triggered when one of the items
     * in the list is selected.
     */
    private class ItemActionListener implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JMenuItem) {

                JMenuItem menuItem = (JMenuItem) e.getSource();

                if(!protocolProvider.isRegistered()) {
                    mainFrame.getLoginManager().login(protocolProvider);
                }
                else {
                    OperationSetPresence presence = mainFrame
                            .getProtocolPresence(protocolProvider);
    
                    Iterator statusSet = presence.getSupportedStatusSet();
    
                    while (statusSet.hasNext()) {
    
                        PresenceStatus status = ((PresenceStatus) statusSet.next());
    
                        if (status.getStatusName().equals(menuItem.getText())
                                && !presence.getPresenceStatus().equals(status)) {
    
                            try {
                                if (status.equals(IcqStatusEnum.ONLINE)) {
    
                                    presence.publishPresenceStatus(status, "");
                                    
                                } else if (status.equals(IcqStatusEnum.OFFLINE)) {
                                    protocolProvider.unregister();
                                } else {
    
                                    presence.publishPresenceStatus(status, "");
                                }
                            } catch (IllegalArgumentException e1) {
    
                                logger.error("Error - changing status", e1);
    
                            } catch (IllegalStateException e1) {
    
                                logger.error("Error - changing status", e1);
    
                            } catch (OperationFailedException e1) {
    
                                if (e1.getErrorCode() 
                                    == OperationFailedException.GENERAL_ERROR) {
                                    SIPCommMsgTextArea msgText 
                                    = new SIPCommMsgTextArea(Messages
                                        .getString("statusChangeGeneralError"));
                                    
                                    JOptionPane.showMessageDialog(null, msgText,
                                            Messages.getString("generalError"),
                                            JOptionPane.ERROR_MESSAGE);
                                }
                                else if (e1.getErrorCode() 
                                        == OperationFailedException
                                            .NETWORK_FAILURE) {
                                    SIPCommMsgTextArea msgText 
                                        = new SIPCommMsgTextArea(
                                            Messages.getString(
                                                "statusChangeNetworkFailure"));
                                    
                                    JOptionPane.showMessageDialog(
                                        null,
                                        msgText,
                                        Messages.getString("networkFailure"),
                                        JOptionPane.ERROR_MESSAGE);
                                } 
                                else if (e1.getErrorCode()
                                        == OperationFailedException
                                            .PROVIDER_NOT_REGISTERED) {
                                    SIPCommMsgTextArea msgText 
                                        = new SIPCommMsgTextArea(
                                            Messages.getString(
                                                "statusChangeNetworkFailure"));
                                    
                                    JOptionPane.showMessageDialog(
                                        null,
                                        msgText,
                                        Messages.getString("networkFailure"),
                                        JOptionPane.ERROR_MESSAGE);
                                }
                                logger.error("Error - changing status", e1);
                            }
                            break;
                        }
                    }
                }
                setSelected(menuItem);
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
    public void startConnecting(BufferedImage[] images) {

        this.animatedImageArray = images;

        this.setIcon(new ImageIcon(images[0]));

        this.connecting.start();
    }

    /**
     * Stops the timer that manages the connecting animated icon.
     */
    public void stopConnecting() {

        this.connecting.stop();
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

                StatusSelectorBox.this.setIcon(new ImageIcon(
                        animatedImageArray[j]));
                j = (j + 1) % animatedImageArray.length;
            }

        }
    }
}
