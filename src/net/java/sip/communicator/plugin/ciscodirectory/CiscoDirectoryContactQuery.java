/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.plugin.ciscodirectory;

import static net.java.sip.communicator.plugin.ciscodirectory
        .CiscoDirectoryActivator.*;
import static org.jitsi.util.StringUtils.*;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.*;
import javax.xml.parsers.*;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import net.java.sip.communicator.service.contactsource.*;
import net.java.sip.communicator.service.contactsource.ContactDetail.*;
import net.java.sip.communicator.service.protocol.*;
import net.java.sip.communicator.service.protocol.globalstatus.*;
import net.java.sip.communicator.util.*;

/**
 * Queries the Cisco directory for contacts matching the given pattern.
 *
 * @author Fabien Cortina<fabien.cortina@gmail.com>
 * @see <a href="https://developer.cisco.com/web/ipps">Documentation</a>
 */
final class CiscoDirectoryContactQuery extends
        AsyncContactQuery<CiscoDirectoryContactSourceService>
{
    private final static Logger LOGGER =
            Logger.getLogger(CiscoDirectoryContactQuery.class);

    private static final int MINIMUM_QUERY_LENGTH = 3;

    private static final String FIRST_NAME_FIELD = "f";
    private static final String LAST_NAME_FIELD = "l";
    private static final String PHONE_NUMBER_FIELD = "n";

    /**
     * Creates a query object for the given source and pattern.
     *
     * @param owner the contact source service that sent this query.
     * @param pattern the pattern to sea.rch for
     */
    CiscoDirectoryContactQuery(CiscoDirectoryContactSourceService owner,
            Pattern pattern)
    {
        super(owner, pattern);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void run()
    {
        try
        {
            if (isValid())
            {
                queryDirectory(buildQueryUrl(FIRST_NAME_FIELD));
                queryDirectory(buildQueryUrl(LAST_NAME_FIELD));
                queryDirectory(buildQueryUrl(PHONE_NUMBER_FIELD));
            }

            stopped(true);
        }
        catch (Throwable caught)
        {
            LOGGER.warn("Could not query the directory", caught);
            stopped(false);
        }
    }

    /**
     * @return true if the query is valid.
     */
    private boolean isValid()
    {
        DirectorySettings settings = getContactSource().getDirectorySettings();
        if (settings == null)
        {
            LOGGER.warn("No settings");
            return false;
        }

        if (!settings.isEnabled())
        {
            return false;
        }

        if (isNullOrEmpty(settings.getDirectoryUrl(), true))
        {
            LOGGER.warn("Directory url is not defined");
            return false;
        }

        String queryString = getSimpleQueryString();
        if (queryString.length() < MINIMUM_QUERY_LENGTH)
        {
            return false;
        }

        return true;
    }

    /**
     * Cleans up the query pattern as this source does not handle regular
     * expressions.
     *
     * @return the query string as a plain string.
     */
    private String getSimpleQueryString()
    {
        String pattern = getPhoneNumberQuery();
        if (pattern.startsWith("^") && pattern.endsWith("$"))
        {
            return pattern.substring(1, pattern.length() - 1);
        }
        else if (pattern.startsWith("\\Q") && pattern.endsWith("\\E"))
        {
            return pattern.substring(2, pattern.length() - 2);
        }
        return pattern;
    }

    /**
     * Builds a query url for a given search field.
     *
     * @param field the field to run the query on
     * @return a url pointing to the directory service
     * @throws MalformedURLException
     */
    private URL buildQueryUrl(String field) throws MalformedURLException
    {
        DirectorySettings settings = getContactSource().getDirectorySettings();
        String directoryUrl = settings.getDirectoryUrl();

        StringBuilder builder = new StringBuilder(directoryUrl);
        builder.append(directoryUrl.contains("?") ? "&" : "?");

        String queryString = getSimpleQueryString();
        builder.append(field).append("=");
        builder.append(urlEncode(queryString));

        return new URL(builder.toString());
    }

    /**
     * Encodes a string so it can be used in a URL.
     *
     * @param string the string to encode
     * @return the url-encoded string.
     */
    private String urlEncode(String string)
    {
        try
        {
            return URLEncoder.encode(string, "UTF-8");
        }
        catch (UnsupportedEncodingException ignore)
        {
            // That's can't happen
            return string;
        }
    }

    /**
     * Runs a query against the directory service at the given urls and adds the
     * matching entries to the current query results.
     *
     * @param url location of the directory service
     * @throws IOException if the request fails or the response can't be read
     * @throws ParserConfigurationException if the parser can't be instantiated
     * @throws SAXException if the response is not a valid XML document
     */
    private void queryDirectory(URL url)
            throws IOException, ParserConfigurationException, SAXException
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("Querying " + url);
        }

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        if (connection.getResponseCode() == 200)
        {
            InputStream inputStream = getInputStream(connection);

            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser saxParser = factory.newSAXParser();

            DefaultHandler handler = new DirectoryEntryHandler();
            saxParser.parse(inputStream, handler);
        }
    }

    /**
     * Cleans up the response content. Notably, it removes empty lines and
     * spaces at the top of the input because they cause the SAXParser to fail.
     *
     * @param connection the connection to the directory service
     * @return a input stream ready to be parsed
     * @throws IOException if the response cannot be read.
     */
    private InputStream getInputStream(HttpURLConnection connection)
            throws IOException
    {
        int contentLength = connection.getContentLength();
        String contentEncoding = connection.getContentEncoding();
        InputStream inputStream = connection.getInputStream();

        try
        {
            InputStreamReader reader = new InputStreamReader(inputStream);
            BufferedReader bufferedReader = new BufferedReader(reader);

            int capacity = contentLength > 0 ? contentLength : 1024;
            StringBuilder builder = new StringBuilder(capacity);

            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                line = line.trim();
                if (!line.isEmpty())
                {
                    builder.append(line);
                }
            }

            String content = builder.toString();
            if (LOGGER.isDebugEnabled())
            {
                LOGGER.debug("Response:\n" + content);
            }

            byte[] bytes = contentEncoding == null
                    ? content.getBytes()
                    : content.getBytes(contentEncoding);
            return new ByteArrayInputStream(bytes);
        }
        finally
        {
            inputStream.close();
        }
    }

    /**
     * This SAX handler parses the directory entries in an XML file and adds
     * them to the query results.
     */
    private final class DirectoryEntryHandler extends DefaultHandler
    {
        private String currentContent;
        private String name;
        private String phone;

        /**
         * {@inheritDoc}
         */
        @Override
        public void characters(char ch[], int start, int length)
                throws SAXException
        {
            this.currentContent = new String(ch, start, length).trim();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void endElement(String uri, String localName, String qName)
                throws SAXException
        {
            if ("Name".equalsIgnoreCase(qName))
            {
                this.name = currentContent;
            }
            else if ("Telephone".equalsIgnoreCase(qName))
            {
                this.phone = currentContent;
            }
            else if ("DirectoryEntry".equalsIgnoreCase(qName))
            {
                addContact();
            }
            currentContent = null;
        }

        /**
         * Adds a contact to the query results if and only if we have a valid
         * name and a phone number stored in {@link #name} and {@link #phone}.
         */
        private void addContact()
        {
            if (isNullOrEmpty(phone, true) || isNullOrEmpty(name, true))
            {
                return;
            }

            name = normalizedName();
            phone = normalizedPhoneNumber();

            ContactDetail detail = new ContactDetail(
                    phone, Category.Phone, new SubCategory[]{SubCategory.Work});
            detail.addSupportedOpSet(OperationSetBasicTelephony.class);
            detail.addSupportedOpSet(OperationSetPersistentPresence.class);

            List<ContactDetail> details = Arrays.asList(detail);
            GenericSourceContact contact = new GenericSourceContact(
                    getContactSource(), name, details);
            contact.setPresenceStatus(GlobalStatusEnum.ONLINE);
            addQueryResult(contact);
        }

        /**
         * @return the normalized form of {@link #name}.
         */
        private String normalizedName()
        {
            int i = name.indexOf(", ");
            String firstName = name.substring(i + 1);
            String lastName = name.substring(0, i);
            return firstName + " " + lastName;
        }

        /**
         * @return the normalized form of {@link #phone}.
         */
        private String normalizedPhoneNumber()
        {
            return getPhoneNumberI18nService().normalize(phone);
        }
    }
}
