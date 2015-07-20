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
package net.java.sip.communicator.impl.credentialsstorage;

import net.java.sip.communicator.service.credentialsstorage.*;
import net.java.sip.communicator.util.*;

import org.osgi.framework.*;

/**
 * Activator for the {@link CredentialsStorageService}.
 *
 * @author Dmitri Melnikov
 */
public class CredentialsStorageActivator
    implements BundleActivator
{
    /**
     * The {@link BundleContext}.
     */
    private static BundleContext bundleContext;

    /**
     * The <tt>Logger</tt> used by the <tt>CredentialsStorageActivator</tt>
     * class and its instances.
     */
    private static final Logger logger
        = Logger.getLogger(CredentialsStorageActivator.class);

    /**
     * Returns service to show master password input dialog.
     * @return return master password service to display input dialog.
     */
    public static MasterPasswordInputService getMasterPasswordInputService()
    {
        return
            ServiceUtils.getService(
                    bundleContext,
                    MasterPasswordInputService.class);
    }

    /**
     * The {@link CredentialsStorageService} implementation.
     */
    private CredentialsStorageServiceImpl impl;

    /**
     * Starts the credentials storage service
     *
     * @param bundleContext the <tt>BundleContext</tt> as provided from the OSGi
     * framework
     * @throws Exception if anything goes wrong
     */
    public void start(BundleContext bundleContext) throws Exception
    {
        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Service Impl: " + getClass().getName() + " [  STARTED ]");
        }

        CredentialsStorageActivator.bundleContext = bundleContext;

        impl = new CredentialsStorageServiceImpl();
        impl.start(bundleContext);

        bundleContext.registerService(
            CredentialsStorageService.class.getName(), impl, null);

        if (logger.isDebugEnabled())
        {
            logger.debug(
                    "Service Impl: " + getClass().getName() + " [REGISTERED]");
        }
    }

    /**
     * Unregisters the credentials storage service.
     *
     * @param bundleContext BundleContext
     * @throws Exception if anything goes wrong
     */
    public void stop(BundleContext bundleContext) throws Exception
    {
        logger.logEntry();
        impl.stop();
        logger.info(
                "The CredentialsStorageService stop method has been called.");
    }
}
