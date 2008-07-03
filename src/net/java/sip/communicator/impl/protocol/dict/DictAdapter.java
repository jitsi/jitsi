/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Layer abstraction of a dict server 
 * 
 * @author LITZELMANN Cedric
 * @author ROTH Damien
 */
public class DictAdapter
{
    /**
     * The host name of the server: i.e. "dict.org"
     */
    private String host;

    /**
     * The port used by the server. The default one for the DICT protocol is
     * 2628.
     */
    private int port;

    /**
     * The name of the strategy used for searching words with command MATCH:
     * i.e.  the strategie can de "prefix", "suffix", "soundex", "levenshtein", etc.
     */
    private String strategy;

    /**
     * A string representation used to identify the client to the serveur. In
     * our case we will use the "SIP Communicator" string for the client name.
     */
    private String clientName = "";
    
    // Status
    /**
     * The socket used to connect to the DICT server.
     */
    private Socket socket;

    /**
     * A output stream piped to the socket in order to send command to the server.
     */
    private PrintWriter out;

    /**
     * A input stream piped to the socket in order to receive messages from the server.
     */
    private BufferedReader in;
    
    /**
     * A boolean telling if we are currently connected to the DICT server.
     */
    private boolean connected;
    
    /**
     * The list of all the databases hosted by the server. Each database
     * correspond to a dictionnary.
     */
    Vector<String> databasesList;
    
    /**
     * Initialize a basic instance with predefined settings
     */
    public DictAdapter()
    {
        this.host = "dict.org";
        this.port = 2628;
        this.strategy = "prefix";
        this.connected = false;
        this.socket = null;
        this.out = null;
        this.in = null;
    }
    
    /**
     * Initialize a basic instance and set th host
     * @param host Host
     */
    public DictAdapter(String host)
    {
        this.host = host;
        this.port = 2628;
        this.strategy = "prefix";
        this.connected = false;
        this.socket = null;
        this.out = null;
        this.in = null;
    }
    
    /**
     * Initialize an instance and set the host and the port
     * @param host Host
     * @param port Port
     */
    public DictAdapter(String host, int port)
    {
        this.host = host;
        this.port = port;
        this.strategy = "prefix";
        this.connected = false;
        this.socket = null;
        this.out = null;
        this.in = null;
    }
    
    /**
     * Initialize an instance and set the host, port and strategy
     * @param host Host
     * @param port Port
     * @param strategy Match strategy
     */
    public DictAdapter(String host, int port, String strategy)
    {
        this.host = host;
        this.port = port;
        this.strategy = strategy;
        this.connected = false;
        this.socket = null;
        this.out = null;
        this.in = null;
    }
    
    
    /**
     * Establish a connexion to the dict server
     * @throws Exception
     * @return DictResultset containing the error - null otherwise
     */
    private void connect() throws Exception
    {
        String fromServer;
        
        if (this.isConnected())
        {
            return;
        }
        
        try
        {
            this.socket = new Socket(this.host, this.port);
            this.out = new PrintWriter(new OutputStreamWriter(this.socket.getOutputStream(),
                        "UTF-8"), true);
            this.in = new BufferedReader(new InputStreamReader(this.socket.getInputStream(),
                        "UTF-8"));
            
            fromServer = this.in.readLine(); // Server banner
            
            if (fromServer.startsWith("220"))
            {   // 220 = connect ok
                this.connected = true;
                this.client("SIP Communicator");
                return;
            }
            else
            {
                throw new DictException(fromServer.substring(0, 3));
            }
        }
        catch(UnknownHostException uhe)
        {
            throw new DictException(uhe);
        }
        catch(IOException ioe)
        {
            throw new DictException(ioe);
        }
    }
    
    /**
     * Close the actual connexion
     * @throws Exception
     */
    public void close() throws Exception
    {
        String fromServer;
        boolean quit = false;
        
        if (!this.isConnected())
        {
            return;
        }
        
        try
        {
            this.out.println("QUIT");
            
            // Clean the socket buffer
            while (quit == false && (fromServer = this.in.readLine()) != null)
            {
                if (fromServer.startsWith("221"))
                { // Quit response
                    quit = true;
                }
            }
            
            this.out.close();
            this.in.close();
            this.socket.close();
            
            this.connected = false;
        }
        catch (IOException ioe)
        {
            throw new DictException(ioe);
        }
    }
    
