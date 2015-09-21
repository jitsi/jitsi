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
package net.java.sip.communicator.slick.callhistory;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 *
 * @author Damian Minkov
 */
public class CallHistoryServiceLick extends TestSuite implements BundleActivator {
    private static Logger logger = Logger.getLogger(CallHistoryServiceLick.class);

    protected static BundleContext bc = null;

    /**
     * Start the History Sevice Implementation Compatibility Kit.
     *
     * @param bundleContext
     *            BundleContext
     * @throws Exception
     */
    public void start(BundleContext bundleContext)
        throws Exception
    {
        CallHistoryServiceLick.bc = bundleContext;

        setName("CallHistoryServiceLick");
        Hashtable<String, String> properties = new Hashtable<String, String>();
        properties.put("service.pid", getName());

        addTest(TestCallHistoryService.suite());
        bundleContext.registerService(getClass().getName(), this, properties);

        logger.debug("Successfully registered " + getClass().getName());
    }

    /**
     * stop
     *
     * @param bundlecontext BundleContext
     * @throws Exception
     */
    public void stop(BundleContext bundlecontext)
        throws Exception
    {
    }
}
