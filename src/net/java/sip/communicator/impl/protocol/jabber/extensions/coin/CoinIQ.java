/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions.coin;

import org.jivesoftware.smack.packet.*;

/**
 * Coin (Conference Info) IQ. It is used to inform conference participants
 * about useful information (users, ...).
 *
 * @author Sebastien Vincent
 */
public class CoinIQ
    extends IQ
{
    /**
     * The namespace that coin belongs to.
     */
    public static final String NAMESPACE =
        "urn:ietf:params:xml:ns:conference-info";

    /**
     * The name of the element that contains the coin data.
     */
    public static final String ELEMENT_NAME = "conference-info";

    /**
     * Entity attribute name.
     */
    public static final String ENTITY_ATTR_NAME = "entity";

    /**
     * Version attribute name.
     */
    public static final String VERSION_ATTR_NAME = "version";

    /**
     * Version attribute name.
     */
    public static final String STATE_ATTR_NAME = "state";

    /**
     * Jingle session ID attribute name.
     */
    public static final String SID_ATTR_NAME = "sid";

    /**
     * Version.
     */
    private int version = 0;

    /**
     * Entity name.
     */
    private String entity = null;

    /**
     * State.
     */
    private StateType state = StateType.full;

    /**
     * Jingle session ID.
     */
    private String sid = null;

    /**
     * Get version.
     *
     * @return version
     */
    public int getVersion()
    {
        return version;
    }

    /**
     * Get state.
     *
     * @return state
     */
    public StateType getState()
    {
        return state;
    }

    /**
     * Get session ID.
     *
     * @return session ID
     */
    public String getSID()
    {
        return sid;
    }

    /**
     * Set version.
     *
     * @param version version
     */
    public void setVersion(int version)
    {
        this.version = version;
    }

    /**
     * Set state.
     *
     * @param state state to set
     */
    public void setState(StateType state)
    {
        this.state = state;
    }

    /**
     * Set session ID.
     *
     * @param sid session ID to set
     */
    public void setSID(String sid)
    {
        this.sid = sid;
    }

    /**
     * Get entity.
     *
     * @return entity
     */
    public String getEntity()
    {
        return entity;
    }

    /**
     * Set entity.
     *
     * @param entity entity
     */
    public void setEntity(String entity)
    {
        this.entity = entity;
    }

    /**
     * Returns the XML string of this Jingle IQ's "section" sub-element.
     *
     * Extensions of this class must override this method.
     *
     * @return the child element section of the IQ XML.
     */
    @Override
    public String getChildElementXML()
    {
        StringBuilder bldr = new StringBuilder("<");

        bldr.append(ELEMENT_NAME);
        bldr.append(" xmlns='" + NAMESPACE + "'");
        bldr.append(" state='" + state + "'");
        bldr.append(" entity='" + entity + "'");
        bldr.append(" version='" + version + "'");

        if(sid != null)
        {
            bldr.append(" sid='" + sid + "'");
        }

        if(getExtensions().size() == 0)
        {
            bldr.append("/>");
        }
        else
        {
            bldr.append(">");
            for(PacketExtension pe : getExtensions())
            {
                bldr.append(pe.toXML());
            }
            bldr.append("</").append(ELEMENT_NAME).append(">");
        }

        return bldr.toString();
    }
}
