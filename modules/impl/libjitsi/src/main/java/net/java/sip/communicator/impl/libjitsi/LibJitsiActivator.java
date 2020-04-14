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

import java.util.*;
import lombok.extern.slf4j.*;
import org.jitsi.service.audionotifier.*;
import org.jitsi.service.fileaccess.*;
import org.jitsi.service.libjitsi.*;
import org.jitsi.service.neomedia.*;
import org.osgi.framework.*;

/**
 * Registers services provided by libjitsi as OSGi services.
 */
@Slf4j
public class LibJitsiActivator
    implements BundleActivator, BundleListener
{
    private static final String LIBJITSI_SYM_NAME = "org.jitsi.libjitsi";

    private BundleContext context;

    @Override
    public void start(BundleContext context) throws BundleException
    {
        this.context = context;
        Optional<Bundle> libjitsiBundle = Arrays.stream(context.getBundles())
            .filter(b -> LIBJITSI_SYM_NAME.equals(b.getSymbolicName()))
            .findFirst();
        if (libjitsiBundle.isPresent())
        {
            switch (libjitsiBundle.get().getState())
            {
            case Bundle.INSTALLED:
            case Bundle.RESOLVED:
                libjitsiBundle.get().start();
                //*fallthrough*
            case Bundle.ACTIVE:
                registerServices(libjitsiBundle.get().getBundleContext());
                break;
            default:
                context.addBundleListener(this);
                break;
            }
        }
        else
        {
            context.addBundleListener(this);
        }
    }

    @Override
    public void stop(BundleContext context)
    {
    }

    @Override
    public void bundleChanged(BundleEvent event)
    {
        if (event.getType() == BundleEvent.STARTED
            && LIBJITSI_SYM_NAME.equals(event.getBundle().getSymbolicName()))
        {
            // register the services in libjitsi itself, not in this bundle
            registerServices(event.getBundle().getBundleContext());
            context.removeBundleListener(this);
        }
    }

    private void registerServices(BundleContext context)
    {
        context.registerService(FileAccessService.class,
            LibJitsi.getFileAccessService(), null);
        context.registerService(MediaService.class,
            LibJitsi.getMediaService(), null);
        context.registerService(AudioNotifierService.class,
            LibJitsi.getAudioNotifierService(), null);
    }
}
