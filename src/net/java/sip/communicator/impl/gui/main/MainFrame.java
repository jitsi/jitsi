/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package net.java.sip.communicator.impl.gui.main;

import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.osgi.framework.*;

import net.java.sip.communicator.impl.gui.*;
import net.java.sip.communicator.impl.gui.i18n.*;
import net.java.sip.communicator.impl.gui.main.call.*;
import net.java.sip.communicator.impl.gui.main.contactlist.*;
import net.java.sip.communicator.impl.gui.main.login.*;
import net.java.sip.communicator.impl.gui.main.menus.*;
import net.java.sip.communicator.impl.gui.utils.*;
import net.java.sip.communicator.service.configuration.*;
import net.java.sip.communicator.service.configuration.PropertyVetoException;
import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

/**
 * The main application window. This class is the core of this ui
 * implementation. It stores all available protocol providers and their
 * operation sets, as well as all registered accounts, the
 * <tt>MetaContactListService</tt> and all sent messages that aren't
 * delivered yet.
 *
 * @author Yana Stamcheva
 */
public class MainFrame
    extends JFrame
{
    private Logger logger = Logger.getLogger(MainFrame.class.getName());

    private JPanel contactListPanel = new JPanel(new BorderLayout());

    private JPanel menusPanel = new JPanel(new BorderLayout());

    private MainMenu menu;

    private CallManager callManager;

    private StatusPanel statusPanel;

    private MainTabbedPane tabbedPane;

    private QuickMenu quickMenu;

    private Hashtable protocolSupportedOperationSets = new Hashtable();

    private Hashtable protocolPresenceSets = new Hashtable();

    private Hashtable protocolTelephonySets = new Hashtable();

    private Hashtable protocolProviders = new Hashtable();

    private Hashtable imOperationSets = new Hashtable();

    private Hashtable tnOperationSets = new Hashtable();

    private Hashtable webContactInfoOperationSets = new Hashtable();

    private MetaContactListService contactList;

    private ArrayList accounts = new ArrayList();

    private Hashtable waitToBeDeliveredMsgs = new Hashtable();

    private LoginManager loginManager;

    /**
     * Creates an instance of <tt>MainFrame</tt>.
     */
    public MainFrame()
    {
        callManager = new CallManager(this);
        tabbedPane = new MainTabbedPane(this);
        quickMenu = new QuickMenu(this);
        statusPanel = new StatusPanel(this);
        menu = new MainMenu(this);

        this.addWindowListener(new MainFrameWindowAdapter());

        this.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        this.setInitialBounds();

        this.setTitle(Messages.getString("sipCommunicator"));

        this.setIconImage(ImageLoader.getImage(ImageLoader.SIP_LOGO));

        this.init();
    }

    /**
     * Initiates the content of this frame.
     */
    private void init()
    {
        this.menusPanel.add(menu, BorderLayout.NORTH);
        this.menusPanel.add(quickMenu, BorderLayout.CENTER);

        this.contactListPanel.add(tabbedPane, BorderLayout.CENTER);
        this.contactListPanel.add(callManager, BorderLayout.SOUTH);

        this.getContentPane().add(menusPanel, BorderLayout.NORTH);
        this.getContentPane().add(contactListPanel, BorderLayout.CENTER);
        this.getContentPane().add(statusPanel, BorderLayout.SOUTH);
    }

    /**
     * Sets frame size and position.
     */
    private void setInitialBounds()
    {
        this.setSize(200, 450);
        this.contactListPanel.setPreferredSize(new Dimension(180, 400));
        this.contactListPanel.setMinimumSize(new Dimension(80, 200));

        this.setLocation(Toolkit.getDefaultToolkit().getScreenSize().width
                - this.getWidth(), 50);
    }

    /**
     * Returns the <tt>MetaContactListService</tt>.
     *
     * @return <tt>MetaContactListService</tt> The current meta contact list.
     */
    public MetaContactListService getContactList()
    {
        return this.contactList;
    }

    /**
     * Initializes the contact list panel.
     *
     * @param contactList The <tt>MetaContactListService</tt> containing
     * the contact list data.
     */
    public void setContactList(MetaContactListService contactList)
    {
        this.contactList = contactList;

        ContactListPanel clistPanel = this.tabbedPane.getContactListPanel();

        clistPanel.initList(contactList);

        CListKeySearchListener keyListener
            = new CListKeySearchListener(clistPanel.getContactList());

        //add a key listener to the tabbed pane, when the contactlist is
        //initialized
        this.tabbedPane.addKeyListener(keyListener);

        clistPanel.addKeyListener(keyListener);

        clistPanel.getContactList().addKeyListener(keyListener);
    }


    /**
     * Returns a set of all operation sets supported by the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider.
     * @return a set of all operation sets supported by the given
     * protocol provider.
     */
    public Map getSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        return (Map) this.protocolSupportedOperationSets.get(protocolProvider);
    }

    /**
     * Adds all protocol supported operation sets.
     *
     * @param protocolProvider The protocol provider.
     */
    public void addProtocolSupportedOperationSets(
            ProtocolProviderService protocolProvider)
    {
        Map supportedOperationSets
            = protocolProvider.getSupportedOperationSets();

        this.protocolSupportedOperationSets.put(protocolProvider,
                supportedOperationSets);

        String ppOpSetClassName = OperationSetPersistentPresence
                                    .class.getName();
        String pOpSetClassName = OperationSetPresence.class.getName();

        if (supportedOperationSets.containsKey(ppOpSetClassName)
                || supportedOperationSets.containsKey(pOpSetClassName)) {

            OperationSetPresence presence = (OperationSetPresence)
                supportedOperationSets.get(ppOpSetClassName);

            if(presence == null) {
                presence = (OperationSetPresence)
                    supportedOperationSets.get(pOpSetClassName);
            }

            this.protocolPresenceSets.put(protocolProvider, presence);

            presence.addProviderPresenceStatusListener(
                        new GUIProviderPresenceStatusListener());
            presence.addContactPresenceStatusListener(
                        new GUIContactPresenceStatusListener());
        }

        String imOpSetClassName = OperationSetBasicInstantMessaging
                                    .class.getName();

        if (supportedOperationSets.containsKey(imOpSetClassName)) {

            OperationSetBasicInstantMessaging im
                = (OperationSetBasicInstantMessaging)
                    supportedOperationSets.get(imOpSetClassName);

            this.imOperationSets.put(protocolProvider, im);
            //Add to all instant messaging operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            im.addMessageListener(this.getContactListPanel());
        }

        String tnOpSetClassName = OperationSetTypingNotifications
                                    .class.getName();

        if (supportedOperationSets.containsKey(tnOpSetClassName)) {

            OperationSetTypingNotifications tn
                = (OperationSetTypingNotifications)
                    supportedOperationSets.get(tnOpSetClassName);

            this.tnOperationSets.put(protocolProvider, tn);

            //Add to all typing notification operation sets the Message
            //listener implemented in the ContactListPanel, which handles
            //all received messages.
            tn.addTypingNotificationsListener(this.getContactListPanel());
        }

        String wciOpSetClassName = OperationSetWebContactInfo.class.getName();

        if (supportedOperationSets.containsKey(wciOpSetClassName)) {

            OperationSetWebContactInfo wContactInfo
                = (OperationSetWebContactInfo)
                    supportedOperationSets.get(wciOpSetClassName);

            this.webContactInfoOperationSets
                .put(protocolProvider, wContactInfo);
        }

        String telOpSetClassName = OperationSetBasicTelephony.class.getName();

        if (supportedOperationSets.containsKey(telOpSetClassName)) {

            OperationSetBasicTelephony telephony
                = (OperationSetBasicTelephony)
                    supportedOperationSets.get(telOpSetClassName);

            telephony.addCallListener(new GUICallListener());

            this.protocolTelephonySets.put(protocolProvider, telephony);
        }
    }

    /**
     * Returns a set of all protocol providers.
     *
     * @return a set of all protocol providers.
     */
    public Iterator getProtocolProviders()
    {
        return this.protocolProviders.keySet().iterator();
    }

    /**
     * Returns the protocol provider associated to the account given
     * by the account user identifier.
     *
     * @param accountName The account user identifier.
     * @return The protocol provider associated to the given account.
     */
    public ProtocolProviderService getProtocolProviderForAccount(
            String accountName)
    {
        Iterator i = this.protocolProviders.keySet().iterator();
        while(i.hasNext()) {
            ProtocolProviderService pps
                = (ProtocolProviderService)i.next();

            if (pps.getAccountID().getUserID().equals(accountName)) {
               return pps;
            }
        }

        return null;
    }

    /**
     * Adds a protocol provider.
     * @param protocolProvider The protocol provider to add.
     */
    public void addProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProviders.put(protocolProvider,
                new Integer(initiateProviderIndex(protocolProvider)));

        this.addProtocolSupportedOperationSets(protocolProvider);

        this.addAccount(protocolProvider);
    }
    
    /**
     * Returns the index of the given protocol provider.
     * @param protocolProvider the protocol provider to search for
     * @return the index of the given protocol provider
     */
    public int getProviderIndex(ProtocolProviderService protocolProvider)
    {
        Object o = protocolProviders.get(protocolProvider);
        
        if(o != null) {
            return ((Integer)o).intValue();
        }
        return 0;
    }

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void addAccount(ProtocolProviderService protocolProvider)
    {
        if (!getStatusPanel().containsAccount(protocolProvider)) {
            this.accounts.add(protocolProvider);

            this.getStatusPanel().addAccount(protocolProvider);

            //request the focus int the contact list panel, which
            //permits to search in the contact list
            this.tabbedPane.getContactListPanel().getContactList()
                    .requestFocus();
        }
    }

    /**
     * Adds an account to the application.
     *
     * @param protocolProvider The protocol provider of the account.
     */
    public void removeProtocolProvider(ProtocolProviderService protocolProvider)
    {
        this.protocolProviders.remove(protocolProvider);
        this.updateProvidersIndexes(protocolProvider);
        
        if (getStatusPanel().containsAccount(protocolProvider)) {
            this.accounts.remove(protocolProvider);
            this.getStatusPanel().removeAccount(protocolProvider);
        }
    }
    
    /**
     * Activates an account. Here we start the connecting process.
     *
     * @param protocolProvider The protocol provider of this account.
     */
    public void activateAccount(ProtocolProviderService protocolProvider)
    {
        this.getStatusPanel().startConnecting(protocolProvider);
    }

    /**
     * Returns the account user id for the given protocol provider.
     * @return The account user id for the given protocol provider.
     */
    public String getAccount(ProtocolProviderService protocolProvider)
    {
        return protocolProvider.getAccountID().getUserID();
    }

    /**
     * Returns the presence operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the
     * presence operation set is searched.
     * @return the presence operation set for the given protocol provider.
     */
    public OperationSetPresence getProtocolPresence(
            ProtocolProviderService protocolProvider)
    {
        return (OperationSetPresence) this.protocolPresenceSets
                .get(protocolProvider);
    }

    /**
     * Returns the basic instant messaging(IM) operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the IM
     * is searched.
     * @return OperationSetBasicInstantMessaging The IM for the given
     * protocol provider.
     */
    public OperationSetBasicInstantMessaging getProtocolIM(
            ProtocolProviderService protocolProvider)
    {
        return (OperationSetBasicInstantMessaging) this.imOperationSets
                .get(protocolProvider);
    }

    /**
     * Returns the typing notifications(TN) operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the TN
     * is searched.
     * @return OperationSetTypingNotifications The TN for the given
     * protocol provider.
     */
    public OperationSetTypingNotifications getTypingNotifications(
            ProtocolProviderService protocolProvider)
    {
        return (OperationSetTypingNotifications) this.tnOperationSets
                .get(protocolProvider);
    }

    /**
     * Returns the Web Contact Info operation set for the given
     * protocol provider.
     *
     * @param protocolProvider The protocol provider for which the TN
     * is searched.
     * @return OperationSetWebContactInfo The Web Contact Info operation
     * set for the given protocol provider.
     */
    public OperationSetWebContactInfo getWebContactInfo(
            ProtocolProviderService protocolProvider)
    {
        return (OperationSetWebContactInfo) this.webContactInfoOperationSets
                .get(protocolProvider);
    }

    /**
     * Returns the telephony operation set for the given protocol provider.
     *
     * @param protocolProvider The protocol provider for which the telephony
     * is searched.
     * @return OperationSetBasicTelephony The telephony operation
     * set for the given protocol provider.
     */
    public OperationSetBasicTelephony getTelephony(
            ProtocolProviderService protocolProvider)
    {
        return (OperationSetBasicTelephony) this.protocolTelephonySets
                .get(protocolProvider);
    }

    /**
     * Returns the call manager.
     * @return CallManager The call manager.
     */
    public CallManager getCallManager()
    {
        return callManager;
    }

    /**
     * Returns the quick menu, placed above the main tabbed pane.
     * @return QuickMenu The quick menu, placed above the main tabbed pane.
     */
    public QuickMenu getQuickMenu()
    {
        return quickMenu;
    }

    /**
     * Returns the status panel.
     * @return StatusPanel The status panel.
     */
    public StatusPanel getStatusPanel()
    {
        return statusPanel;
    }

    /**
     * Listens for all contactPresenceStatusChanged events in order
     * to refresh tha contact list, when a status is changed.
     */
    private class GUIContactPresenceStatusListener implements
            ContactPresenceStatusListener
    {
        public void contactPresenceStatusChanged(
                ContactPresenceStatusChangeEvent evt) {

            ContactListPanel clistPanel = tabbedPane.getContactListPanel();
            
            clistPanel.getContactList().refresh();
            
            Contact sourceContact = evt.getSourceContact();

            MetaContact metaContact = contactList
                    .findMetaContactByContact(sourceContact);

            if (metaContact != null) {
                if(!evt.getOldStatus().equals(evt.getNewStatus()))
                    clistPanel.updateChatContactStatus(
                            metaContact, sourceContact);
            }
        }
    }

    /**
     * Listens for all providerStatusChanged and providerStatusMessageChanged
     * events in order to refresh the account status panel, when a status is
     * changed.
     */
    private class GUIProviderPresenceStatusListener implements
            ProviderPresenceStatusListener
    {
        public void providerStatusChanged(ProviderPresenceStatusChangeEvent evt) {

        }

        public void providerStatusMessageChanged(PropertyChangeEvent evt) {

        }
    }

    /**
     * Listens for all CallReceivedEvents.
     */
    private class GUICallListener implements CallListener {

        /**
         * This method is called by a protocol provider whenever an incoming call
         * is received.
         * @param event a CallEvent instance describing the new incoming
         * call
         */
        public void incomingCallReceived(CallEvent event)
        {
            /**@todo implement incomingCallReceived() */
            System.out.println("@todo implement incomingCallReceived()");
        }

        /**
         * Indicates that all participants have left the source call and that it
         * has been ended. The event may be considered redundant since there are
         * already events issued upon termination of a single call participant but
         * we've decided to keep it for listeners that are only intersted in call
         * duration and don't want to follow other call details.
         * @param event the <tt>CallEvent</tt> containing the source call.
         */
        public void callEnded(CallEvent event)
        {
            /**@todo implement incomingCallReceived() */
            System.out.println("@todo implement incomingCallReceived()");
        }

        public void outgoingCallCreated(CallEvent event)
        {
            // TODO Auto-generated method stub

        }

    }

    public Hashtable getWaitToBeDeliveredMsgs()
    {
        return waitToBeDeliveredMsgs;
    }

    /**
     * Returns the list of all groups.
     * @return The list of all groups.
     */
    public Iterator getAllGroups()
    {
        return getContactListPanel()
            .getContactList().getAllGroups();
    }

    /**
     * Returns the Meta Contact Group corresponding to the given MetaUID.
     *
     * @param metaUID An identifier of a group.
     * @return The Meta Contact Group corresponding to the given MetaUID.
     */
    public MetaContactGroup getGroupByID(String metaUID)
    {
        return getContactListPanel()
            .getContactList().getGroupByID(metaUID);
    }

    /**
     * Before closing the application window saves the current size and position
     * through the <tt>ConfigurationService</tt>.
     */
    public class MainFrameWindowAdapter extends WindowAdapter
    {
        public void windowClosing(WindowEvent e) {
            ConfigurationService configService
                = GuiActivator.getConfigurationService();

            try {
                configService.setProperty(
                    "net.java.sip.communicator.impl.ui.mainWindowWidth",
                    new Integer(getWidth()));

                configService.setProperty(
                    "net.java.sip.communicator.impl.ui.mainWindowHeight",
                    new Integer(getHeight()));

                configService.setProperty(
                        "net.java.sip.communicator.impl.ui.mainWindowX",
                        new Integer(getX()));

                configService.setProperty(
                        "net.java.sip.communicator.impl.ui.mainWindowY",
                        new Integer(getY()));

                configService.setProperty(
                        "net.java.sip.communicator.impl.ui.showCallPanel",
                        new Boolean(callManager.isShown()));
            }
            catch (PropertyVetoException e1) {
                logger.error("The proposed property change "
                        + "represents an unacceptable value");
            }
        }
        
        public void windowClosed(WindowEvent e) {
            try {
                GuiActivator.bundleContext.getBundle(0).stop();
            } catch (BundleException ex) {
                logger.error("Failed to gently shutdown Oscar", ex);
                System.exit(0);
            }
        }
    }

    /**
     * Sets the window size and position.
     */
    public void setSizeAndLocation() {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();

        String width = configService.getString(
            "net.java.sip.communicator.impl.ui.mainWindowWidth");

        String height = configService.getString(
            "net.java.sip.communicator.impl.ui.mainWindowHeight");

        String x = configService.getString(
            "net.java.sip.communicator.impl.ui.mainWindowX");

        String y = configService.getString(
            "net.java.sip.communicator.impl.ui.mainWindowY");

        String isShown = configService.getString(
            "net.java.sip.communicator.impl.ui.showCallPanel");

        if(width != null && height != null)
            this.setSize(new Integer(width).intValue(),
                    new Integer(height).intValue());

        if(x != null && y != null)
            this.setLocation(new Integer(x).intValue(),
                    new Integer(y).intValue());

        if(isShown != null && isShown != "") {
            callManager.setShown(new Boolean(isShown).booleanValue());
        }
        else {
            callManager.setShown(true);
        }
    }

    /**
     * Returns the class that manages user login.
     * @return the class that manages user login.
     */
    public LoginManager getLoginManager() {
        return loginManager;
    }

    /**
     * Sets the class that manages user login.
     * @param loginManager The user login manager.
     */
    public void setLoginManager(LoginManager loginManager) {
        this.loginManager = loginManager;
    }

    /**
     * Saves the last status for all accounts. This information is used
     * on loging. Each time user logs in he's logged with the same status
     * as he was the last time before closing the application.
     */
    public void saveStatusInformation(ProtocolProviderService protocolProvider,
            PresenceStatus status)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
        
        String prefix = "net.java.sip.communicator.impl.ui.accounts";

        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        boolean savedAccount = false;
        Iterator accountsIter = accounts.iterator();

        while(accountsIter.hasNext()) {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {

                configService.setProperty(
                        accountRootPropName + ".lastAccountStatus",
                        status.getStatusName());

                savedAccount = true;
            }
        }

        if(!savedAccount) {
            String accNodeName
                = "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage
                = "net.java.sip.communicator.impl.ui.accounts."
                        + accNodeName;

            configService.setProperty(accountPackage,
                    protocolProvider.getAccountID().getAccountUniqueID());

            configService.setProperty(
                    accountPackage+".lastAccountStatus",
                    status.getStatusName());
        }
        
    }

    /**
     * Returns the panel containing the ContactList.
     * @return ContactListPanel the panel containing the ContactList
     */
    public ContactListPanel getContactListPanel() {
        return this.tabbedPane.getContactListPanel();
    }

    /**
     * Adds a tab in the main tabbed pane, where the given call panel
     * will be added.
     */
    public void addCallPanel(CallPanel callPanel) {
        this.tabbedPane.addTab(callPanel.getTitle(), callPanel);
        this.tabbedPane.setSelectedIndex(tabbedPane.getTabCount() - 1);
        this.tabbedPane.revalidate();
    }
         
    /**
     * Checks in the configuration xml if there is already stored index for
     * this provider and if yes, returns it, otherwise creates a new account
     * index and stores it.
     * 
     * @param protocolProvider the protocol provider
     * @return the protocol provider index
     */
    private int initiateProviderIndex(
            ProtocolProviderService protocolProvider)
    {                
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
    
        String prefix = "net.java.sip.communicator.impl.ui.accounts";
        
        List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);

        Iterator accountsIter = accounts.iterator();

        boolean savedAccount = false;
        
        while(accountsIter.hasNext()) {
            String accountRootPropName
                = (String) accountsIter.next();

            String accountUID
                = configService.getString(accountRootPropName);

            if(accountUID.equals(protocolProvider
                    .getAccountID().getAccountUniqueID())) {
                
                savedAccount = true;
                String  index = configService.getString(
                        accountRootPropName + ".accountIndex");
         
                if(index != null) {
                    //if we have found the accountIndex for this protocol provider
                    //return this index
                    return new Integer(index).intValue();
                }
                else {
                    //if there's no stored accountIndex for this protocol
                    //provider, calculate the index, set it in the configuration
                    //service and return it.

                    int accountIndex = createAccountIndex(protocolProvider,
                            accountRootPropName);
                    return accountIndex;
                }
            }
        }
        
        if(!savedAccount) {
            String accNodeName
                = "acc" + Long.toString(System.currentTimeMillis());

            String accountPackage
                = "net.java.sip.communicator.impl.ui.accounts."
                        + accNodeName;

            configService.setProperty(accountPackage,
                    protocolProvider.getAccountID().getAccountUniqueID());

            int accountIndex = createAccountIndex(protocolProvider,
                    accountPackage);
            
            return accountIndex;
        }
        return -1;
    }
    
    /**
     * Creates and calculates the account index for the given protocol
     * provider.
     * @param protocolProvider the protocol provider
     * @param accountRootPropName the path to where the index should be saved
     * in the configuration xml  
     * @return the created index
     */
    private int createAccountIndex(ProtocolProviderService protocolProvider,
            String accountRootPropName)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
     
        int accountIndex = -1;
        Enumeration pproviders = protocolProviders.keys();
        ProtocolProviderService pps;
            
        while(pproviders.hasMoreElements()) {
            pps = (ProtocolProviderService)pproviders.nextElement();
            
            if(pps.getProtocolName().equals(
                    protocolProvider.getProtocolName())
                    && !pps.equals(protocolProvider)) {
                
                int index  = ((Integer)protocolProviders.get(pps))
                                        .intValue(); 
                                
                if(accountIndex < index) {
                    accountIndex = index;
                }
            }
        }
        accountIndex++;
        configService.setProperty(
                accountRootPropName + ".accountIndex",
                new Integer(accountIndex));
        
        return accountIndex;
    }
    
    /**
     * Updates the indexes in the configuration xml, when a provider has been
     * removed.     
     * @param removedProvider the removed protocol provider
     */
    private void updateProvidersIndexes(ProtocolProviderService removedProvider)
    {
        ConfigurationService configService
            = GuiActivator.getConfigurationService();
     
        String prefix = "net.java.sip.communicator.impl.ui.accounts";
        
        Enumeration pproviders = protocolProviders.keys();
        ProtocolProviderService currentProvider = null;
        int sameProtocolProvidersCount = 0;        
        
        while(pproviders.hasMoreElements()) {
            ProtocolProviderService pps 
                = (ProtocolProviderService)pproviders.nextElement();
            
            if(pps.getProtocolName().equals(
                    removedProvider.getProtocolName())) {
                
                sameProtocolProvidersCount++;
                if(sameProtocolProvidersCount > 1) {
                    break;
                }
                currentProvider = pps;
            }
        }
        
        if(sameProtocolProvidersCount < 2 && currentProvider != null) {
            protocolProviders.put(currentProvider, new Integer(0));
            
            List accounts = configService
                .getPropertyNamesByPrefix(prefix, true);
    
            Iterator accountsIter = accounts.iterator();
    
            while(accountsIter.hasNext()) {
                String rootPropName
                    = (String) accountsIter.next();
                
                String accountUID
                    = configService.getString(rootPropName);
                
                if(accountUID.equals(currentProvider
                        .getAccountID().getAccountUniqueID())) {
                    
                    configService.setProperty(
                            rootPropName + ".accountIndex",
                            new Integer(0));
                }
            }
            this.getStatusPanel().updateAccount(currentProvider);
        }
    }
}
