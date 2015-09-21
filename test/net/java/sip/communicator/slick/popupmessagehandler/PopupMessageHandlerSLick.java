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
package net.java.sip.communicator.slick.popupmessagehandler;

import java.util.*;

import junit.framework.*;
import net.java.sip.communicator.service.systray.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 *
 * @author Symphorien Wanko
 */
public class PopupMessageHandlerSLick extends TestSuite implements BundleActivator
{
    /** Logger for this class */
    private static Logger logger =
            Logger.getLogger(PopupMessageHandlerSLick.class);

    /** our bundle context */
    protected static BundleContext bundleContext = null;

    /** implements BundleActivator.start() */
    public void start(BundleContext bc) throws Exception
    {
        logger.info("starting popup message test ");

        bundleContext = bc;

        setName("PopupMessageHandlerSLick");

        Hashtable<String, String> properties = new Hashtable<String, String>();

        properties.put("service.pid", getName());

        // we maybe are running on machine without WM and systray
        // (test server machine), skip tests
        if(ServiceUtils.getService(bc, SystrayService.class) != null)
        {
            addTest(TestPopupMessageHandler.suite());
        }

        bundleContext.registerService(getClass().getName(), this, properties);
    }

    /** implements BundleActivator.stop() */
    public void stop(BundleContext bc) throws Exception
    {}

}
