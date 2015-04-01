/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.busylampfield;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import java.util.*;

/**
 * The query that will update lines.
 *
 * @author Damian Minkov
 */
public class BLFContactQuery
    extends AbstractContactQuery<BLFContactSourceService>
{
    /**
     * The query string.
     */
    private String queryString;

    /**
     * The query result list.
     */
    private final List<SourceContact> results;

    /**
     * Constructs new query.
     *
     * @param parentService the parent contact source service
     * @param queryString the query string
     * @param contactCount contacts to process, not used currently.
     */
    public BLFContactQuery(
        BLFContactSourceService parentService,
        Collection<OperationSetTelephonyBLF.Line> monitoredLines,
        String queryString, int contactCount)
    {
        super(parentService);

        this.queryString = queryString;
        this.results = new ArrayList<SourceContact>();
        for(OperationSetTelephonyBLF.Line line : monitoredLines)
        {
            addLine(line, false);
        }
    }

    /**
     * Returns the query string.
     *
     * @return the query string
     */
    @Override
    public String getQueryString()
    {
        return queryString;
    }

    /**
     * Returns the list of query results.
     *
     * @return the list of query results
     */
    @Override
    public List<SourceContact> getQueryResults()
    {
        return results;
    }

    /**
     * Starts this <tt>BLFContactQuery</tt>.
     */
    @Override
    public void start()
    {
        for(SourceContact sc : results)
        {
            fireContactReceived(sc, false);
        }
    }

    /**
     * Adds new line to display.
     * @param line to display status.
     * @param fireEvent whether to fire events
     */
    void addLine(OperationSetTelephonyBLF.Line line, boolean fireEvent)
    {
        for(SourceContact sc : results)
        {
            BLFSourceContact blfSC = (BLFSourceContact)sc;

            if(blfSC.getLine().equals(line))
                return;
        }

        BLFSourceContact sc
            = new BLFSourceContact(getContactSource(), line);
        results.add(sc);

        if(fireEvent)
            fireContactReceived(sc, false);
    }

    /**
     * Updates the source contact status.
     * @param line
     * @param status
     */
    void updateLineStatus(OperationSetTelephonyBLF.Line line, int status)
    {
        for(SourceContact sc : results)
        {
            BLFSourceContact blfSC = (BLFSourceContact)sc;
            if(blfSC.getLine().equals(line))
            {
                blfSC.setPresenceStatus(getPresenceStatus(status));
                fireContactChanged(blfSC);
                break;
            }
        }
    }

    /**
     * Maps BLFStatusEvent.type to BLFPresenceStatus.
     * @param status the staus to map.
     * @return the corresponding BLFPresenceStatus.
     */
    private BLFPresenceStatus getPresenceStatus(int status)
    {
        switch(status)
        {
            case BLFStatusEvent.STATUS_BUSY:
                return BLFPresenceStatus.BLF_BUSY;
            case BLFStatusEvent.STATUS_OFFLINE:
                return BLFPresenceStatus.BLF_OFFLINE;
            case BLFStatusEvent.STATUS_RINGING:
                return BLFPresenceStatus.BLF_RINGING;
            case BLFStatusEvent.STATUS_FREE:
                return BLFPresenceStatus.BLF_FREE;
            default:
                return null;
        }
    }
}
