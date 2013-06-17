/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.util;

import org.jitsi.util.xml.*;
import org.w3c.dom.*;

/**
 * A class that represents a Conference Information XML document as defined in
 * RFC4575. It wraps around a DOM <tt>Document</tt> providing convenience
 * functions.
 *
 * {@link "http://tools.ietf.org/html/rfc4575"}
 *
 * @author Boris Grozev
 * @author Sebastien Vincent
 */
public class ConferenceInfoDocument
{
    /**
     * The <tt>Logger</tt> used by the <tt>ConferenceInfoDocument</tt> class
     * and its instances for logging output.
     */
    private static final Logger logger
            = Logger.getLogger(ConferenceInfoDocument.class);

    /**
     * The namespace of the conference-info element.
     */
    public static String NAMESPACE = "urn:ietf:params:xml:ns:conference-info";

    /**
     * The name of the "state" attribute.
     */
    public static String STATE_ATTR_NAME = "state";

    /**
     * The name of the "entity" attribute.
     */
    public static String ENTITY_ATTR_NAME = "entity";

    /**
     * The name of the "version" attribute.
     */
    public static String VERSION_ATTR_NAME = "version";

    /**
     * The <tt>Document</tt> object that we wrap around.
     */
    private Document document;

    /**
     * The single <tt>conference-info</tt> element of <tt>document</tt>
     */
    private Element conferenceInfo;

    /**
     * The <tt>conference-description</tt> child element of
     * <tt>conference-info</tt>.
     */
    private Element conferenceDescription;

    /**
     * The <tt>conference-state</tt> child element of <tt>conference-info</tt>.
     */
    private Element conferenceState;

    /**
     * The <tt>conference-state</tt> child element of <tt>conference-state</tt>.
     */
    private Element userCount;

    /**
     * The <tt>users</tt> child element of <tt>conference-info</tt>.
     */
    private Element users;

    /**
     * Creates a new <tt>ConferenceInfoDocument</tt> instance.
     */
    public ConferenceInfoDocument()
            throws Exception
    {
        try
        {
            document = XMLUtils.createDocument();
        }
        catch (Exception e)
        {
            logger.error("Failed to create a new document.", e);
            throw(e);
        }


        conferenceInfo = document.createElementNS(NAMESPACE, "conference-info");
        document.appendChild(conferenceInfo);

        conferenceDescription = document.createElement("conference-description");
        conferenceInfo.appendChild(conferenceDescription);

        conferenceState = document.createElement("conference-state");
        conferenceInfo.appendChild(conferenceState);

        userCount = document.createElement("user-count");
        userCount.setTextContent("0");
        conferenceState.appendChild(userCount);

        users = document.createElement("users");
        conferenceState.appendChild(users);
    }

    /**
     * Creates a new <tt>ConferenceInfoDocument</tt> instance and populates it
     * by parsing the XML in <tt>xml</tt>
     *
     * @param xml the XML string to parse
     */
    public ConferenceInfoDocument(String xml) throws Exception
    {
        try
        {
            document = XMLUtils.createDocument(xml);
        }
        catch (Exception e)
        {
            logger.error("Failed to create a new document.", e);
        }

        //XXX this is not tested yet. do we need to set a namespace?
        try
        {
            conferenceInfo = document.getElementById("conference-info");
            conferenceDescription = XMLUtils.findChild(conferenceInfo, "conference-description");
            conferenceState = XMLUtils.findChild(conferenceInfo, "conference-state");
            userCount = XMLUtils.findChild(conferenceState, "user-count");
            users = XMLUtils.findChild(conferenceInfo, "users");
        }
        catch (Exception e)
        {
            logger.warn("Failed to parse document: " + xml);
            throw(e);
        }
    }

    /**
     * Returns the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @return the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> element.
     */
    public int getVersion()
    {
        String versionString = conferenceInfo.getAttribute(VERSION_ATTR_NAME);
        int version = -1;
        try
        {
            version = Integer.parseInt(versionString);
        }
        catch (Exception e){}

        return version;
    }

    /**
     * Sets the <tt>version</tt> attribute of the <tt>conference-info</tt>
     * element.
     * @param version the value to set the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> element to.
     */
    public void setVersion(int version)
    {
        conferenceInfo.setAttribute(VERSION_ATTR_NAME, Integer.toString(version));
    }

    /**
     * Gets the value of the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @return the value of the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element.
     */
    public State getState()
    {
        return State.parseString(conferenceInfo.getAttribute(STATE_ATTR_NAME));
    }

    /**
     * Sets the value of the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @param state the value to set the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element to.
     */
    public void setState(State state)
    {
        conferenceInfo.setAttribute(STATE_ATTR_NAME, state.toString());
    }

