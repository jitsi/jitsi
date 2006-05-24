/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.Image;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.Timer;

import net.java.sip.communicator.impl.gui.main.customcontrols.SIPCommSelectorBox;
import net.java.sip.communicator.impl.gui.main.i18n.Messages;
import net.java.sip.communicator.service.protocol.OperationFailedException;
import net.java.sip.communicator.service.protocol.OperationSetPresence;
import net.java.sip.communicator.service.protocol.PresenceStatus;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.service.protocol.icqconstants.IcqStatusEnum;
import net.java.sip.communicator.util.Logger;

/**
 * The component containging the list of statuses for a protocol,
 * where the user could change its status.
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

    public StatusSelectorBox(MainFrame mainFrame,
            ProtocolProviderService protocolProvider) {
        super();

        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;
    }

    public StatusSelectorBox(MainFrame mainFrame,
            ProtocolProviderService protocolProvider, Map itemsMap,
            Image selectedItem) {
        super(selectedItem);

        this.itemsMap = itemsMap;
        this.mainFrame = mainFrame;
        this.protocolProvider = protocolProvider;

        this.init();
    }

    public void init() {
        Iterator iter = itemsMap.entrySet().iterator();

        while (iter.hasNext()) {
            Map.Entry entry = (Map.Entry) iter.next();

            this.addItem(((IcqStatusEnum) entry.getKey()).getStatusName(),
                    new ImageIcon((Image) entry.getValue()),
                    new ItemActionListener());
        }
    }

    private class ItemActionListener implements ActionListener {

        public void actionPerformed(ActionEvent e) {

            if (e.getSource() instanceof JMenuItem) {

                JMenuItem menuItem = (JMenuItem) e.getSource();

                OperationSetPresence presence = mainFrame
                        .getProtocolPresence(protocolProvider);

                Iterator statusSet = presence.getSupportedStatusSet();

                while (statusSet.hasNext()) {

                    PresenceStatus status = ((PresenceStatus) statusSet.next());

                    if (status.getStatusName().equals(menuItem.getText())
                            && !presence.getPresenceStatus().equals(status)) {

                        try {

                            if (status.equals(IcqStatusEnum.ONLINE)) {

                                if (protocolProvider.isRegistered()) {

                                    presence.publishPresenceStatus(status, "");
                                } else {
                                    protocolProvider.register(null);
                                }
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
                                JOptionPane.showMessageDialog(null, Messages
                                        .getString("statusChangeGeneralError"),
                                        Messages.getString("generalError"),
                                        JOptionPane.ERROR_MESSAGE);
                            }
                            else if (e1.getErrorCode() 
                                    == OperationFailedException.NETWORK_FAILURE) {

                                JOptionPane.showMessageDialog(
                                    null,
                                    Messages.getString("statusChangeNetworkFailure"),
                                    Messages.getString("networkFailure"),
                                    JOptionPane.ERROR_MESSAGE);
                            } 
                            else if (e1.getErrorCode()
                                    == OperationFailedException.PROVIDER_NOT_REGISTERED) {
                                JOptionPane.showMessageDialog(
                                    null,
                                    Messages.getString("statusChangeNetworkFailure"),
                                    Messages.getString("networkFailure"),
                                    JOptionPane.ERROR_MESSAGE);
                            }
                            logger.error("Error - changing status", e1);
                        }
                        break;
                    }
                }
                setSelected(menuItem);
            }
        }
    }

    public void startConnecting(BufferedImage[] images) {

        this.animatedImageArray = images;

        this.setIcon(new ImageIcon(images[0]));

        this.connecting.start();
    }

    public void stopConnecting() {

        this.connecting.stop();
    }

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
