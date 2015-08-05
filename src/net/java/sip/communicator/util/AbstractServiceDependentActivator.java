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

import org.osgi.framework.*;

/**
 * Bundle activator that will start the bundle when certain
 * service is available.
 *
 * @author Damian Minkov
 */
public abstract class AbstractServiceDependentActivator
    implements BundleActivator
{
    /**
     * The service we are dependent on.
     */
    private Object dependentService = null;

    /**
     * Starts the bundle.
     * @param bundleContext the currently valid <tt>BundleContext</tt>.
     */
    @Override
    public void start(BundleContext bundleContext)
        throws Exception
    {
        setBundleContext(bundleContext);

        if(getDependentService(bundleContext) == null)
        {
            try
            {
                bundleContext.addServiceListener(
                        new DependentServiceListener(bundleContext),
                        '(' + Constants.OBJECTCLASS + '='
                            + getDependentServiceClass().getName() + ')');
            }
            catch (InvalidSyntaxException ise)
            {
                // Oh, it should not really happen.
            }
            return;
        }
        else
        {
            start(getDependentService(bundleContext));
        }
    }

    /**
     * The dependent service is available and the bundle will start.
     * @param dependentService the service this activator is waiting.
     */
    public abstract void start(Object dependentService);

    /**
     * The class of the service which this activator is interested in.
     * @return the class name.
     */
    public abstract Class<?> getDependentServiceClass();

    /**
     * Setting context to the activator, as soon as we have one.
     *
     * @param context the context to set.
     */
    public abstract void setBundleContext(BundleContext context);

    /**
     * Obtain the dependent service. Null if missing.
     * @param context the current context to use for obtaining.
     * @return the dependent service object or null.
     */
    private Object getDependentService(BundleContext context)
    {
        if(dependentService == null)
        {
            ServiceReference<?> serviceRef
                = context.getServiceReference(
                        getDependentServiceClass().getName());

            if(serviceRef != null)
                dependentService = context.getService(serviceRef);
        }
        return dependentService;
    }

    /**
     * Implements a <tt>ServiceListener</tt> which waits for an
     * the dependent service implementation to become available, invokes
     * {@link #start(Object)} and un-registers itself.
     */
    private class DependentServiceListener
        implements ServiceListener
    {
        private final BundleContext context;

        DependentServiceListener(BundleContext context)
        {
            this.context = context;
        }

        @Override
        public void serviceChanged(ServiceEvent serviceEvent)
        {
            Object depService = getDependentService(context);

            if (depService != null)
            {
                /*
                 * This ServiceListener has successfully waited for a Service
                 * implementation to become available so it no longer need to
                 * listen.
                 */
                context.removeServiceListener(this);

                start(depService);
            }
        }
    }
}

