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
import net.java.sip.communicator.util.*;
import net.kano.joscar.flapcmd.*;
import net.kano.joscar.snac.*;

/**
 * @author Damian Minkov
 */
public class InfoRetreiver
{
    private static final Logger logger =
        Logger.getLogger(InfoRetreiver.class);

    /**
     * A callback to the ICQ provider that created us.
     */
    private ProtocolProviderServiceIcqImpl icqProvider = null;

    // the uin of the account using us,
    // used when sending commands for user info to the server
    private String ownerUin = null;

    // here is kept all the details retreived so far
    private Hashtable retreivedDetails = new Hashtable();

    // used to generate request id when sending commands for retreiving user info
    private static int requestID = 0;

    protected InfoRetreiver
        (ProtocolProviderServiceIcqImpl icqProvider, String ownerUin)
    {
        this.icqProvider = icqProvider;
        this.ownerUin = ownerUin;
    }

    /**
     * returns the user details from the specified class or its descendants
     * the class is one from the
     * net.java.sip.communicator.service.protocol.ServerStoredDetails
     * or implemented one in the operation set for the user info
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator getDetailsAndDescendants(String uin, Class detailClass)
    {
        List details = getContactDetails(uin);
        List result = new LinkedList();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = (Object) iter.next();
            if(detailClass.isInstance(item))
                result.add(item);
        }

        return result.iterator();
    }

    /**
     * returns the user details from the specified class
     * exactly that class not its descendants
     *
     * @param uin String
     * @param detailClass Class
     * @return Iterator
     */
    public Iterator getDetails(String uin, Class detailClass)
    {
        List details = getContactDetails(uin);
        List result = new LinkedList();

        Iterator iter = details.iterator();
        while (iter.hasNext())
        {
            Object item = (Object) iter.next();
            if(detailClass.equals(item.getClass()))
                result.add(item);
        }

        return result.iterator();
    }

    /**
     * request the full info for the given uin
     * waits and return this details
     *
     * @param uin String
     * @return Vector
     */
    protected List getContactDetails(String uin)
    {
        List result = (List)retreivedDetails.get(uin);

        if(result == null)
        {
            //retreive the details
            long toICQUin = Long.parseLong(uin);
            FullInfoRequest infoRequest = new FullInfoRequest(toICQUin);

            int reqID = requestID++;

            UserInfoResponseRetriever responseRetriever =
                new UserInfoResponseRetriever(reqID);

            SnacCommand cmd = new ToIcqCmd(
                Long.parseLong(ownerUin),
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
            retreivedDetails.put(uin, result);
        }

        return result;
    }

    /**
     * waits for the last snac from the full info response sequence
     */
    private class UserInfoResponseRetriever extends SnacRequestAdapter
    {
        int requestID;
        List result = null;

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

    /**
     * wait for response of our ShorInfo Requests
     */
    private class ShortInfoResponseRetriever extends SnacRequestAdapter
    {
        String nickname = null;

        public void handleResponse(SnacResponseEvent e)
        {
            SnacCommand snac = e.getSnacCommand();

            if (snac instanceof MetaShortInfoCmd)
            {
                MetaShortInfoCmd infoSnac = (MetaShortInfoCmd)snac;

                nickname = infoSnac.getNickname();

                synchronized(this){this.notifyAll();}
            }
        }
    }


    /**
     * when detail is changed we remove it from the cache,
     * from retreivedDetails so the next time we want the details
     * we are shure they are get from the server and are actual
     *
     * @param uin String
     */
    protected void detailsChanged(String uin)
    {
        retreivedDetails.remove(uin);
    }

    /**
     * Get the nickname of the specified uin
     * @param uin String the uin
     * @return String the nickname of the uin
     */
    public String getNickName(String uin)
    {
        ShortInfoResponseRetriever responseRetriever =
                new ShortInfoResponseRetriever();

        long longUin = Long.parseLong(uin);
        MetaShortInfoRequest req = new MetaShortInfoRequest(longUin);

        SnacCommand cmd = new ToIcqCmd(
                        Long.parseLong(ownerUin),
                        req.getType(),
                        2,
                        req);

        icqProvider.getAimConnection().getInfoService()
            .sendSnacRequest(cmd, responseRetriever);

        synchronized(responseRetriever)
        {
            try{
                responseRetriever.wait(30000);
            }
            catch (InterruptedException ex)
            {
                //we don't care
            }
        }

        return responseRetriever.nickname;
    }
}
