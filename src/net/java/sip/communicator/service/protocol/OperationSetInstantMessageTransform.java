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
package net.java.sip.communicator.service.protocol;

/**
 * The Instant Message Transform operation set allows, when \
 * supported to insert message transform layers that could change incoming
 * messages before they are delivered to the user and outgoing ones before
 * they reach the protocol stack and get sent. One use case of this operation
 * set is support of upper layer encryption mechanisms like OTR. Other cases may
 * include hyperlink presentation, support for wiki words etc.
 * <p/>
 * Important Notice: As of May 5 2009, this operation set is still a work in
 * progress and may change significantly in the following months. Any work based
 * on this interface is therefore likely to require frequent updates to keep
 * compatibility.
 *
 * @author Emil Ivov
 *
 */
public interface OperationSetInstantMessageTransform
    extends OperationSet
{
    /**
     * Adds a transformation layer to this protocol provider using a default
     * priority value.
     *
     * @param transformLayer the <tt>TransformLayer</tt> that we'd like to add
     * to our protocol provider.
     */
    public void addTransformLayer(TransformLayer transformLayer);

    /**
     * Adds <tt>transformLayer</tt> to the layers currrently used for message
     * transformation in this provider and assigns the specified
     * <tt>priotity</tt> to it.
     *
     * @param priority the priority/order index that we'd like to insert
     * <tt>transportLayer</tt> at.
     * @param transformLayer the layer we are registering
     */
    public void addTransformLayer(int priority, TransformLayer transformLayer);

    /**
     * Removes <tt>transformLayer</tt> from the list of currently registered
     * transform layers so that it won't be notified for further message events.
     *
     * @param transformLayer the layer we are trying to remove.
     */
    public void removeTransformLayer(TransformLayer transformLayer);

    /**
     * Determines whether <tt>layer</tt> is currently registered with this
     * provider.
     *
     * @param layer the layer for which we'd like to know whether it is
     * currently registered with this provider.
     *
     * @return <tt>true</tt> if <tt>layer</tt> is currently registered with this
     * provider and <tt>false</tt> otherwise.
     */
    public boolean containsLayer(TransformLayer layer);
}
