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
package net.java.sip.communicator.impl.protocol.jabber.extensions.jibri;

import net.java.sip.communicator.impl.protocol.jabber.extensions.*;

import net.java.sip.communicator.impl.protocol.jabber.extensions.health.HealthStatusPacketExt;
import org.jitsi.util.*;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.*;

import java.util.List;

/**
 * Status extension included in MUC presence by Jibri to indicate its overall status.
 * Overall status is defined by two sub-extensions:
 * {@link HealthStatusPacketExt} - whether or not this Jibri is healthy
 * {@link JibriBusyStatusPacketExt} - whether or not this Jibri is busy
 */
public class JibriStatusPacketExt
    extends AbstractPacketExtension
{
    /**
     * The namespace of this packet extension.
     */
    public static final String NAMESPACE = JibriIq.NAMESPACE;

    /**
     * XML element name of this packet extension.
     */
    public static final String ELEMENT_NAME = "jibri-status";

    /**
     * Creates new instance of <tt>VideoMutedExtension</tt>.
     */
    public JibriStatusPacketExt()
    {
        super(NAMESPACE, ELEMENT_NAME);
    }

    static public void registerExtensionProvider()
    {
        ProviderManager.addExtensionProvider(
                ELEMENT_NAME,
                NAMESPACE,
                new DefaultPacketExtensionProvider<>(JibriStatusPacketExt.class)
        );
    }

    private <T extends ExtensionElement> T getFirstChildExtensionOrNull(Class<T> type)
    {
        List<T> childExts = getChildExtensionsOfType(type);
        if (!childExts.isEmpty())
        {
            return childExts.get(0);
        }
        else
        {
            return null;
        }
    }

    public JibriBusyStatusPacketExt getBusyStatus()
    {
        return getFirstChildExtensionOrNull(JibriBusyStatusPacketExt.class);
    }

    public void setBusyStatus(JibriBusyStatusPacketExt busyStatus)
    {
        setChildExtension(busyStatus);
    }

    public HealthStatusPacketExt getHealthStatus()
    {
        return getFirstChildExtensionOrNull(HealthStatusPacketExt.class);
    }

    public void setHealthStatus(HealthStatusPacketExt healthStatus)
    {
        setChildExtension(healthStatus);
    }

    /**
     * Provides a convenient helper to determine if this Jibri is available or not by looking at
     * both the busy status and the health status.
     * @return true if this Jibri should be considered available for use according to this presence, false
     * otherwise
     */
    public boolean isAvailable()
    {
        return getHealthStatus().getStatus().equals(HealthStatusPacketExt.Health.HEALTHY) &&
                getBusyStatus().getStatus().equals(JibriBusyStatusPacketExt.BusyStatus.IDLE);
    }
}
