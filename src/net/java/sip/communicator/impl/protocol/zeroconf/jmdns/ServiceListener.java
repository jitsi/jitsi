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

/**
 * Listener for service updates.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Werner Randelshofer
 */
public interface ServiceListener extends EventListener
{
    /**
     * A service has been added.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */

    void serviceAdded(ServiceEvent event);

    /**
     * A service has been removed.
     *
     * @param event The ServiceEvent providing the name and fully qualified type
     *              of the service.
     */
    void serviceRemoved(ServiceEvent event);

    /**
     * A service has been resolved. Its details are now available in the
     * ServiceInfo record.
     *
     * @param event The ServiceEvent providing the name, the fully qualified
     *              type of the service, and the service info record,
     *              or null if the service could not be resolved.
     */

    void serviceResolved(ServiceEvent event);
}
