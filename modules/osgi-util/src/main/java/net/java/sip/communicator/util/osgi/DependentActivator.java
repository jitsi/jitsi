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
package net.java.sip.communicator.util.osgi;

import java.util.*;
import java.util.stream.*;
import org.osgi.framework.*;
import org.osgi.util.tracker.*;
import org.slf4j.*;

/**
 * Bundle activator that will start the bundle when the requested dependent
 * services are available.
 */
public abstract class DependentActivator
    implements BundleActivator, ServiceTrackerCustomizer<Object, Object>
{
    private static final Map<BundleActivator, Set<Class<?>>> openTrackers
        = Collections.synchronizedMap(new HashMap<>());

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Map<Class<?>, ServiceTracker<?, ?>>
        dependentServices = new HashMap<>();

    private final Set<Object> runningServices = new HashSet<>();

    private BundleContext bundleContext;

    protected DependentActivator(Iterable<Class<?>> dependentServices)
    {
        dependentServices.forEach(d -> this.dependentServices.put(d, null));
    }

    protected DependentActivator(Class<?>... dependentServices)
    {
        Set<Class<?>> set = Collections.synchronizedSet(new HashSet<>());
        openTrackers.put(this, set);
        for (Class<?> d : dependentServices)
        {
            this.dependentServices.put(d, null);
            set.add(d);
        }
    }

    /**
     * Starts the bundle.
     *
     * @param bundleContext the currently valid <tt>BundleContext</tt>.
     */
    @Override
    public final void start(BundleContext bundleContext)
    {
        logger.info(
            "Starting, setting up service tracker for {}  dependencies",
                + dependentServices.size());
        this.bundleContext = bundleContext;
        for (Map.Entry<Class<?>, ServiceTracker<?, ?>> ds
            : dependentServices.entrySet())
        {
            ServiceTracker<?, ?> st =
                new ServiceTracker<>(bundleContext, ds.getKey().getName(),
                    this);
            st.open();
            ds.setValue(st);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception
    {
        dependentServices.values().forEach(ServiceTracker::close);
    }

    @Override
    public Object addingService(ServiceReference<Object> reference)
    {
        Object service = bundleContext.getService(reference);
        runningServices.add(service);
        if (runningServices.size() == dependentServices.size())
        {
            openTrackers.remove(this);
            logger.debug("Got service {}, starting now",
                service.getClass().getSimpleName()
            );
            try
            {
                startWithServices(bundleContext);
            }
            catch (Exception e)
            {
                logger.error("Failed to start bundle with services", e);
            }
        }
        else if (logger.isTraceEnabled())
        {
            Set<Class<?>> missingServices =
                new HashSet<>(dependentServices.keySet());
            missingServices.removeIf(s -> runningServices.stream()
                .anyMatch(rs -> s.isAssignableFrom(rs.getClass())));
            openTrackers.put(this, missingServices);
            logger.trace(
                "Got service {}, still waiting for {} services: \n\t{}",
                service.getClass().getSimpleName(),
                missingServices.size(),
                missingServices.stream().map(Class::getSimpleName)
                    .collect(Collectors.joining(",\n\t"))
            );
        }

        if (logger.isTraceEnabled() && !openTrackers.isEmpty())
        {
            synchronized(openTrackers)
            {
                logger.trace("Open service requests:\n\t{}",
                    openTrackers.entrySet().stream()
                        .map(e -> e.getKey().getClass().getSimpleName()
                            + " is waiting for "
                            + e.getValue().size()
                            + " services:\n\t\t"
                            + e.getValue().stream()
                            .map(Class::getSimpleName)
                            .collect(Collectors.joining(",\n\t\t")))
                        .collect(Collectors.joining("\n\t"))
                );
            }
        }

        return service;
    }

    @SuppressWarnings("unchecked")
    protected <T> T getService(Class<T> serviceClass)
    {
        for (Object instance : runningServices)
        {
            if (serviceClass.isAssignableFrom(instance.getClass()))
            {
                return (T) instance;
            }
        }

        throw new IllegalStateException("Service not yet started: " + serviceClass.getName());
    }

    @Override
    public void modifiedService(ServiceReference<Object> reference,
        Object service)
    {
    }

    @Override
    public void removedService(ServiceReference<Object> reference,
        Object service)
    {
    }

    protected abstract void startWithServices(BundleContext bundleContext)
        throws Exception;
}
