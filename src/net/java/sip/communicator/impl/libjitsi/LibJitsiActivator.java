/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
         * benefit of the Jitsi VideoBridge project (which uses Jitsi's libjitsi
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
