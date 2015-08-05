/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.plugin.desktoputil.presence;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;

import javax.swing.*;

import net.java.sip.communicator.plugin.desktoputil.*;
import net.java.sip.communicator.util.*;
import org.jitsi.service.resources.*;
import org.jitsi.util.*;

/**
 * The <tt>AbstractStatusMessageMenu<tt> is added to every status selector box
 * in order to enable the user to choose a status message.
 *
 * @author Yana Stamcheva
 * @author Damian Minkov
 */
public abstract class AbstractStatusMessageMenu
    implements ActionListener,
               ItemListener,
               PropertyChangeListener
{
    /**
     * Property used to fire property change that the status message
     * has changed.
     */
    public static final String STATUS_MESSAGE_UPDATED_PROP =
        "STATUS_MESSAGE_UPDATED";

    /**
     * Property used to fire property change that the custom status messages
     * have changed, a new one has been added or they are cleared.
     */
    public static final String CUSTOM_STATUS_MESSAGES_UPDATED_PROP =
        "CUSTOM_STATUS_MESSAGES_UPDATED";

    /**
     * The prefix used to store custom status messages.
     */
    private final static String CUSTOM_MESSAGES_PREFIX =
        "service.gui.CUSTOM_STATUS_MESSAGE";

    /**
     * The prefix to search for provisioned messages.
     */
    private final static String PROVISIONED_MESSAGES_PREFIX =
        "service.gui.PROVISIONED_STATUS_MESSAGE";

    private static final String BRB_MESSAGE
        = DesktopUtilActivator.getResources()
        .getI18NString("service.gui.BRB_MESSAGE");

    private static final String BUSY_MESSAGE
        = DesktopUtilActivator.getResources()
        .getI18NString("service.gui.BUSY_MESSAGE");

    private Object noMessageItem;

    private Object newMessageItem;

    /**
     * To clear and delete currently saved custom messages.
     */
    private Object clearCustomMessageItem;

    /**
     * The pre-set busy message.
     */
    private Object busyMessageItem;

    /**
     * The pre-set BRB message.
     */
    private Object brbMessageItem;

    /**
     * The menu we will be populating.
     */
    private Object menu;

    /**
     * All property change listeners registered so far.
     * Static so we can communicate between status message menus.
     */
    private static java.util.List<PropertyChangeListener>
        propertyChangeListeners = new ArrayList<PropertyChangeListener>();

    /**
     * Creates an instance of <tt>AbstractStatusMessageMenu</tt>.
     *
     * @param swing should we use swing or awt
     */
    public AbstractStatusMessageMenu(boolean swing)
    {
        ResourceManagementService R = DesktopUtilActivator.getResources();

        String text = R.getI18NString("service.gui.SET_STATUS_MESSAGE");
        if (swing)
        {
            JMenu menuInstance = new JMenu(text);

            Icon icon = getMenuIcon();

            if(icon != null)
                menuInstance.setIcon(icon);

            menu = menuInstance;
        }
        else
        {
            menu = new Menu(text);
        }

        noMessageItem = createMenuItem(
            R.getI18NString("service.gui.NO_MESSAGE"));
        newMessageItem = createMenuItem(
            R.getI18NString("service.gui.NEW_MESSAGE"));
        clearCustomMessageItem = createMenuItem(
            R.getI18NString("service.gui.CLEAR_CUSTOM_MESSAGES"));

        // check should we show the preset messages
        if(ConfigurationUtils.isPresetStatusMessagesEnabled())
        {
            this.addSeparator();
            busyMessageItem = createsCheckBoxMenuItem(BUSY_MESSAGE);
            brbMessageItem = createsCheckBoxMenuItem(BRB_MESSAGE);
        }
        else
        {
            busyMessageItem = null;
            brbMessageItem = null;
        }

        // load provisioned messages if any
        loadProvisionedStatusMessages();

        // load custom message
        loadCustomStatusMessages();

        addPropertyChangeListener(this);
    }

    /**
     * Creates the appropriate menu item. Depending on the
     * menu.
     * @param text the menu item text.
     * @return the item.
     */
    private Object createMenuItem(String text)
    {
        if (menu instanceof JMenu)
        {
            JMenuItem menuItem = new JMenuItem(text);
            menuItem.setName(text);
            menuItem.addActionListener(this);
            ((JMenu) menu).add(menuItem);
            return menuItem;
        }
        else
        {
            MenuItem menuItem = new MenuItem(text);
            menuItem.setName(text);
            menuItem.addActionListener(this);
            ((Menu) menu).add(menuItem);
            return menuItem;
        }
    }

    /**
     * Creates the appropriate menu item. Depending on the
     * menu.
     * @param text the menu item text.
     * @return the item.
     */
    private Object createsCheckBoxMenuItem(String text)
    {
        if (menu instanceof JMenu)
        {
            JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(text);
            menuItem.setName(text);
            menuItem.addActionListener(this);
            ((JMenu) menu).add(menuItem);
            return menuItem;
        }
        else
        {
            CheckboxMenuItem menuItem = new CheckboxMenuItem(text);
            menuItem.setName(text);
            menuItem.addItemListener(this);
            ((Menu) menu).add(menuItem);
            return menuItem;
        }
    }

    /**
     * Creates a separator.
     */
    private void addSeparator()
    {
        if (menu instanceof JMenu)
            ((JMenu) menu).addSeparator();
        else
            ((Menu) menu).addSeparator();
    }

    /**
     * Returns the menu used for status messages.
     * When AbstractStatusMessageMenu created with swing==true this returns
     * javax.swing.JMenu and if it is false it returns java.awt.Menu.
     *
     * @return the menu.
     */
    public Object getMenu()
    {
        return menu;
    }

    /**
     * Returns the menu components count.
     * @return the menu components count.
     */
    private int getMenuComponentCount()
    {
        if (menu instanceof JMenu)
            return ((JMenu) menu).getMenuComponentCount();
        else
            return ((Menu) menu).getItemCount();
    }

    /**
     * Returns array of menu components.
     * @return array of menu components.
     */
    private Object[] getMenuComponents()
    {
        if (menu instanceof JMenu)
            return ((JMenu) menu).getMenuComponents();
        else
        {
            int c = ((Menu) menu).getItemCount();
            Object[] res = new Object[c];
            for(int i = 0; i < c; i++)
            {
                res[i] = ((Menu) menu).getItem(i);
            }
            return res;
        }
    }

    /**
     * Returns the menu component on the specified index.
     * @param index the index for the component.
     * @return the menu component.
     */
    private Object getMenuComponent(int index)
    {
        if (menu instanceof JMenu)
            return ((JMenu) menu).getMenuComponent(index);
        else
            return ((Menu) menu).getItem(index);
    }

    /**
     * Removes the menu component at the specified index.
     * @param index of the component to remove.
     */
    private void removeMenuComponent(int index)
    {
        if (menu instanceof JMenu)
            ((JMenu) menu).remove(index);
        else
            ((Menu) menu).remove(index);
    }

    /**
     * Removes the component and return the index it was using.
     * @param item the item to remove.
     * @return the index the item was placed before removing.
     */
    private int removeMenuComponent(Object item)
    {
        if (menu instanceof JMenu)
        {
            int ix = ((JMenu) menu).getPopupMenu()
                .getComponentIndex((JMenuItem)item);
            ((JMenu) menu).remove((JMenuItem)item);
            return ix;
        }
        else
        {
            int ix = ((MenuItem)item).getAccessibleContext()
                .getAccessibleIndexInParent();
            ((Menu) menu).remove((MenuItem)item);
            return ix;
        }
    }

    /**
     * Action is performed on any of the items.
     * @param e the event
     */
    public void actionPerformed(ActionEvent e)
    {
        actionPerformed(e.getSource());
    }

    /**
     * Returns the currently set status message.
     * @return the currently set status message.
     */
    public abstract String getCurrentStatusMessage();

    /**
     * Performs action on the selected menuItem.
     * @param menuItem the selected menu item.
     */
    public void actionPerformed(Object menuItem)
    {
        String statusMessage = "";

        if (menuItem.equals(newMessageItem))
        {
            String currentStatusMessage = getCurrentStatusMessage();
            NewStatusMessageDialog dialog = new NewStatusMessageDialog(
                currentStatusMessage == null ?
                    "" : currentStatusMessage,
                this);

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
            removeAllCustomStatusMessages();

            // and now let's delete the saved values
            java.util.List<String> customMessagesProps =
                DesktopUtilActivator.getConfigurationService()
                    .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

            for(String p : customMessagesProps)
            {
                DesktopUtilActivator.getConfigurationService()
                    .removeProperty(p);
            }

            // fire that a change has occur
            fireCustomStatusMessagesUpdated();
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
            statusMessage = ((CustomMessageItems)menuItem).getName();
        }
        else if (menuItem instanceof ProvisionedMessageItems)
        {
            statusMessage = ((ProvisionedMessageItems)menuItem).getName();
        }

        // we cannot get here after clicking 'new message'
        publishStatusMessage(statusMessage, menuItem, false);

        setCurrentMessage(statusMessage, menuItem, false);
    }

    /**
     * Action on any of the CheckboxItem.
     * @param e the event.
     */
    public void itemStateChanged(ItemEvent e)
    {
        actionPerformed(e.getSource());
    }

    /**
     * Will remove and clear all custom status messages.
     */
    private void removeAllCustomStatusMessages()
    {
        // lets remove, we need the latest index removed so we can
        // remove and the separator that is before it
        int lastIx = -1;
        for(Object c : getMenuComponents())
        {
            if(c instanceof CustomMessageItems)
            {
                lastIx = removeMenuComponent(c);
            }
        }

        // remove the separator
        if(lastIx - 1 >= 0 )
            removeMenuComponent(lastIx - 1);
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
    public abstract void publishStatusMessage(
        String message,
        Object menuItem,
        boolean saveIfNewMessage);

    /**
     * Returns the button for new messages.
     * @return the button for new messages.
     */
    Object getNewMessageItem()
    {
        return newMessageItem;
    }

    /**
     * Changes current message text in its item.
     * @param message
     * @param menuItem the menu item that was clicked
     * @param saveNewMessage whether to save the newly created message
     */
    protected void setCurrentMessage(final String message,
                                   final Object menuItem,
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

        String oldMesage = getCurrentMessage();

        // if message is null we cleared the status message
        if(StringUtils.isNullOrEmpty(message))
        {
            clearSelectedItems();

            fireStatusMessageUpdated(oldMesage, null);

            return;
        }

        // a ne message was created
        if(menuItem.equals(newMessageItem))
        {
            clearSelectedItems();

            int ix = getLastCustomMessageIndex();
            if(ix == -1)
            {
                this.addSeparator();
                createsCustomMessageItem(message, -1, true);
            }
            else
            {
                createsCustomMessageItem(message, ix + 1, true);
            }

            if(saveNewMessage)
            {
                // lets save it
                int saveIx = getLastCustomStatusMessageIndex();
                DesktopUtilActivator.getConfigurationService().setProperty(
                    CUSTOM_MESSAGES_PREFIX + "." +  String.valueOf(saveIx + 1),
                    message
                );
            }

            // fire that we have added a new message
            fireCustomStatusMessagesUpdated();

            fireStatusMessageUpdated(oldMesage, message);
        }
        else if(menuItem instanceof JCheckBoxMenuItem
            || menuItem instanceof CheckboxMenuItem)
        {
            clearSelectedItems();

            selectMenuItem(menuItem, oldMesage);
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
            DesktopUtilActivator.getConfigurationService()
                .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

        int prefixLen = CUSTOM_MESSAGES_PREFIX.length() + 1;
        for(String p : customMessagesProps)
        {
            if(prefixLen > p.length())
                continue;

            String s = p.substring(prefixLen);

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
    private void loadCustomStatusMessages()
    {
        java.util.List<String> customMessagesProps =
            DesktopUtilActivator.getConfigurationService()
                .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

        if(customMessagesProps.size() > 0)
        {
            this.addSeparator();
        }

        for(String p : customMessagesProps)
        {
            String message =
                DesktopUtilActivator.getConfigurationService().getString(p);

            createsCustomMessageItem(message, -1, false);
        }
    }

    /**
     * Loads the provisioned status messages.
     */
    private void loadProvisionedStatusMessages()
    {
        java.util.List<String> provMessagesProps =
            DesktopUtilActivator.getConfigurationService()
                .getPropertyNamesByPrefix(PROVISIONED_MESSAGES_PREFIX, false);

        if(provMessagesProps.size() > 0)
        {
            this.addSeparator();
        }

        for(String p : provMessagesProps)
        {
            String message =
                DesktopUtilActivator.getConfigurationService().getString(p);

            createsProvisionedMessageItem(message);
        }
    }

    /**
     * Clears all items that they are not selected and its name is not bold.
     */
    public void clearSelectedItems()
    {
        for(int i = 0; i < getMenuComponentCount(); i++)
        {
            Object c = getMenuComponent(i);
            if(c instanceof JCheckBoxMenuItem)
            {
                JCheckBoxMenuItem checkItem =
                    (JCheckBoxMenuItem)c;
                checkItem.setSelected(false);
                checkItem.setText(checkItem.getName());
            }
            else if(c instanceof CheckboxMenuItem)
            {
                CheckboxMenuItem checkItem =
                    (CheckboxMenuItem)c;
                checkItem.setState(false);
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
        for(int i = 0; i < getMenuComponentCount(); i++)
        {
            Object c = getMenuComponent(i);
            if(c instanceof CustomMessageItems)
            {
                if(i > ix)
                    ix = i;
            }
        }

        return ix;
    }

    public String getCurrentMessage()
    {
        for(int i = 0; i < getMenuComponentCount(); i++)
        {
            Object c = getMenuComponent(i);
            if(c instanceof JCheckBoxMenuItem
                && ((JCheckBoxMenuItem) c).isSelected())
            {
                return ((JCheckBoxMenuItem) c).getName();
            }
            else if(c instanceof CheckboxMenuItem
                && ((CheckboxMenuItem) c).getState())
            {
                return ((CheckboxMenuItem) c).getName();
            }
        }

        return null;
    }

    /**
     * Add a PropertyChangeListener to the listener list.
     * The listener is registered for all properties.
     *
     * @param listener  The PropertyChangeChangeListener to be added
     */
    public void addPropertyChangeListener(
        PropertyChangeListener listener)
    {
        synchronized(propertyChangeListeners)
        {
            if(!propertyChangeListeners.contains(listener))
                propertyChangeListeners.add(listener);
        }
    }

    /**
     * Remove a PropertyChangeListener from the listener list.
     * This removes a ConfigurationChangeListener that was registered
     * for all properties.
     *
     * @param listener The PropertyChangeListener to be removed
     */
    public void removePropertyChangeListener(
        PropertyChangeListener listener)
    {
        synchronized(propertyChangeListeners)
        {
            propertyChangeListeners.remove(listener);
        }
    }

    /**
     * Returns the descriptor common for this status message menu instance.
     * @return the descriptor common for this status message menu instance.
     */
    public abstract Object getDescriptor();

    /**
     * Fires that the status message has changed.
     * @param oldMessage the old message
     * @param newMessage the new message
     */
    protected void fireStatusMessageUpdated(
        String oldMessage, String newMessage)
    {
        PropertyChangeEvent evt =
            new PropertyChangeEvent(
                getDescriptor(),
                STATUS_MESSAGE_UPDATED_PROP,
                oldMessage, newMessage);



        java.util.List<PropertyChangeListener> listeners;
        synchronized(propertyChangeListeners)
        {
            listeners = new ArrayList<PropertyChangeListener>(
                propertyChangeListeners);
        }

        for (PropertyChangeListener target : listeners)
            target.propertyChange(evt);
    }

    /**
     * Fires that the custom status messages have changed.
     */
    private void fireCustomStatusMessagesUpdated()
    {
        PropertyChangeEvent evt =
            new PropertyChangeEvent(
                getDescriptor(),
                CUSTOM_STATUS_MESSAGES_UPDATED_PROP,
                null, null);

        java.util.List<PropertyChangeListener> listeners;
        synchronized(propertyChangeListeners)
        {
            listeners = new ArrayList<PropertyChangeListener>(
                propertyChangeListeners);
        }

        for (PropertyChangeListener target : listeners)
            target.propertyChange(evt);
    }

    /**
     * Creates the appropriate menu item. Depending on the
     * menu.
     * @param text the menu item text.
     * @return the item.
     */
    private Object createsProvisionedMessageItem(String text)
    {
        if (menu instanceof JMenu)
        {
            ProvisionedMessageItemsSwing newMenuItem
                = new ProvisionedMessageItemsSwing(text);
            newMenuItem.setName(text);
            newMenuItem.addActionListener(this);
            ((JMenu) menu).add(newMenuItem);

            return newMenuItem;
        }
        else
        {
            ProvisionedMessageItemsAwt newMenuItem
                = new ProvisionedMessageItemsAwt(text);
            newMenuItem.setName(text);
            newMenuItem.addItemListener(this);

            ((Menu) menu).add(newMenuItem);

            return newMenuItem;
        }
    }

    /**
     * Creates the appropriate menu item. Depending on the
     * menu.
     * @param text the menu item text.
     * @param index the index to instert the item, -1 to add it at end.
     * @param selected whether the new item to be selected
     * @return the item.
     */
    private Object createsCustomMessageItem(String text,
                                            int index,
                                            boolean selected)
    {
        if (menu instanceof JMenu)
        {
            CustomMessageItemsSwing newMenuItem
                = new CustomMessageItemsSwing(text);
            if(selected)
                newMenuItem.setSelected(true);
            newMenuItem.setName(text);
            newMenuItem.addActionListener(this);

            if(index == -1)
            {
                ((JMenu) menu).add(newMenuItem);
            }
            else
            {
                // insert the item on the ix position
                ((JMenu) menu).insert(newMenuItem, index);
            }

            return newMenuItem;
        }
        else
        {
            CustomMessageItemsAwt newMenuItem
                = new CustomMessageItemsAwt(text);
            if(selected)
                newMenuItem.setState(true);

            newMenuItem.setName(text);
            newMenuItem.addItemListener(this);

            if(index == -1)
            {
                ((Menu) menu).add(newMenuItem);
            }
            else
            {
                ((Menu) menu).insert(newMenuItem, index);
            }

            return newMenuItem;
        }
    }

    /**
     * Selects the item and changes its text to be bold, fires the change of
     * status message and return the name of the object which we've changed.
     *
     * @param item the item to change
     * @return the name of the item.
     */
    private String selectMenuItem(Object item, String oldMesage)
    {
        String name = null;
        if(item instanceof JCheckBoxMenuItem)
        {
            JCheckBoxMenuItem checkItem = (JCheckBoxMenuItem)item;
            name = checkItem.getName();
            checkItem.setSelected(true);
            checkItem.setText("<html><b>" + name + "</b></html>");
        }
        else if(item instanceof CheckboxMenuItem)
        {
            CheckboxMenuItem checkItem = (CheckboxMenuItem)item;
            name = checkItem.getName();
            checkItem.setState(true);
        }
        fireStatusMessageUpdated(oldMesage, name);

        return null;
    }

    /**
     * Listens for changes in the custom status messages and update.
     * Compares what is saved in the configuration and update according to that.
     * @param evt
     */
    public void propertyChange(PropertyChangeEvent evt)
    {
        if(evt.getPropertyName().equals(CUSTOM_STATUS_MESSAGES_UPDATED_PROP))
        {
            java.util.List<String> customMessagesProps =
                DesktopUtilActivator.getConfigurationService()
                    .getPropertyNamesByPrefix(CUSTOM_MESSAGES_PREFIX, false);

            if(customMessagesProps.isEmpty())
            {
                // someone cleared all messages, let we do the same
                removeAllCustomStatusMessages();

                return;
            }

            // if we are here someone has added new custom message, let's find it
            // and add it on the appropriate place
            java.util.List<String> customMessages = new ArrayList<String>();
            for(String p : customMessagesProps)
            {
                customMessages.add(
                    DesktopUtilActivator.getConfigurationService().getString(p));
            }

            for(Object o : getMenuComponents())
            {
                if(o instanceof CustomMessageItems)
                {
                    customMessages.remove(((CustomMessageItems)o).getName());
                }
            }

            // ok, in the customMessages has left only the new ones, lets add them
            for(String message : customMessages)
            {
                int ix = getLastCustomMessageIndex();
                if(ix == -1)
                {
                    this.addSeparator();
                    createsCustomMessageItem(message, -1, false);
                }
                else
                {
                    createsCustomMessageItem(message, ix + 1, false);
                }
            }
        }
        else if(evt.getPropertyName().equals(STATUS_MESSAGE_UPDATED_PROP))
        {
            // ignore updates for different providers
            if(!evt.getSource().equals(getDescriptor()))
                return;

            clearSelectedItems();

            for(Object o : getMenuComponents())
            {
                if(o instanceof JCheckBoxMenuItem)
                {
                    JCheckBoxMenuItem item = (JCheckBoxMenuItem)o;

                    if(!item.isSelected()
                        && item.getName().equals(evt.getNewValue()))
                    {
                        item.setSelected(true);
                    }
                }
                else if(o instanceof CheckboxMenuItem)
                {
                    CheckboxMenuItem item = (CheckboxMenuItem)o;
                    if(!item.getState()
                        && item.getName().equals(evt.getNewValue()))
                    {
                        item.setState(true);
                    }
                }
            }
        }
    }

    /**
     * Clears resources.
     */
    public void dispose()
    {
        removePropertyChangeListener(this);

        noMessageItem = null;
        newMessageItem = null;
        clearCustomMessageItem = null;
        busyMessageItem = null;
        brbMessageItem = null;
        menu = null;
    }

    /**
     * The icon to use for this menu.
     * @return
     */
    protected Icon getMenuIcon()
    {
        return null;
    }

    /**
     * The custom message items.
     */
    private interface CustomMessageItems
    {
        public String getName();
    }

    /**
     * The custom message items for swing impl.
     */
    private class CustomMessageItemsSwing
        extends JCheckBoxMenuItem
        implements CustomMessageItems
    {
        public CustomMessageItemsSwing(String text)
        {
            super(text);
        }
    }

    /**
     * The custom message items for awt impl.
     */
    private class CustomMessageItemsAwt
        extends CheckboxMenuItem
        implements CustomMessageItems
    {
        public CustomMessageItemsAwt(String text)
        {
            super(text);
        }
    }

    /**
     * The provisioned message items.
     */
    private interface ProvisionedMessageItems
    {
        public String getName();
    }

    /**
     * The provisioned message items for swing impl.
     */
    private class ProvisionedMessageItemsSwing
        extends JCheckBoxMenuItem
        implements ProvisionedMessageItems
    {
        public ProvisionedMessageItemsSwing(String text)
        {
            super(text);
        }
    }

    /**
     * The provisioned message items for awt impl.
     */
    private class ProvisionedMessageItemsAwt
        extends CheckboxMenuItem
        implements ProvisionedMessageItems
    {
        public ProvisionedMessageItemsAwt(String text)
        {
            super(text);
        }
    }
}
