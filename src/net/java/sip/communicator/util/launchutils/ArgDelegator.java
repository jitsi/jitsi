/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright @ 2015 Atlassian Pty Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.java.sip.communicator.util.launchutils;

import java.util.*;

import net.java.sip.communicator.util.*;

/**
 * The <tt>ArgDelegator</tt> implements an utility for handling args that have
 * been passed as command line arguments but that need the OSGi environment
 * and SIP Communicator to be fully loaded. The class maintains a list of
 * registered delegates (<tt>ArgDelegationPeer</tt>s) that do the actual arg
 * handling. The <tt>ArgDelegator</tt> is previewed for use with the SIP
 * Communicator argdelegation service. It would therefore record all args
 * until the corresponding <tt>DelegationPeer</tt> has registered here.
 *
 * @author Emil Ivov
 */
class ArgDelegator
{
    /**
     * The <tt>Logger</tt> used by the <tt>ArgDelegator</tt> class and its
     * instances for logging output.
     */
    private static final Logger logger = Logger.getLogger(ArgDelegator.class);

    /**
    * The delegation peer that we pass arguments to. This peer is going to
    * get set only after Felix starts and all its services have been properly
    * loaded.
    */
    private ArgDelegationPeer uriDelegationPeer = null;

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
    public void setDelegationPeer(ArgDelegationPeer delegationPeer)
    {
        synchronized(recordedArgs)
        {
            if (logger.isTraceEnabled())
                logger.trace("Someone set a delegationPeer. "
                            +"Will dispatch "+ recordedArgs.size() +" args");
            this.uriDelegationPeer = delegationPeer;

            for (String arg : recordedArgs)
            {
                if (logger.isTraceEnabled())
                    logger.trace("Dispatching arg: " + arg);
                uriDelegationPeer.handleUri(arg);
            }

            recordedArgs.clear();
        }
    }

    /**
     * Called when the user has tried to launch a second instance of
     * SIP Communicator while a first one was already running. This method
     * simply calls its peer method from the <tt>ArgDelegationPeer</tt> and
     * does nothing if no peer is currently registered.
     */
    public void handleConcurrentInvocationRequest()
    {
        synchronized(recordedArgs)
        {
            if(uriDelegationPeer != null)
                uriDelegationPeer.handleConcurrentInvocationRequest();
        }
    }
}
