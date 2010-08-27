/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.jabberaccregwizz;

import java.util.*;

/**
 * The <tt>JabberAccountRegistration</tt> is used to store all user input data
 * through the <tt>JabberAccountRegistrationWizard</tt>.
 *
 * @author Yana Stamcheva
 */
public class JabberAccountRegistration
{
    /**
     * The default value of server port for jabber accounts.
     */
    public static final String DEFAULT_PORT = "5222";

    /**
     * The default value of the priority property.
     */
    public static final String DEFAULT_PRIORITY = "10";

    /**
     * The default value of the resource property.
     */
    public static final String DEFAULT_RESOURCE = "sip-comm";

    /**
     * The default value of stun server port for jabber accounts.
     */
    public static final String DEFAULT_STUN_PORT = "3478";

    /**
     * The user identifier.
     */
    private String userID;

    /**
     * The password.
     */
    private String password;

    /**
     * Indicates if the password should be remembered.
     */
    private boolean rememberPassword = true;

    /**
     * The server address.
     */
    private String serverAddress;

    /**
     * The port.
     */
    private int port = new Integer(DEFAULT_PORT).intValue();

    /**
     * The resource property, initialized to the default resource.
     */
    private String resource = DEFAULT_RESOURCE;

    /**
     * The priority property.
     */
    private int priority = new Integer(DEFAULT_PRIORITY).intValue();

    /**
     * Indicates if keep alive packets should be send.
     */
    private boolean sendKeepAlive = true;

    /**
     * Indicates if gmail notifications should be enabled.
     */
    private boolean enableGmailNotification = false;

    /**
     * Indicates if ICE should be used.
     */
    private boolean isUseIce = false;

    /**
     * Indicates if STUN server should be automatically discovered.
     */
    private boolean isAutoDiscoverStun = false;

    /**
     * The list of additional STUN servers entered by user.
     */
    private LinkedList<StunServer> additionalStunServers;

    /**
     * Returns the password of the jabber registration account.
     * @return the password of the jabber registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the jabber registration account.
     * @param password the password of the jabber registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns TRUE if password has to remembered, FALSE otherwise.
     * @return TRUE if password has to remembered, FALSE otherwise
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Sets the rememberPassword value of this jabber account registration.
     * @param rememberPassword TRUE if password has to remembered, FALSE
     * otherwise
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }

    /**
     * Returns the User ID of the jabber registration account.
     * @return the User ID of the jabber registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * The address of the server we will use for this account
     * @return String
     */
    public String getServerAddress()
    {
        return serverAddress;
    }

    /**
     * The port on the specified server
     * @return int
     */
    public int getPort()
    {
        return port;
    }

    /**
     * Determines whether sending of keep alive packets is enabled.
     *
     * @return <tt>true</tt> if keep alive packets are to be sent for this
     * account and <tt>false</tt> otherwise.
     */
    public boolean isSendKeepAlive()
    {
        return sendKeepAlive;
    }

    /**
     * Determines whether SIP Communicator should be querying GMail servers
     * for unread mail messages.
     *
     * @return <tt>true</tt> if we are to enable GMail notifications and
     * <tt>false</tt> otherwise.
     */
    public boolean isGmailNotificationEnabled()
    {
        return enableGmailNotification;
    }

    /**
     * Sets the User ID of the jabber registration account.
     *
     * @param userID the identifier of the jabber registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Sets the server
     *
     * @param serverAddress the IP address or FQDN of the server.
     */
    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    /**
     * Sets the server port number.
     *
     * @param port the server port number
     */
    public void setPort(int port)
    {
        this.port = port;
    }

    /**
     * Specifies whether SIP Communicator should send send keep alive packets
     * to keep this account registered.
     *
     * @param sendKeepAlive <tt>true</tt> if we are to send keep alive packets
     * and <tt>false</tt> otherwise.
     */
    public void setSendKeepAlive(boolean sendKeepAlive)
    {
        this.sendKeepAlive = sendKeepAlive;
    }

    /**
     * Specifies whether SIP Communicator should be querying GMail servers
     * for unread mail messages.
     *
     * @param enabled <tt>true</tt> if we are to enable GMail notification and
     * <tt>false</tt> otherwise.
     */
    public void setGmailNotificationEnabled(boolean enabled)
    {
        this.enableGmailNotification = enabled;
    }

    /**
     * Returns the resource.
     * @return the resource
     */
    public String getResource()
    {
        return resource;
    }

    /**
     * Sets the resource.
     * @param resource the resource for the jabber account
     */
    public void setResource(String resource)
    {
        this.resource = resource;
    }

    /**
     * Returns the priority property.
     * @return priority
     */
    public int getPriority()
    {
        return priority;
    }

    /**
     * Sets the priority property.
     * @param priority the priority to set
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    /**
     * Indicates if ice should be used for this account.
     * @return <tt>true</tt> if ICE should be used for this account, otherwise
     * returns <tt>false</tt>
     */
    public boolean isUseIce()
    {
        return isUseIce;
    }

    /**
     * Sets the <tt>useIce</tt> property.
     * @param isUseIce <tt>true</tt> to indicate that ICE should be used for
     * this account, <tt>false</tt> - otherwise.
     */
    public void setUseIce(boolean isUseIce)
    {
        this.isUseIce = isUseIce;
    }

    /**
     * Indicates if the stun server should be automatically discovered.
     * @return <tt>true</tt> if the stun server should be automatically
     * discovered, otherwise returns <tt>false</tt>.
     */
    public boolean isAutoDiscoverStun()
    {
        return isAutoDiscoverStun;
    }

    /**
     * Sets the <tt>autoDiscoverStun</tt> property.
     * @param isAutoDiscover <tt>true</tt> to indicate that stun server should
     * be auto-discovered, <tt>false</tt> - otherwise.
     */
    public void setAutoDiscoverStun(boolean isAutoDiscover)
    {
        this.isAutoDiscoverStun = isAutoDiscover;
    }

    /**
     * Adds the given <tt>stunServer</tt> to the list of additional stun servers.
     * @param stunServer the <tt>StunServer</tt> to add
     */
    public void addStunServer(StunServer stunServer)
    {
        if (additionalStunServers == null)
            additionalStunServers = new LinkedList<StunServer>();

        additionalStunServers.add(stunServer);
    }

    /**
     * Returns an <tt>Iterator</tt> over a list of all additional stun servers
     * entered by the user.
     * @return an <tt>Iterator</tt> over a list of all additional stun servers
     */
    public Iterator<StunServer> getAdditionalStunServers()
    {
        if (additionalStunServers != null)
            return additionalStunServers.iterator();
        return null;
    }
}
