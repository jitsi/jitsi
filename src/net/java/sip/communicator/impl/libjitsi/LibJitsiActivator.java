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
package net.java.sip.communicator.impl.libjitsi;

import java.lang.reflect.*;

import org.jitsi.service.libjitsi.*;
import org.osgi.framework.*;

public class LibJitsiActivator
    implements BundleActivator
{
    public void start(BundleContext bundleContext)
        throws Exception
    {
        /*
         * XXX To start/initialize the libjitsi library, simply call
         * LibJitsi#start(). The following is a temporary workaround for the
         * benefit of the Jitsi Videobridge project (which uses Jitsi's libjitsi
         * bundle and runs on an incomplete OSGi implementation) and not the
         * Jitsi project.
         */
        Method start;

        try
        {
            start = LibJitsi.class.getDeclaredMethod("start", Object.class);
            if (Modifier.isStatic(start.getModifiers()))
            {
                start.setAccessible(true);
                if (!start.isAccessible())
                    start = null;
            }
            else
                start = null;
        }
        catch (NoSuchMethodException nsme)
        {
            start = null;
        }
        catch (SecurityException se)
        {
            start = null;
        }
        if (start == null)
            LibJitsi.start();
        else
            start.invoke(null, bundleContext);
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
        LibJitsi.stop();
    }
}
