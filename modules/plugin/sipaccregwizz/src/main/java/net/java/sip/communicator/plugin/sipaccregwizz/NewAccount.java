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
package net.java.sip.communicator.plugin.sipaccregwizz;

/**
 * @author Yana Stamcheva
 */
public class NewAccount
{
    /**
     * The account user name.
     */
    private String userName;

    /**
     * The account password.
     */
    private char[] password;

    /**
     * The server address.
     */
    private String serverAddress;

    /**
     * The proxy address.
     */
    private String proxyAddress;

    /**
     * The xcapRoot URI.
     */
    private String xcapRoot;

    /**
     * Creates a new account by specifying the account user name and password.
     * @param userName the account user name
     * @param password the account password
     * @param serverAddress the server address to set
     * @param proxyAddress the proxy address to set
     */
    public NewAccount(  String userName,
                        char[] password,
                        String serverAddress,
                        String proxyAddress)
    {
        this.userName = userName;
        this.password = password;
        this.serverAddress = serverAddress;
        this.proxyAddress = proxyAddress;
    }

    /**
     * Sets the account user name.
     * @param userName the user name of the account
     */
    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    /**
     * Returns the account user name.
     * @return the account user name
     */
    public String getUserName()
    {
        return userName;
    }

    /**
     * Sets the account password.
     * @param password the account password
     */
    public void setPassword(char[] password)
    {
        this.password = password;
    }

    /**
     * Returns the account password.
     * @return the account password
     */
    public char[] getPassword()
    {
        return password;
    }

    /**
     * Sets the server address.
     * @param serverAddress the server address to set
     */
    public void setServerAddress(String serverAddress)
    {
        this.serverAddress = serverAddress;
    }

    /**
     * Returns the server address.
     * @return the server address
     */
    public String getServerAddress()
    {
        return serverAddress;
    }

    /**
     * Sets the proxy address.
     * @param proxyAddress the proxy address to set
     */
    public void setProxyAddress(String proxyAddress)
    {
        this.proxyAddress = proxyAddress;
    }

    /**
     * Returns the proxy address.
     * @return the proxy address
     */
    public String getProxyAddress()
    {
        return proxyAddress;
    }

    /**
     * Returns the xcapRoot.
     * @return the xcapRoot
     */
    public String getXcapRoot()
    {
        return xcapRoot;
    }

    /**
     * Sets xcapRoot.
     * @param xcapRoot the xcapRoot to set
     */
    public void setXcapRoot(String xcapRoot)
    {
        this.xcapRoot = xcapRoot;
    }
}
