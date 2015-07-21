/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Copyright 2003-2005 Arthur van Hoff Rick Blair
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
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.util.*;
import java.util.logging.*;

/**
 * ServiceEvent.
 *
 * @author Werner Randelshofer, Rick Blair
 * @version %I%, %G%
 */
public class ServiceEvent
    extends EventObject
{
    private static Logger logger =
        Logger.getLogger(ServiceEvent.class.toString());
    /**
     * The type name of the service.
     */
    private String type;
    /**
     * The instance name of the service. Or null, if the event was
     * fired to a service type listener.
     */
    private String name;
    /**
     * The service info record, or null if the service could be be resolved.
     * This is also null, if the event was fired to a service type listener.
     */
    private ServiceInfo info;

    /**
     * Creates a new instance.
     *
     * @param source the JmDNS instance which originated the event.
     * @param type   the type name of the service.
     * @param name   the instance name of the service.
     * @param info   the service info record, or null if the
     *      service could be be resolved.
     */
    public ServiceEvent(JmDNS source, String type, String name, ServiceInfo info)
    {
        super(source);
        this.type = type;
        this.name = name;
        this.info = info;

        String SLevel = System.getProperty("jmdns.debug");
        if (SLevel == null) SLevel = "INFO";
        logger.setLevel(Level.parse(SLevel));
    }

    /**
     * Returns the JmDNS instance which originated the event.
     * @return Returns the JmDNS instance which originated the event.
     */
    public JmDNS getDNS()
    {
        return (JmDNS) getSource();
    }

    /**
     * Returns the fully qualified type of the service.
     * @return Returns the fully qualified type of the service.
     */
    public String getType()
    {
        return type;
    }

    /**
     * Returns the instance name of the service.
     * Always returns null, if the event is sent to a service type listener.
     * @return Returns the instance name of the service.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the service info record, or null if the service could not be
     * resolved.
     * Always returns null, if the event is sent to a service type listener.
     * @return Returns the service info record.
     */
    public ServiceInfo getInfo()
    {
        return info;
    }

    @Override
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append("<" + getClass().getName() + "> ");
        buf.append(super.toString());
        buf.append(" name ");
        buf.append(getName());
        buf.append(" type ");
        buf.append(getType());
        buf.append(" info ");
        buf.append(getInfo());
        return buf.toString();
    }

}
