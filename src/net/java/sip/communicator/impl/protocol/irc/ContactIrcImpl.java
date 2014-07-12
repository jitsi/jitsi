package net.java.sip.communicator.impl.protocol.irc;

import net.java.sip.communicator.service.protocol.*;

public class ContactIrcImpl
    extends AbstractContact
{
    private ProtocolProviderServiceIrcImpl provider;
    
    private String id;

    public ContactIrcImpl(ProtocolProviderServiceIrcImpl provider, String id)
    {
        if (provider == null)
            throw new IllegalArgumentException("provider cannot be null");
        this.provider = provider;
        if (id == null)
            throw new IllegalArgumentException("id cannot be null");
        this.id = id;
    }

    @Override
    public String getAddress()
    {
        return this.id;
    }

    @Override
    public String getDisplayName()
    {
        return this.id;
    }

    @Override
    public byte[] getImage()
    {
        return null;
    }

    @Override
    public PresenceStatus getPresenceStatus()
    {
        return IrcStatusEnum.ONLINE;
    }

    @Override
    public ContactGroup getParentContactGroup()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ProtocolProviderService getProtocolProvider()
    {
        return this.provider;
    }

    @Override
    public boolean isPersistent()
    {
        return false;
    }

    @Override
    public boolean isResolved()
    {
        return false;
    }

    @Override
    public String getPersistentData()
    {
        return null;
    }

    @Override
    public String getStatusMessage()
    {
        return null;
    }

}