    /**
     * Gets the value of the <tt>sid</tt> attribute of the
     * <tt>conference-info</tt> element.
     * This is not part of RFC4575 and is here because we are temporarily using
     * it in our XMPP implementation.
     * TODO: remote it when we define another way to handle the Jingle SID
     *
     * @return the value of the <tt>sid</tt> attribute of the
     * <tt>conference-info</tt> element.
     *
     */
    public String getSid()
    {
        return conferenceInfo.getAttribute(STATE_ATTR_NAME);
    }

   /**
     * Sets the value of the <tt>sid</tt> attribute of the
     * <tt>conference-info</tt> element.
     * This is not part of RFC4575 and is here because we are temporarily using
     * it in our XMPP implementation.
     * TODO: remote it when we define another way to handle the Jingle SID
     *
     * @param sid the value to set the <tt>sid</tt> attribute of the
     * <tt>conference-info</tt> element to.
     *
     */
    public void setSid(String sid)
    {
        conferenceInfo.setAttribute("sid", sid);
    }

    public void setEntity(String entity)
    {
        conferenceInfo.setAttribute("entity", entity);
    }

    public void setUserCount(int count)
    {
        userCount.setTextContent(Integer.toString(count));
    }

    public int getUserCount()
    {
        int ret = 0;
        try
        {
            ret = Integer.parseInt(userCount.getTextContent());
        }
        catch (Exception e)
        {
            logger.warn("Could not parse user-count field");
        }
        return ret;
    }

