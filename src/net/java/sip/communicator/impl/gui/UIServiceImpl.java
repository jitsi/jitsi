/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JFrame;

import net.java.sip.communicator.impl.gui.main.MainFrame;
import net.java.sip.communicator.impl.gui.main.account.AccountRegWizardContainerImpl;
import net.java.sip.communicator.impl.gui.main.configforms.ConfigurationFrame;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPanel;
import net.java.sip.communicator.service.contactlist.MetaContact;
import net.java.sip.communicator.service.gui.AccountRegistrationWizardContainer;
import net.java.sip.communicator.service.gui.ContainerID;
import net.java.sip.communicator.service.gui.DialogID;
import net.java.sip.communicator.service.gui.ExportedDialog;
import net.java.sip.communicator.service.gui.PopupDialog;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.event.PluginComponentEvent;
import net.java.sip.communicator.service.gui.event.PluginComponentListener;
import net.java.sip.communicator.service.protocol.Contact;
import net.java.sip.communicator.util.Logger;

/**
 * An implementation of the <tt>UIService</tt> that gives access to 
 * other bundles to this particular swing ui implementation.
 * 
 * @author Yana Stamcheva
 */
public class UIServiceImpl implements UIService {

    private static final Logger logger
        = Logger.getLogger(UIServiceImpl.class);

    private PopupDialogImpl popupDialog;
    
    private AccountRegWizardContainerImpl wizardContainer;
    
    private Map registeredPlugins = new Hashtable();

    private Vector containerPluginListeners = new Vector();

    private static final List supportedContainers = new ArrayList();
    static {
        supportedContainers.add(UIService.CONTAINER_MAIN_TOOL_BAR);
        supportedContainers.add(UIService.CONTAINER_CHAT_TOOL_BAR);
    }
    
    private static final Hashtable exportedDialogs = new Hashtable();
        
    private MainFrame mainFrame;
    
    private ContactListPanel contactList;

    /**
     * Creates an instance of <tt>UIServiceImpl</tt>.
     * @param mainFrame The main application window.
     */
    public UIServiceImpl(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        this.contactList = mainFrame.getTabbedPane().getContactListPanel();
        
        this.popupDialog = new PopupDialogImpl(mainFrame);
        this.wizardContainer = new AccountRegWizardContainerImpl();
        
        this.exportedDialogs.put(UIService.DIALOG_CONFIGURATION, 
                new ConfigurationFrame(mainFrame));
    }
    
    /**
     * Implements addComponent in UIService interface. Stores a plugin component
     * and fires a PluginComponentEvent to inform all interested listeners
     * that a plugin component has been added.
     * 
     * @param containerID The <tt>ContainerID</tt> of the plugable container.
     * @param component The component to add.
     * 
     * @see UIService#addComponent(ContainerID, Object)
     */
    public void addComponent(ContainerID containerID, Object component)
            throws ClassCastException, IllegalArgumentException {

        if (!supportedContainers.contains(containerID)) {
            throw new IllegalArgumentException(
                    "The constraint that you specified is not"
                            + " supported by this UIService implementation.");
        }
        else if (!(component instanceof Component)) {
            throw new ClassCastException(
                "The specified plugin is not a valid swing or awt component.");
        } else {
            if (registeredPlugins.containsKey(containerID)) {
                ((Vector) registeredPlugins.get(containerID)).add(component);
            } else {
                Vector plugins = new Vector();
                plugins.add(component);
                registeredPlugins.put(containerID, plugins);
            }
            this.firePluginEvent(component, containerID,
                    PluginComponentEvent.PLUGIN_COMPONENT_ADDED);
        }
    }

    /**
     * Implements <code>UISercie.getSupportedContainers</code>. Returns the
     * list of supported containers by this implementation .
     * 
     * @see UIService#getSupportedContainers()
     */
    public Iterator getSupportedContainers() {
        return Collections.unmodifiableList(supportedContainers).iterator();
    }

    /**
     * Implements getComponentsForConstraint in UIService interface.
     * @see UIService#getComponentsForContainer(ContainerID)
     */
    public Iterator getComponentsForContainer(ContainerID containerID)
            throws IllegalArgumentException {

        Vector plugins = (Vector) this.registeredPlugins.get(containerID);

        if (plugins != null)
            return plugins.iterator();
        else
            throw new IllegalArgumentException(
                    "The container that you specified is not "
                            + "supported by this UIService implementation.");
    }

    /**
     * Implements <code>UIService.addComponent(ContainerID, String, Object)
     * </code>. For now this method only invokes addComponent(containerID,
     * component).
     * @see UIService#addComponent(ContainerID, String, Object)
     */
    public void addComponent(ContainerID containerID, String constraint,
            Object component) throws ClassCastException,
            IllegalArgumentException {
        this.addComponent(containerID, component);
    }

    /**
     * Not yet implemented.
     * @see UIService#getConstraintsForContainer(ContainerID)
     */
    public Iterator getConstraintsForContainer(ContainerID containerID) {
        return null;
    }

