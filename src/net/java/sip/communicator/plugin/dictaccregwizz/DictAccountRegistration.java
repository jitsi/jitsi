/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dictaccregwizz;

/**
 * The <tt>DictAccountRegistration</tt> is used to store all user input data
 * through the <tt>DictAccountRegistrationWizard</tt>.
 *
 * @author ROTH Damien
 * @author LITZELMANN Cédric
 */
public class DictAccountRegistration
{
    private String userID;
    private String password;

    /**
     * The hostname of the DICT server.
     */
    private String host;
    
    /**
     * The port of the DICT server.
     */
    private int port;
    
    /**
     * The code id of the strategie selected for the matching of words in dictionnaries.
     */
    private String strategyCode;
    
    /**
     * The real name of the strategie selected for the matching of words in dictionnaries.
     */
    private String strategy;

    /**
     * Returns the User ID of the dict registration account.
     * @return the User ID of the dict registration account.
     */
    public String getUserID()
    {
        return userID;
    }

    /**
     * Sets the password of the dict registration account.
     *
     * @param password the password of the dict registration account.
     */
    public void setPassword(String password)
    {
        this.password = password;
    }

    /**
     * Returns the port of the dict registration account.
     * @return the port of the dict registration account.
     */
    public int getPort() {
        return this.port;
    }

    /**
     * Sets the port of the dict registration account.
     * @param port the port of the dict registration account.
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Returns the host of the dict registration account.
     * @return the host of the dict registration account.
     */
    public String getHost() {
        return this.host;
    }

    /**
     * Sets the host of the dict registration account.
     * @param host The host of the dict registration account.
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * Returns the strategy that will be used for this dict account.
     * @return the strategy that will be used for this dict account.
     */
    public String getStrategy() {
        return this.strategy;
    }

    /**
     * Sets the strategy for this dict account.
     * @param strategy the strategy for this dict account.
     */
    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }
    
    /**
     * Returns the strategy code that will be used for this dict account.
     * @return the strategy code that will be used for this dict account.
     */
    public String getStrategyCode() {
        return this.strategyCode;
    }

    /**
     * Sets the strategy code for this dict account.
     * @param strategyCode the strategy code for this dict account.
     */
    public void setStrategyCode(String strategyCode) {
        this.strategyCode = strategyCode;
    }
}