    /**
     * Get the database list from the server
     * @throws Exception
     * @return a DictResultset containing the database list - otherwise the error code
     */
    public DictResultset showDB() throws Exception
    {
        String fromServer;
        boolean quit = false;
        DictResultset result = new DictResultset();
        this.connect();
        
        try
        {
            fromServer = this.query("SHOW DB");
            
            if (fromServer.startsWith("110"))
            {   // OK - getting responses from the server
                result.newResultset();
                while (quit == false && (fromServer = this.in.readLine()) != null)
                {
                    if (fromServer.startsWith("250"))
                    {
                        quit = true;
                    }
                    else if (!fromServer.equals("."))
                    {
                        result.addResult(fromServer);
                    }
                }
            }
            else
            {
                throw new DictException(fromServer.substring(0,3));
            }
        }
        catch (IOException ioe)
        {
            throw new DictException(ioe);
        }
        
        return result;
    }
    
    /**
     * Get the strategies allowed by the server for the MATCH command
     * @throws Exception
     * @return a DictResultset containing the database list - otherwise the error code
     */
    public DictResultset showStrat() throws Exception
    {
        String fromServer;
        boolean quit = false;
        DictResultset result = new DictResultset();
        this.connect();
        
        try
        {
            fromServer = this.query("SHOW STRAT");
            
            if (fromServer.startsWith("111"))
            {   // OK - getting responses from the server
                result.newResultset();
                while (quit == false && (fromServer = this.in.readLine()) != null)
                {
                    if (fromServer.startsWith("250"))
                    {
                        quit = true;
                    }
                    else if (!fromServer.equals("."))
                    {
                        result.addResult(fromServer);
                    }
                }
            }
            else
            {
                throw new DictException(fromServer.substring(0,3));
            }
        }
        catch (IOException ioe)
        {
            throw new DictException(ioe);
        }
        
        return result;
    }
    
    
    /**
     * Get the definition of a word
     * @param database the database in which the word will be searched
     * @param word the search word
     * @throws Exception
     * @return a DictResultset containing the database list - otherwise the error code
     */
    public DictResultset define(String database, String word) throws Exception
    {
        String fromServer;
        boolean quit = false;
        DictResultset result = new DictResultset();
        String[] test;
        this.connect();
        
        try
        {
            fromServer = this.query("DEFINE " + database + " " + word);
            
            if (fromServer.startsWith("150"))
            {
                while (quit == false && (fromServer = this.in.readLine()) != null)
                {
                    if (fromServer.startsWith("151"))
                    {   // First line - Contains the DB Name
                        test = fromServer.split(" ", 4);
                        result.newResultset(test[3].substring(1, test[3].length() - 1));
                        continue;
                    }
                    else if (fromServer.startsWith("250"))
                    {   // End of the request
                        quit = true;
                    }
                    else if (!fromServer.equals("."))
                    {
                        result.addResult(fromServer);
                    }
                }
            }
            else
            {
                throw new DictException(fromServer.substring(0,3));
            }
        }
        catch (IOException ioe)
        {
            throw new DictException(ioe);
        }
        
        return result;
    }
    
    /**
     * Get words that match with a strategie form a word with the stored strategy
     * @param database The database in which the words will be searched
     * @param word The base word
     * @return a DictResultset containing the words list - otherwise throw an exception
     * @throws Exception
     */
    public DictResultset match(String database, String word) throws Exception
    {
        return this.match(database, this.strategy, word);
    }
    
    /**
     * Get words that match with a strategie from a word
     * @param database the database in which the words will be searched
     * @param strat the strategies used
     * @param word the base word
     * @throws Exception
     * @return a DictResultset containing the words list - otherwise the error code
     */
    public DictResultset match(String database, String strat, String word) throws Exception
    {
        String fromServer;
        boolean quit = false;
        DictResultset result = new DictResultset();
        this.connect();
        
        try
        {
            fromServer = this.query("MATCH " + database + " " + strat + " " + word);
            
            if (fromServer.startsWith("152"))
            {
                result.newResultset();
                while (quit == false && (fromServer = this.in.readLine()) != null)
                {
                    if (fromServer.startsWith("250"))
                    {
                        quit = true;
                    }
                    else if (!fromServer.equals("."))
                    {
                        result.addResult(fromServer); 
                    }
                }
            }
            else
            {
                throw new DictException(fromServer.substring(0,3));
            }
        }
        catch (IOException ioe)
        {
            throw new DictException(ioe);
        }
        
        return result;
    }

