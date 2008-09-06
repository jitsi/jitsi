/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.util.launchutils;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>UriArgManager</tt> implements an utility for handling URIs that have
 * been passed as command line arguments. The class maintains a list of
 * registered delegates that do the actual URI handling. The UriArgDelegator
 * is previewed for use with SIP Communicator argdelegation service. It would
 * therefore record all URIs until the corresponding DelegationPeer has been
 * registered with the UriArgManager.
 *
 * @author Emil Ivov
 */
class UriArgManager
{
    private static final Logger logger = Logger.getLogger(UriArgManager.class);

    /**
    * The delegation peer that we pass arguments to. This peer is going to
    * get set only after Felix starts and all its services have been properly
    * loaded.
    */
    private UriDelegationPeer uriDelegationPeer = null;

    /**
    * We use this list to store arguments that we have been asked to handle
    * before we had a registered delegation peer.
    */
    private List<String> recordedArgs = new LinkedList<String>();

    /**
    * Passes the <tt>uriArg</tt> to the uri delegation peer or, in case
    * no peer is currently registered, stores it and keeps it until one
    * appears.
    *
    * @param uriArg the uri argument that we'd like to delegate to our peer.
    */
    protected void handleUri(String uriArg)
    {
        synchronized(recordedArgs)
        {
            if(uriDelegationPeer == null)
            {
                recordedArgs.add(uriArg);
                return;
            }
        }

        uriDelegationPeer.handleUri(uriArg);
    }

    /**
     * Sets a delegation peer that we can now use to pass arguments to and
     * makes it handle all arguments that have been already registered.
     *
     * @param delegationPeer the delegation peer that we can use to deliver
     * command line URIs to.
     */
    public void setDelegationPeer(UriDelegationPeer delegationPeer)
    {
        synchronized(recordedArgs)
        {
            logger.trace("Someone set a delegationPeer. "
                            +"Will dispatch "+ recordedArgs.size() +" args");
            this.uriDelegationPeer = delegationPeer;

            for (String arg : recordedArgs)
            {
                logger.trace("Dispatching arg: " + arg);
                uriDelegationPeer.handleUri(arg);
            }

            recordedArgs.clear();
        }
    }
}
