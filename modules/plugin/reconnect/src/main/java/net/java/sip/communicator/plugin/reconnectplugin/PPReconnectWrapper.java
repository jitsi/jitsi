/*
 * Copyright @ 2018 - present 8x8, Inc.
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
 *
 */
package net.java.sip.communicator.plugin.reconnectplugin;

import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.event.*;

import java.util.*;

import static net.java.sip.communicator.plugin.reconnectplugin.ReconnectPluginActivator.*;

/**
 * Wraps a provider to listen for registration state changes and act
 * appropriately to make sure we try reconnect it.
 * Keeps a local state to make sure we do not try to process same event twice
 * and schedule undesired reconnects.
 */
public class PPReconnectWrapper
    implements RegistrationStateChangeListener
{
    /**
     * Logger of this class
     */
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PPReconnectWrapper.class);

    /**
     * The provider instance.
     */
    private final ProtocolProviderService provider;

    /**
     * The local state of the wrapper, we sync this and update it to avoid
     * double processing of multiple events in multithreaded environment.
     */
    private RegistrationState localState = null;

    /**
     * The local state mutex.
     */
    private final Object localStateMutex = new Object();

    /**
     * Timer for scheduling the reconnect operation.
     */
    private Timer timer = null;

    /**
     * Whether we had scheduled unregister for this provider.
     */
    private boolean currentlyUnregistering = false;

    /**
     * Non null value indicates that on next UNREGISTERED or CONNECTION_FAILED
     * event we need to schedule a reconnect with delay using the value.
     */
    private Long reconnectOnNextUnregisteredDelay = null;

    /**
     * The current reconnect task.
     */
    private ReconnectTask currentReconnect = null;

    /**
     * Protects currentReconnect field.
     */
    private final Object reconnectTaskMutex = new Object();

    /**
     * Creates new wrapper.
     *
     * @param provider the provider that will be handled by this wrapper.
     */
    public PPReconnectWrapper(ProtocolProviderService provider)
    {
        this.provider = provider;
        this.timer = new Timer("Reconnect timer p:"
            + provider.getAccountID().getAccountUniqueID(), true);

        provider.addRegistrationStateChangeListener(this);
    }

    /**
     * Returns the provider instance.
     * @return the provider instance.
     */
    public ProtocolProviderService getProvider()
    {
        return provider;
    }

    /**
     * Clears any listener or resource used.
     */
    public void clear()
    {
        if(timer != null)
        {
            timer.cancel();
            timer = null;
        }

        this.provider.removeRegistrationStateChangeListener(this);

        // if currently reconnecting cancel
        cancelReconnect();
    }

    /**
     * The method is called by a <code>ProtocolProviderService</code>
     * implementation whenever a change in the registration state of the
     * corresponding provider had occurred.
     *
     * @param evt the event describing the status change.
     */
    @Override
    public void registrationStateChanged(RegistrationStateChangeEvent evt)
    {
        RegistrationState state = evt.getNewState();
        // we don't care about protocol providers that don't support
        // reconnection and we are interested only in few state changes
        if (!(evt.getSource() instanceof ProtocolProviderService)
            || !(state.equals(RegistrationState.REGISTERED)
            || state.equals(RegistrationState.UNREGISTERED)
            || state.equals(RegistrationState.CONNECTION_FAILED)))
            return;

        ProtocolProviderService pp = (ProtocolProviderService) evt.getSource();

        synchronized(localStateMutex)
        {
            // state is already handled, nothing to do
            if (state.equals(localState))
            {
                return;
            }

            this.localState = state;

            // if we are in a process of scheduling a reconnect with unregister
            // before that process
            if (this.reconnectOnNextUnregisteredDelay != null)
            {
                long delay = this.reconnectOnNextUnregisteredDelay;
                this.reconnectOnNextUnregisteredDelay = null;

                if ((state.equals(RegistrationState.UNREGISTERED)
                        || state.equals(RegistrationState.CONNECTION_FAILED))
                    && !evt.isUserRequest()
                    && this.currentlyUnregistering)
                {
                    // this is us who triggered the unregister
                    this.currentlyUnregistering = false;

                    createReconnect(delay);
                    return;
                }
            }

            boolean isServerReturnedErroneousInputEvent =
                state.equals(RegistrationState.CONNECTION_FAILED)
                    && evt.getReasonCode() == RegistrationStateChangeEvent
                        .REASON_SERVER_RETURNED_ERRONEOUS_INPUT;

            try
            {
                if (state.equals(RegistrationState.REGISTERED))
                {
                    addReconnectEnabledProvider(this);

                    // if currently reconnecting cancel
                    cancelReconnect();

                    if (logger.isTraceEnabled())
                    {
                        logger.trace("Got Registered for " + pp);
                        traceCurrentPPState();
                    }
                }
                else if (state.equals(RegistrationState.CONNECTION_FAILED)
                        && !isServerReturnedErroneousInputEvent)
                {
                    if (!hasAtLeastOneSuccessfulConnection(pp))
                    {
                        // ignore providers which haven't registered successfully
                        // till now, they maybe miss-configured
                        notifyConnectionFailed(evt);

                        return;
                    }

                    // if currentlyUnregistering it means
                    // we got conn failed cause the pp has tried to unregister
                    // with sending network packet
                    // but this unregister is scheduled from us so skip
                    if (this.currentlyUnregistering)
                    {
                        this.currentlyUnregistering = false;
                        return;
                    }

                    if (anyConnectedInterfaces())
                    {
                        // network is up but something happen and cannot reconnect
                        // strange lets try again after some time
                        reconnect(currentReconnect != null ?
                            currentReconnect.delay : -1);
                    }

                    if(logger.isTraceEnabled())
                    {
                        logger.trace("Got Connection Failed for " + pp,
                            new Exception("tracing exception"));
                        traceCurrentPPState();
                    }
                }
                else if (state.equals(RegistrationState.UNREGISTERED)
                    || isServerReturnedErroneousInputEvent)
                {
                    this.currentlyUnregistering = false;

                    // Removes from list of auto-reconnect only if the unregister
                    // event is by user request
                    if (evt.isUserRequest() || isServerReturnedErroneousInputEvent)
                    {
                        this.clear();

                        removeReconnectEnabledProviders(this);
                    }

                    // if currently reconnecting cancel
                    cancelReconnect();

                    if(logger.isTraceEnabled())
                    {
                        logger.trace("Got Unregistered for " + pp);
                        traceCurrentPPState();
                    }
                }
            }
            catch(Throwable ex)
            {
                logger.error("Error dispatching protocol registration change", ex);
            }
        }
    }

    /**
     * Cancels currently scheduled reconnect task.
     */
    private void cancelReconnect()
    {
        synchronized(reconnectTaskMutex)
        {
            if (this.currentReconnect != null)
            {
                if(logger.isInfoEnabled())
                    logger.info("Cancel reconnect " + this.currentReconnect);

                this.currentReconnect.cancel();
                this.currentReconnect = null;
            }
        }
    }

    /**
     * Creates and schedules new reconnect task if such is not already created.
     * @param delay the delay to use.
     */
    private void createReconnect(long delay)
    {
        synchronized(reconnectTaskMutex)
        {
            if (this.currentReconnect == null)
            {
                this.currentReconnect
                    = scheduleReconnectIfNeeded(delay, this.provider);
            }
            else
            {
                logger.warn("Reconnect with delay:"
                    + this.currentReconnect.delay + " already scheduled for "
                    + this.provider + " attempted schedule with delay:"
                    + delay);
            }
        }
    }

    /**
     * Schedules a reconnect.
     */
    void reconnect()
    {
        // if currently reconnecting cancel and try again
        cancelReconnect();

        this.reconnect(-1);
    }

    /**
     * Schedules a reconnect.
     * @param previousDelay the delay used in the previous reconnect or -1;
     */
    private void reconnect(long previousDelay)
    {
        long delay;

        if (previousDelay != -1)
        {
            delay = Math.min(previousDelay * 2, MAX_RECONNECT_DELAY*1000);
        }
        else
        {
            delay = (long)(RECONNECT_DELAY_MIN
                + Math.random() * RECONNECT_DELAY_MAX)*1000;
        }

        if (this.provider.getRegistrationState().equals(
                RegistrationState.UNREGISTERING)
            || this.provider.getRegistrationState().equals(
                RegistrationState.UNREGISTERED)
            || this.provider.getRegistrationState().equals(
                RegistrationState.CONNECTION_FAILED))
        {
            createReconnect(delay);
        }
        else
        {
            synchronized(localStateMutex)
            {
                // start registering after the pp has unregistered
                this.reconnectOnNextUnregisteredDelay = delay;

                // as we will reconnect, lets unregister
                unregister();
            }
        }
    }

    /**
     * Unregisters the ProtocolProvider.
     */
    void unregister()
    {
        this.currentlyUnregistering = true;

        // if currently reconnecting cancel
        cancelReconnect();

        try
        {
            this.provider.unregister();
        }
        catch(Throwable t)
        {
            logger.error("Error unregistering pp:" + this.provider, t);
        }
    }

    /**
     * Prints current wrapper state.
     * @return string representing current wrapper state.
     */
    @Override
    public String toString()
    {
        StringBuilder builder = new StringBuilder();
        builder.append(getClass().getSimpleName())
            .append("[provider=").append(provider)
            .append(", currentlyUnregistering=").append(currentlyUnregistering)
            .append(", currentReconnect=").append(currentReconnect)
            .append(", reconnectOnNextUnregisteredDelay=")
                .append(reconnectOnNextUnregisteredDelay)
            .append("]");

        return builder.toString();
    }

    /**
     * Schedules a reconnect if needed (if there is timer and connected
     * interfaces and user request is not null).
     * @param delay The delay to use when creating the reconnect task.
     * @param pp the protocol provider that will be reconnected.
     */
    private ReconnectTask scheduleReconnectIfNeeded(
        long delay, ProtocolProviderService pp)
    {
        final ReconnectTask task = new ReconnectTask();
        task.delay = delay;

        if (timer == null)
        {
            return null;
        }

        if (!anyConnectedInterfaces())
        {
            // There is no network, nothing to do, when
            // network is back it will be scheduled to reconnect.
            // This means we started unregistering while
            // network was going down and meanwhile there
            // were no connected interface, this happens
            // when we have more than one connected
            // interface and we got 2 events for down iface

            return null;
        }

        if(logger.isInfoEnabled())
            logger.info("Reconnect " + pp + " after " + task.delay + " ms.");

        timer.schedule(task, task.delay);

        return task;
    }

    /**
     * The task executed by the timer when time for reconnect comes.
     */
    private class ReconnectTask
        extends TimerTask
    {
        /**
         * The delay with which was this task scheduled.
         */
        long delay;

        /**
         * Reconnects the provider.
         */
        @Override
        public void run()
        {
            try
            {
                if (logger.isInfoEnabled())
                    logger.info("Start reconnecting " + provider);

                provider.register(
                    getUIService().getDefaultSecurityAuthority(provider));
            } catch (OperationFailedException ex)
            {
                logger.error("cannot re-register provider will keep going",
                    ex);
            }
        }

        @Override
        public String toString()
        {
            return ReconnectTask.class.getSimpleName()
                + " [delay=" + delay + ", provider=" + provider + "]";
        }
    }
}
