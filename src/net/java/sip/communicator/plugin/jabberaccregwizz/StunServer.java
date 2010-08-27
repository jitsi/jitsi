package net.java.sip.communicator.plugin.jabberaccregwizz;

/**
 * The <tt>StunServer</tt> keeps all information related to the stun server.
 *
 * @author Yana Stamcheva
 */
public class StunServer
{
    /**
     * The IP address of the server.
     */
    private String ipAddress;

    /**
     * The port of the server.
     */
    private String port;

    /**
     * Indicates if Turn is supported by this server.
     */
    private boolean supportTurn;

    /**
     * The username.
     */
    private String username;

    /**
     * The password.
     */
    private char[] password;

    /**
     * The index of this server in the additional servers table.
     */
    private int index;

    /**
     * Creates an instance of <tt>StunServer</tt> by specifying all parameters.
     * @param ipAddress the IP address of the STUN server
     * @param port the port of the server
     * @param supportTurn indicates if this STUN server supports TURN
     * @param username the user name for authenticating
     * @param password the password
     * @param index the index of this server in the additional servers table
     */
    public StunServer(  String ipAddress,
                        String port,
                        boolean supportTurn,
                        String username,
                        char[] password,
                        int index)
    {
        this.ipAddress = ipAddress;
        this.port = port;
        this.supportTurn = supportTurn;
        this.username = username;
        this.password = password;
        this.index = index;
    }

    /**
     * Returns the IP address of this server.
     * @return the IP address of this server
     */
    String getIpAddress()
    {
        return ipAddress;
    }

    /**
     * Sets the IP address of this server.
     * @param ipAddress the IP address to set
     */
    void setIpAddress(String ipAddress)
    {
        this.ipAddress = ipAddress;
    }

    /**
     * Returns the port of this server.
     * @return the port of this server
     */
    String getPort()
    {
        return port;
    }

    /**
     * Sets the port corresponding to this server.
     * @param port the port to set
     */
    void setPort(String port)
    {
        this.port = port;
    }

    /**
     * Indicates if Turn is supported by this server.
     * @return <tt>true</tt> if Turn is supported by this server, otherwise -
     * returns <tt>false</tt>
     */
    boolean isSupportTurn()
    {
        return supportTurn;
    }

    /**
     * Sets the support turn property to indicate if this server supports Turn.
     * @param supportTurn <tt>true</tt> to indicate that Turn is supported,
     * <tt>false</tt> - otherwise
     */
    void setSupportTurn(boolean supportTurn)
    {
        this.supportTurn = supportTurn;
    }

    /**
     * Returns the username associated to this server.
     * @return the username associated to this server
     */
    String getUsername()
    {
        return username;
    }

    /**
     * Sets the username associated to this server.
     * @param username the username to set
     */
    void setUsername(String username)
    {
        this.username = username;
    }

    /**
     * Returns the password associated to this server username.
     * @return the password associated to this server username
     */
    char[] getPassword()
    {
        return password;
    }

    /**
     * Sets the password associated to this server username.
     * @param password the password to set
     */
    void setPassword(char[] password)
    {
        this.password = password;
    }

    /**
     * The index of this server int the additional stun servers table.
     * @return the index of this server int the additional stun servers table
     */
    int getIndex()
    {
        return index;
    }

    /**
     * Sets the index of this server int the additional stun servers table.
     * @param index the index to set
     */
    void setIndex(int index)
    {
        this.index = index;
    }
}