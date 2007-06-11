//Copyright 2003-2005 Arthur van Hoff, Rick Blair
//Licensed under Apache License version 2.0
//Original license LGPL
package net.java.sip.communicator.impl.protocol.zeroconf.jmdns;

import java.util.*;

/**
 * Listener for service types.
 *
 * @version %I%, %G%
 * @author  Arthur van Hoff, Werner Randelshofer
 */
public interface ServiceTypeListener extends EventListener
{
    /**
     * A new service type was discovered.
     *
     * @param event The service event providing the fully qualified type of
     *              the service.
     */
    void serviceTypeAdded(ServiceEvent event);
}
