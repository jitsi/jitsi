/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
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