    /**
     * Creates the corresponding PluginComponentEvent and notifies all
     * <tt>ContainerPluginListener</tt>s that a plugin component is added or
     * removed from the container.
     *
     * @param pluginComponent the plugin component that is added to the
     * container.
     * @param containerID the containerID that corresponds to the container 
     * where the component is added.
     * @param eventID
     *            one of the PLUGIN_COMPONENT_XXX static fields indicating the
     *            nature of the event.
     */
    private void firePluginEvent(Object pluginComponent,
            ContainerID containerID, int eventID) {
        PluginComponentEvent evt = new PluginComponentEvent(pluginComponent,
                containerID, eventID);

        logger.trace("Will dispatch the following plugin component event: "
                + evt);

        synchronized (containerPluginListeners) {
            Iterator listeners = this.containerPluginListeners.iterator();

            while (listeners.hasNext()) {
                PluginComponentListener l = (PluginComponentListener) listeners
                        .next();

                switch (evt.getEventID()) {
                case PluginComponentEvent.PLUGIN_COMPONENT_ADDED:
                    l.pluginComponentAdded(evt);
                    break;
                case PluginComponentEvent.PLUGIN_COMPONENT_REMOVED:
                    l.pluginComponentRemoved(evt);
                    break;
                default:
                    logger.error("Unknown event type " + evt.getEventID());
                }
            }
        }
    }

    /**
     * Implements <code>isVisible</code> in the UIService interface.
     * Checks if the main application window is visible.
     * @return <code>true</code> if main application window is visible, 
     * <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    public boolean isVisible() {
        return this.mainFrame.isVisible();
    }

    /**
     * Implements <code>setVisible</code> in the UIService interface.
     * Shows or hides the main application window depending on the parameter
     * <code>visible</code>.
     * @see UIService#setVisible(boolean)
     */
    public void setVisible(boolean visible) {
        this.mainFrame.setVisible(visible);        
    }

    /**
     * Implements <code>minimize</code> in the UIService interface.
     * Minimizes the main application window.
     * @see UIService#minimize()
     */
    public void minimize() {
        this.mainFrame.setExtendedState(JFrame.ICONIFIED);
    }

    /**
     * Implements <code>maximize</code> in the UIService interface.
     * Maximizes the main application window.
     * @see UIService#maximize()
     */
    public void maximize() {
        this.mainFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);        
    }

    /**
     * Implements <code>restore</code> in the UIService interface.
     * Restores the main application window.
     * @see UIService#restore()
     */
    public void restore() {
        this.mainFrame.setExtendedState(JFrame.NORMAL);        
    }

    /**
     * Implements <code>resize</code> in the UIService interface.
     * Resizes the main application window.
     * @see UIService#resize(int, int)
     */
    public void resize(int width, int height) {
        this.mainFrame.setSize(width, height);
    }

    /**
     * Implements <code>move</code> in the UIService interface.
     * Moves the main application window to the point with coordinates - x, y.
     * @see UIService#move(int, int)
     */
    public void move(int x, int y) {
        this.mainFrame.setLocation(x, y);
    }

    /**
     * Implements <code>getExportedDialogs</code> in the UIService interface.
     * Returns an iterator over a set of all dialogs exported by this
     * implementation.
     * @see UIService#getExportedDialogs()
     */
    public Iterator getExportedDialogs() {
        return Collections.unmodifiableMap(exportedDialogs)
            .values().iterator();
    }

    /**
     * Implements <code>getApplicationDialog</code> in the UIService interface.
     * Returns the <tt>Dialog</tt> corresponding to the given
     * <tt>DialogID</tt>.
     * @see UIService#getApplicationDialog(DialogID)
     */
    public ExportedDialog getApplicationDialog(DialogID dialogID) {
        if (exportedDialogs.contains(dialogID)) {
            return (ExportedDialog) exportedDialogs.get(dialogID);
        }
        return null;
    }

    /**
     * Implements <code>getPopupDialog</code> in the UIService interface.
     * Returns a <tt>PopupDialog</tt> that could be used to show simple
     * messages, warnings, errors, etc.
     * @see UIService#getPopupDialog()
     */
    public PopupDialog getPopupDialog() {
        return this.popupDialog;
    }

    /**
     * Implements <code>getChatDialog</code> in the UIService interface. If
     * a chat dialog for the given contact exists already returns it,
     * otherwise creates a new one.
     * @see UIService#getChatDialog(Contact)
     */
    public ExportedDialog getChatDialog(Contact contact) {
        
        MetaContact metaContact = mainFrame.getContactList()
            .findMetaContactByContact(contact);
                
        Hashtable contactChats 
            = contactList.getTabbedChatWindow().getContactChatsTable();
        
        if (contactChats.get(metaContact.getMetaUID()) != null) {
            return (ExportedDialog) contactChats.get(metaContact.getMetaUID());
        }
        else {            
            return contactList.getTabbedChatWindow().createChat(
                    metaContact, contact.getPresenceStatus(), contact);        
        }
    }

    /**
     * Checks if there's an exported dialog for the given <tt>DialogID</tt>.
     * @see UIService#isDialogExported(DialogID)
     */
    public boolean isDialogExported(DialogID dialogID) {
        return exportedDialogs.contains(dialogID);
    }

    /**
     * Checks if the plugable container with the given ContainerID is supported
     * by this implementation. 
     * @see UIService#isContainerSupported(ContainerID)
     */
    public boolean isContainerSupported(ContainerID containderID) {
        return supportedContainers.contains(containderID);
    }

    /**
     * Returns the current implementation of the
     * <tt>AccountRegistrationWizardContainer</tt>.
     * @see UIService#getAccountRegWizardContainer()
     */
    public AccountRegistrationWizardContainer getAccountRegWizardContainer() {
        return this.wizardContainer;
    }
}
