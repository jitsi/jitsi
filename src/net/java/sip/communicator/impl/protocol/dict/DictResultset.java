/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.dict;

import java.util.*;

/**
 * Class managing the results of a dict query
 * 
 * @author ROTH Damien
 * @author LITZELMANN Cedric
 */
public class DictResultset
{
    /**
     * The index number of the last resultset for this DictResultset. This
     * parametre is set to "-1" if there is no resultset.
     */
    private int cursor;

    /**
     * The list containing all the resultsets for this DictResultset.
     */
    private ArrayList<DictResult> data;
    
    /**
     * Initialize a resultset list
     */
    public DictResultset()
    {
        this.cursor = -1;
        this.data = new ArrayList<DictResult>();
    }
    
    /**
     * Create a new resultset
     */
    public void newResultset()
    {
        this.cursor++;
        this.data.add(new DictResult());
    }
    
    /**
     * Create a new resultset and save the database name
     * @param dbn Database name
     */
    public void newResultset(String dbn)
    {
        this.cursor++;
        this.data.add(new DictResult(dbn));
    }
    
    /**
     * Set the database name for the current resultset
     * @param dbn Database name
     */
    public void setDatabaseName(String dbn)
    {
        this.data.get(this.cursor).setDatabaseName(dbn);
    }
    
    /**
     * Add a result in the current resultset
     * @param res a result line from a dict query
     */
    public void addResult(String res)
    {
        this.data.get(this.cursor).add(res);
    }
    
    /**
     * Return true if there is a resultset
     * @return return true if there is a resultset - false otherwise
     */
    public boolean hasResult()
    {
        return this.data.size() > 0;
    }
    
    /**
     * Return the resultset at the given index 
     * @param index Index of the wished resultset 
     * @return a DictResult - null otherwise
     */
    public DictResult getResultset(int index)
    {
        if (index < this.data.size())
        {
            return (DictResult) this.data.get(index);
        }
        return null;
    }
    
    /**
     * Return the number of resultsets
     * @return the number of resultsets
     */
    public int getNbResults()
    {
        return this.data.size();
    }
}
