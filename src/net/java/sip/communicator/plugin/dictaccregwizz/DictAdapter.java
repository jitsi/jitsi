/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.io.*;
import java.net.*;
import java.util.*;

import net.java.sip.communicator.util.Logger;

/**
 * Copy of DictAdapter class to solve the import problem
 * @author ROTH Damien
 * @author LITZELMANN Cédric
 */

public class DictAdapter
{
    private static Logger logger = Logger.getLogger(DictAdapter.class);
    
    /**
     * The socket used to connect to the DICT server.
     */
    private Socket socket = null;

    /**
     * A output stream piped to the socket in order to send command to the server.
     */
    private PrintWriter out = null;

    /**
     * A input stream piped to the socket in order to receive messages from the server.
     */
    private BufferedReader in = null;
    
    /**
     * A boolean telling if we are currently connected to the DICT server.
     */
    private boolean connected = false;
    
    /**
     * Get the strategies allowed by the server for the MATCH command
     * @return a HashMap containing the database list - otherwise null
     */
    public ArrayList<String> getStrategies()
    {
        String fromServer;
        boolean quit = false;
        ArrayList<String> result = null;
        
        // Connexion
        if (!this.connected)
        {
            // Not connected
            return null;
        }
                
        try
        {
            this.out.println("SHOW STRAT");
            fromServer = this.in.readLine();
            
            if (fromServer.startsWith("111"))
            {   // OK - getting responses from the server
                result = new ArrayList<String>();
                while (quit == false && (fromServer = this.in.readLine()) != null)
                {
                    if (fromServer.startsWith("250"))
                    {
                        quit = true;
                    }
                    else if (!fromServer.equals("."))
                    {
                        result.add(fromServer);
                    }
                }
            }
        }
        catch (IOException ioe)
        {
            logger.trace("Cannot get the strategies : " + ioe.getMessage());
            result = null;
        }
        return result;
    }
    
    
    /**
     * Open a connection to the given host on the given port
     * @param host The hostname of the server.
     * @param port The port used by the server.
     * @return true, if a connection is open - false otherwise
     */
    public boolean connect(String host, int port)
    {
        this.connected = false;
        String fromServer;
        
        try
        {
            this.socket = new Socket(host, port);
            this.out = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(),
                        "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),
                        "UTF-8"));
            
            fromServer = this.in.readLine(); // Server banner
            
            if (fromServer.startsWith("220"))
            {   // 220 = connect ok
                this.connected = true;
            }
        }
        catch(Exception ex)
        {   // If an exception is throw == connexion impossible
            logger.trace("Cannot establish a connexion to the server ("
                    + host + ":" + port + ")", ex);
        }
        
        return this.connected;
    }
    
    /**
     * Close the connexion to the server
     */
    public void close()
    {
        String fromServer;
        
        try
        {
            this.out.println("QUIT");
            
            // Clean the socket buffer
            while ((fromServer = this.in.readLine()) != null)
            {
                if (fromServer.startsWith("221"))
                { // Quit response
                    break;
                }
            }
            
            this.out.close();
            this.in.close();
            this.socket.close();
        }
        catch (IOException ioe)
        {
            logger.info("Cannot close the connextion to the server", ioe);
        }
    }
    
    /**
     * Checks if the given url is correct and exists
     * @param host The url that we have to test if it is correct and if it
     * exists.
     * @return true if the url exists - false otherwise
     */
    public static boolean isUrl(String host)
    {
        boolean ok = false;
        
        if (host == null || host.length() == 0)
        {
            return false;
        }
        
        // If an exception is throw, the host format isn't correct or isn't recheable
        try
        {
            InetAddress.getByName(host);
            ok = true;
        }
        catch (UnknownHostException uhex)
        {
            logger.trace("Test URL ("+host+") : " + uhex.getMessage(), uhex);
        }
        return ok;
    }
}
