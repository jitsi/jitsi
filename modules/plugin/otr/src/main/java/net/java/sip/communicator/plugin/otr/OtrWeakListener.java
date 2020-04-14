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
package net.java.sip.communicator.plugin.otr;

import java.lang.ref.*;

import net.java.sip.communicator.plugin.otr.OtrContactManager.OtrContact;
import net.java.sip.communicator.service.protocol.*;

/**
 * Implements a <tt>ScOtrEngineListener</tt> and
 * <tt>ScOtrKeyManagerListener</tt> listener for the purposes of
 * <tt>OtrContactMenu</tt> and <tt>OtrMetaContactButton</tt> which listen to
 * <tt>ScOtrEngine</tt> and <tt>ScOtrKeyManager</tt> while weakly referencing
 * them. Fixes a memory leak of <tt>OtrContactMenu</tt> and
 * <tt>OtrMetaContactButton</tt> instances because these cannot determine when
 * they are to be explicitly disposed.
 *
 * @author Lyubomir Marinov
 */
public class OtrWeakListener
    <T extends ScOtrEngineListener &
               ScOtrKeyManagerListener>
    implements ScOtrEngineListener,
               ScOtrKeyManagerListener
{
    /**
     * The <tt>ScOtrEngine</tt> the <tt>T</tt> associated with
     * this instance is to listen to.
     */
    private final ScOtrEngine engine;

    /**
     * The <tt>ScOtrKeyManager</tt> the <tt>T</tt> associated
     * with this instance is to listen to.
     */
    private final ScOtrKeyManager keyManager;

    /**
     * The <tt>T</tt> which is associated with this instance
     * and which is to listen to {@link #engine} and {@link #keyManager}.
     */
    private final WeakReference<T> listener;

    /**
     * Initializes a new <tt>OtrWeakListener</tt> instance which is to allow
     * a specific <tt>T</tt> to listener to a specific
     * <tt>ScOtrEngine</tt> and a specific <tt>ScOtrKeyManager</tt> without
     * being retained by them forever (because they live forever).
     *
     * @param listener the <tt>T</tt> which is to listen to the
     * specified <tt>engine</tt> and <tt>keyManager</tt>
     * @param engine the <tt>ScOtrEngine</tt> which is to be listened to by
     * the specified <tt>T</tt>
     * @param keyManager the <tt>ScOtrKeyManager</tt> which is to be
     * listened to by the specified <tt>T</tt>
     */
    public OtrWeakListener(
            T listener,
            ScOtrEngine engine, ScOtrKeyManager keyManager)
    {
        if (listener == null)
            throw new NullPointerException("listener");

        this.listener = new WeakReference<T>(listener);
        this.engine = engine;
        this.keyManager = keyManager;

        this.engine.addListener(this);
        this.keyManager.addListener(this);
    }

    /**
     * {@inheritDoc}
     *
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void contactPolicyChanged(Contact contact)
    {
        ScOtrEngineListener l = getListener();

        if (l != null)
            l.contactPolicyChanged(contact);
    }

    /**
     * {@inheritDoc}
     *
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void contactVerificationStatusChanged(OtrContact contact)
    {
        ScOtrKeyManagerListener l = getListener();

        if (l != null)
            l.contactVerificationStatusChanged(contact);
    }

    /**
     * Gets the <tt>T</tt> which is listening to {@link #engine} 
     * and {@link #keyManager}. If the <tt>T</tt> is no longer needed by
     * the application, this instance seizes listening to <tt>engine</tt> and
     * <tt>keyManager</tt> and allows the memory used by this instance to be
     * reclaimed by the Java virtual machine.
     *
     * @return the <tt>T</tt> which is listening to
     * <tt>engine</tt> and <tt>keyManager</tt> if it is still needed by the
     * application; otherwise, <tt>null</tt>
     */
    private T getListener()
    {
        T l = this.listener.get();

        if (l == null)
        {
            engine.removeListener(this);
            keyManager.removeListener(this);
        }

        return l;
    }

    /**
     * {@inheritDoc}
     *
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void globalPolicyChanged()
    {
        ScOtrEngineListener l = getListener();

        if (l != null)
            l.globalPolicyChanged();
    }

    /**
     * {@inheritDoc}
     *
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void sessionStatusChanged(OtrContact contact)
    {
        ScOtrEngineListener l = getListener();

        if (l != null)
            l.sessionStatusChanged(contact);
    }

    /**
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void multipleInstancesDetected(OtrContact contact)
    {
        ScOtrEngineListener l = getListener();

        if (l != null)
            l.multipleInstancesDetected(contact);
    }

    /**
     * Forwards the event/notification to the associated
     * <tt>T</tt> if it is still needed by the application.
     */
    public void outgoingSessionChanged(OtrContact contact)
    {
        ScOtrEngineListener l = getListener();

        if (l != null)
            l.outgoingSessionChanged(contact);
    }
}
