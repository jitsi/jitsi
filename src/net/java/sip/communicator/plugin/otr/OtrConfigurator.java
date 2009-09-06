/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.otr;

import org.bouncycastle.util.encoders.*;

/**
 * 
 * @author George Politis
 *
 */
class Configurator
{
    private String getXmlFriendlyString(String s)
    {
        if (s == null || s.length() < 1)
            return s;

        // XML Tags are not allowed to start with digits,
        // insert a dummy "p" char.
        if (Character.isDigit(s.charAt(0)))
            s = "p" + s;

        char[] cId = new char[s.length()];
        for (int i = 0; i < cId.length; i++)
        {
            char c = s.charAt(i);
            cId[i] = (Character.isLetterOrDigit(c)) ? c : '_';
        }

        return new String(cId);
    }

    private String getID(String id)
    {
        return "net.java.sip.communicator.plugin.otr."
            + getXmlFriendlyString(id);
    }

    public byte[] getPropertyBytes(String id)
    {
        String value =
            (String) OtrActivator.configService.getProperty(this.getID(id));
        if (value == null)
            return null;

        return Base64.decode(value.getBytes());
    }

    public Boolean getPropertyBoolean(String id, boolean defaultValue)
    {
        return OtrActivator.configService.getBoolean(this.getID(id),
            defaultValue);
    }

    public void setProperty(String id, byte[] value)
    {
        String valueToStore = new String(Base64.encode(value));

        OtrActivator.configService
            .setProperty(this.getID(id), valueToStore);
    }

    public void setProperty(String id, boolean value)
    {
        OtrActivator.configService.setProperty(this.getID(id), value);
    }

    public void setProperty(String id, Integer value)
    {
        OtrActivator.configService.setProperty(this.getID(id), value);
    }

    public void removeProperty(String id)
    {
        OtrActivator.configService.removeProperty(this.getID(id));
    }

    public int getPropertyInt(String id, int defaultValue)
    {
        return OtrActivator.configService.getInt(getID(id), defaultValue);
    }
}
