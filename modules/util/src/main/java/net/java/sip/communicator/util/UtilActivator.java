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
package net.java.sip.communicator.util;

import lombok.extern.slf4j.*;
import net.java.sip.communicator.util.osgi.*;
import org.jitsi.service.configuration.*;
import org.osgi.framework.*;

/**
 * The only raison d'etre for this Activator is so that it would set a global
 * exception handler. It doesn't export any services and neither it runs any
 * initialization - all it does is call
 * <tt>Thread.setUncaughtExceptionHandler()</tt>
 *
 * @author Emil Ivov
 */
@Slf4j
public class UtilActivator extends DependentActivator
    implements Thread.UncaughtExceptionHandler
{
    private static ConfigurationService configurationService;

    public UtilActivator()
    {
        super(ConfigurationService.class);
    }

    /**
     * Calls <tt>Thread.setUncaughtExceptionHandler()</tt>
     *
     * @throws Exception If this method throws an exception, this bundle is
     *   marked as stopped and the Framework will remove this bundle's
     *   listeners, unregister all services registered by this bundle, and
     *   release all services used by this bundle.
     */
    @Override
    public void startWithServices(BundleContext bundleContext) throws Exception
    {
        configurationService = getService(ConfigurationService.class);
        logger.trace("Setting default uncaught exception handler");
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * Method invoked when a thread would terminate due to the given uncaught
     * exception. All we do here is simply log the exception using the system
     * logger.
     *
     * <p>Any exception thrown by this method will be ignored by the
     * Java Virtual Machine and thus won't screw our application.
     *
     * @param thread the thread
     * @param exc the exception
     */
    public void uncaughtException(Thread thread, Throwable exc)
    {
        logger.error(
            "An uncaught exception occurred in thread={}, and message was: {}",
            thread, exc.getMessage(), exc);
    }

    /**
     * Returns the <tt>ConfigurationService</tt> currently registered.
     *
     * @return the <tt>ConfigurationService</tt>
     */
    public static ConfigurationService getConfigurationService()
    {
        return configurationService;
    }
}
