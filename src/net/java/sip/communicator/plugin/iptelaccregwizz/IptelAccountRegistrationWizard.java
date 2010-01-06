/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.iptelaccregwizz;

import java.awt.*;
import java.util.*;

import net.java.sip.communicator.service.gui.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * The <tt>IPPIAccountRegistrationWizard</tt> is an implementation of the
 * <tt>AccountRegistrationWizard</tt> for the SIP protocol. It should allow
 * the user to create and configure a new SIP account.
 *
 * @author Yana Stamcheva
 */
public class IptelAccountRegistrationWizard
    implements AccountRegistrationWizard
{
    /**
     * The protocol name.
     */
    public static final String PROTOCOL = "iptel.org";

    private FirstWizardPage firstWizardPage;

    private IptelAccountRegistration registration
        = new IptelAccountRegistration();

    private final WizardContainer wizardContainer;

    private ProtocolProviderService protocolProvider;

    private boolean isModification;

    private static final Logger logger
        = Logger.getLogger(IptelAccountRegistrationWizard.class);

    /**
     * Creates an instance of <tt>IPPIAccountRegistrationWizard</tt>.
     * @param wizardContainer the wizard container, where this wizard
     * is added
     */
    public IptelAccountRegistrationWizard(WizardContainer wizardContainer)
    {
        this.wizardContainer = wizardContainer;

        this.wizardContainer.setFinishButtonText(
            Resources.getString("service.gui.SIGN_IN"));
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getIcon</code> method.
     * Returns the icon to be used for this wizard.
     * @return byte[]
     */
    public byte[] getIcon()
    {
        return Resources.getImage(Resources.PROTOCOL_ICON);
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getPageImage</code> method.
     * Returns the image used to decorate the wizard page
     *
     * @return byte[] the image used to decorate the wizard page
     */
    public byte[] getPageImage()
    {
        return Resources.getImage(Resources.PAGE_IMAGE);
    }


    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolName</code>
     * method. Returns the protocol name for this wizard.
     * @return String
     */
    public String getProtocolName()
    {
        return Resources.getString("plugin.iptelaccregwizz.PROTOCOL_NAME");
    }

    /**
     * Implements the <code>AccountRegistrationWizard.getProtocolDescription
     * </code> method. Returns the description of the protocol for this wizard.
     * @return String
     */
    public String getProtocolDescription()
    {
        return Resources.getString("plugin.iptelaccregwizz.PROTOCOL_DESCRIPTION");
    }

    /**
     * Returns the set of pages contained in this wizard.
     * @return Iterator
     */
    public Iterator<WizardPage> getPages() {
        java.util.List<WizardPage> pages = new ArrayList<WizardPage>();
        firstWizardPage = new FirstWizardPage(this);

        pages.add(firstWizardPage);

        return pages.iterator();
    }

    /**
     * Returns the set of data that user has entered through this wizard.
     * @return Iterator
     */
    public Iterator<Map.Entry<String, String>> getSummary() 
    {
        ArrayList<Map.Entry<String, String>> summaryTable
            = new ArrayList<Map.Entry<String, String>>();
        boolean rememberPswd = registration.isRememberPassword();
        String rememberPswdString = Resources.getString(
                rememberPswd ? "service.gui.YES" : "service.gui.NO");

        summaryTable.add(new SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.USERNAME"),
            registration.getId()));
        summaryTable.add(new SimpleEntry<String, String>(
            Resources.getString("service.gui.REMEMBER_PASSWORD"),
            rememberPswdString));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.REGISTRAR"),
            registration.getServerAddress()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.AUTH_NAME"),
            registration.getAuthorizationName()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.SERVER_PORT"),
            registration.getServerPort()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.PROXY"),
            registration.getProxy()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.PROXY_PORT"),
            registration.getProxyPort()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.PREFERRED_TRANSPORT"),
            registration.getPreferredTransport()));

        if (registration.isEnablePresence())
        {
            summaryTable.add(new  SimpleEntry<String, String>(
                Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
                Resources.getString("service.gui.YES")));
        }
        else
        {
            summaryTable.add(new  SimpleEntry<String, String>(
                Resources.getString("plugin.sipaccregwizz.ENABLE_PRESENCE"),
                Resources.getString("service.gui.NO")));
        }

        if (registration.isForceP2PMode())
        {
            summaryTable.add(new  SimpleEntry<String, String>(
                Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
                Resources.getString("service.gui.YES")));
        }
        else
        {
            summaryTable.add(new  SimpleEntry<String, String>(
                Resources.getString("plugin.sipaccregwizz.FORCE_P2P_PRESENCE"),
                Resources.getString("service.gui.NO")));
        }

        if (registration.isDefaultEncryption())
        {
            summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
                Resources.getString("service.gui.YES")));
        }
        else
        {
            summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_DEFAULT_ENCRYPTION"),
                Resources.getString("service.gui.NO")));
        }

        if (registration.isSipZrtpAttribute())
        {
            summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
                Resources.getString("service.gui.YES")));
        }
        else
        {
            summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.ENABLE_SIPZRTP_ATTRIBUTE"),
                Resources.getString("service.gui.NO")));
        }

        summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.OFFLINE_CONTACT_POLLING_PERIOD"),
                registration.getPollingPeriod()));
        summaryTable.add(new  SimpleEntry<String, String>(Resources.getString(
                "plugin.sipaccregwizz.SUBSCRIPTION_EXPIRATION"),
                registration.getSubscriptionExpiration()));

        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_METHOD"),
            registration.getKeepAliveMethod()));
        summaryTable.add(new  SimpleEntry<String, String>(
            Resources.getString("plugin.sipaccregwizz.KEEP_ALIVE_INTERVAL"),
            registration.getKeepAliveInterval()));

        return summaryTable.iterator();
    }

    /**
     * Installs the account created through this wizard.
     * @return the created <tt>ProtocolProviderService</tt>
     * @throws OperationFailedException if the sign in operation fails
     */
    public ProtocolProviderService signin()
        throws OperationFailedException
    {
        firstWizardPage.commitPage();

        return signin(registration.getId(), registration.getPassword());
    }

    /**
     * Installs the account with the given user name and password.
     * @param username the username to sign in with
     * @param password the password used for login 
     * @return the <tt>ProtocolProviderService</tt> corresponding to the newly
     * created account.
     * @throws OperationFailedException if the sign in operation fails
     */
    public ProtocolProviderService signin(String username, String password)
        throws OperationFailedException
    {
        ProtocolProviderFactory factory
            = IptelAccRegWizzActivator.getIptelProtocolProviderFactory();

        ProtocolProviderService pps = null;
        if (factory != null)
            pps = this.installAccount(  factory,
                                        username,
                                        password);

        return pps;
    }

    /**
     * Creates an account for the given user and password.
     *
     * @param providerFactory the ProtocolProviderFactory which will create
     * the account
     * @param userName the user identifier
     * @param passwd the password
     * @return the <tt>ProtocolProviderService</tt> for the new account.
     * @throws OperationFailedException if the install account operation fails
     */
    private ProtocolProviderService installAccount(
            ProtocolProviderFactory providerFactory,
            String userName,
            String passwd)
        throws OperationFailedException
    {
        Hashtable<String, String> accountProperties
            = new Hashtable<String, String>();

        /* Make the account use the resources specific to iptel.org. */
        accountProperties.put(ProtocolProviderFactory.PROTOCOL, PROTOCOL);
        accountProperties
            .put(ProtocolProviderFactory.PROTOCOL_ICON_PATH,
                "resources/images/protocol/iptel");

        if(registration.isRememberPassword())
        {
            accountProperties.put(ProtocolProviderFactory.PASSWORD, passwd);
        }

        String serverAddress = null;
        if (registration.getServerAddress() != null)
            serverAddress = registration.getServerAddress();
        else
            serverAddress = getServerFromUserName(userName);

        if (serverAddress != null)
        {
            accountProperties.put(ProtocolProviderFactory.SERVER_ADDRESS,
                serverAddress);

            if (userName.indexOf(serverAddress) < 0)
                accountProperties.put(
                    ProtocolProviderFactory.IS_SERVER_OVERRIDDEN,
                    Boolean.toString(true));
        }

        if(registration.getAuthorizationName() != null)
            accountProperties.put(ProtocolProviderFactory.AUTHORIZATION_NAME,
                registration.getAuthorizationName());

        accountProperties.put(ProtocolProviderFactory.SERVER_PORT,
                registration.getServerPort());

        String proxyAddress = null;
        if (registration.getProxy() != null)
            proxyAddress = registration.getProxy();
        else
            proxyAddress = getServerFromUserName(userName);

        if (proxyAddress != null)
            accountProperties.put(ProtocolProviderFactory.PROXY_ADDRESS,
                proxyAddress);

        accountProperties.put(ProtocolProviderFactory.PROXY_PORT,
                registration.getProxyPort());

        accountProperties.put(ProtocolProviderFactory.PREFERRED_TRANSPORT,
                registration.getPreferredTransport());

        accountProperties.put(ProtocolProviderFactory.IS_PRESENCE_ENABLED,
                Boolean.toString(registration.isEnablePresence()));

        accountProperties.put(ProtocolProviderFactory.FORCE_P2P_MODE,
                Boolean.toString(registration.isForceP2PMode()));

        accountProperties.put(ProtocolProviderFactory.DEFAULT_ENCRYPTION,
                Boolean.toString(registration.isDefaultEncryption()));

        accountProperties.put(ProtocolProviderFactory.DEFAULT_SIPZRTP_ATTRIBUTE,
                Boolean.toString(registration.isSipZrtpAttribute()));

        accountProperties.put(ProtocolProviderFactory.POLLING_PERIOD,
                registration.getPollingPeriod());

        accountProperties.put(ProtocolProviderFactory.SUBSCRIPTION_EXPIRATION,
                registration.getSubscriptionExpiration());

        accountProperties.put("KEEP_ALIVE_METHOD",
                registration.getKeepAliveMethod());

        accountProperties.put("KEEP_ALIVE_INTERVAL",
            registration.getKeepAliveInterval());

        if(isModification)
        {
            accountProperties.put(ProtocolProviderFactory.USER_ID, userName);
            providerFactory.modifyAccount(  protocolProvider,
                                            accountProperties);

            this.isModification  = false;

            return protocolProvider;
        }

        try
        {
            AccountID accountID = providerFactory.installAccount(
                    userName, accountProperties);

            ServiceReference serRef = providerFactory
                .getProviderForAccount(accountID);

            protocolProvider
                = (ProtocolProviderService) IptelAccRegWizzActivator
                    .bundleContext.getService(serRef);
        }
        catch (IllegalStateException exc)
        {
            logger.warn(exc.getMessage());

            throw new OperationFailedException(
                "Account already exists.",
                OperationFailedException.IDENTIFICATION_CONFLICT);
        }
        catch (Exception exc)
        {
            exc.printStackTrace();

            throw new OperationFailedException(
                exc.getMessage(),
                OperationFailedException.GENERAL_ERROR);
        }

        return protocolProvider;
    }

    /**
     * Fills the id and Password fields in this panel with the data coming
     * from the given protocolProvider.
     * @param protocolProvider The <tt>ProtocolProviderService</tt> to load the
     * data from.
     */
    public void loadAccount(ProtocolProviderService protocolProvider)
    {
        this.isModification = true;

        this.protocolProvider = protocolProvider;

        this.registration = new IptelAccountRegistration();

        this.firstWizardPage.loadAccount(protocolProvider);
    }

    /**
     * Indicates if this wizard is opened for modification or for creating a
     * new account.
     *
     * @return <code>true</code> if this wizard is opened for modification and
     * <code>false</code> otherwise.
     */
    public boolean isModification()
    {
        return isModification;
    }

    /**
     * Returns the wizard container, where all pages are added.
     *
     * @return the wizard container, where all pages are added
     */
    public WizardContainer getWizardContainer()
    {
        return wizardContainer;
    }

    /**
     * Returns the registration object, which will store all the data through
     * the wizard.
     *
     * @return the registration object, which will store all the data through
     * the wizard
     */
    public IptelAccountRegistration getRegistration()
    {
        return registration;
    }

    /**
     * Returns the size of this wizard.
     * @return the size of this wizard
     */
    public Dimension getSize()
    {
        return new Dimension(600, 500);
    }

    /**
     * Returns the identifier of the page to show first in the wizard.
     * @return the identifier of the page to show first in the wizard.
     */
    public Object getFirstPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Returns the identifier of the page to show last in the wizard.
     * @return the identifier of the page to show last in the wizard.
     */
    public Object getLastPageIdentifier()
    {
        return firstWizardPage.getIdentifier();
    }

    /**
     * Sets the modification property to indicate if this wizard is opened for
     * a modification.
     *
     * @param isModification indicates if this wizard is opened for modification
     * or for creating a new account.
     */
    public void setModification(boolean isModification)
    {
        this.isModification = isModification;
    }

    /**
     * Returns an example string, which should indicate to the user how the
     * user name should look like.
     * @return an example string, which should indicate to the user how the
     * user name should look like.
     */
    public String getUserNameExample()
    {
        return FirstWizardPage.USER_NAME_EXAMPLE;
    }

    /**
     * Enables the simple "Sign in" form.
     * @return <tt>true</tt> if the simple form is enabled, otherwise returns
     * <tt>false</tt>
     */
    public boolean isSimpleFormEnabled()
    {
        return true;
    }

    /**
     * Return the server part of the sip user name.
     *
     * @param userName the user name entered by the user
     * @return the server part of the sip user name.
     */
    protected String getServerFromUserName(String userName)
    {
        int delimIndex = userName.indexOf("@");
        if (delimIndex != -1)
        {
            return userName.substring(delimIndex + 1);
        }

        return null;
    }

    /**
     * Opens the browser on the page sign up
     */
    public void webSignup()
    {
        IptelAccRegWizzActivator.getBrowserLauncher()
            .openURL("https://serweb.iptel.org/user/reg/index.php");
    }

    /**
     * Returns <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise.
     * @return <code>true</code> if the web sign up is supported by the current
     * implementation, <code>false</code> - otherwise
     */
    public boolean isWebSignupSupported()
    {
        return true;
    }

    /**
     * Returns the simple form used in the simple account wizard.
     * @return the simple form used in the simple account wizard
     */
    public Object getSimpleForm()
    {
        firstWizardPage = new FirstWizardPage(this);
        return firstWizardPage.getSimpleForm();
    }
    
    /**
     * A class which implements Map.Entry. 
     * 
     * This private class is necessary to compile SC using Java 5. This
     * class was copied from GNU classpath and modified to fit here.
     *
     * @author Jon Zeppieri
     * @author Eric Blake (ebb9@email.byu.edu)
     * 
     * @since 1.6
     */
    private static class SimpleEntry<K, V> implements Map.Entry<K, V>
    {

      /**
       * Compatible with JDK 1.6
       */
      private static final long serialVersionUID = -8499721149061103585L;

      /**
       * The key. Package visible for direct manipulation.
       */
      K key;

      /**
       * The value. Package visible for direct manipulation.
       */
      V value;

      /**
       * Basic constructor initializes the fields.
       * @param newKey the key
       * @param newValue the value
       */
      protected SimpleEntry(K newKey, V newValue)
      {
        key = newKey;
        value = newValue;
      }
      
      protected SimpleEntry(Map.Entry<? extends K, ? extends V> entry)
      {
        this(entry.getKey(), entry.getValue());
      }

      static final boolean equals(Object o1, Object o2)
      {
        return o1 == o2 || (o1 != null && o1.equals(o2));
      }

      static final int hashCode(Object o)
      {
        return o == null ? 0 : o.hashCode();
      }

      /**
       * Compares the specified object with this entry. Returns true only if
       * the object is a mapping of identical key and value. In other words,
       * this must be:<br>
       * <pre>(o instanceof Map.Entry)
       *       && (getKey() == null ? ((HashMap) o).getKey() == null
       *           : getKey().equals(((HashMap) o).getKey()))
       *       && (getValue() == null ? ((HashMap) o).getValue() == null
       *           : getValue().equals(((HashMap) o).getValue()))</pre>
       *
       * @param o the object to compare
       * @return <code>true</code> if it is equal
       */
      @SuppressWarnings("unchecked")
      public boolean equals(Object o)
      {
        if (! (o instanceof Map.Entry))
          return false;
        // Optimize for our own entries.
        if (o instanceof SimpleEntry)
          {
            SimpleEntry<K,V> e = (SimpleEntry<K,V>) o;
            return (SimpleEntry.equals(key, e.key)
                    && SimpleEntry.equals(value, e.value));
          }
        Map.Entry<K,V> e = (Map.Entry<K,V>) o;
        return (SimpleEntry.equals(key, e.getKey())
                && SimpleEntry.equals(value, e.getValue()));
      }

      /**
       * Get the key corresponding to this entry.
       *
       * @return the key
       */
      public K getKey()
      {
        return key;
      }

      /**
       * Get the value corresponding to this entry. If you already called
       * Iterator.remove(), the behavior undefined, but in this case it works.
       *
       * @return the value
       */
      public V getValue()
      {
        return value;
      }

      /**
       * Returns the hash code of the entry.  This is defined as the exclusive-or
       * of the hashcodes of the key and value (using 0 for null). In other
       * words, this must be:<br>
       * <pre>(getKey() == null ? 0 : getKey().hashCode())
       *       ^ (getValue() == null ? 0 : getValue().hashCode())</pre>
       *
       * @return the hash code
       */
      public int hashCode()
      {
        return (SimpleEntry.hashCode(key) ^ SimpleEntry.hashCode(value));
      }

      /**
       * Replaces the value with the specified object. This writes through
       * to the map, unless you have already called Iterator.remove(). It
       * may be overridden to restrict a null value.
       *
       * @param newVal the new value to store
       * @return the old value
       * @throws NullPointerException if the map forbids null values.
       * @throws UnsupportedOperationException if the map doesn't support
       *          <code>put()</code>.
       * @throws ClassCastException if the value is of a type unsupported
       *         by the map.
       * @throws IllegalArgumentException if something else about this
       *         value prevents it being stored in the map.
       */
      public V setValue(V newVal)
      {
        V r = value;
        value = newVal;
        return r;
      }

      /**
       * This provides a string representation of the entry. It is of the form
       * "key=value", where string concatenation is used on key and value.
       *
       * @return the string representation
       */
      public String toString()
      {
        return key + "=" + value;
      }
    } // class SimpleEntry
}