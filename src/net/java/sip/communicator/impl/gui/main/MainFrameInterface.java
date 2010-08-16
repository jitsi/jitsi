package net.java.sip.communicator.impl.gui.main;

import java.util.Iterator;

import net.java.sip.communicator.impl.gui.event.PluginComponentEvent;
import net.java.sip.communicator.impl.gui.main.contactlist.ContactListPane;
import net.java.sip.communicator.impl.gui.main.menus.MainMenu;
import net.java.sip.communicator.impl.gui.main.presence.AccountStatusPanel;
import net.java.sip.communicator.service.contacteventhandler.ContactEventHandler;
import net.java.sip.communicator.service.contactlist.MetaContactListService;
import net.java.sip.communicator.service.gui.ExportedWindow;
import net.java.sip.communicator.service.gui.UIService;
import net.java.sip.communicator.service.gui.WindowID;
import net.java.sip.communicator.service.protocol.OperationSetAdHocMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetBasicTelephony;
import net.java.sip.communicator.service.protocol.OperationSetMultiUserChat;
import net.java.sip.communicator.service.protocol.OperationSetWebContactInfo;
import net.java.sip.communicator.service.protocol.ProtocolProviderService;
import net.java.sip.communicator.util.swing.event.TextFieldChangeListener;

public interface MainFrameInterface {

    /**
     * Requests the focus in the center panel, which contains either the
     * contact list or the unknown contact panel.
     */
    void requestFocusInCenterPanel();

    /**
     * Sets frame size and position.
     */
    void initBounds();

    /**
     * Enters or exits the "unknown contact" view. This view will propose to
     * the user some specific operations if the current filter doesn't match
     * any contacts.
     * @param isEnabled <tt>true</tt> to enable the "unknown contact" view,
     * <tt>false</tt> - otherwise.
     */
    void enableUnknownContactView(boolean isEnabled);

    /**
     * Initializes the contact list panel.
     *
     * @param contactList The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    void setContactList(MetaContactListService contactList);

    /**
     * Adds all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    void addProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider);

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    Iterator<ProtocolProviderService> getProtocolProviders();

    /**
     * Returns the protocol provider associated to the account given
     * by the account user identifier.
     *
     * @param accountName The account user identifier.
     * @return The protocol provider associated to the given account.
     */
    ProtocolProviderService getProtocolProviderForAccount(String accountName);

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    void addProtocolProvider(ProtocolProviderService protocolProvider);

    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    int getProviderIndex(ProtocolProviderService protocolProvider);

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    void addAccount(ProtocolProviderService protocolProvider);

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    void removeProtocolProvider(ProtocolProviderService protocolProvider);

    /**
     * Returns the account user id for the given protocol provider.
     * @param protocolProvider the protocol provider corresponding to the
     * account to add
     * @return The account user id for the given protocol provider.
     */
    String getAccount(ProtocolProviderService protocolProvider);

    /**
     * Returns the Web Contact Info operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the TN
     * is searched.
     * @return OperationSetWebContactInfo The Web Contact Info operation
     * set for the given protocol provider.
     */
    OperationSetWebContactInfo getWebContactInfoOpSet(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the telephony operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the telephony
     * operation set is about.
     * @return OperationSetBasicTelephony The telephony operation
     * set for the given protocol provider.
     */
    OperationSetBasicTelephony getTelephonyOpSet(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetAdHocMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    OperationSetAdHocMultiUserChat getAdHocMultiUserChatOpSet(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the multi user chat operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the multi user
     * chat operation set is about.
     * @return OperationSetMultiUserChat The telephony operation
     * set for the given protocol provider.
     */
    OperationSetMultiUserChat getMultiUserChatOpSet(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    ContactListPane getContactListPanel();

    /**
     * Returns the text currently shown in the search field.
     * @return the text currently shown in the search field
     */
    String getCurrentSearchText();

    /**
     * Clears the current text in the search field.
     */
    void clearCurrentSearchText();

    /**
     * Adds the given <tt>TextFieldChangeListener</tt> to listen for any changes
     * that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to add
     */
    void addSearchFieldListener(TextFieldChangeListener l);

    /**
     * Removes the given <tt>TextFieldChangeListener</tt> that listens for any
     * changes that occur in the search field.
     * @param l the <tt>TextFieldChangeListener</tt> to remove
     */
    void removeSearchFieldListener(TextFieldChangeListener l);

    /**
     * If the protocol provider supports presence operation set searches the
     * last status which was selected, otherwise returns null.
     *
     * @param protocolProvider the protocol provider we're interested in.
     * @return the last protocol provider presence status, or null if this
     * provider doesn't support presence operation set
     */
    Object getProtocolProviderLastStatus(
            ProtocolProviderService protocolProvider);

    /**
     * Returns the main menu in the application window.
     * @return the main menu in the application window
     */
    MainMenu getMainMenu();

    /**
     *
     * @param protocolProvider
     * @param contactHandler
     */
    void addProviderContactHandler(ProtocolProviderService protocolProvider,
            ContactEventHandler contactHandler);

    /**
     * Returns the <tt>ContactEventHandler</tt> registered for this protocol
     * provider.
     *
     * @param protocolProvider the <tt>ProtocolProviderService</tt> for which
     * we are searching a <tt>ContactEventHandler</tt>.
     * @return the <tt>ContactEventHandler</tt> registered for this protocol
     * provider
     */
    ContactEventHandler getContactHandler(
            ProtocolProviderService protocolProvider);

    /**
     * Adds the associated with this <tt>PluginComponentEvent</tt> component to
     * the appropriate container.
     * @param event the <tt>PluginComponentEvent</tt> that has notified us of
     * the add of a plugin component
     */
    void pluginComponentAdded(PluginComponentEvent event);

    /**
     * Removes the associated with this <tt>PluginComponentEvent</tt> component
     * from this container.
     * @param event the <tt>PluginComponentEvent</tt> that notified us of the
     * remove of a plugin component
     */
    void pluginComponentRemoved(PluginComponentEvent event);

    /**
     * Adds all native plugins to this container.
     */
    void addNativePlugins();

    /**
     * Brings this window to front.
     */
    void bringToFront();

    /**
     * Returns the identifier of this window.
     * @return the identifier of this window
     */
    WindowID getIdentifier();

    /**
     * Returns this window.
     * @return this window
     */
    Object getSource();

    /**
     * Maximizes this window.
     */
    void maximize();

    /**
     * Minimizes this window.
     */
    void minimize();

    /**
     * Implements <code>isVisible</code> in the UIService interface. Checks if
     * the main application window is visible.
     *
     * @return <code>true</code> if main application window is visible,
     *         <code>false</code> otherwise
     * @see UIService#isVisible()
     */
    boolean isVisible();

    /**
     * Implements <code>setVisible</code> in the UIService interface. Shows or
     * hides the main application window depending on the parameter
     * <code>visible</code>.
     *
     * @param isVisible true if we are to show the main application frame and
     * false otherwise.
     *
     * @see UIService#setVisible(boolean)
     */
    void setVisible(final boolean isVisible);

    /**
     * Returns the account status panel.
     * @return the account status panel.
     */
    AccountStatusPanel getAccountStatusPanel();

    /**
     * Implementation of {@link ExportedWindow#setParams(Object[])}.
     */
    void setParams(Object[] windowParams);

    /**
     * Called when the CTRL-ENTER or CMD-ENTER keys were typed when this window
     * was the focused window. Performs the appropriate actions depending on the
     * current state of the contact list.
     */
    void ctrlEnterKeyTyped();

}