/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.service.protocol;

import java.util.*;

import net.java.sip.communicator.service.protocol.ServerStoredDetails.*;
import net.java.sip.communicator.service.protocol.event.*;
import net.java.sip.communicator.util.Logger;

/**
 * Represents a default implementation of {@link OperationSetAvatar} in order to
 * make it easier for implementers to provide complete solutions while focusing
 * on implementation-specific details.
 * 
 * @author Damien Roth
 */
public abstract class AbstractOperationSetAvatar<T extends ProtocolProviderService>
        implements OperationSetAvatar
{
    private static Logger logger = Logger
            .getLogger(AbstractOperationSetAvatar.class);

    /**
     * The maximum avatar width. Zero mean no maximum
     */
    private int maxWidth = 0;

    /**
     * The maximum avatar height. Zero mean no maximum
     */
    private int maxHeight = 0;

    /**
     * The maximum avatar size. Zero mean no maximum
     */
    private int maxSize = 0;

    /**
     * The provider that created us.
     */
    private T parentProvider;

    private OperationSetServerStoredAccountInfo accountInfoOpSet;

    /**
     * The list of listeners interested in <tt>AvatarEvent</tt>s.
     */
    private List<AvatarListener> avatarListeners = new ArrayList<AvatarListener>();

    protected AbstractOperationSetAvatar(T parentProvider,
            OperationSetServerStoredAccountInfo accountInfoOpSet, int maxWidth,
            int maxHeight, int maxSize)
    {
        this.parentProvider = parentProvider;
        this.accountInfoOpSet = accountInfoOpSet;
        
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
        this.maxSize = maxSize;
    }

    @Override
    public int getMaxWidth()
    {
        return this.maxWidth;
    }

    @Override
    public int getMaxHeight()
    {
        return this.maxHeight;
    }

    @Override
    public int getMaxSize()
    {
        return this.maxSize;
    }

    @Override
    public byte[] getAvatar()
    {
        return AccountInfoUtils.getImage(this.accountInfoOpSet);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void setAvatar(byte[] avatar)
    {
        ImageDetail oldDetail = null;
        ImageDetail newDetail = new ImageDetail("avatar", avatar);

        Iterator<GenericDetail> imageDetails = this.accountInfoOpSet
                .getDetails(ServerStoredDetails.ImageDetail.class);
        if (imageDetails.hasNext())
        {
            oldDetail = (ImageDetail) imageDetails.next();
        }

        try
        {
            if (oldDetail == null)
                this.accountInfoOpSet.addDetail(newDetail);
            else
                this.accountInfoOpSet.replaceDetail(oldDetail, newDetail);
        } catch (OperationFailedException e)
        {
            logger.warn("Unable to set new avatar", e);
        }

        fireAvatarChanged(avatar);
    }

    @Override
    public void addAvatarListener(AvatarListener listener)
    {
        synchronized (this.avatarListeners)
        {
            if (!this.avatarListeners.contains(listener))
                this.avatarListeners.add(listener);
        }
    }

    @Override
    public void removeAvatarListener(AvatarListener listener)
    {
        synchronized (this.avatarListeners)
        {
            if (this.avatarListeners.contains(listener))
                this.avatarListeners.remove(listener);
        }
    }

    /**
     * Notifies all registered listeners of the new event.
     * 
     * @param newAvatar
     *            the new avatar
     */
    protected void fireAvatarChanged(byte[] newAvatar)
    {
        AvatarEvent event = new AvatarEvent(this, this.parentProvider,
                newAvatar);

        Collection<AvatarListener> listeners;
        synchronized (this.avatarListeners)
        {
            listeners = new ArrayList<AvatarListener>(this.avatarListeners);
        }

        for (AvatarListener l : listeners)
            l.avatarChanged(event);
    }
}
