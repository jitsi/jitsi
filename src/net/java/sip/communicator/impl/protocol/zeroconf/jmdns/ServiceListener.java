//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
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
