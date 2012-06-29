/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.libjitsi;

import org.jitsi.service.libjitsi.*;
import org.osgi.framework.*;

public class LibJitsiActivator
    implements BundleActivator
{
    public void start(BundleContext bundleContext)
        throws Exception
    {
        LibJitsi.start();
    }

    public void stop(BundleContext bundleContext)
        throws Exception
    {
        LibJitsi.stop();
    }
}
