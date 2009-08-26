package net.java.sip.communicator.plugin.otr;

import net.java.sip.communicator.service.protocol.Contact;

public interface ScOtrEngineListener
{
    public abstract void sessionStatusChanged(Contact contact);

    public abstract void contactPolicyChanged(Contact contact);
    
    public abstract void globalPolicyChanged();
}