    /**
     * Returns the XML representation of the document.
     * @return the XML representation of the document.
     */
    public String toString()
    {
        try
        {
            return XMLUtils.createXml(document);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    public User addNewUser(String entity)
    {
        Element userElement = document.createElement("user");
        User user = new User(userElement);
        user.setEntity(entity);

        users.appendChild(user.user);

        return user;
    }

    /**
     * Represents the possible value for the <tt>state</tt> attribute (see
     * RFC4575)
     */
    public enum State
    {
        /**
         * State <tt>full</tt>
         */
        FULL("full"),

        /**
         * State <tt>partial</tt>
         */
        PARTIAL("partial"),

        /**
         * State <tt>deleted</tt>
         */
        DELETED("deleted");

        /**
         * The name of this <tt>State</tt>
         */
        private String name;

        /**
         * Creates a <tt>State</tt> instance with the specified name.
         * @param name
         */
        private State(String name)
        {
            this.name = name;
        }

        /**
         * Returns the name of this <tt>State</tt>
         * @return the name of this <tt>State</tt>
         */
        @Override
        public String toString()
        {
            return name;
        }

        /**
         * Returns a <tt>State</tt> value corresponding to the specified
         * <tt>name</tt>
         */
        public static State parseString(String name)
        {
            if (FULL.toString().equals(name))
                return FULL;
            else if(PARTIAL.toString().equals(name))
                return PARTIAL;
            else if(DELETED.toString().equals(name))
                return DELETED;
            else
                return null;
        }
    }

    /**
     * Wraps around an <tt>Element</tt> and represents a <tt>user</tt>
     * element (child of the <tt>users</tt> element). See RFC4575.
     */
    public class User
    {
        /**
         * The underlying <tt>Element</tt>.
         */
        Element user;

        /**
         * Creates a new <tt>User</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param user the <tt>Element</tt> to use
         */
        private User(Element user)
        {
            this.user = user;
        }

        /**
         * Sets the <tt>entity</tt> attribute of this <tt>User</tt> to
         * <tt>entity</tt>
         * @param entity the value to set for the <tt>entity</tt> attribute.
         */
        public void setEntity(String entity)
        {
            user.setAttribute(ENTITY_ATTR_NAME, entity);
        }

        /**
         * Sets the <tt>state</tt> attribute of this <tt>User</tt> to
         * <tt>state</tt>
         * @param state the value to set for the <tt>state</tt> attribute.
         */
        public void setState(State state)
        {
            user.setAttribute(STATE_ATTR_NAME, state.toString());
        }

        public Endpoint addNewEndpoint(String entity)
        {
            Element endpointElement = document.createElement("endpoint");
            Endpoint endpoint = new Endpoint(endpointElement);
            endpoint.setEntity(entity);

            user.appendChild(endpoint.endpoint);

            return endpoint;
        }

        /**
         * Adds a <tt>display-text</tt> child element to this <tt>User</tt>
         * @param text the text content to use for the <tt>display-text</tt>
         * element.
         */
        public void setDisplayText(String text)
        {
            Element displayText = document.createElement("display-text");
            displayText.setTextContent(text);
            user.appendChild(displayText);
        }
    }

    /**
     * Wraps around an <tt>Element</tt> and represents an <tt>endpoint</tt>
     * element. See RFC4575.
     */
    public class Endpoint
    {
        /**
         * The underlying <tt>Element</tt>.
         */
        private Element endpoint;

        /**
         * Creates a new <tt>Endpoint</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param endpoint the <tt>Element</tt> to use
         */
        private Endpoint(Element endpoint)
        {
            this.endpoint = endpoint;
        }

        /**
         * Sets the <tt>entity</tt> attribute of this <tt>Endpoint</tt> to
         * <tt>entity</tt>
         * @param entity the value to set for the <tt>entity</tt> attribute.
         */
        public void setEntity(String entity)
        {
            endpoint.setAttribute("entity", entity);
        }

        /**
         * Adds a <tt>status</tt> child element to this <tt>Endpoint</tt>
         * @param status the value to be used for the text content of the
         * added child.
         */
        public void setStatus(EndpointStatusType status)
        {
            Element statusElement = document.createElement("status");
            statusElement.setTextContent(status.toString());
            endpoint.appendChild(statusElement);
        }

        public Media addNewMedia(String id)
        {
            Element mediaElement = document.createElement("media");
            Media media = new Media(mediaElement);
            media.setId(id);

            endpoint.appendChild(media.media);

            return media;
        }
    }

    /**
     * Wraps around an <tt>Element</tt> and represents a <tt>media</tt>
     * element. See RFC4575.
     */
    public class Media
    {
        /**
         * The underlying <tt>Element</tt>.
         */
        Element media;

        /**
         * Creates a new <tt>Media</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param media the <tt>Element</tt> to use
         */
        private Media(Element media)
        {
            this.media = media;
        }

        /**
         * Sets the <tt>id</tt> attribute of this <tt>Media</tt> to
         * <tt>id</tt>
         * @param id the value to set for the <tt>id</tt> attribute.
         */
        public void setId(String id)
        {
            media.setAttribute("id", id);
        }

        /**
         * Adds a <tt>src-id</tt> child element to this <tt>Media</tt>
         * @param srcId the value to be used in the text content of the
         * added child.
         */
        public void setSrcId(String srcId)
        {
            Element element = document.createElement("src-id");
            element.setTextContent(srcId);
            media.appendChild(element);
        }

        /**
         * Adds a <tt>type</tt> child element to this <tt>Media</tt>
         * @param type the value to be used in the text content of the
         * added child.
         */
        public void setType(String type)
        {
            Element element = document.createElement("type");
            element.setTextContent(type);
            media.appendChild(element);
        }

        /**
         * Adds a <tt>status</tt> child element to this <tt>Media</tt>
         * @param status the value to be used in the text content of the
         * added child.
         */
        public void setStatus(String status)
        {
            Element element = document.createElement("status");
            element.setTextContent(status);
            media.appendChild(element);
        }
    }

    /**
     * Endpoint status type.
     *
     * @author Sebastien Vincent
     */
    public enum EndpointStatusType
    {
        /**
         * Pending.
         */
        pending("pending"),

        /**
         * Dialing-out.
         */
        dialing_out ("dialing-out"),

        /**
         * Dialing-in.
         */
        dialing_in("dialing-in"),

        /**
         * Alerting.
         */
        alerting("alerting"),

        /**
         * On-hold.
         */
        on_hold("on-hold"),

        /**
         * Connected.
         */
        connected("connected"),

        /**
         * Muted via focus.
         */
        muted_via_focus("mute-via-focus"),

        /**
         * Disconnecting.
         */
        disconnecting("disconnecting"),

        /**
         * Disconnected.
         */
        disconnected("disconnected");

        /**
         * The name of this type.
         */
        private final String type;

        /**
         * Creates a <tt>EndPointType</tt> instance with the specified name.
         *
         * @param type type name.
         */
        private EndpointStatusType(String type)
        {
            this.type = type;
        }

        /**
         * Returns the type name.
         *
         * @return type name
         */
        @Override
        public String toString()
        {
            return type;
        }

        /**
         * Returns a <tt>EndPointType</tt>.
         *
         * @param typeStr the <tt>String</tt> that we'd like to
         * parse.
         * @return an EndPointType.
         *
         * @throws IllegalArgumentException in case <tt>typeStr</tt> is
         * not a valid <tt>EndPointType</tt>.
         */
        public static EndpointStatusType parseString(String typeStr)
                throws IllegalArgumentException
        {
            for (EndpointStatusType value : values())
                if (value.toString().equals(typeStr))
                    return value;

            throw new IllegalArgumentException(
                    typeStr + " is not a valid reason");
        }
    }
}
