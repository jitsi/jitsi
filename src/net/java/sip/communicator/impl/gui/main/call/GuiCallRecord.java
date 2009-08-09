/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.gui.main.call;

import java.util.*;

import net.java.sip.communicator.service.callhistory.*;

/**
 * The <tt>GuiCallRecord</tt> is meant to be used in the call history to
 * represent a history call record. It wraps a <tt>Call</tt> or a
 * <tt>CallRecord</tt> object.
 *
 * @author Yana Stamcheva
 */
public class GuiCallRecord
{
    private Vector<GuiCallPeerRecord> peers;

    private Date startTime;

    private Date endTime;

    /**
     * Creates an instance of <tt>GuiCallRecord<tt>.
     * @param guiPeerRecords peer records contained in this call
     * record
     * @param startTime call start time
     * @param endTime call end time
     */
    public GuiCallRecord(Vector<GuiCallPeerRecord> guiPeerRecords,
            Date startTime,
            Date endTime)
    {
        this.startTime = startTime;

        this.endTime = endTime;

        peers = guiPeerRecords;
    }

    /**
     * Creates a <tt>GuiCallRecord</tt> from a <tt>CallRecord</tt>. The
     * <tt>GuiCallRecord</tt> will be used in the call history.
     *
     * @param callRecord the <tt>CallPeerRecord</tt>
     */
    public GuiCallRecord(CallRecord callRecord)
    {
        this.startTime = callRecord.getStartTime();

        this.endTime = callRecord.getEndTime();

        this.peers = new Vector<GuiCallPeerRecord>();

        Iterator<CallPeerRecord> records = callRecord.getPeerRecords().iterator();

        while(records.hasNext()) {
            CallPeerRecord record
                = records.next();

            GuiCallPeerRecord newRecord
                = new GuiCallPeerRecord(
                        record, callRecord.getDirection());

            this.peers.add(newRecord);
        }
    }

    public Date getEndTime()
    {
        return endTime;
    }

    public Iterator<GuiCallPeerRecord> getPeers()
    {
        return peers.iterator();
    }

    public int getPeersCount()
    {
        return peers.size();
    }

    public Date getStartTime()
    {
        return startTime;
    }
}
