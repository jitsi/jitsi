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
package net.java.sip.communicator.impl.ldap;

import java.util.*;
import java.util.regex.*;

import javax.naming.*;
import javax.naming.directory.*;

import net.java.sip.communicator.service.ldap.*;
import net.java.sip.communicator.service.ldap.event.*;
import net.java.sip.communicator.util.*;

/**
 * An LdapDirectory stores settings for one directory server
 * and performs ldap operations (search)
 *
 * @author Sebastien Mazy
 */
public class LdapDirectoryImpl
    extends DefaultLdapEventManager
    implements LdapDirectory,
               LdapListener,
               LdapConstants
{
    /**
     * the logger for this class
     */
    private final static Logger logger = Logger
        .getLogger(LdapDirectoryImpl.class);

    static
    {
        logger.setLevelTrace();
    }

    /**
     * The settings for this directory
     */
    private LdapDirectorySettings settings;

    /**
     * Stores the pending searches
     *
     * @see LdapPendingSearch
     */
    private HashMap<LdapQuery, LdapPendingSearch> pendingSearches =
        new HashMap<LdapQuery, LdapPendingSearch>();

    /**
     * Name of avatar attribute.
     */
    private static final String[] PHOTO_ATTRIBUTES = new String[]
    {
        "jpegPhoto",
        "thumbnailPhoto"
    };

    /**
     * data structure used to store the LDAP attributes that
     * could contain our search string
     * e.g. cn, sn, givenname, uid
     */
    private static final Set<String> searchableAttributes
        = new HashSet<String>();

    /**
     * Map that contains list of attributes that match a certain type of
     * attributes (i.e. all attributes that match a mail, a home phone, a
     * mobile phones, ...).
     */
    private Map<String, List<String> > attributesMap = new
        HashMap<String, List<String> >();

    /**
     * List of searchables attributes.
     */
    private List<String> searchableAttrs = new ArrayList<String>();

    static
    {
        searchableAttributes.add("displayName");
        searchableAttributes.add("cn");
        searchableAttributes.add("commonname");
        searchableAttributes.add("sn");
        searchableAttributes.add("surname");
        searchableAttributes.add("gn");
        searchableAttributes.add("givenname");
        //searchableAttributes.add("mail");
        searchableAttributes.add("uid");
    }

    /**
     * data structure used to store the attribute whose values
     * could be a contact address for the person found
     * e.g. mail, telephoneNumber
     */
    private static final Set<String> retrievableAttributes
        = new HashSet<String>();

    static
    {
        retrievableAttributes.add("displayName");
        retrievableAttributes.add("cn");
        retrievableAttributes.add("commonname");
        retrievableAttributes.add("sn");
        retrievableAttributes.add("surname");
        retrievableAttributes.add("givenName");
        retrievableAttributes.add("givenname");
        retrievableAttributes.add("gn");
        retrievableAttributes.add("o");
        retrievableAttributes.add("organizationName");
        retrievableAttributes.add("company");
        retrievableAttributes.add("ou");
        retrievableAttributes.add("orgunit");
        retrievableAttributes.add("organizationalUnitName");
        retrievableAttributes.add("department");
        retrievableAttributes.add("departmentNumber");
    }

    /**
     * the env HashTable stores the settings used to create
     * an InitialDirContext (i.e. connect to the LDAP directory)
     */
    private final Hashtable<String, String> env =
        new Hashtable<String, String>();

    /**
     * List of all phone number attributes.
     */
    private final List<String> phoneNumberAttributes = new ArrayList<String>();

    /**
     * The contructor for this class.
     * Since this element is immutable (otherwise it would be a real pain
     * to use with a Set), it takes all the settings we could need to store
     * This constructor will not modify the <tt>settings</tt>
     * or save a reference to it, but may save a clone.
     *
     * @param settings settings for this new server
     *
     * @see LdapDirectorySettings
     */
    public LdapDirectoryImpl(LdapDirectorySettings settings)
    {
        String portText;

        /* settings checks */
        if(!textHasContent(settings.getName()))
            throw new IllegalArgumentException("name has no content.");
        if(!textHasContent(settings.getHostname()))
            throw new IllegalArgumentException("Hostname has no content.");
        if(settings.getAuth() != Auth.NONE && !textHasContent(
                settings.getBindDN()))
            throw new IllegalArgumentException("Bind DN has no content.");
        if(settings.getAuth() != Auth.NONE && settings.getPassword() == null)
            throw new IllegalArgumentException("password is null.");
        if(settings.getPort() < 0 || settings.getPort() > 65535)
            throw new IllegalArgumentException("Illegal port number.");
        if(settings.getBaseDN() == null)
            throw new IllegalArgumentException("Base DN has no content.");

        this.settings = settings.clone();

        if(this.settings.getPort() == 0)
            portText = ":" + this.settings.getEncryption().defaultPort();
        else
            portText = ":" + this.settings.getPort();

        /* fills environment for InitialDirContext */
        this.env.put(Context.INITIAL_CONTEXT_FACTORY,
                "com.sun.jndi.ldap.LdapCtxFactory");
        this.env.put("com.sun.jndi.ldap.connect.timeout", LDAP_CONNECT_TIMEOUT);
        this.env.put("com.sun.jndi.ldap.read.timeout", LDAP_READ_TIMEOUT);
        this.env.put(Context.PROVIDER_URL, settings.getEncryption().
                protocolString() + settings.getHostname() + portText +"/");
        //connection pooling
        this.env.put("com.sun.jndi.ldap.connect.pool", "true");

        /* TODO STARTTLS */
        switch(this.settings.getEncryption())
        {
            case CLEAR:
                break;
            case SSL:
                this.env.put(Context.SECURITY_PROTOCOL, "ssl");
                this.env.put("java.naming.ldap.factory.socket",
                    LdapSSLSocketFactoryDelegate.class.getName());
                break;
        }

        /* TODO SASL */
        switch(this.settings.getAuth())
        {
            case NONE:
                this.env.put(Context.SECURITY_AUTHENTICATION, "none");
                break;
            case SIMPLE:
                this.env.put(Context.SECURITY_AUTHENTICATION, "simple");
                this.env.put(Context.SECURITY_PRINCIPAL,
                        this.settings.getBindDN());
                this.env.put(Context.SECURITY_CREDENTIALS,
                        this.settings.getPassword());
                break;
        }

        List<String> workPhoneOverrides = settings.getWorkPhoneSearchFields();
        List<String> mobilePhoneOverrides
            = settings.getMobilePhoneSearchFields();
        List<String> homePhoneOverrides = settings.getHomePhoneSearchFields();

        phoneNumberAttributes.addAll(workPhoneOverrides);
        phoneNumberAttributes.addAll(mobilePhoneOverrides);
        phoneNumberAttributes.addAll(homePhoneOverrides);

        attributesMap.put("mail", settings.getMailSearchFields());
        attributesMap.put("workPhone", workPhoneOverrides);
        attributesMap.put("mobilePhone", mobilePhoneOverrides);
        attributesMap.put("homePhone", homePhoneOverrides);

        this.searchableAttrs.addAll(searchableAttributes);
        for(String s : settings.getMailSearchFields())
        {
            searchableAttrs.add(s);
        }
        for(String s : workPhoneOverrides)
        {
            searchableAttrs.add(s);
        }
        for(String s : mobilePhoneOverrides)
        {
            searchableAttrs.add(s);
        }
        for(String s : homePhoneOverrides)
        {
            searchableAttrs.add(s);
        }

        if (settings.isPhotoInline())
        {
            retrievableAttributes.add("jpegPhoto");
            retrievableAttributes.add("thumbnailPhoto");
        }
    }

    /**
     * Returns the state of the enabled marker.
     * Required by LdapDirectory interface.
     *
     * @return the state of the enabled marker
     *
     * @see LdapDirectory#isEnabled
     */
    public boolean isEnabled()
    {
        return this.settings.isEnabled();
    }

    /**
     * Sets the state of the enabled marker
     * Required by LdapDirectory interface.
     *
     * @param enabled whether the server is marked as enabled
     *
     * @see LdapDirectory#setEnabled
     */
    public void setEnabled(boolean enabled)
    {
        this.settings.setEnabled(enabled);
    }

    /**
     * Returns an LdapDirectorySettings object containing
     * a copy of the settings of this server
     *
     * @return a copy of this server settings
     *
     * @see LdapDirectorySettings
     * @see LdapDirectory#getSettings
     */
    public LdapDirectorySettings getSettings()
    {
        return this.settings.clone();
    }

    /**
     * Connects to the remote directory
     */
    private InitialDirContext connect()
        throws NamingException
    {
        logger.trace("connecting to directory \"" + this + "\"");
        long time0 = System.currentTimeMillis();
        InitialDirContext dirContext =
            new InitialDirContext(this.env);
        long time1 = System.currentTimeMillis();
        logger.trace("connection to directory \"" + this + "\" took " +
                (time1-time0)  + " ms");
        return dirContext;
    }

    /**
     * closes the ldap connection
     */
    private void disconnect(InitialDirContext dirContext)
    {
        if(dirContext == null)
            throw new NullPointerException("dirContext is null");

        try
        {
            dirContext.close();
        }
        catch(NamingException e)
        {
            logger.trace("disconnection from directory \"" + this +
                    "\" failed!");
        }

        logger.trace("disconnection achieved!");
    }

    /**
     * Searches a person in the directory, based on a search string.
     * Since that method might take time to process, it should be
     * implemented asynchronously and send the results (LdapPersonFound)
     * with an LdapEvent to its listeners
     *
     * @param query assumed name (can be partial) of the person searched
     * e.g. "john", "doe", "john doe"
     * @param caller the LdapListener which called the method and will
     * receive results.
     * @param searchSettings custom settings for this search, null if you
     * want to stick with the defaults
     *
     * @see LdapDirectory#searchPerson
     * @see LdapPersonFound
     * @see LdapEvent
     */
    public void searchPerson(final LdapQuery query, final LdapListener caller,
            LdapSearchSettings searchSettings)
    {
        if(query == null)
            throw new NullPointerException("query shouldn't be null!");
        if(caller == null)
            throw new NullPointerException("caller shouldn't be null!");
        if(searchSettings == null)
            searchSettings = new LdapSearchSettingsImpl();

        // if the initial query string was "john d",
        // the intermediate query strings could be:
        // "*john d*" and "d*john"
        final String[] intermediateQueryStrings
            = buildIntermediateQueryStrings(query.toString());

        // the servers list contains this directory as many times as
        // the number of intermediate query strings
        List<LdapDirectory> serversList = new ArrayList<LdapDirectory>();
        //for(String queryString : intermediateQueryStrings)
        for(int i = 0 ; i < intermediateQueryStrings.length ; i++)
            serversList.add(this);

        // when the pendingSearches element will be empty,
        // all intermediate query strings will have been searched
        // and the search will be finished
        this.pendingSearches.put(query, new LdapPendingSearch(serversList,
                caller));

        // really performs the search
        for(String queryString : intermediateQueryStrings)
            this.performSearch(query, queryString, searchSettings, this);
    }

    private void performSearch(final LdapQuery query,
            final String realQueryString,
            final LdapSearchSettings searchSettings,
            final LdapListener caller)
    {
        Thread searchThread = new Thread()
        {
            int cancelState = 0;

            @Override
            public void run()
            {
                String filter = buildSearchFilter(realQueryString);
                logger.trace("starting search for " + filter +
                        " (initial query: \"" + query.toString() +
                        "\") on directory \"" + LdapDirectoryImpl.this + "\"");

                Pattern searchPattern = Pattern.compile(query.toString(),
                    Pattern.CASE_INSENSITIVE | Pattern.LITERAL);

                SearchControls searchControls =
                    buildSearchControls(searchSettings);

                LdapEvent endEvent = null;
                InitialDirContext dirContext = null;

                try
                {
                    if(searchSettings.isDelaySet())
                        Thread.sleep(searchSettings.getDelay());

                    checkCancel();
                    dirContext = connect();
                    checkCancel();

                    long time0 = System.currentTimeMillis();

                    NamingEnumeration<?> results = dirContext.search(
                            LdapDirectoryImpl.this.settings.getBaseDN(),
                            filter,
                            searchControls
                            );

                    checkCancel();

                    while (results.hasMore())
                    {
                        checkCancel();

                        SearchResult searchResult =
                            (SearchResult) results.next();
                        Map<String, Set<Object>> retrievedAttributes =
                            retrieveAttributes(searchResult);

                        if(!checkRetrievedAttributes(
                                query.toString(),
                                searchPattern,
                                retrievedAttributes))
                            continue;

                        LdapPersonFound person =
                            buildPerson(
                                query,
                                searchResult.getName(),
                                retrievedAttributes
                                );
                        LdapEvent resultEvent =
                            new LdapEvent(LdapDirectoryImpl.this,
                                    LdapEvent.LdapEventCause.NEW_SEARCH_RESULT,
                                    person);
                        fireLdapEvent(resultEvent, caller);
                    }

                    long time1 = System.currentTimeMillis();
                    logger.trace("search for real query \"" + filter +
                            "\" (initial query: \"" + query.toString() +
                            "\") on directory \"" + LdapDirectoryImpl.this +
                            "\" took " + (time1-time0) + "ms");

                    endEvent = new LdapEvent(LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_ACHIEVED, query);
                }
                catch(OperationNotSupportedException e)
                {
                    logger.error(
                            "use bind DN without password during search" +
                            " for real query \"" +
                            filter + "\" (initial query: \"" +
                            query.toString() + "\") on directory \"" +
                            LdapDirectoryImpl.this + "\": " + e);
                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_AUTH_ERROR,
                            query
                            );
                }
                catch(AuthenticationException e)
                {
                    logger.error(
                            "authentication failed during search" +
                            " for real query \"" +
                            filter + "\" (initial query: \"" +
                            query.toString() + "\") on directory \"" +
                            LdapDirectoryImpl.this + "\": " + e);
                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_AUTH_ERROR,
                            query
                            );
                }
                catch(NamingException e)
                {
                    logger.error(
                            "an external exception was thrown during search" +
                            " for real query \"" +
                            filter + "\" (initial query: \"" +
                            query.toString() + "\") on directory \"" +
                            LdapDirectoryImpl.this + "\": " + e);
                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_ERROR,
                            query
                            );
                }
                catch(LdapQueryCancelledException e)
                {
                    logger.trace("search for real query \"" + filter +
                            "\" (initial query: \"" + query.toString() +
                            "\") on " + LdapDirectoryImpl.this +
                            " cancelled at state " + cancelState);
                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_CANCELLED,
                            query
                            );

                }
                catch(InterruptedException e)
                {
                    // whether sleep was interrupted
                    // is not that important
                }
                catch (Exception e)
                {
                    logger.error("search for real query \"" + filter +
                            "\" (initial query: \"" + query.toString() +
                            "\") on " + LdapDirectoryImpl.this +
                            " cancelled at state " + cancelState, e);

                    endEvent = new LdapEvent(
                            LdapDirectoryImpl.this,
                            LdapEvent.LdapEventCause.SEARCH_ERROR,
                            query
                            );
                }
                finally
                {
                    fireLdapEvent(endEvent, caller);
                    if(dirContext != null)
                        disconnect(dirContext);
                }
            }

            /**
             * Checks if the query that triggered this search has
             * been marked as cancelled. If that's the case, the
             * search thread should be stopped and this method will
             * send a search cancelled event to the search initiator.
             * This method should be called by the search thread as
             * often as possible to quickly interrupt when needed.
             */
            private void checkCancel()
                throws LdapQueryCancelledException
            {
                if(query.getState() == LdapQuery.State.CANCELLED)
                {
                    throw new LdapQueryCancelledException();
                }
                this.cancelState++;
            }
        };

        // setting the classloader is necessary so that the BundleContext can be
        // accessed from classes instantiated from JNDI (specifically from our
        // custom SocketFactory)
        searchThread.setContextClassLoader(getClass().getClassLoader());
        searchThread.setDaemon(true);
        searchThread.start();
    }

    /**
     * Checks whether the found attributes match the current query.
     * @param searchPattern the pattern we use for checking
     * @param retrievedAttributes the attributes.
     * @return whether the found attributes match the current query.
     */
    private boolean checkRetrievedAttributes(
        String queryString,
        Pattern searchPattern,
        Map<String, Set<Object>> retrievedAttributes)
    {
        for(Map.Entry<String, Set<Object>> en : retrievedAttributes.entrySet())
        {
            if(!searchableAttrs.contains(en.getKey()))
            {
                continue;
            }

            boolean isPhoneNumber = phoneNumberAttributes.contains(en.getKey());

            for(Object o : en.getValue())
            {
                if(!(o instanceof String))
                    continue;

                if(searchPattern.matcher((String)o).find()
                    || (isPhoneNumber
                        && LdapActivator.getPhoneNumberI18nService()
                            .phoneNumbersMatch(queryString, (String)o)))
                    return true;
            }
        }

        return false;
    }

    private String[]
        buildIntermediateQueryStrings(String initialQueryString)
    {
        if (!this.settings.isMangleQuery())
        {
            return new String[] { initialQueryString };
        }

        // search for "doe john" as well "as john doe"
        String[] words = initialQueryString.split(" ");
        String[] intermediateQueryStrings;

        if(words.length == 2)
        {
            intermediateQueryStrings = new String[2];
            intermediateQueryStrings[0] = "*" + words[0] + " " + words[1] + "*";
            intermediateQueryStrings[1] = words[1] + "*" + words[0];
        }
        else
        {
            // one word or too many combinations
            intermediateQueryStrings = new String[1];
            intermediateQueryStrings[0] = "*" + initialQueryString + "*";
        }

        return intermediateQueryStrings;
    }

    /**
     * Fills the retrievedAttributes map with
     * "retrievable" string attributes (name, telephone number, ...)
     *
     * @param searchResult the results to browse for attributes
     * @return the attributes in a Map
     */
    private Map<String, Set<Object>>
        retrieveAttributes(SearchResult searchResult)
        throws NamingException
    {
        Attributes attributes =
            searchResult.getAttributes();
        Map<String, Set<Object>> retrievedAttributes =
            new HashMap<String, Set<Object>>();
        NamingEnumeration<String> ids = attributes.getIDs();
        while(ids.hasMore())
        {
            String id = ids.next();
            if(retrievableAttributes.contains(id) || containsAttribute(id))
            {
                Set<Object> valuesSet = new HashSet<Object>();
                retrievedAttributes.put(id, valuesSet);
                Attribute attribute = attributes.get(id);
                NamingEnumeration<?> values = attribute.getAll();
                while(values.hasMore())
                {
                    valuesSet.add(values.next());
                }
            }
        }
        return retrievedAttributes;
    }

    /**
     * Builds an LdapPersonFound with the retrieved attributes
     *
     * @param query the initial query issued
     * @param dn the distinguished name of the person in the directory
     * @return the LdapPersonFoulnd built
     */
    private LdapPersonFound
        buildPerson(
                LdapQuery query,
                String dn,
                Map<String,
                Set<Object>> retrievedAttributes
                )
        {
            LdapPersonFound person =
                new LdapPersonFoundImpl(LdapDirectoryImpl.this, dn, query);

            /* first name */
            if(retrievedAttributes.get("givenname") != null)
            {
                String firstName =
                    (String) retrievedAttributes.get("givenname")
                        .iterator().next();
                person.setFirstName(firstName);
            }
            else if(retrievedAttributes.get("givenName") != null)
            {
                String firstName =
                    (String) retrievedAttributes.get("givenName")
                        .iterator().next();
                person.setFirstName(firstName);
            }
            else if(retrievedAttributes.get("gn") != null)
            {
                String firstName =
                    (String) retrievedAttributes.get("gn")
                        .iterator().next();
                person.setFirstName(firstName);
            }

            /* surname */
            if(retrievedAttributes.get("sn") != null)
            {
                String surname =
                    (String) retrievedAttributes.get("sn")
                        .iterator().next();
                person.setSurname(surname);
            }
            else if(retrievedAttributes.get("surname") != null)
            {
                String surname =
                    (String) retrievedAttributes.get("surname")
                        .iterator().next();
                person.setSurname(surname);
            }

            /* displayed name */
            if(retrievedAttributes.get("displayName") != null)
            {
                String displayName =
                    (String) retrievedAttributes.get("displayName")
                        .iterator().next();
                person.setDisplayName(displayName);
            }
            else if(retrievedAttributes.get("cn") != null)
            {
                String displayName =
                    (String) retrievedAttributes.get("cn")
                        .iterator().next();
                person.setDisplayName(displayName);
            }
            else if(retrievedAttributes.get("commonname") != null)
            {
                String displayName =
                    (String) retrievedAttributes.get("commonname")
                        .iterator().next();
                person.setDisplayName(displayName);
            }
            if(person.getDisplayName() == null)
            {
                person.setDisplayName("" + person.getFirstName() +
                        person.getSurname());
            }
            //should never happen
            if(person.getDisplayName() == null)
            {
                throw new RuntimeException("display name is null!");
            }

            /* organization */
            if(retrievedAttributes.get("o") != null)
            {
                String organization =
                    (String) retrievedAttributes.get("o").iterator().next();
                person.setOrganization(organization);
            }
            else if(retrievedAttributes.get("organizationName") != null)
            {
                String organization =
                    (String) retrievedAttributes.get("organizationName")
                        .iterator().next();
                person.setOrganization(organization);
            }
            else if(retrievedAttributes.get("company") != null)
            {
                String organization =
                    (String) retrievedAttributes.get("company")
                        .iterator().next();
                person.setOrganization(organization);
            }

            /* department */
            if(retrievedAttributes.get("company") != null)
            {
                String department =
                    (String) retrievedAttributes.get("company")
                        .iterator().next();
                person.setDepartment(department);
            }
            else if(retrievedAttributes.get("ou") != null)
            {
                String department =
                    (String) retrievedAttributes.get("ou").iterator().next();
                person.setDepartment(department);
            }
            else if(retrievedAttributes.get("orgunit") != null)
            {
                String department =
                    (String) retrievedAttributes.get("orgunit")
                        .iterator().next();
                person.setDepartment(department);
            }
            else if(retrievedAttributes.get("organizationalUnitName") != null)
            {
                String department =
                    (String) retrievedAttributes.get("organizationalUnitName").
                        iterator().next();
                person.setDepartment(department);
            }
            else if(retrievedAttributes.get("department") != null)
            {
                String department =
                    (String) retrievedAttributes.get("department")
                        .iterator().next();
                person.setDepartment(department);
            }
            else if(retrievedAttributes.get("departmentNumber") != null)
            {
                String department =
                    (String) retrievedAttributes.get("departmentNumber")
                        .iterator().next();
                person.setDepartment(department);
            }

            if(retrievedAttributes.get("jpegPhoto") != null)
            {
                byte[] photo =
                    (byte[])retrievedAttributes.get("jpegPhoto")
                        .iterator().next();
                person.setPhoto(photo);
            }
            else if(retrievedAttributes.get("thumbnailPhoto") != null)
            {
                byte[] photo =
                    (byte[])retrievedAttributes.get("thumbnailPhoto")
                        .iterator().next();
                person.setPhoto(photo);
            }

            // mail
            List<String> attrs = attributesMap.get("mail");

            for(String attr : attrs)
            {
                if(retrievedAttributes.get(attr) != null)
                {
                    for(Object o : retrievedAttributes.get(attr))
                    {
                        String mail = o.toString();
                        if(!mail.contains("@"))
                        {
                            if(settings.getMailSuffix() != null)
                            {
                                mail += settings.getMailSuffix();
                            }
                            else
                                continue;
                        }
                        person.addMail(mail);
                    }
                }
            }

            // work phone
            attrs = attributesMap.get("workPhone");

            for(String attr : attrs)
            {
                if(retrievedAttributes.get(attr) != null)
                {
                    String phone =
                        (String) retrievedAttributes.get(attr)
                            .iterator().next();
                    person.addWorkPhone(phone);
                }
            }

            // mobile phone
            attrs = attributesMap.get("mobilePhone");

            for(String attr : attrs)
            {
                if(retrievedAttributes.get(attr) != null)
                {
                    for(Object phone : retrievedAttributes.get(attr))
                    {
                        person.addMobilePhone(phone.toString());
                    }
                }
            }

            // home phone
            attrs = attributesMap.get("homePhone");

            for(String attr : attrs)
            {
                if(retrievedAttributes.get(attr) != null)
                {
                    for(Object phone : retrievedAttributes.get(attr))
                    {
                        person.addHomePhone(phone.toString());
                    }
                }
            }

            return person;
        }

    /**
     * Turns LdapDirectoryImpl into a printable object
     * Used for debugging purposes
     *
     * @return a printable string
     *
     */
    @Override
    public String toString()
    {
        return this.settings.getName();
    }

    /**
     * An LdapDirectory is comparable in order to display LdapDirectory(s)
     * in alphabetic order in the UI.
     *
     * @see java.lang.Comparable
     */
    public int compareTo(LdapDirectory server)
    {
        return this.settings.getName().compareTo(server.getSettings().
                getName());
    }

    /**
     * Two LdapDirectory(s) with the same displayed name
     * should not exist in the same LdapDirectorySet,
     * thus this function
     */
    @Override
    public boolean equals(Object anObject)
    {
        if(anObject instanceof LdapDirectory)
        {
            LdapDirectory anLdapDirectory = (LdapDirectory) anObject;
            return this.settings.getName().equals(anLdapDirectory.getSettings().
                    getName());
        }
        return false;
    }

    /**
     * We override the equals method so we also do for
     * hashCode to keep consistent behavior
     */
    @Override
    public int hashCode()
    {
        return this.settings.getName().hashCode();
    }

    /**
     * Used to check method input parameters
     *
     * @return wether the text is not empty
     */
    private boolean textHasContent(String aText)
    {
        String EMPTY_STRING = "";
        return (aText != null) && (!aText.trim().equals(EMPTY_STRING));
    }

    /**
     * Builds an LDAP search filter, based on the query string entered and the
     * searchable fields defined in the static constructor. If a custom query is
     * defined this is used instead. e.g.
     * (|(|(mail=query)(telephoneNumber=query)))
     * 
     * @return an LDAP search filter
     */
    private String buildSearchFilter(String query)
    {
        if ("custom".equals(settings.getQueryMode()))
        {
            return settings.getCustomQuery().replace("<query>", query);
        }

        StringBuilder searchFilter = new StringBuilder();
        searchFilter.append("(|");

        /* cn=*query* OR sn=*query* OR ... */
        for(String attribute : searchableAttrs)
        {
            searchFilter.append("(");
            searchFilter.append(attribute);
            searchFilter.append("=");
            searchFilter.append(query);
            searchFilter.append(")");
        }

        //searchFilter.append("))");
        searchFilter.append(")");
        return searchFilter.toString();
    }

    /**
     * search the children nodes of the given dn
     *
     * @param dn the distinguished name of the node to search for children
     *
     * @see net.java.sip.communicator.service.ldap.LdapDirectory#searchChildren
     */
    public Collection<String> searchChildren(final String dn)
    {
        final Vector<String> nodes = new Vector<String>();
        InitialDirContext dirContext = null;

        if(dn.equals(""))
        {
            String[] returningAttributes = { "namingContexts" };
            /* use our custom search control */
            final SearchControls searchCtl = new SearchControls();
            searchCtl.setSearchScope(SearchControls.OBJECT_SCOPE);
            searchCtl.setReturningAttributes(returningAttributes);
            searchCtl.setTimeLimit(5000);

            logger.trace("starting search...");
            try
            {
                dirContext = connect();
                NamingEnumeration<?> result = dirContext.search(
                        dn, "(objectClass=*)", searchCtl);
                while (result.hasMore())
                {
                    SearchResult searchResult = (SearchResult) result.next();
                    Attributes attributes = searchResult.getAttributes();
                    Attribute attribute = attributes.get("namingContexts");
                    NamingEnumeration<?> values = attribute.getAll();
                    while (values.hasMore())
                    {
                        nodes.add((String) values.next());
                    }
                }
            }
            catch (NamingException e)
            {
                logger.trace("error when performing ldap search query" + e);
            }
            finally
            {
                if(dirContext != null)
                    disconnect(dirContext);
            }
        }
        else
        {
            /* use our custom search control */
            final SearchControls searchCtl = new SearchControls();
            searchCtl.setSearchScope(SearchControls.ONELEVEL_SCOPE);

            logger.trace("starting search...");
            try
            {
                dirContext = connect();
                NamingEnumeration<?> result = dirContext.search(
                        dn, "(objectClass=*)", searchCtl);
                while (result.hasMore())
                {
                    SearchResult sr = (SearchResult) result.next();
                    nodes.add(sr.getName());
                    logger.trace(sr.getName());
                }
            }
            catch (NamingException e)
            {
                logger.trace("error when performing ldap search query" + e);
                e.printStackTrace();
            }
            finally
            {
                if(dirContext != null)
                    disconnect(dirContext);
            }
        }

        return nodes;
    }

    /**
     * Tries to fetch the photo of the person with
     * the given distinguished name in the directory
     *
     * @param dn distinguished name of the person to fetch the photo
     * @return the bytes of the photo
     */
    byte[] fetchPhotoForPerson(String dn)
    {
        if (this.settings.isPhotoInline())
        {
            return null;
        }

        byte[] photo = null;
        InitialDirContext dirContext = null;

        /* use our custom search control */

        final SearchControls searchCtl = new SearchControls();
        searchCtl.setSearchScope(SearchControls.OBJECT_SCOPE);
        searchCtl.setReturningAttributes(PHOTO_ATTRIBUTES);

        logger.trace("starting photo retrieval...");
        try
        {
            dirContext = connect();
            String newBaseDN;
            if(settings.getBaseDN().equals(""))
                newBaseDN = dn;
            else
                newBaseDN = dn + "," + this.settings.getBaseDN();
            NamingEnumeration<?> result = dirContext.search(
                    newBaseDN, "(objectClass=*)", searchCtl);
            if(result.hasMore())
            {
                SearchResult searchResult = (SearchResult) result.next();
                Attributes attributes = searchResult.getAttributes();
                for (String a : PHOTO_ATTRIBUTES)
                {
                    Attribute attribute = attributes.get(a);
                    if(attribute != null)
                    {
                        NamingEnumeration<?> values = attribute.getAll();
                        if(values.hasMore())
                        {
                            photo = (byte[]) values.next();
                        }
                    }
                }
            }
        }
        catch (NamingException e)
        {
            logger.trace("error when performing photo retrieval" + e);
            e.printStackTrace();
        }
        finally
        {
            if(dirContext != null)
                disconnect(dirContext);
        }

        return photo;
    }

    private SearchControls buildSearchControls(LdapSearchSettings
            searchSettings)
    {
        SearchControls searchControls = new SearchControls();

        if(searchSettings.isMaxResultsSet())
        {
            // take value from searchSettings
            searchControls.setCountLimit(searchSettings.getMaxResults());
        }
        else
        {
            // take default from SearchControls: no limit
        }

        if(searchSettings.isScopeSet())
        {
            // take value from searchSettings
            searchControls.setSearchScope(
                    searchSettings.getScope().getConstant()
                    );
        }
        else
        {
            //take default from directory
            searchControls.setSearchScope(
                    this.getSettings().getScope().getConstant()
                    );
        }

        List<String> retrievableAttrs = new ArrayList<String>();

        retrievableAttrs.addAll(retrievableAttributes);
        for(String key : attributesMap.keySet())
        {
            List<String> attrs = attributesMap.get(key);
            for(String attr : attrs)
            {
                retrievableAttrs.add(attr);
            }
        }

        searchControls.setReturningAttributes(retrievableAttrs.toArray(
                new String[0]));

        return searchControls;
    }

    /**
     * Required by LdapListener.
     *
     * Dispatches event received from LdapDirectory-s to
     * real search initiators (the search dialog for example)
     *
     * @param event An LdapEvent probably sent by an LdapDirectory
     */
    public synchronized void ldapEventReceived(LdapEvent event)
    {
        LdapQuery query;
        switch(event.getCause())
        {
        case NEW_SEARCH_RESULT:
            LdapPersonFound result = (LdapPersonFound) event.getContent();
            query = result.getQuery();
            if(this.pendingSearches.get(query) != null)
            {
                this.fireLdapEvent(event,
                        pendingSearches.get(query).getCaller());
                logger.trace("result event for query \"" +
                        result.getQuery().toString() + "\" forwaded");
            }
            break;
        case SEARCH_ERROR:
        case SEARCH_AUTH_ERROR:
        case SEARCH_CANCELLED:
        case SEARCH_ACHIEVED:
            query = (LdapQuery) event.getContent();
            if(this.pendingSearches.get(query) != null)
            {
                this.pendingSearches.get(query).getPendingServers().
                remove(event.getSource());
                int sizeLeft = pendingSearches.get(query).
                    getPendingServers().size();
                logger.trace("end event received for initial query \"" +
                        query.toString() + "\" on directory \"" +
                        event.getSource() + "\"\nthere is " + sizeLeft +
                        " search pending for this initial query on directory \"" +
                        event.getSource() + "\"");
                if(sizeLeft == 0)
                {
                    fireLdapEvent(event, pendingSearches.get(query).
                            getCaller());
                    event = new LdapEvent(this,
                            LdapEvent.LdapEventCause.SEARCH_ACHIEVED,
                            query);
                    pendingSearches.remove(query);
                }
            }
            break;
        }
    }

    /**
     * Returns true if Map contains <tt>attribute</tt>.
     *
     * @param attribute attribute to search
     * @return true if Map contains <tt>attribute</tt>
     */
    private boolean containsAttribute(String attribute)
    {
        for(String key : attributesMap.keySet())
        {
            List<String> attrs = attributesMap.get(key);

            if(attrs.contains(attribute))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Overrides attributes name for searching for a specific type (i.e mail,
     * homePhone, ...).
     *
     * @param attribute name
     * @param names list of attributes name
     */
    public void overrideAttributesSearch(String attribute, List<String> names)
    {
        attributesMap.put(attribute, names);
    }

    /**
     * A custom exception used internally by LdapDirectoryImpl
     * to indicate that a query was cancelled
     *
     * @author Sebastien Mazy
     */
    public class LdapQueryCancelledException extends Exception
    {
        /**
         * Serial version UID.
         */
        private final static long serialVersionUID = 0L;
    }
}
