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

import java.util.*;

/**
 * Class keeps up to date list of services that implement given interface.
 * Can be used as a replacement for expensive calls to
 * <tt>getServiceReferences</tt>.
 *
 * @author Pawel Domas
 */
public class ServiceObserver<T>
    implements ServiceListener
{
    /**
     * Service class name.
     */
    private final Class<T> clazz;

    /**
     * The OSGi context.
     */
    private BundleContext context;

    /**
     * Service instances list.
     */
    private final List<T> services = new ArrayList<T>();

    /**
     * Creates new instance of <tt>ServiceObserver</tt> that will observe
     * services of given <tt>className</tt>.
     *
     * @param clazz the <tt>Class</tt> of the service to observe.
     */
    public ServiceObserver(Class<T> clazz)
    {
        this.clazz = clazz;
    }

    /**
     * Returns list of services compatible with service class observed by
     * this instance.
     * @return list of services compatible with service class observed by
     *         this instance.
     */
    public List<T> getServices()
    {
        return Collections.unmodifiableList(services);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void serviceChanged(ServiceEvent serviceEvent)
    {
        Object service = context.getService(serviceEvent.getServiceReference());

        if(!clazz.isInstance(service))
        {
            return;
        }

        int eventType = serviceEvent.getType();

        if(eventType == ServiceEvent.REGISTERED)
        {
            services.add((T) service);
        }
        else if(eventType == ServiceEvent.UNREGISTERING)
        {
            services.remove(service);
        }
    }

    /**
     * This method must be called when OSGi i s starting to initialize the
     * observer.
     * @param ctx the OSGi bundle context.
     */
    public void start(BundleContext ctx)
    {
        this.context = ctx;

        ctx.addServiceListener(this);

        Collection<ServiceReference<T>> refs
            = ServiceUtils.getServiceReferences(ctx, clazz);

        for(ServiceReference<T> ref : refs)
            services.add(ctx.getService(ref));
    }

    /**
     * This method should be called on bundle shutdown to properly release
     * the resources.
     *
     * @param ctx OSGi context
     */
    public void stop(BundleContext ctx)
    {
        ctx.removeServiceListener(this);
        services.clear();
        this.context = null;
    }
}
