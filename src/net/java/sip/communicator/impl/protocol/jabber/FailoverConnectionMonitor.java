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
package net.java.sip.communicator.impl.protocol.jabber;

import java.util.*;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.*;

import org.jivesoftware.smack.*;

/**
 * When provider registers, check whether we are connected
 * to the first server in the dns records, if its not - considers in
 * failover state and start a task that will periodically checks
 * to see if server has come back and when this happens, re-register
 * to it. If in the meanwhile, while in failover state the server we are
 * connected became primary (became with highest priority in srv records)
 * we consider ourselves out of failover state and stop all checks considering
 * as we are connected to primary one.
 *
 * @author Damian Minkov
 */
public class FailoverConnectionMonitor
    implements RegistrationStateChangeListener
{
    /**
     * Property to enable/disable failover functionality.
     */
    public static final String REVERSE_FAILOVER_ENABLED_PROP =
        "net.java.sip.communicator.impl.protocol.jabber.REVERSE_FAILOVER_ENABLED";

    /**
     * Property to specify the interval between checks when in failover state.
     * Default is one minute.
     */
    public static final String FAILOVER_CHECK_INTERVAL_PROP =
        "net.java.sip.communicator.impl.protocol.jabber.FAILOVER_CHECK_INTERVAL";

    /**
     * The logger.
     */
    private static final Logger logger =
        Logger.getLogger(FailoverConnectionMonitor.class);

    /**
     * The parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider;

    /**
     * Table of all failover connection monitors for a jabber PP.
     */
    private static Hashtable<ProtocolProviderServiceJabberImpl, FailoverConnectionMonitor>
        providerFilovers = new Hashtable<ProtocolProviderServiceJabberImpl, FailoverConnectionMonitor>();

    /**
     * The timer that periodically will trigger a task to check primary server
     * if we are in failover state.
     */
    private Timer checkTimer;

    /**
     * The task to be triggered to check primary server or whether we are now
     * connected to primary one.
     */
    private CheckPrimaryTask task;

    /**
     * The interval between checks (default is 1 minute).
     */
    private static int CHECK_FOR_PRIMARY_UP_INTERVAL = 60000;

    /**
     * The current address we are connecting to.
     */
    private String currentAddress;

    /**
     * The current service name.
     */
    private String serviceName;

    /**
     * Only we create failover monitor.
     * @param provider the provider which connection we will monitor.
     */
    private FailoverConnectionMonitor(
        ProtocolProviderServiceJabberImpl provider)
    {
        this.parentProvider = provider;

        this.parentProvider.addRegistrationStateChangeListener(this);

        // checks for custom interval check configuration
        CHECK_FOR_PRIMARY_UP_INTERVAL =
            JabberActivator.getConfigurationService().getInt(
                FAILOVER_CHECK_INTERVAL_PROP,
                CHECK_FOR_PRIMARY_UP_INTERVAL);
    }

    /**
     * Returns instance of the monitor for provider, if missing create it.
     * @param provider the povider for the monitor we will return
     * @return the monitor for the provider.
     */
    public static FailoverConnectionMonitor getInstance(
        ProtocolProviderServiceJabberImpl provider)
    {
        FailoverConnectionMonitor fov;

        synchronized(providerFilovers)
        {
            fov = providerFilovers.get(provider);

            if(fov == null)
            {
                fov = new FailoverConnectionMonitor(provider);
                providerFilovers.put(provider, fov);
            }
        }

        return fov;
    }

    /**
     * Sets current values that PP will use for connecting.
     * @param serviceName the service name.
     * @param currentAddress the current address used.
     */
    void setCurrent(String serviceName,
                    String currentAddress)
    {
        this.currentAddress = currentAddress;
        this.serviceName = serviceName;
    }

    /**
     * Whether we are connected to primary server for supplied records.
     * @param recs the srv records.
     * @return whether we are connected to primary server for supplied records.
     */
    private boolean isConnectedToPrimary(SRVRecord[] recs)
    {
        String primaryAddress = getPrimaryServerRecord(recs).getTarget();
        if(primaryAddress != null && primaryAddress.equals(currentAddress))
            return true;
        else
            return false;
    }

    /**
     * Returns the primary server record, the one with highest priority.
     * @param recs the srv records to search.
     * @return the primary server record.
     */
    private SRVRecord getPrimaryServerRecord(SRVRecord[] recs)
    {
        if(recs.length >= 1)
        {
            SRVRecord primary = recs[0];
            for(SRVRecord srv : recs)
            {
                if(srv.getPriority() < primary.getPriority())
                {
                    primary = srv;
                }
            }

            return primary;
        }
        else
            return null;
    }

    /**
     * Get notified for server registration change events.
     * @param evt the event
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if(evt.getNewState() == RegistrationState.REGISTERED)
        {
            if(checkTimer == null)
                checkTimer = new Timer(
                    FailoverConnectionMonitor.class.getName(), true);

            if(task == null)
                task = new CheckPrimaryTask();

            checkTimer.schedule(task,
                CHECK_FOR_PRIMARY_UP_INTERVAL,
                CHECK_FOR_PRIMARY_UP_INTERVAL);
        }
        else if(evt.getNewState() == RegistrationState.UNREGISTERED
             || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED
             || evt.getNewState() == RegistrationState.CONNECTION_FAILED)
        {
            synchronized(providerFilovers)
            {
                providerFilovers.remove(parentProvider);

                parentProvider.removeRegistrationStateChangeListener(this);
            }

            if(checkTimer != null)
            {
                checkTimer.cancel();
                checkTimer = null;
            }

            if(task != null)
            {
                task.cancel();
                task = null;
            }
        }
    }

    /**
     * The task that will make the checks.
     */
    private class CheckPrimaryTask
        extends TimerTask
    {
        @Override
        public void run()
        {
            try
            {
                // make srv lookup to check if dns changed and we are
                // moved to first place, go out of failover state
                SRVRecord[] currentRecords =
                    NetworkUtils.getSRVRecords(
                        "xmpp-client", "tcp", serviceName, false);

                if(isConnectedToPrimary(currentRecords))
                    return;

                // Clear DNS cache.
                NetworkUtils.clearDefaultDNSCache();

                SRVRecord srv = getPrimaryServerRecord(currentRecords);

                ConnectionConfiguration confConn = new ConnectionConfiguration(
                                srv.getTarget(),
                                srv.getPort());

                confConn.setReconnectionAllowed(false);
                XMPPConnection connection = new XMPPConnection(confConn);
                connection.connect();

                connection.disconnect();

                // connection to primary server is successful its back
                // lets reconnect ot it
                try
                {
                    // first disconnect from slave and clean connection.
                    parentProvider.unregister();
                }
                catch(Throwable t)
                {
                    logger.error("Error un-registering before " +
                        "connecting to primary", t);
                }

                // no connect
                parentProvider.register(
                    JabberActivator.getUIService().getDefaultSecurityAuthority(
                        parentProvider));
            }
            catch(Throwable t)
            {
                // primary server is not up
            }
        }
    }
}
