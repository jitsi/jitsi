/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.icq;

import java.util.*;

import net.java.sip.communicator.impl.protocol.icq.message.common.*;
import net.java.sip.communicator.impl.protocol.icq.message.usrinfo.*;
import net.java.sip.communicator.service.protocol.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;

/**
 * @author Damian Minkov
 */
public class OperationSetServerStoredContactInfoIcqImpl
    implements OperationSetServerStoredContactInfo
{
    private Hashtable retreivedDetails = new Hashtable();
    private static int requestID = 0;

    /**
     * A callback to the ICQ provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    protected OperationSetServerStoredContactInfoIcqImpl
        (ProtocolProviderServiceIcqImpl icqProvider)
    {
        this.icqProvider = icqProvider;
    }

    public Iterator getDetailsAndDescendants(Contact contat, Class detailClass)
    {
        Vector details = getContactDetails(contat);
        Vector result = new Vector();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = (Object) iter.next();
            if(detailClass.isInstance(item))
                result.add(item);
        }

        return result.iterator();
    }

    public Iterator getDetails(Contact contat, Class detailClass)
    {
        Vector details = getContactDetails(contat);
        Vector result = new Vector();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = (Object) iter.next();
            if(detailClass.equals(item.getClass()))
                result.add(item);
        }

        return result.iterator();
    }

    public Iterator getAllDetailsForContact(Contact contact)
    {
        return getContactDetails(contact).iterator();
    }

    private Vector getContactDetails(Contact contact)
    {
        Vector result = (Vector)retreivedDetails.get(contact);

        if(result == null)
        {
            //retreive the details
            long toICQUin = Long.parseLong(contact.getAddress());
            FullInfoRequest infoRequest = new FullInfoRequest(toICQUin);

            int reqID = requestID++;

            UserInfoResponseRetriever responseRetriever =
                new UserInfoResponseRetriever(reqID);

            SnacCommand cmd = new ToIcqCmd(
                toICQUin,
                infoRequest.getType(),
                reqID,
                infoRequest);

            icqProvider.getAimConnection().getInfoService()
                .sendSnacRequest(cmd, responseRetriever);

            synchronized(responseRetriever)
            {
                try{
                    responseRetriever.wait(10000);
                }
                catch (InterruptedException ex)
                {
                    //we don't care
                }
            }

            result = responseRetriever.result;
            retreivedDetails.put(contact, result);
        }

        return result;
    }

    private class UserInfoResponseRetriever extends SnacRequestAdapter
    {
        int requestID;
        Vector result = null;

        UserInfoResponseRetriever(int requestID)
        {
            this.requestID = requestID;
        }

        public void handleResponse(SnacResponseEvent e)
        {
            SnacCommand snac = e.getSnacCommand();

            if (snac instanceof FullInfoCmd)
            {
                FullInfoCmd infoSnac = (FullInfoCmd)snac;

                if(infoSnac.isLastOfSequences() &&
                   infoSnac.getRequestID() == requestID)
                {
                    //get the result
                    result = infoSnac.getInfo();
                    synchronized(this){this.notifyAll();}
                }
            }
        }
    }
}
