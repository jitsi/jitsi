/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.sip;

import net.java.sip.communicator.util.*;

import javax.sip.header.*;
import javax.sip.message.*;
import java.text.*;
import java.util.*;

/**
 * Checks the <tt>protocolProvider</tt>'s account properties for configured
 * custom headers and add them to the outgoing requests.
 * The header account properties are of form:
 *     ConfigHeader.N.Name=HeaderName
 *     ConfigHeader.N.Value=HeaderValue
 *     ConfigHeader.N.Method=SIP_MethodName (optional)
 * Where N is the index of the header, multiple custom headers can be added.
 * Name is the header name to use and Value is its value. The optional
 * property is whether to use a specific request method to attach headers to
 * or if missing we will attach it to all requests.
 *
 * @author Damian Minkov
 */
public class ConfigHeaders
{
    /**
     * The logger for this class
     */
    public final static Logger logger = Logger.getLogger(ConfigHeaders.class);

    /**
     * The account property holding all the custom pre-configured headers.
     */
    private final static String ACC_PROPERTY_CONFIG_HEADER = "ConfigHeader";

    /**
     * The config property suffix for custom header name.
     */
    private final static String ACC_PROPERTY_CONFIG_HEADER_NAME
        = "Name";

    /**
     * The config property suffix for custom header value.
     */
    private final static String ACC_PROPERTY_CONFIG_HEADER_VALUE
        = "Value";

    /**
     * The config property suffix for custom header method.
     */
    private final static String ACC_PROPERTY_CONFIG_HEADER_METHOD
        = "Method";

    /**
     * Attach any custom headers pre configured for the account. Added only
     * to message Requests.
     * The header account properties are of form:
     *     ConfigHeader.N.Name=HeaderName
     *     ConfigHeader.N.Value=HeaderValue
     *     ConfigHeader.N.Method=SIP_MethodName (optional)
     * Where N is the index of the header, multiple custom headers can be added.
     * Name is the header name to use and Value is its value. The optional
     * property is whether to use a specific request method to attach headers to
     * or if missing we will attach it to all requests.
     *
     * @param message the message that we'd like to attach custom headers to.
     * @param protocolProvider the protocol provider to check for configured
     * custom headers.
     */
    static void attachConfigHeaders(
        Message message,
        ProtocolProviderServiceSipImpl protocolProvider)
    {
        if(message instanceof Response)
            return;

        Request request = (Request)message;

        Map<String, String> props
            = protocolProvider.getAccountID().getAccountProperties();

        Map<String,Map<String,String>> headers
            = new HashMap<String, Map<String, String>>();

        // parse the properties into the map where the index is the key
        for(Map.Entry<String, String> entry : props.entrySet())
        {
            String pName = entry.getKey();
            String prefStr = entry.getValue().trim();
            String name;
            String ix;

            if(!pName.startsWith(ACC_PROPERTY_CONFIG_HEADER))
                continue;

            if(pName.contains("."))
            {
                pName = pName.replaceAll(ACC_PROPERTY_CONFIG_HEADER + ".", "");
                name = pName.substring(pName.lastIndexOf('.') + 1).trim();

                if(!pName.contains("."))
                    continue;

                ix = pName.substring(0, pName.lastIndexOf('.')).trim();
            }
            else
                continue;

            Map<String,String> headerValues = headers.get(ix);

            if(headerValues == null)
            {
                headerValues = new HashMap<String, String>();
                headers.put(ix, headerValues);
            }

            headerValues.put(name, prefStr);
        }

        // process the found custom headers
        for(Map<String, String> headerValues : headers.values())
        {
            String method = headerValues.get(ACC_PROPERTY_CONFIG_HEADER_METHOD);

            // if there is a method setting and is different from
            // current request method skip this header
            // if any of the required properties are missing skip (Name & Value)
            if((method != null
                && !request.getMethod().equalsIgnoreCase(method))
                || !headerValues.containsKey(ACC_PROPERTY_CONFIG_HEADER_NAME)
                || !headerValues.containsKey(ACC_PROPERTY_CONFIG_HEADER_VALUE))
                continue;

            try
            {
                Header customHeader = protocolProvider.getHeaderFactory()
                    .createHeader(
                        headerValues.get(ACC_PROPERTY_CONFIG_HEADER_NAME),
                        headerValues.get(ACC_PROPERTY_CONFIG_HEADER_VALUE));

                request.setHeader(customHeader);
            }
            catch(ParseException e)
            {
                logger.error("Cannot create custom header", e);
            }
        }
    }
}
