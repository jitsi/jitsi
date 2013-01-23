/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.presence.message;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.main.presence.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;
import net.java.sip.communicator.util.Logger;
import org.jitsi.util.*;

/**
 * The <tt>StatusMessageMenu<tt> is added to every status selector box in order
 * to enable the user to choose a status message.
 * 
 * @author Yana Stamcheva
 */
public class StatusMessageMenu
    extends JMenu
    implements ActionListener
{
    /**
     * Our logger.
     */
    private final static Logger logger
        = Logger.getLogger(StatusMessageMenu.class);

    /**
     * The prefix used to store custom status messages.
     */
    private final static String CUSTOM_MESSAGES_PREFIX =
        "service.gui.CUSTOM_STATUS_MESSAGE";

    /**
     * The prefix to search for provisioned messages.
     */
    private final static String PROVISIONED_MESSAGES_PREFIX =
        "service.gui.CUSTOM_STATUS_MESSAGE.";

    private static final String BRB_MESSAGE
        = GuiActivator.getResources().getI18NString("service.gui.BRB_MESSAGE");

    private static final String BUSY_MESSAGE
        = GuiActivator.getResources().getI18NString("service.gui.BUSY_MESSAGE");

    private JMenuItem noMessageItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.NO_MESSAGE"));

    private JMenuItem newMessageItem = new JMenuItem(
        GuiActivator.getResources().getI18NString("service.gui.NEW_MESSAGE"));

    /**
     * To clear and delete currently saved custom messages.
     */
    private JMenuItem clearCustomMessageItem = new JMenuItem(
            GuiActivator.getResources()
                .getI18NString("service.gui.CLEAR_CUSTOM_MESSAGES"));

    /**
     * The pre-set busy message.
     */
    private JCheckBoxMenuItem busyMessageItem
        = new JCheckBoxMenuItem(BUSY_MESSAGE);

    /**
     * The pre-set BRB message.
     */
    private JCheckBoxMenuItem brbMessageItem
        = new JCheckBoxMenuItem(BRB_MESSAGE);

    private ProtocolProviderService protocolProvider;

    /**
     * The parent menu of this menu.
     */
    private PresenceStatusMenu parentMenu;

    /**
     * Creates an instance of <tt>StatusMessageMenu</tt>, by specifying the
     * <tt>ProtocolProviderService</tt> to which this menu belongs.
     * 
     * @param protocolProvider the protocol provider service to which this
     * menu belongs
     */
    public StatusMessageMenu(ProtocolProviderService protocolProvider,
                             PresenceStatusMenu parentMenu)
    {
        super(GuiActivator.getResources()
            .getI18NString("service.gui.SET_STATUS_MESSAGE"));

        this.protocolProvider = protocolProvider;
        this.parentMenu = parentMenu;

        this.noMessageItem.addActionListener(this);
        this.newMessageItem.addActionListener(this);
        this.busyMessageItem.addActionListener(this);
        this.brbMessageItem.addActionListener(this);
        this.clearCustomMessageItem.addActionListener(this);

        this.add(noMessageItem);
        this.add(newMessageItem);
        this.add(clearCustomMessageItem);

        // check should we show the preset messages
        if(ConfigurationUtils.isPresetStatusMessagesEnabled())
        {
            busyMessageItem.setName(busyMessageItem.getText());
            brbMessageItem.setName(brbMessageItem.getText());

            this.addSeparator();
            this.add(busyMessageItem);
            this.add(brbMessageItem);
        }

        // load custom message
        loadCustomStatusMessageIndex();

        // load provisioned messages if any

    }

    public void actionPerformed(ActionEvent e)
    {
        JMenuItem menuItem = (JMenuItem) e.getSource();

        String statusMessage = "";

        if (menuItem.equals(newMessageItem))
        {
            NewStatusMessageDialog dialog
                = new NewStatusMessageDialog(protocolProvider, this);

            dialog.setLocation(
                Toolkit.getDefaultToolkit().getScreenSize().width/2
                    - dialog.getWidth()/2,
                Toolkit.getDefaultToolkit().getScreenSize().height/2
                    - dialog.getHeight()/2
                );

            dialog.setVisible(true);

            dialog.requestFocusInField();

            // we will set status from the Status Message Dialog
            return;
        }
        else if (menuItem.equals(clearCustomMessageItem))
        {
            // lets remove, we need the latest index removed so we can
            // remove and the separator that is before it
            int lastIx = -1;
            for(Component c : getMenuComponents())
            {
                if(c instanceof CustomMessageItems)
                {
                    lastIx = getPopupMenu().getComponentIndex(c);
                    remove((CustomMessageItems)c);
                }
            }

            // remove the separator
            if(lastIx - 1 >= 0 )
                getPopupMenu().remove(lastIx - 1);

            // and now let's delete the saved values
            java.util.List<String> customMessagesProps =
                GuiActivator.getConfigurationService()
                    .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

            for(String p : customMessagesProps)
            {
                GuiActivator.getConfigurationService().removeProperty(p);
            }
        }
        else if (menuItem.equals(busyMessageItem))
        {
            statusMessage = BUSY_MESSAGE;
        }
        else if (menuItem.equals(brbMessageItem))
        {
            statusMessage = BRB_MESSAGE;
        }
        else if (menuItem instanceof CustomMessageItems)
        {
            statusMessage = menuItem.getName();
        }
        else if (menuItem instanceof ProvisionedMessageItems)
        {
            statusMessage = menuItem.getName();
        }

        // we cannot get here after clicking 'new message'
        publishStatusMessage(statusMessage, menuItem, false);
    }

    /**
     * Publishes the new message in separate thread. If successfully ended
     * the message item is created and added to te list of current status
     * messages and if needed the message is saved.
     * @param message the message to save
     * @param menuItem the item which was clicked to set this status
     * @param saveIfNewMessage whether to save the status on the custom
     *                         statuses list.
     */
    void publishStatusMessage(String message,
                              JMenuItem menuItem,
                              boolean saveIfNewMessage)
    {
        new PublishStatusMessageThread(message, menuItem, saveIfNewMessage)
                .start();
    }

    /**
     * Returns the button for new messages.
     * @return the button for new messages.
     */
    JMenuItem getNewMessageItem()
    {
        return newMessageItem;
    }

    /**
     * Changes current message text in its item.
     * @param message
     * @param menuItem the menu item that was clicked
     * @param saveNewMessage whether to save the newly created message
     */
    private void setCurrentMessage(final String message,
                                   final JMenuItem menuItem,
                                   final boolean saveNewMessage)
    {
        if(menuItem == null)
            return;

        /// we will be working with swing components, make sure it is in EDT
        if(!SwingUtilities.isEventDispatchThread())
        {
            SwingUtilities.invokeLater(new Runnable()
            {
                public void run()
                {
                    setCurrentMessage(message, menuItem, saveNewMessage);
                }
            });

            return;
        }

        // if message is null we cleared the status message
        if(StringUtils.isNullOrEmpty(message))
        {
            clearSelectedItems();
            parentMenu.updateTitleArea();

            return;
        }

        // a ne message was created
        if(menuItem.equals(newMessageItem))
        {
            clearSelectedItems();

            CustomMessageItems newMenuItem = new CustomMessageItems(message);
            newMenuItem.setName(message);
            newMenuItem.setSelected(true);
            newMenuItem.addActionListener(this);

            int ix = getLastCustomMessageIndex();
            if(ix == -1)
            {
                this.addSeparator();
                this.add(newMenuItem);
            }
            else
            {
                insert(newMenuItem, ix + 1);
            }

            parentMenu.updateTitleArea();

            if(saveNewMessage)
            {
                // lets save it
                int saveIx = getLastCustomStatusMessageIndex();
                GuiActivator.getConfigurationService().setProperty(
                    CUSTOM_MESSAGES_PREFIX + "." +  String.valueOf(saveIx + 1),
                    message
                );
            }
        }
        else if(menuItem instanceof JCheckBoxMenuItem)
        {
            clearSelectedItems();

            menuItem.setSelected(true);
            menuItem.setText("<html><b>" + menuItem.getName() +"</b></html>");
            parentMenu.updateTitleArea();
        }
    }

    /**
     * Searches for custom messages in configuration service and
     * gets the highest index.
     * @return the highest index of custom status messages.
     */
    private int getLastCustomStatusMessageIndex()
    {
        int ix = -1;

        java.util.List<String> customMessagesProps =
            GuiActivator.getConfigurationService()
                .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

        for(String p : customMessagesProps)
        {
            String s = p.substring(CUSTOM_MESSAGES_PREFIX.length() + 1);

            try
            {
                int i = Integer.parseInt(s);
                if(i > ix)
                    ix = i;
            }
            catch(Throwable t)
            {}
        }

        return ix;
    }

    /**
     * Loads the previously saved custom status messages.
     */
    private void loadCustomStatusMessageIndex()
    {
        java.util.List<String> customMessagesProps =
            GuiActivator.getConfigurationService()
                .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

        if(customMessagesProps.size() > 0)
        {
            this.addSeparator();
        }

        for(String p : customMessagesProps)
        {
            String message =
                GuiActivator.getConfigurationService().getString(p);

            CustomMessageItems newMenuItem = new CustomMessageItems(message);
            newMenuItem.setName(message);
            newMenuItem.addActionListener(this);

            this.add(newMenuItem);
        }
    }

    /**
     * Clears all items that they are not selected and its name is not bold.
     */
    private void clearSelectedItems()
    {
        for(Component c : getMenuComponents())
        {
            if(c instanceof JCheckBoxMenuItem)
            {
                JCheckBoxMenuItem checkItem =
                    (JCheckBoxMenuItem)c;
                checkItem.setSelected(false);
                checkItem.setText(checkItem.getName());
            }
        }
    }

    /**
     * Checks for the index of the last component of CustomMessageItems class.
     * @return the last component index of CustomMessageItems.
     */
    private int getLastCustomMessageIndex()
    {
        int ix = -1;
        JPopupMenu popupMenu = getPopupMenu();
        for(Component c : getMenuComponents())
        {
            if(c instanceof CustomMessageItems)
            {
                int cIx = popupMenu.getComponentIndex(c);
                if(cIx > ix)
                    ix = cIx;
            }
        }

        return ix;
    }

    public String getCurrentMessage()
    {
        for(Component c : getMenuComponents())
        {
            if(c instanceof JCheckBoxMenuItem
               && ((JCheckBoxMenuItem) c).isSelected())
            {
                return c.getName();
            }
        }

        return null;
    }

    /**
     * The custom message items.
     */
    private class CustomMessageItems
        extends JCheckBoxMenuItem
    {
        public CustomMessageItems(String text)
        {
            super(text);
        }
    }

    /**
     * The provisioned message items.
     */
    private class ProvisionedMessageItems
        extends JCheckBoxMenuItem
    {
        public ProvisionedMessageItems(String text)
        {
            super(text);
        }
    }

    /**
     *  This class allow to use a thread to change the presence status message.
     */
    private class PublishStatusMessageThread extends Thread
    {
        private String message;

        private PresenceStatus currentStatus;

        private OperationSetPresence presenceOpSet;

        private JMenuItem menuItem;

        private boolean saveIfNewMessage;

        public PublishStatusMessageThread(
                    String message,
                    JMenuItem menuItem,
                    boolean saveIfNewMessage)
        {
            this.message = message;

            presenceOpSet
                = protocolProvider.getOperationSet(OperationSetPresence.class);

            this.currentStatus = presenceOpSet.getPresenceStatus();

            this.menuItem = menuItem;

            this.saveIfNewMessage = saveIfNewMessage;
        }

        public void run()
        {
            try
            {
                presenceOpSet.publishPresenceStatus(currentStatus, message);

                setCurrentMessage(message, menuItem, saveIfNewMessage);
            }
            catch (IllegalArgumentException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (IllegalStateException e1)
            {

                logger.error("Error - changing status", e1);
            }
            catch (OperationFailedException e1)
            {
                
                if (e1.getErrorCode()
                    == OperationFailedException.GENERAL_ERROR)
                {
                    logger.error(
                        "General error occured while "
                        + "publishing presence status.",
                        e1);
                }
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .NETWORK_FAILURE) 
                {
                    logger.error(
                        "Network failure occured while "
                        + "publishing presence status.",
                        e1);
                } 
                else if (e1.getErrorCode()
                        == OperationFailedException
                            .PROVIDER_NOT_REGISTERED) 
                {
                    logger.error(
                        "Protocol provider must be"
                        + "registered in order to change status.",
                        e1);
                }
            }
        }
    }
}
