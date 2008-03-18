/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.util.*;

import javax.swing.event.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.customcontrols.*;
import net.java.sip.communicator.impl.gui.event.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.chatroomslist.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.gui.Container;
import net.java.sip.communicator.service.gui.event.*;

/** 
 * The <tt>MainTabbedPane</tt> is a <tt>SIPCommTabbedPane</tt> that contains
 * three tabs: the <tt>ContactListPanel</tt>, the call list panel and
 * the <tt>DialPanel</tt>.
 * 
 * @author Yana Stamcheva
 */
public class MainTabbedPane
    extends SIPCommTabbedPane
    implements  ChangeListener,
                PluginComponentListener
{
    private DialPanel dialPanel;

    private CallListPanel callHistoryPanel;
    
    private ContactListPanel contactListPanel;

    private ChatRoomsListPanel chatRoomsListPanel;
    /**
     * Constructs the <tt>MainTabbedPane</tt>.
     * 
     * @param parent The main application frame.
     */
    public MainTabbedPane(MainFrame parent) {
        super(false, false);

        this.setCloseIcon(false);
        this.setMaxIcon(false);

        contactListPanel = new ContactListPanel(parent);

        callHistoryPanel = new CallListPanel(parent);

        dialPanel = new DialPanel(parent.getCallManager());

        chatRoomsListPanel = new ChatRoomsListPanel(parent);

        this.addTab(Messages.getI18NString("contacts").getText(),
                    contactListPanel);
        this.addTab(Messages.getI18NString("chatRooms").getText(),
                    chatRoomsListPanel);
        this.addTab(Messages.getI18NString("callList").getText(),
                    callHistoryPanel);
        this.addTab(Messages.getI18NString("dial").getText(), dialPanel);

        this.addChangeListener(this);

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    /**
     * Returns the <tt>ContactListPanel</tt> contained in this tabbed pane.
     * @return the <tt>ContactListPanel</tt> contained in this tabbed pane.
     */
    public ContactListPanel getContactListPanel()
    {
        return contactListPanel;
    }

    /**
     * Returns the <tt>CallListPanel</tt> contained in this tabbed pane.
     * @return the <tt>CallListPanel</tt> contained in this tabbed pane
     */
    public CallListPanel getCallListPanel()
    {
        return this.callHistoryPanel;
    }

    /**
     * Returns the <tt>ChatRoomsListPanel</tt> contained in this tabbed pane.
     * @return the <tt>ChatRoomsListPanel</tt> contained in this tabbed pane
     */
    public ChatRoomsListPanel getChatRoomsListPanel()
    {
        return this.chatRoomsListPanel;
    }

    /**
     * Implements ChangeListener.stateChanged. Requests the focus in the
     * selected panel.
     * @param e the event containing information about the change
     */
    public void stateChanged(ChangeEvent e)
    {
        Component selectedPanel = this.getSelectedComponent();

        if (selectedPanel == null)
            return;

        if (selectedPanel instanceof ContactListPanel)
        {
            final ContactList contactList
                = ((ContactListPanel) selectedPanel).getContactList();

            if (contactList == null)
                return;

            GuiUtils.requestFocus(contactList);
        }
        else if (selectedPanel instanceof ChatRoomsListPanel)
        {
            final ChatRoomsList chatRoomList
                = ((ChatRoomsListPanel) selectedPanel).getChatRoomsList();

            if (chatRoomList == null)
                return;

            GuiUtils.requestFocus(chatRoomList);
        }
    }

    /**
     * Initiates plugin components.
     */
    private void initPluginComponents()
    {
        Iterator pluginComponents = GuiActivator.getUIService()
            .getComponentsForContainer(
                Container.CONTAINER_MAIN_TABBED_PANE);

        while (pluginComponents.hasNext())
        {
            Component c = (Component)pluginComponents.next();

            this.addTab(c.getName(), c);
        }

        // Search for plugin components registered through the OSGI bundle
        // context.
        ServiceReference[] serRefs = null;

        String osgiFilter = "("
            + Container.CONTAINER_ID
            + "="+Container.CONTAINER_MAIN_TABBED_PANE.getID()+")";

        try
        {
            serRefs = GuiActivator.bundleContext.getServiceReferences(
                PluginComponent.class.getName(),
                osgiFilter);
        }
        catch (InvalidSyntaxException exc)
        {
            exc.printStackTrace();
        }

        if (serRefs == null)
            return;

        for (int i = 0; i < serRefs.length; i ++)
        {
            PluginComponent component = (PluginComponent) GuiActivator
                .bundleContext.getService(serRefs[i]);;

            this.addTab(component.getName(),
                        (Component) component.getComponent());
        }

        GuiActivator.getUIService().addPluginComponentListener(this);
    }

    public void pluginComponentAdded(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();

        if(c.getContainer()
                .equals(Container.CONTAINER_MAIN_TABBED_PANE))
        {
            this.addTab(c.getName(), (Component) c.getComponent());

            this.repaint();
        }
    }

    public void pluginComponentRemoved(PluginComponentEvent event)
    {
        PluginComponent c = event.getPluginComponent();
        
        if(c.getContainer()
                .equals(Container.CONTAINER_MAIN_TABBED_PANE))
        {
            this.remove((Component) c.getComponent());
        }
    }
}
