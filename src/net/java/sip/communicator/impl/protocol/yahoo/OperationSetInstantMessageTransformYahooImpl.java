package net.java.sip.communicator.impl.protocol.yahoo;

import java.util.*;
import net.java.sip.communicator.service.protocol.*;

public class OperationSetInstantMessageTransformYahooImpl
    implements OperationSetInstantMessageTransform
{
    public final Map<Integer, Vector<TransformLayer>> transformLayers =
        new Hashtable<Integer, Vector<TransformLayer>>();

    private final int defaultPriority = 1;

//    @Override
    public void addTransformLayer(TransformLayer transformLayer)
    {
        this.addTransformLayer(defaultPriority, transformLayer);
    }

//    @Override
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

//    @Override
    public boolean containsLayer(TransformLayer layer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry : transformLayers
                .entrySet())
            {
                if (entry.getValue().contains(layer))
                    return true;
            }

        }
        return false;
    }

//    @Override
    public void removeTransformLayer(TransformLayer transformLayer)
    {
        synchronized (transformLayers)
        {
            for (Map.Entry<Integer, Vector<TransformLayer>> entry : transformLayers
                .entrySet())
            {
                entry.getValue().remove(transformLayer);
            }

        }
    }
}