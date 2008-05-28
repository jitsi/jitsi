/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.dictaccregwizz;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>StrategyThread</tt> is the thread called by the wizzard to populate
 * the strategies list.
 * 
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class StrategyThread
    extends Thread
{
    private static Logger logger = Logger.getLogger(StrategyThread.class);
    
    /**
     * The hostname of the DICT server.
     */
    private String host;

    /**
     * The port used by the DICT server.
     */
    private int port;
    
    /**
     * True if the thread is running.
     */
    private boolean isRunning = false;

    /**
     * True if we need to search the strategies handled by the server in order
     * to populate the list.
     */
    private boolean needProcess = false;
    
    /**
     * The first page wizard for the DICT protocole.
     */
    private FirstWizardPage wizard;
    
    /**
     * The java abstraction of the DICT server.
     */
    private DictAdapter adapter = null;
    
    /**
     * Create a new StrategyThread
     * @param wizard the wizard for callback methods
     */
    public StrategyThread(FirstWizardPage wizard)
    {
        this.wizard = wizard;
    }
    
    /**
     * Thread method running until it's destroy
     */
    public void run()
    {
        while (true)
        {
            if (this.needProcess())
            {
                this.setRunning(true);
                this.process();
                this.processDone();
                this.setRunning(false);
            }
            try
            {
                this.sleep(500);
            }
            catch (InterruptedException ie)
            {
                // Action de log
                logger.info("DICT THREAD : " + ie);
            }
        }
    }
    
    /**
     * Search the strategies on the server and populate the list
     */
    private void process()
    {
        ArrayList<String> strategies = null;
        String temp[];
        
        if (adapter == null) {
            adapter = new DictAdapter();
        }
        
        this.wizard.setStrategyButtonEnable(false);
        
        // Initialize the connexion
        this.wizard.threadMessage("Trying to connect to server");
        if (!adapter.connect(this.host, this.port))
        {
            // Connexion attempt failed
            this.wizard.threadMessage("Connexion attempt failed, this isn't a"
                    + "dict server or the server is offline");
            return;
        }
        
        // Retrieving strategies
        this.wizard.threadMessage("Retrieving strategies");
        strategies = adapter.getStrategies();
        if (strategies == null)
        {
            // No strategy found
            this.wizard.threadMessage("No strategy found on the server");
            return;
        }
        
        
        // Insert the strategies in the list
        for (int i=0; i<strategies.size(); i++)
        {
            temp = strategies.get(i).split(" ", 2);
            
            this.wizard.threadAddStrategy(temp[0], temp[1].replace("\"", ""));
        }
        
        this.wizard.autoSelectStrategy();
        
        // Closing connexion
        this.wizard.threadMessage("Closing connexion");
        adapter.close();
        
        this.wizard.threadRemoveMessage();
        this.wizard.setStrategyButtonEnable(true);
    }
    
    /**
     * Set the hostname of the dict server
     * @param host  The hostname of the server.
     * @return      The thread for populating strategie list.
     */
    public StrategyThread setHost(String host)
    {
        this.host = host;
        return this;
    }
    
    /**
     * Set the port of the dict server
     * @param port  The port of the DICT server.
     * @return      The thread for populating strategie list.
     */
    public StrategyThread setPort(int port)
    {
        this.port = port;
        return this;
    }
    
    /**
     * Checks if the thread is processing a query
     * @return TRUE if the thread is processing a query - FALSE otherwise
     */
    public synchronized boolean isRunning()
    {
        return this.isRunning || this.needProcess;
    }
    
    /**
     * Marks the thread as running or not
     * @param r     Activate or desactivate the strategie thread.
     */
    public synchronized void setRunning(boolean r)
    {
        this.isRunning = r;
    }
    
    /**
     * Checks if we need to process a query
     * @return      Returns true if we need to search and populate the strategie list. False otherwise.
     */
    private synchronized boolean needProcess()
    {
        return this.needProcess;
    }
    
    /**
     * Init the request
     */
    public synchronized void sendProcessRequest()
    {
        this.needProcess = true;
    }
    
    /**
     * Defines that the thread has done the query
     */
    private synchronized void processDone()
    {
        this.needProcess = false;
    }
}
