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
package net.java.sip.communicator.plugin.ircaccregwizz;

/**
 * The <tt>IrcAccountRegistration</tt> is used to store all user input data
 * through the <tt>IrcAccountRegistrationWizard</tt>.
 *
 * @author Lionel Ferreira & Michael Tarantino
 * @author Danny van Heumen
 */
public class IrcAccountRegistration
{
    private String userID;
    private String password;
    private String server;
    private String port;
    private boolean rememberPassword;
    private boolean autoChangeNick;
    private boolean isRequiredPassword;
    private boolean secureConnection;
    private boolean saslEnabled;
    private String saslUser;
    private String saslRole;
    private boolean resolveDnsThroughProxy;

    /**
     * Option for activating contact presence task.
     */
    private boolean contactPresenceTaskEnabled;

    /**
     * Option for activating chat room members presence task.
     */
    private boolean chatroomPresenceTaskEnabled;

    /**
     * Returns the User ID of the IRC registration account.
     *
     * @return the User ID of the IRC registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the user ID of the IRC registration account.
     *
     * @param userID the userID of the IRC registration account.
     */
    public void setUserID(String userID)
    {
        this.userID = userID;
    }

    /**
     * Returns the password of the IRC registration account.
     *
     * @return the password of the IRC registration account.
     */
    public String getPassword()
    {
        return password;
    }

    /**
     * Sets the password of the IRC registration account.
     *
     * @param password the password of the IRC registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns the server address.
     *
     * @return the server address.
     */
    public String getServer()
    {
        return server;
    }

    /**
     * Sets the server address.
     *
     * @param server the address of the server
     */
    public void setServer(String server)
    {
        this.server = server;
    }

    /**
     * Returns the port to use.
     *
     * @return the port to use
     */
    public String getPort()
    {
        return this.port;
    }

    /**
     * Sets the port to use.
     *
     * @param port the port to use
     */
    public void setPort(String port)
    {
        this.port = port;
    }

    /**
     * Returns <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     *
     * @return <tt>true</tt> if password has to remembered, <tt>false</tt>
     * otherwise.
     */
    public boolean isRememberPassword()
    {
        return rememberPassword;
    }

    /**
     * Indicates if the nick should be changed automatically in case of nick
     * collision.
     *
     * @return <code>true</code> if the nick should be changed,
     * <code>false</code> - otherwise.
     */
    public boolean isAutoChangeNick()
    {
        return autoChangeNick;
    }

    /**
     * Sets the property indicating if the nick should be changed automatically
     * in case of nick collision.
     * @param autoChangeNick <code>true</code> to indicate that the nick could
     * be changed, <code>false</code> - otherwise.
     */
    public void setAutoChangeNick(boolean autoChangeNick)
    {
        this.autoChangeNick = autoChangeNick;
    }

    /**
     * Indicates if the password is required or not.
     * @return <code>true</code> to indicate that the password is required,*
     * <code>false</code> - otherwise.
     */
    public boolean isRequiredPassword()
    {
        return isRequiredPassword;
    }

    /**
     * Sets the <tt>isRequiredPassword</tt> property.
     *
     * @param isRequiredPassword <code>true</code> to indicate that the password
     * is required, <code>false</code> - otherwise.
     */
    public void setRequiredPassword(boolean isRequiredPassword)
    {
        this.isRequiredPassword = isRequiredPassword;
    }

    /**
     * Sets the rememberPassword value of this IRC account registration.
     *
     * @param rememberPassword <tt>true</tt> if password has to remembered,
     * <tt>false</tt> otherwise.
     */
    public void setRememberPassword(boolean rememberPassword)
    {
        this.rememberPassword = rememberPassword;
    }
    
    /**
     * Indicates if the the connection must be secure or not.
     *
     * @return returns <code>true</code> to indicate that the connection should
     *         be secure, or false for unsecured connection.
     */
    public boolean isSecureConnection()
    {
        return this.secureConnection;
    }
    
    /**
     * Set the <tt>useSecureConnection</tt> property.
     *
     * @param secureConnection true to require secure connection, or false
     *            for unsecured connections
     */
    public void setSecureConnection(boolean secureConnection)
    {
        this.secureConnection = secureConnection;
    }

    /**
     * Get contact presence task enabled.
     *
     * @return returns <tt>true</tt> if task should be enabled
     */
    public boolean isContactPresenceTaskEnabled()
    {
        return this.contactPresenceTaskEnabled;
    }

    /**
     * Set contact presence task.
     *
     * @param value value
     */
    public void setContactPresenceTaskEnabled(final boolean value)
    {
        this.contactPresenceTaskEnabled = value;
    }

    /**
     * Get chat room presence task.
     *
     * @return returns <tt>true</tt> if task should be enabled
     */
    public boolean isChatRoomPresenceTaskEnabled()
    {
        return this.chatroomPresenceTaskEnabled;
    }

    /**
     * Set chat room presence task.
     *
     * @param value value
     */
    public void setChatRoomPresenceTaskEnabled(final boolean value)
    {
        this.chatroomPresenceTaskEnabled = value;
    }

    /**
     * Get SASL enable status.
     *
     * @return Returns <tt>true</tt> if SASL is enabled, or <tt>false</tt> if
     *         SASL is disabled.
     */
    public boolean isSaslEnabled()
    {
        return this.saslEnabled;
    }

    /**
     * Set SASL enabled.
     *
     * @param enabled <tt>true</tt> to enable SASL, <tt>false</tt> to disable
     */
    public void setSaslEnabled(final boolean enabled)
    {
        this.saslEnabled = enabled;
    }

    /**
     * Get SASL user name.
     *
     * @return Returns SASL user name.
     */
    public String getSaslUser()
    {
        return this.saslUser;
    }

    /**
     * Set SASL user name. (Mandatory)
     *
     * @param user SASL user name
     */
    public void setSaslUser(final String user)
    {
        this.saslUser = user;
    }

    /**
     * Get SASL authorization role. (Optional)
     *
     * @return Returns the SASL authorization role if configured, of
     *         <tt>null</tt> if no role known.
     */
    public String getSaslRole()
    {
        return this.saslRole;
    }

    /**
     * Set SASL authorization role.
     *
     * @param role the SASL authorization role
     */
    public void setSaslRole(final String role)
    {
        this.saslRole = role;
    }

    /**
     * Get property for resolving DNS names through configured proxy server.
     * <tt>true</tt> to resolve DNS names through configured proxy server, or
     * <tt>false</tt> to resolve using own DNS server.
     */
    public boolean isResolveDnsThroughProxy()
    {
        return this.resolveDnsThroughProxy;
    }

    /**
     * Set property for resolving DNS through configured proxy server.
     *
     * @param value <tt>true</tt> to enable resolving through proxy server, or
     *            <tt>false</tt> to resolve via local DNS server
     */
    public void setResolveDnsThroughProxy(final boolean value)
    {
        this.resolveDnsThroughProxy = value;
    }
}
