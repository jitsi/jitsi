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
package net.java.sip.communicator.impl.contactlist;

import net.java.sip.communicator.service.contactlist.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.util.*;

import org.jitsi.service.fileaccess.*;
import org.jitsi.service.resources.*;
import org.osgi.framework.*;

/**
 * @author Emil Ivov
 */
public class ContactlistActivator
    implements BundleActivator
{
    private static final Logger logger =
        Logger.getLogger(ContactlistActivator.class);

    private MetaContactListServiceImpl mclServiceImpl  = null;

    private static FileAccessService fileAccessService;

    private static AccountManager accountManager;
    
    private static ResourceManagementService resourcesService;

    private static BundleContext bundleContext;

    /**
     * Called when this bundle is started.
     *
     * @param context The execution context of the bundle being started.
     * @throws Exception If
     */
    public void start(BundleContext context) throws Exception
    {
        bundleContext = context;

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [  STARTED ]");

        mclServiceImpl = new MetaContactListServiceImpl();

        //reg the icq account man.
        context.registerService(MetaContactListService.class.getName(),
                mclServiceImpl, null);

        mclServiceImpl.start(context);

        if (logger.isDebugEnabled())
            logger.debug("Service Impl: " + getClass().getName() + " [REGISTERED]");
    }

    /**
     * Called when this bundle is stopped so the Framework can perform the
     * bundle-specific activities necessary to stop the bundle.
     *
     * @param context The execution context of the bundle being stopped.
     * @throws Exception If this method throws an exception, the bundle is
     *   still marked as stopped, and the Framework will remove the bundle's
     *   listeners, unregister all services registered by the bundle, and
     *   release all services used by the bundle.
     */
    public void stop(BundleContext context) throws Exception
    {
        if (logger.isTraceEnabled())
            logger.trace("Stopping the contact list.");
        if(mclServiceImpl != null)
            mclServiceImpl.stop(context);
    }

    /**
     * Returns the <tt>FileAccessService</tt> obtained from the bundle context.
     *
     * @return the <tt>FileAccessService</tt> obtained from the bundle context
     */
    public static FileAccessService getFileAccessService()
    {
        if (fileAccessService == null)
        {
            fileAccessService
                = ServiceUtils.getService(
                        bundleContext,
                        FileAccessService.class);
        }
        return fileAccessService;
    }

    /**
     * Returns the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     *
     * @return the <tt>ResourceManagementService</tt>, through which we will
     * access all resources.
     */
    public static ResourceManagementService getResources()
    {
        if (resourcesService == null)
        {
            resourcesService
                = ServiceUtils.getService(
                        bundleContext,
                        ResourceManagementService.class);
        }
        return resourcesService;
    }
    
    /**
     * Returns the <tt>AccountManager</tt> obtained from the bundle context.
     * @return the <tt>AccountManager</tt> obtained from the bundle context
     */
    public static AccountManager getAccountManager()
    {
        if(accountManager == null)
        {
            accountManager
                = ServiceUtils.getService(bundleContext, AccountManager.class);
        }
        return accountManager;
    }
}
