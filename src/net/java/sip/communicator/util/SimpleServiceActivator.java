/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import org.osgi.framework.*;

/**
 * Base class for activators which only register new service in bundle context.
 * Service registration activity is logged on <tt>INFO</tt> level.
 *
 * @param <T> service implementation template type
 *           (for convenient instance access)
 *
 * @author Pawel Domas
 */
public abstract class SimpleServiceActivator<T>
    implements BundleActivator
{
    /**
     * The <tt>Logger</tt> instance used for logging output.
     */
    private final Logger logger;

    /**
     * Class of the service
     */
    private final Class<?> serviceClass;

    /**
     * Service name that will be used in log messages
     */
    private final String serviceName;

    /**
     * Instance of service implementation
     */
    protected T serviceImpl;

    /**
     * Creates new instance of <tt>SimpleServiceActivator</tt>
     *
     * @param serviceClass class of service that will be registered on bundle
     *                     startup
     * @param serviceName  service name that wil be used in log messages
     */
    public SimpleServiceActivator(Class<?> serviceClass, String serviceName)
    {
        this.serviceClass = serviceClass;
        this.serviceName = serviceName;
        logger = Logger.getLogger(this.getClass().getName());
    }

    /**
     * Initialize and start the service.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if initializing and starting this service fails
     */
    public void start(BundleContext bundleContext)
            throws Exception
    {
        //Create the service impl
        serviceImpl = createServiceImpl();

        if (logger.isInfoEnabled())
            logger.info(serviceName+" STARTED");

        bundleContext
                .registerService(
                        serviceClass.getName(),
                        serviceImpl,
                        null);

        if (logger.isInfoEnabled())
            logger.info(serviceName+" REGISTERED");
    }

    /**
     * Stops this bundle.
     *
     * @param bundleContext the <tt>BundleContext</tt>
     * @throws Exception if the stop operation goes wrong
     */
    public void stop(BundleContext bundleContext)
            throws Exception
    {
    }

    /**
     * Called on bundle startup in order to create service implementation
     * instance.
     *
     * @return should return new instance of service implementation.
     */
    protected abstract T createServiceImpl();
}