    /**
     * Provide information to the server about the clientname, for logging and statistical purposes
     * @param clientname Client name
     * @throws Exception
     */
    public void client(String clientname) throws Exception 
    {
        String fromServer;
        this.connect();
        
        fromServer = this.query("CLIENT " + clientname);
        
        // 250 code is the only possible answer
        if (!fromServer.startsWith("250"))
        {
            throw new DictException(fromServer.substring(0, 3));
        }
    }
    
    /**
     * Set the host
     * @param newHost host address
     */
    public void setHost(String newHost) throws Exception
    {
        if (isUrl(newHost))
        {
            this.host = newHost;
        }
        else 
        {
            throw new DictException(900, "Host URL is incorrect");
        }
    }
    
    /**
     * Set the host port
     * @param newPort Port
     */
    public void setPort(int newPort)
    {
        this.port = newPort;
    }

    /**
     * Set the strategy
     * @param newStrat Strategy
     */
    public void setStrategy(String newStrat)
    {
        this.strategy = newStrat;
    }
    
    /**
     * Set the client name which is communicated to the server
     * @param cn Client name
     */
    public void setClientName(String cn)
    {
        this.clientName = cn;
    }

    /**
     * Return the host
     * @return return the host
     */
    public String getHost()
    {
        return this.host;
    }
    
    /**
     * Return the port
     * @return return the port
     */
    public int getPort()
    {
        return this.port;
    }
    
    /**
     * Return the strategy
     * @return return the strategy
     */
    public String getStrategy()
    {
        return this.strategy;
    }
    
    /**
     * Return the client name
     * @return return the client name
     */
    public String getClientName()
    {
        return this.clientName;
    }
    
    /**
     * Gets the database's list from the server
     * @return List of the databases
     * @throws Exception
     */
    public Vector<String> getDatabases() throws Exception
    {
        if (this.databasesList == null)
        {
            DictResultset drs = this.showDB();
            DictResult list = drs.getResultset(0);
            
            this.databasesList = new Vector<String>();
                
            while(list.hasNext())
            {
                this.databasesList.add(list.next());
            }
        }
        return this.databasesList;
    }
    
    /**
     * Gets the dictionary name from the databases list
     * @param code Dictionary code
     * @return the dictionary name
     * @throws Exception
     */
    public String getDictionaryName(String code) throws Exception
    {
        int dictionary_id_and_description_separator;
        String dictionary_id_and_description;
        String dictionary_id;
        String dictionary_description;
        
        // First, we check if the code is a special code
        // Checks the RFC-2229 for more details
        if (code.equals("*"))
        {
            return "Any dictionary";
        }
        else if (code.equals("!"))
        {
            return "First match";
        }
        
        // Gets the databases list
        if (this.databasesList == null)
        {
            getDatabases();
            
        }
        
        // Look down the databases list to get the name 
        for (int i=0; i<this.databasesList.size(); i++)
        {
            dictionary_id_and_description = this.databasesList.get(i);
            dictionary_id_and_description_separator = dictionary_id_and_description.indexOf(' ');
            dictionary_id = dictionary_id_and_description.substring(0, dictionary_id_and_description_separator); 
            
            if (dictionary_id.equals(code))
            {
                dictionary_description = dictionary_id_and_description.substring(dictionary_id_and_description_separator + 1); 
                return dictionary_description.replace("\"", "");
            }
        }
        
        // If the name isn't in the list, return null
        return null;
    }
    
    /**
     * Check if we are connected to the server
     * @return true if we are connected - false otherwise
     */
    public boolean isConnected()
    {
        return this.connected;
    }

    /**
     * Check if the URL is correct and a server exists
     * @param url an Url
     * @return true if everything is ok - false otherwise
     */
    public static boolean isUrl(String url)
    {
        boolean ok;
        if (url == null)
        {
            return false;
        }
        
        try
        {
            InetAddress.getByName(url);
            ok = true;
        }
        catch (UnknownHostException uhex)
        {
            ok = false;
        }
        
        return ok;
    }
    
    /**
     * Executes a query and deals with the automatic deconnexion
     * @param query A query to send to the server
     * @return The first ligne of the response from the server
     * @throws Exception IOException and DictException
     */
    private String query(String query) throws Exception
    {
        String result = null;

        this.out.println(query);
        result = in.readLine();

        if (result == null)
        {
            // The connexion may be close, reconnexion
            this.connected = false;
            this.connect();

            this.out.println(query);
            result = in.readLine();

            if (result == null)
            {
                // If result is still equal to null, the server is unavailable
                // We send the appropriate exception
                throw new DictException(420);
            }
        }

        return result;
    }
}
