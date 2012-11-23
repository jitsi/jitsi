/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
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
    public void start(BundleContext bundleContext)
        throws
        Exception
    {
        setBundleContext(bundleContext);

        if(getDependentService(bundleContext) == null)
        {
            try
            {
                bundleContext.addServiceListener(
                        new DependentServiceListener(bundleContext),
                        '('
                            + Constants.OBJECTCLASS
                            + '='
                            + getDependentServiceClass().getName()
                            + ')');
            }
            catch (InvalidSyntaxException ise)
            {
                /*
                 * Oh, it should not really happen.
                 */
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
            ServiceReference serviceRef = context
                .getServiceReference(getDependentServiceClass().getName());

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
        private BundleContext context;

        DependentServiceListener(BundleContext context)
        {
            this.context = context;
        }

        public void serviceChanged(ServiceEvent serviceEvent)
        {
            Object depService = getDependentService(context);

            if (depService != null)
            {
                /*
                 * This ServiceListener has successfully waited for a
                 * Service implementation to become available so it no
                 * longer need to listen.
                 */
                this.context.removeServiceListener(this);

                start(depService);
            }
        }
    }
}
