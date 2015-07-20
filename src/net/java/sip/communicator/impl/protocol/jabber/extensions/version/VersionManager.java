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
package net.java.sip.communicator.impl.protocol.jabber.extensions.version;

import net.java.sip.communicator.impl.protocol.jabber.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import org.jitsi.service.version.*;
import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

/**
 * XEP-0092: Software Version.
 * Provider that fills the version IQ using our Version Service.
 *
 * @author Damian Minkov
 */
public class VersionManager
    implements RegistrationStateChangeListener,
               PacketListener
{
    /**
     * Our parent provider.
     */
    private ProtocolProviderServiceJabberImpl parentProvider = null;

    /**
     * Creates and registers the provider.
     * @param parentProvider
     */
    public VersionManager(ProtocolProviderServiceJabberImpl parentProvider)
    {
        this.parentProvider = parentProvider;

        this.parentProvider.addRegistrationStateChangeListener(this);
    }

    /**
     * The method is called by a ProtocolProvider implementation whenever
     * a change in the registration state of the corresponding provider had
     * occurred.
     * @param evt ProviderStatusChangeEvent the event describing the status
     * change.
     */
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        if (evt.getNewState() == RegistrationState.REGISTERED)
        {
            parentProvider.getConnection().removePacketListener(this);
            parentProvider.getConnection().addPacketListener(this,
                new AndFilter(new IQTypeFilter(IQ.Type.GET),
                    new PacketTypeFilter(
                            org.jivesoftware.smackx.packet.Version.class)));
        }
        else if(evt.getNewState() == RegistrationState.UNREGISTERED
            || evt.getNewState() == RegistrationState.CONNECTION_FAILED
            || evt.getNewState() == RegistrationState.AUTHENTICATION_FAILED)
        {
            if(parentProvider.getConnection() != null)
                parentProvider.getConnection().removePacketListener(this);
        }
    }

    /**
     * A packet Listener for incoming Version packets.
     * @param packet an incoming packet
     */
    public void processPacket(Packet packet)
    {
        // send packet
        org.jivesoftware.smackx.packet.Version versionIQ =
            new org.jivesoftware.smackx.packet.Version();
        versionIQ.setType(IQ.Type.RESULT);
        versionIQ.setTo(packet.getFrom());
        versionIQ.setFrom(packet.getTo());
        versionIQ.setPacketID(packet.getPacketID());

        Version ver = JabberActivator.getVersionService().getCurrentVersion();
        String appName = ver.getApplicationName();
        if(!appName.toLowerCase().contains("jitsi"))
            appName += "-Jitsi";

        versionIQ.setName(appName);
        versionIQ.setVersion(ver.toString());
        versionIQ.setOs(System.getProperty("os.name"));

        parentProvider.getConnection().sendPacket(versionIQ);
    }
}
