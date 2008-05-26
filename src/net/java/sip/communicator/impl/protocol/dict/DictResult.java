/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

/**
 * Representation of the results of a response to a query
 * 
 * @author ROTH Damien
 * @author LITZEMANN Cédric
 */
public class DictResult
    implements Iterator<String>
{
    private String databaseName;
    private int index;
    private ArrayList<String> data;
    
    /**
     * Basic construct initializing the result
     */
    public DictResult()
    {
        this.index = 0;
        this.data = new ArrayList<String>();
        this.databaseName = "";
    }
    
    /**
     * Initialize the result and save the database name 
     * @param dbn Database name
     */
    public DictResult(String dbn)
    {
        this.index = 0;
        this.data = new ArrayList<String>();
        this.databaseName = dbn;
    }
    
    /**
     * Add a result
     * @param s result
     */
    public void add(String s)
    {
        this.data.add(s);
    }
    
    /**
     * From the Iterator implementation, return the next part of the result
     * @return the next part of the result
     */
    public String next()
    {
        return (String) this.data.get(this.index++);
    }
    
    /**
     * From the Iterator implementation, return true if the iteration has more elements
     * @return true if the iteration has more elements - false otherwise
     */
    public boolean hasNext()
    {
        return (this.index < this.data.size());
    }
    
    /**
     * From the Iterator implementation but unsupported
     */
    public void remove()
    {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Set the database name
     * @param dbn Database name
     */
    public void setDatabaseName(String dbn)
    {
        this.databaseName = dbn;
    }
    
    /**
     * Return the database name
     * @return the database name
     */
    public String getDatabaseName()
    {
        return this.databaseName;
    }
}
