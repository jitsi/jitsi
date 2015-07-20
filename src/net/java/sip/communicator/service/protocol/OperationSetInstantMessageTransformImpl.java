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

import java.util.*;

/**
 * @author George Politis
 */
public class OperationSetInstantMessageTransformImpl
    implements OperationSetInstantMessageTransform
{
    public final Map<Integer, Vector<TransformLayer>> transformLayers
        = new Hashtable<Integer, Vector<TransformLayer>>();

    private static final int defaultPriority = 1;

    public void addTransformLayer(TransformLayer transformLayer)
    {
        this.addTransformLayer(defaultPriority, transformLayer);
    }

    public void addTransformLayer(int priority, TransformLayer transformLayer)
    {
        synchronized (transformLayers)
        {
            if (!transformLayers.containsKey(defaultPriority))
                transformLayers.put(defaultPriority,
                    new Vector<TransformLayer>());

            transformLayers.get(defaultPriority).add(transformLayer);
        }
    }

    public boolean containsLayer(TransformLayer layer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry
                    : transformLayers.entrySet())
            {
                if (entry.getValue().contains(layer))
                    return true;
            }

        }
        return false;
    }

    public void removeTransformLayer(TransformLayer transformLayer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry
                    : transformLayers.entrySet())
            {
                entry.getValue().remove(transformLayer);
            }
        }
    }

}
