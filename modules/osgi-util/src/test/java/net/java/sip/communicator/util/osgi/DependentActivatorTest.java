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

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import org.junit.*;
import org.osgi.framework.*;

/**
 * Tests when {@link DependentActivator} calls
 * {@link DependentActivator#startWithServices(BundleContext)}.
 */
public class DependentActivatorTest
{
    private interface ServiceA
    {
    }

    private interface ServiceB
    {
    }

    private static class ServiceAImpl
        implements ServiceA
    {
    }

    private static class ServiceBImpl
        implements ServiceB
    {
    }

    private static class ServiceABImpl
        implements ServiceA, ServiceB
    {
    }

    /**
     * Counts the calls to {@link #startWithServices(BundleContext)}.
     */
    private static class CountingActivator
        extends DependentActivator
    {
        final AtomicInteger starts = new AtomicInteger();

        CountingActivator(Class<?>... dependencies)
        {
            super(dependencies);
        }

        @Override
        protected void startWithServices(BundleContext bundleContext)
        {
            starts.incrementAndGet();
        }
    }

    private ExecutorService executor;

    @Before
    public void setUp()
    {
        executor = Executors.newFixedThreadPool(2);
    }

    @After
    public void tearDown()
    {
        executor.shutdownNow();
    }

    @Test
    public void startsOnceWhenTwoServicesOfOneTypeArriveTogether()
        throws Exception
    {
        for (int i = 0; i < 20000; i++)
        {
            CountingActivator activator
                = new CountingActivator(ServiceA.class);
            activator.start(newBundleContext());

            ServiceReference<Object> first = newServiceReference(
                ServiceAImpl::new);
            ServiceReference<Object> second = newServiceReference(
                ServiceAImpl::new);
            CyclicBarrier barrier = new CyclicBarrier(2);
            Future<?> f1 = executor.submit(() ->
            {
                barrier.await();
                return activator.addingService(first);
            });
            Future<?> f2 = executor.submit(() ->
            {
                barrier.await();
                return activator.addingService(second);
            });
            f1.get(10, TimeUnit.SECONDS);
            f2.get(10, TimeUnit.SECONDS);

            assertEquals("startWithServices calls in iteration " + i,
                1, activator.starts.get());
            activator.stop(null);
        }
    }

    @Test
    public void waitsForEveryTypeWhenOneTypeHasSeveralServices()
        throws Exception
    {
        CountingActivator activator
            = new CountingActivator(ServiceA.class, ServiceB.class);
        activator.start(newBundleContext());

        activator.addingService(newServiceReference(ServiceAImpl::new));
        activator.addingService(newServiceReference(ServiceAImpl::new));
        assertEquals(0, activator.starts.get());

        activator.addingService(newServiceReference(ServiceBImpl::new));
        assertEquals(1, activator.starts.get());

        activator.addingService(newServiceReference(ServiceAImpl::new));
        activator.addingService(newServiceReference(ServiceBImpl::new));
        assertEquals(1, activator.starts.get());
        activator.stop(null);
    }

    @Test
    public void startsWhenOneServiceProvidesEveryType()
        throws Exception
    {
        CountingActivator activator
            = new CountingActivator(ServiceA.class, ServiceB.class);
        activator.start(newBundleContext());

        ServiceABImpl service = new ServiceABImpl();
        activator.addingService(newServiceReference(() -> service));
        assertEquals(1, activator.starts.get());
        assertSame(service, activator.getService(ServiceA.class));
        assertSame(service, activator.getService(ServiceB.class));
        activator.stop(null);
    }

    /**
     * Creates a {@link BundleContext} that returns the service object of
     * {@link #newServiceReference(Supplier)} references and does nothing
     * else.
     */
    private static BundleContext newBundleContext()
    {
        return proxy(BundleContext.class, (method, args) ->
        {
            if ("getService".equals(method.getName()))
            {
                return ((ServiceReferenceHandler) Proxy.getInvocationHandler(
                    args[0])).service.get();
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    private static ServiceReference<Object> newServiceReference(
        Supplier<Object> service)
    {
        return (ServiceReference<Object>) Proxy.newProxyInstance(
            DependentActivatorTest.class.getClassLoader(),
            new Class<?>[] { ServiceReference.class },
            new ServiceReferenceHandler(service));
    }

    private static class ServiceReferenceHandler
        implements InvocationHandler
    {
        final Supplier<Object> service;

        ServiceReferenceHandler(Supplier<Object> service)
        {
            this.service = service;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args)
        {
            switch (method.getName())
            {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "ServiceReference@"
                    + Integer.toHexString(System.identityHashCode(proxy));
            default:
                return defaultValue(method.getReturnType());
            }
        }
    }

    private static <T> T proxy(
        Class<T> type, BiFunction<Method, Object[], Object> handler)
    {
        return type.cast(Proxy.newProxyInstance(
            DependentActivatorTest.class.getClassLoader(),
            new Class<?>[] { type },
            (proxy, method, args) ->
            {
                switch (method.getName())
                {
                case "equals":
                    return proxy == args[0];
                case "hashCode":
                    return System.identityHashCode(proxy);
                case "toString":
                    return type.getSimpleName() + "@"
                        + Integer.toHexString(System.identityHashCode(proxy));
                default:
                    Object result = handler.apply(method, args);
                    return result != null
                        ? result
                        : defaultValue(method.getReturnType());
                }
            }));
    }

    private static Object defaultValue(Class<?> type)
    {
        if (type == boolean.class)
        {
            return false;
        }
        if (type == int.class)
        {
            return 0;
        }
        if (type == long.class)
        {
            return 0L;
        }
        return null;
    }
}
