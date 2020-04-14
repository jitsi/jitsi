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
package net.java.sip.communicator.service.protocol.media;

import net.java.sip.communicator.util.*;
import org.jitsi.util.xml.*;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import java.io.*;
import java.util.*;

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
    public static final String NAMESPACE
            = "urn:ietf:params:xml:ns:conference-info";

    /**
     * The name of the "conference-info" element.
     */
    public static final String CONFERENCE_INFO_ELEMENT_NAME = "conference-info";

    /**
     * The name of the "conference-description" element.
     */
    public static final String CONFERENCE_DESCRIPTION_ELEMENT_NAME
            = "conference-description";

    /**
     * The name of the "conference-state" element.
     */
    public static final String CONFERENCE_STATE_ELEMENT_NAME
            = "conference-state";

    /**
     * The name of the "state" attribute.
     */
    public static final String STATE_ATTR_NAME = "state";

    /**
     * The name of the "entity" attribute.
     */
    public static final String ENTITY_ATTR_NAME = "entity";

    /**
     * The name of the "version" attribute.
     */
    public static final String VERSION_ATTR_NAME = "version";

    /**
     * The name of the "user" element.
     */
    public static final String USER_ELEMENT_NAME = "user";

    /**
     * The name of the "users" element.
     */
    public static final String USERS_ELEMENT_NAME = "users";

    /**
     * The name of the "endpoint" element.
     */
    public static final String ENDPOINT_ELEMENT_NAME = "endpoint";

    /**
     * The name of the "media" element.
     */
    public static final String MEDIA_ELEMENT_NAME = "media";

    /**
     * The name of the "id" attribute.
     */
    public static final String ID_ATTR_NAME = "id";

    /**
     * The name of the "status" element.
     */
    public static final String STATUS_ELEMENT_NAME = "status";

    /**
     * The name of the "src-id" element.
     */
    public static final String SRC_ID_ELEMENT_NAME = "src-id";

    /**
     * The name of the "type" element.
     */
    public static final String TYPE_ELEMENT_NAME = "type";

    /**
     * The name of the "user-count" element.
     */
    public static final String USER_COUNT_ELEMENT_NAME = "user-count";

    /**
     * The mane of the "display-text" element.
     */
    public static final String DISPLAY_TEXT_ELEMENT_NAME = "display-text";

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
     * A list of <tt>User</tt>s representing the children of <tt>users</tt>
     */
    private final List<User> usersList = new LinkedList<User>();

    /**
     * Creates a new <tt>ConferenceInfoDocument</tt> instance.
     *
     * @throws XMLException if a document failed to be created.
     */
    public ConferenceInfoDocument()
            throws XMLException
    {
        try
        {
            document = XMLUtils.createDocument();
        }
        catch (Exception e)
        {
            logger.error("Failed to create a new document.", e);
            throw(new XMLException(e.getMessage()));
        }


        conferenceInfo = document
                .createElementNS(NAMESPACE, CONFERENCE_INFO_ELEMENT_NAME);
        document.appendChild(conferenceInfo);

        setVersion(1);

        conferenceDescription
                = document.createElement(CONFERENCE_DESCRIPTION_ELEMENT_NAME);
        conferenceInfo.appendChild(conferenceDescription);

        conferenceState = document.createElement(CONFERENCE_STATE_ELEMENT_NAME);
        conferenceInfo.appendChild(conferenceState);
        setUserCount(0);

        users = document.createElement(USERS_ELEMENT_NAME);
        conferenceInfo.appendChild(users);
    }

    /**
     * Creates a new <tt>ConferenceInfoDocument</tt> instance and populates it
     * by parsing the XML in <tt>xml</tt>
     *
     * @param xml the XML string to parse
     *
     * @throws XMLException If parsing failed
     */
    public ConferenceInfoDocument(String xml)
            throws XMLException
    {
        byte[] bytes;

        try
        {
            bytes = xml.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException uee)
        {
            logger.warn(
                    "Failed to gets bytes from String for the UTF-8 charset",
                    uee);
            bytes = xml.getBytes();
        }

        try
        {
            document
                = XMLUtils.newDocumentBuilderFactory().newDocumentBuilder()
                        .parse(new ByteArrayInputStream(bytes));
        }
        catch (Exception e)
        {
            throw new XMLException(e.getMessage());
        }

        conferenceInfo = document.getDocumentElement();
        if (conferenceInfo == null)
        {
            throw new XMLException("Could not parse conference-info document,"
                    + " conference-info element not found");
        }

        conferenceDescription = XMLUtils
                .findChild(conferenceInfo, CONFERENCE_DESCRIPTION_ELEMENT_NAME);
        //conference-description is mandatory
        if (conferenceDescription == null)
        {
            throw new XMLException("Could not parse conference-info document,"
                    + " conference-description element not found");
        }

        conferenceState
            = XMLUtils.findChild(conferenceInfo, CONFERENCE_STATE_ELEMENT_NAME);
        if (conferenceState != null)
            userCount = XMLUtils
                    .findChild(conferenceState, USER_COUNT_ELEMENT_NAME);

        users = XMLUtils.findChild(conferenceInfo, USERS_ELEMENT_NAME);
        if (users == null)
        {
            throw new XMLException("Could not parse conference-info document,"
                    + " 'users' element not found");
        }
        NodeList usersNodeList = users.getElementsByTagName(USER_ELEMENT_NAME);
        for(int i=0; i<usersNodeList.getLength(); i++)
        {
            User user = new User((Element)usersNodeList.item(i));
            usersList.add(user);
        }
    }

    /**
     * Creates a new <tt>ConferenceInfoDocument</tt> instance that represents
     * a copy of <tt>confInfo</tt>
     * @param confInfo the document to copy
     * @throws XMLException if a document failed to be created.
     */
    public ConferenceInfoDocument(ConferenceInfoDocument confInfo)
            throws XMLException
    {
        this();

        //temporary
        String sid = confInfo.getSid();
        if(sid != null && !sid.equals(""))
            setSid(sid);

        setEntity(confInfo.getEntity());
        setState(confInfo.getState());
        setUserCount(confInfo.getUserCount());
        setUsersState(confInfo.getUsersState());
        setVersion(confInfo.getVersion());
        for (User user : confInfo.getUsers())
            addUser(user);
    }

    /**
     * Returns the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> element, or -1 if there is no <tt>version</tt>
     * attribute or if it's value couldn't be parsed as an integer.
     * @return the value of the <tt>version</tt> attribute of the
     * <tt>conference-info</tt> element, or -1 if there is no <tt>version</tt>
     * attribute or if it's value couldn't be parsed as an integer.
     */
    public int getVersion()
    {
        String versionString = conferenceInfo.getAttribute(VERSION_ATTR_NAME);
        if (versionString == null)
            return -1;
        int version = -1;
        try
        {
            version = Integer.parseInt(versionString);
        }
        catch (NumberFormatException e)
        {
            if (logger.isInfoEnabled())
                logger.info("Failed to parse version string: " + versionString);
        }

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
        return getState(conferenceInfo);
    }

    /**
     * Returns the value of the <tt>state</tt> attribute of the <tt>users</tt>
     * child of the <tt>conference-info</tt> element.
     *
     * @return the value of the <tt>state</tt> attribute of the <tt>users</tt>
     * child of the <tt>conference-info</tt> element.
     */
    public State getUsersState()
    {
        return getState(users);
    }

    /**
     * Sets the <tt>state</tt> attribute of the <tt>users</tt> chuld of the
     * <tt>conference-info</tt> element.
     *
     * @param state the state to set
     */
    public void setUsersState(State state)
    {
        setState(users, state);
    }

    /**
     * Sets the value of the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @param state the value to set the <tt>state</tt> attribute of the
     * <tt>conference-info</tt> element to.
     */
    public void setState(State state)
    {
        setState(conferenceInfo, state);
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
     */
    public void setSid(String sid)
    {
        if (sid == null || sid.equals(""))
            conferenceInfo.removeAttribute("sid");
        else
            conferenceInfo.setAttribute("sid", sid);
    }

    /**
     * Gets the value of the <tt>sid</tt> attribute of the
     * <tt>conference-info</tt> element.
     * This is not part of RFC4575 and is here because we are temporarily using
     * it in our XMPP implementation.
     * TODO: remote it when we define another way to handle the Jingle SID
     */
    public String getSid()
    {
        return conferenceInfo.getAttribute("sid");
    }

    /**
     * Sets the value of the <tt>entity</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @param entity the value to set the <tt>entity</tt> attribute of the
     * <tt>conference-info</tt> document to.
     */
    public void setEntity(String entity)
    {
        if (entity == null || entity.equals(""))
            conferenceInfo.removeAttribute(ENTITY_ATTR_NAME);
        else
            conferenceInfo.setAttribute(ENTITY_ATTR_NAME, entity);
    }

    /**
     * Gets the value of the <tt>entity</tt> attribute of the
     * <tt>conference-info</tt> element.
     * @return The value of the <tt>entity</tt> attribute of the
     * <tt>conference-info</tt> element.
     */
    public String getEntity()
    {
        return conferenceInfo.getAttribute(ENTITY_ATTR_NAME);
    }

    /**
     * Sets the content of the <tt>user-count</tt> child element of the
     * <tt>conference-state</tt> child element of <tt>conference-info</tt>
     * @param count the value to set the content of <tt>user-count</tt> to
     */
    public void setUserCount(int count)
    {
        // conference-state and its user-count child aren't mandatory
        if (userCount != null)
        {
            userCount.setTextContent(Integer.toString(count));
        }
        else
        {
            if (conferenceState == null)
            {
                conferenceState
                        = document.createElement(CONFERENCE_STATE_ELEMENT_NAME);
                conferenceInfo.appendChild(conferenceState);
            }

            userCount = document.createElement(USER_COUNT_ELEMENT_NAME);
            userCount.setTextContent(Integer.toString(count));
            conferenceState.appendChild(userCount);
        }
    }

    /**
     * Returns the content of the <tt>user-count</tt> child of the
     * <tt>conference-state</tt> child of <tt>conference-info</tt>, parsed as
     * an integer, if they exist. Returns -1 if either there isn't a
     * <tt>conference-state</tt> element, it doesn't have a <tt>user-count</tt>
     * child, or parsing as integer failed.
     *
     * @return the content of the <tt>user-count</tt> child of the
     * <tt>conference-state</tt> child of <tt>conference-info</tt> element.
     */
    public int getUserCount()
    {
        int ret = -1;
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
     * Returns the XML representation of the <tt>conference-info</tt> tree,
     * or <tt>null</tt> if an error occurs while trying to get it.
     *
     * @return the XML representation of the <tt>conference-info</tt> tree,
     * or <tt>null</tt> if an error occurs while trying to get it.
     */
    public String toXml()
    {
        try
        {
            Transformer transformer
                    = TransformerFactory.newInstance().newTransformer();
            StringWriter buffer = new StringWriter();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,
                    "yes");
            transformer.transform(new DOMSource(conferenceInfo),
                    new StreamResult(buffer));
            return buffer.toString();
        }
        catch (Exception e)
        {
            return null;
        }
    }

    /**
     * Returns the XML representation of the document (from the
     * <tt>conference-info</tt> element down), or an error string in case the
     * XML cannot be generated for some reason.
     * @return the XML representation of the document or an error string.
     */
    @Override
    public String toString()
    {
        String s = toXml();
        return s == null
                ? "Could not get conference-info XML"
                : s;
    }

    /**
     * Returns the list of <tt>User</tt> that represents the <tt>user</tt>
     * children of the <tt>users</tt> child element of <tt>conference-info</tt>
     * @return the list of <tt>User</tt> that represents the <tt>user</tt>
     * children of the <tt>users</tt> child element of <tt>conference-info</tt>
     */
    public List<User> getUsers()
    {
        return usersList;
    }

    /**
     * Searches this document's <tt>User</tt>s and returns the one with
     * <tt>entity</tt> attribute <tt>entity</tt>, or <tt>null</tt> if one
     * wasn't found.
     * @param entity The value of the <tt>entity</tt> attribute to search for.
     * @return the <tt>User</tt> of this document with <tt>entity</tt>
     * attribute <tt>entity</tt>, or <tt>null</tt> if one wasn't found.
     * */
    public User getUser(String entity)
    {
        if (entity == null)
            return null;
        for(User u : usersList)
        {
            if (entity.equals(u.getEntity()))
                return u;
        }
        return null;
    }

    /**
     * Creates a new <tt>User</tt> instance, adds it to the document and
     * returns it.
     * @param entity The value to use for the <tt>entity</tt> attribute of the
     * new <tt>User</tt>.
     * @return the newly created <tt>User</tt> instance.
     */
    public User addNewUser(String entity)
    {
        Element userElement = document.createElement(USER_ELEMENT_NAME);
        User user = new User(userElement);
        user.setEntity(entity);

        users.appendChild(userElement);
        usersList.add(user);

        return user;
    }

    /**
     * Adds a copy of <tt>user</tt> to this <tt>ConferenceInfoDocument</tt>
     * @param user the <tt>User</tt> to add a copy of
     */
    public void addUser(User user)
    {
        User newUser = addNewUser(user.getEntity());
        newUser.setDisplayText(user.getDisplayText());
        newUser.setState(user.getState());
        for (Endpoint endpoint : user.getEndpoints())
            newUser.addEndpoint(endpoint);
    }

    /**
     * Removes a specific <tt>User</tt> (the one with entity <tt>entity</tt>)
     * from the document.
     * @param entity the entity of the <tt>User</tt> to remove.
     */
    public void removeUser(String entity)
    {
        User user = getUser(entity);
        if (user != null)
        {
            usersList.remove(user);
            users.removeChild(user.userElement);
        }
    }

    /**
     * Returns the <tt>Document</tt> that this instance wraps around.
     * @return the <tt>Document</tt> that this instance wraps around.
     */
    public Document getDocument()
    {
        return document;
    }

    /**
     * Returns the <tt>State</tt> corresponding to the <tt>state</tt> attribute
     * of an <tt>Element</tt>. Default to <tt>State.FULL</tt> which is the
     * RFC4575 default.
     * @param element the <tt>Element</tt>
     * @return the <tt>State</tt> corresponding to the <tt>state</tt> attribute
     * of an <tt>Element</tt>.
     */
    private State getState(Element element)
    {
        State state = State.parseString(element.getAttribute(STATE_ATTR_NAME));
        return state == null
                ? State.FULL
                : state;
    }

    /**
     * Sets the "state" attribute of <tt>element</tt> to <tt>state</tt>.
     * If <tt>state</tt> is <tt>State.FULL</tt> removes the "state" attribute,
     * because this is the default value.
     * @param element The <tt>Element</tt> for which to set the "state"
     * attribute of.
     * @param state the <tt>State</tt> which to set.
     */
    private void setState(Element element, State state)
    {
        if (element != null)
        {
            if (state == State.FULL || state == null)
                element.removeAttribute(STATE_ATTR_NAME);
            else
                element.setAttribute(STATE_ATTR_NAME, state.toString());
        }
    }

    /**
     * Sets the <tt>status</tt> child element of <tt>element</tt>. If
     * <tt>statusString</tt> is <tt>null</tt>, the child element is removed
     * if present.
     * @param element the <tt>Element</tt> for which to set the <tt>status</tt>
     * child element.
     * @param statusString the <tt>String</tt> to use for the text content of
     * the <tt>status</tt> element
     */
    private void setStatus(Element element, String statusString)
    {
        Element statusElement
                = XMLUtils.findChild(element, STATUS_ELEMENT_NAME);
        if (statusString == null || statusString.equals(""))
        {
            if(statusElement != null)
                element.removeChild(statusElement);
        }
        else
        {
            if (statusElement == null)
            {
                statusElement = document.createElement(STATUS_ELEMENT_NAME);
                element.appendChild(statusElement);
            }
            statusElement.setTextContent(statusString);
        }
    }

    /**
     * Represents the possible values for the <tt>state</tt> attribute (see
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
         * @return a <tt>State</tt> value corresponding to the specified
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
        private Element userElement;

        /**
         * The list of <tt>Endpoint</tt>s representing the <tt>endpoint</tt>
         * children of this <tt>User</tt>'s element.
         */
        private List<Endpoint> endpointsList = new LinkedList<Endpoint>();

        /**
         * Creates a new <tt>User</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param user the <tt>Element</tt> to use
         */
        private User(Element user)
        {
            this.userElement = user;
            NodeList endpointsNodeList
                    = user.getElementsByTagName(ENDPOINT_ELEMENT_NAME);
            for (int i=0; i<endpointsNodeList.getLength(); i++)
            {
                Endpoint endpoint
                        = new Endpoint((Element)endpointsNodeList.item(i));
                endpointsList.add(endpoint);
            }
        }

        /**
         * Sets the <tt>entity</tt> attribute of this <tt>User</tt>'s element
         * to <tt>entity</tt>
         * @param entity the value to set for the <tt>entity</tt> attribute.
         */
        public void setEntity(String entity)
        {
            if (entity == null || entity.equals(""))
                userElement.removeAttribute(ENTITY_ATTR_NAME);
            else
                userElement.setAttribute(ENTITY_ATTR_NAME, entity);
        }

        /**
         * Returns the value of the <tt>entity</tt> attribute of this
         * <tt>User</tt>'s element.
         * @return the value of the <tt>entity</tt> attribute of this
         * <tt>User</tt>'s element.
         */
        public String getEntity()
        {
            return userElement.getAttribute(ENTITY_ATTR_NAME);
        }

        /**
         * Sets the <tt>state</tt> attribute of this <tt>User</tt>'s element to
         * <tt>state</tt>
         * @param state the value to use for the <tt>state</tt> attribute.
         */
        public void setState(State state)
        {
            ConferenceInfoDocument.this.setState(userElement, state);
        }

        /**
         * Returns the value of the <tt>state</tt> attribute of this
         * <tt>User</tt>'s element
         * @return the value of the <tt>state</tt> attribute of this
         * <tt>User</tt>'s element
         */
        public State getState()
        {
            return ConferenceInfoDocument.this.getState(userElement);
        }

        /**
         * Sets the <tt>display-text</tt> child element to this <tt>User</tt>'s
         * element.
         * @param text the text content to use for the <tt>display-text</tt>
         * element.
         */
        public void setDisplayText(String text)
        {
            Element displayText
                    = XMLUtils.findChild(userElement, DISPLAY_TEXT_ELEMENT_NAME);
            if (text == null || text.equals(""))
            {
                if (displayText != null)
                    userElement.removeChild(displayText);
            }
            else
            {
                if (displayText == null)
                {
                    displayText
                            = document.createElement(DISPLAY_TEXT_ELEMENT_NAME);
                    userElement.appendChild(displayText);
                }
                displayText.setTextContent(text);
            }
        }

        /**
         * Returns the text content of the <tt>display-text</tt> child element
         * of this <tt>User</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         * @return the text content of the <tt>display-text</tt> child element
         * of this <tt>User</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         */
        public String getDisplayText()
        {
            Element displayText
                    = XMLUtils.findChild(userElement, DISPLAY_TEXT_ELEMENT_NAME);
            if (displayText != null)
                return displayText.getTextContent();

            return null;
        }

        /**
         * Returns the list of <tt>Endpoint</tt>s which represent the
         * <tt>endpoint</tt> children of this <tt>User</tt>'s element.
         * @return the list of <tt>Endpoint</tt>s which represent the
         * <tt>endpoint</tt> children of this <tt>User</tt>'s element.
         */
        public List<Endpoint> getEndpoints()
        {
            return endpointsList;
        }

        /**
         * Searches this <tt>User</tt>'s associated <tt>Endpoint</tt>s
         * and returns the one with <tt>entity</tt> attribute <tt>entity</tt>,
         * or <tt>null</tt> if one wasn't found.
         * @param entity The value of the <tt>entity</tt> attribute to search
         * for.
         * @return The <tt>Endpoint</tt> with <tt>entity</tt> attribute
         * <tt>entity</tt>, or <tt>null</tt> if one wasn't found.
         */
        public Endpoint getEndpoint(String entity)
        {
            if (entity == null)
                return null;
            for (Endpoint e : endpointsList)
            {
                if (entity.equals(e.getEntity()))
                    return e;
            }
            return null;
        }

        /**
         * Creates a new <tt>Endpoint</tt> instance, adds it to this
         * <tt>User</tt> and returns it.
         * @param entity The value to use for the <tt>entity</tt> attribute of
         * the new <tt>Endpoint</tt>.
         * @return the newly created <tt>Endpoint</tt> instance.
         */
        public Endpoint addNewEndpoint(String entity)
        {
            Element endpointElement
                    = document.createElement(ENDPOINT_ELEMENT_NAME);
            Endpoint endpoint = new Endpoint(endpointElement);
            endpoint.setEntity(entity);

            userElement.appendChild(endpointElement);
            endpointsList.add(endpoint);

            return endpoint;
        }

        /**
         * Adds a copy of <tt>endpoint</tt> to this <tt>User</tt>
         * @param endpoint the <tt>Endpoint</tt> to add a copy of
         */
        public void addEndpoint(Endpoint endpoint)
        {
            Endpoint newEndpoint = addNewEndpoint(endpoint.getEntity());
            newEndpoint.setStatus(endpoint.getStatus());
            newEndpoint.setState(endpoint.getState());
            for (Media media : endpoint.getMedias())
                newEndpoint.addMedia(media);
        }

        /**
         * Removes a specific <tt>Endpoint</tt> (the one with entity
         * <tt>entity</tt>) from this <tt>User</tt>.
         * @param entity the <tt>entity</tt> of the <tt>Endpoint</tt> to remove
         */
        public void removeEndpoint(String entity)
        {
            Endpoint endpoint = getEndpoint(entity);
            if (endpoint != null)
            {
                endpointsList.remove(endpoint);
                userElement.removeChild(endpoint.endpointElement);
            }
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
        private Element endpointElement;

        /**
         * The list of <tt>Media</tt>s representing the <tt>media</tt>
         * children elements of this <tt>Endpoint</tt>'s element.
         */
        private List<Media> mediasList = new LinkedList<Media>();

        /**
         * Creates a new <tt>Endpoint</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param endpoint the <tt>Element</tt> to use
         */
        private Endpoint(Element endpoint)
        {
            this.endpointElement = endpoint;
            NodeList mediaNodeList
                    = endpoint.getElementsByTagName(MEDIA_ELEMENT_NAME);
            for (int i=0; i<mediaNodeList.getLength(); i++)
            {
                Media media = new Media((Element)mediaNodeList.item(i));
                mediasList.add(media);
            }
        }

        /**
         * Sets the <tt>entity</tt> attribute of this <tt>Endpoint</tt>'s
         * element to <tt>entity</tt>
         * @param entity the value to set for the <tt>entity</tt> attribute.
         */
        public void setEntity(String entity)
        {
            if (entity == null || entity.equals(""))
                endpointElement.removeAttribute(ENTITY_ATTR_NAME);
            else
                endpointElement.setAttribute(ENTITY_ATTR_NAME, entity);
        }

        /**
         * Returns the <tt>entity</tt> attribute of this <tt>Endpoint</tt>'s
         * element.
         * @return the <tt>entity</tt> attribute of this <tt>Endpoint</tt>'s
         * element.
         */
        public String getEntity()
        {
            return endpointElement.getAttribute(ENTITY_ATTR_NAME);
        }

        /**
         * Sets the <tt>state</tt> attribute of this <tt>User</tt>'s element to
         * <tt>state</tt>
         * @param state the value to use for the <tt>state</tt> attribute.
         */
        public void setState(State state)
        {
            ConferenceInfoDocument.this.setState(endpointElement, state);
        }

        /**
         * Returns the value of the <tt>state</tt> attribute of this
         * <tt>Endpoint</tt>'s element
         * @return the value of the <tt>state</tt> attribute of this
         * <tt>Endpoint</tt>'s element
         */
        public State getState()
        {
            return ConferenceInfoDocument.this.getState(endpointElement);
        }

        /**
         * Sets the <tt>status</tt> child element of this <tt>Endpoint</tt>'s
         * element.
         * @param status the value to be used for the text content of the
         * <tt>status</tt> element.
         */
        public void setStatus(EndpointStatusType status)
        {
            ConferenceInfoDocument.this.setStatus(endpointElement,
                    status == null
                    ? null
                    : status.toString());
        }

        /**
         * Returns the <tt>EndpointStatusType</tt> corresponding to the
         * <tt>status</tt> child of this <tt>Endpoint</tt>'s element, or
         * <tt>null</tt>.
         * @return the <tt>EndpointStatusType</tt> corresponding to the
         * <tt>status</tt> child of this <tt>Endpoint</tt>'s element, or
         * <tt>null</tt>.
         */
        public EndpointStatusType getStatus()
        {
            Element statusElement
                    = XMLUtils.findChild(endpointElement, STATUS_ELEMENT_NAME);
            return statusElement == null
                ? null
                : EndpointStatusType.parseString(statusElement.getTextContent());
        }

        /**
         * Returns the list of <tt>Media</tt>s which represent the
         * <tt>media</tt> children of this <tt>Endpoint</tt>'s element.
         * @return the list of <tt>Media</tt>s which represent the
         * <tt>media</tt> children of this <tt>Endpoint</tt>'s element.
         */
        public List<Media> getMedias()
        {
            return mediasList;
        }

        /**
         * Searches this <tt>Endpoint</tt>'s associated <tt>Media</tt>s
         * and returns the one with <tt>id</tt> attribute <tt>id</tt>, or
         * <tt>null</tt> if one wasn't found.
         * @param id The value of the <tt>id</tt> attribute to search
         * for.
         * @return The <tt>Media</tt>s with <tt>id</tt> attribute <tt>id</tt>,
         * or <tt>null</tt> if one wasn't found.
         */
        public Media getMedia(String id)
        {
            if (id == null)
                return null;
            for (Media m : mediasList)
            {
                if (id.equals(m.getId()))
                    return m;
            }
            return null;
        }

        /**
         * Creates a new <tt>Media</tt> instance, adds it to this
         * <tt>Endpoint</tt> and returns it.
         * @param id The value to use for the <tt>id</tt> attribute of the
         * new <tt>Media</tt>'s element.
         * @return the newly created <tt>Media</tt> instance.
         */
        public Media addNewMedia(String id)
        {
            Element mediaElement = document.createElement(MEDIA_ELEMENT_NAME);
            Media media = new Media(mediaElement);
            media.setId(id);

            endpointElement.appendChild(mediaElement);
            mediasList.add(media);

            return media;
        }

        /**
         * Adds a copy of <tt>media</tt> to this <tt>Endpoint</tt>
         * @param media the <tt>Media</tt> to add a copy of
         */
        public void addMedia(Media media)
        {
            Media newMedia = addNewMedia(media.getId());
            newMedia.setSrcId(media.getSrcId());
            newMedia.setType(media.getType());
            newMedia.setStatus(media.getStatus());
        }

        /**
         * Removes a specific <tt>Media</tt> (the one with id <tt>id</tt>) from
         * this <tt>Endpoint</tt>.
         * @param id the <tt>id</tt> of the <tt>Media</tt> to remove.
         */
        public void removeMedia(String id)
        {
            Media media = getMedia(id);
            if (media != null)
            {
                mediasList.remove(media);
                endpointElement.removeChild(media.mediaElement);
            }
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
        private Element mediaElement;

        /**
         * Creates a new <tt>Media</tt> instance with the specified
         * <tt>Element</tt> as its underlying element.
         * @param media the <tt>Element</tt> to use
         */
        private Media(Element media)
        {
            this.mediaElement = media;
        }

        /**
         * Sets the <tt>id</tt> attribute of this <tt>Media</tt>'s element to
         * <tt>id</tt>
         * @param id the value to set for the <tt>id</tt> attribute.
         */
        public void setId(String id)
        {
            if (id == null || id.equals(""))
                mediaElement.removeAttribute(ID_ATTR_NAME);
            else
                mediaElement.setAttribute(ID_ATTR_NAME, id);
        }

        /**
         * Returns the <tt>id</tt> attribute of this <tt>Media</tt>'s element.
         * @return the <tt>id</tt> attribute of this <tt>Media</tt>'s element.
         */
        public String getId()
        {
            return mediaElement.getAttribute(ID_ATTR_NAME);
        }

        /**
         * Sets the <tt>src-id</tt> child element of this <tt>Media</tt>'s
         * element.
         * @param srcId the value to be used for the text content of the
         * <tt>src-id</tt> element.
         */
        public void setSrcId(String srcId)
        {
            Element srcIdElement
                    = XMLUtils.findChild(mediaElement, SRC_ID_ELEMENT_NAME);
            if (srcId == null || srcId.equals(""))
            {
                if (srcIdElement != null)
                    mediaElement.removeChild(srcIdElement);
            }
            else
            {
                if (srcIdElement == null)
                {
                    srcIdElement
                            = document.createElement(SRC_ID_ELEMENT_NAME);
                    mediaElement.appendChild(srcIdElement);
                }
                srcIdElement.setTextContent(srcId);
            }
        }

        /**
         * Returns the text content of the <tt>src-id</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         * @return the text content of the <tt>src-id</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         */
        public String getSrcId()
        {
            Element srcIdElement
                    = XMLUtils.findChild(mediaElement, SRC_ID_ELEMENT_NAME);
            return srcIdElement == null
                    ? null
                    : srcIdElement.getTextContent();
        }

        /**
         * Sets the <tt>type</tt> child element of this <tt>Media</tt>'s
         * element.
         * @param type the value to be used for the text content of the
         * <tt>type</tt> element.
         */
        public void setType(String type)
        {
            Element typeElement
                    = XMLUtils.findChild(mediaElement, TYPE_ELEMENT_NAME);
            if (type == null || type.equals(""))
            {
                if (typeElement != null)
                    mediaElement.removeChild(typeElement);
            }
            else
            {
                if (typeElement == null)
                {
                    typeElement = document.createElement(TYPE_ELEMENT_NAME);
                    mediaElement.appendChild(typeElement);
                }
                typeElement.setTextContent(type);
            }
        }

        /**
         * Returns the text content of the <tt>type</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         * @return the text content of the <tt>type</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         */
        public String getType()
        {
            Element typeElement
                    = XMLUtils.findChild(mediaElement, TYPE_ELEMENT_NAME);
            return typeElement == null
                    ? null
                    : typeElement.getTextContent();
        }

        /**
         * Sets the <tt>status</tt> child element of this <tt>Media</tt>'s
         * element.
         * @param status the value to be used for the text content of the
         * <tt>status</tt> element.
         */
        public void setStatus(String status)
        {
            ConferenceInfoDocument.this.setStatus(mediaElement, status);
        }

        /**
         * Returns the text content of the <tt>status</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         * @return the text content of the <tt>status</tt> child element
         * of this <tt>Media</tt>'s element, if it has such a child. Returns
         * <tt>null</tt> otherwise.
         */
        public String getStatus()
        {
            Element statusElement
                    = XMLUtils.findChild(mediaElement, STATUS_ELEMENT_NAME);
            return statusElement == null
                    ? null
                    : statusElement.getTextContent();
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
