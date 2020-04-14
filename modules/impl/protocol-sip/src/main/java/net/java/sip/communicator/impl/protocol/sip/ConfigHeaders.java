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
package net.java.sip.communicator.impl.protocol.sip;

import gov.nist.javax.sip.header.*;
import net.java.sip.communicator.util.*;

import javax.sip.address.*;
import javax.sip.header.*;
import javax.sip.message.*;
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
            String prefStr = entry.getValue();
            String name;
            String ix;

            if(!pName.startsWith(ACC_PROPERTY_CONFIG_HEADER) || prefStr == null)
                continue;

            prefStr = prefStr.trim();

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
                String name = headerValues.get(ACC_PROPERTY_CONFIG_HEADER_NAME);
                String value = processParams(
                    headerValues.get(ACC_PROPERTY_CONFIG_HEADER_VALUE),
                    request);

                Header h = request.getHeader(name);

                // makes possible overriding already created headers which
                // are not custom one
                // RouteHeader is used by ProxyRouter/DefaultRouter and we
                // cannot use it as custom header if we want to add it
                if((h != null && !(h instanceof CustomHeader))
                    || name.equals(SIPHeaderNames.ROUTE))
                {
                    request.setHeader(protocolProvider.getHeaderFactory()
                        .createHeader(name, value));
                }
                else
                    request.addHeader(new CustomHeaderList(name, value));
            }
            catch(Exception e)
            {
                logger.error("Cannot create custom header", e);
            }
        }
    }

    /**
     * Checks for certain params existence in the value, and replace them
     * with real values obtained from <tt>request</tt>.
     * @param value the value of the header param
     * @param request the request we are processing
     * @return the value with replaced params
     */
    private static String processParams(String value, Request request)
    {
        if(value.indexOf("${from.address}") != -1)
        {
            FromHeader fromHeader
                = (FromHeader)request.getHeader(FromHeader.NAME);

            if(fromHeader != null)
            {
                value = value.replace(
                    "${from.address}",
                    fromHeader.getAddress().getURI().toString());
            }
        }

        if(value.indexOf("${from.userID}") != -1)
        {
            FromHeader fromHeader
                = (FromHeader)request.getHeader(FromHeader.NAME);

            if(fromHeader != null)
            {
                URI fromURI = fromHeader.getAddress().getURI();
                String fromAddr = fromURI.toString();

                // strips sip: or sips:
                if(fromURI.isSipURI())
                {
                    fromAddr
                        = fromAddr.replaceFirst(fromURI.getScheme() + ":", "");
                }

                // take the userID part
                int index = fromAddr.indexOf('@');
                if ( index > -1 )
                    fromAddr = fromAddr.substring(0, index);

                value = value.replace("${from.userID}", fromAddr);
            }
        }

        if(value.indexOf("${to.address}") != -1)
        {
            ToHeader toHeader =
                (ToHeader)request.getHeader(ToHeader.NAME);

            if(toHeader != null)
            {
                value = value.replace(
                    "${to.address}",
                    toHeader.getAddress().getURI().toString());
            }
        }

        if(value.indexOf("${to.userID}") != -1)
        {
            ToHeader toHeader =
                (ToHeader)request.getHeader(ToHeader.NAME);

            if(toHeader != null)
            {
                URI toURI = toHeader.getAddress().getURI();
                String toAddr = toURI.toString();

                // strips sip: or sips:
                if(toURI.isSipURI())
                {
                    toAddr = toAddr.replaceFirst(toURI.getScheme() + ":", "");
                }

                // take the userID part
                int index = toAddr.indexOf('@');
                if ( index > -1 )
                    toAddr = toAddr.substring(0, index);

                value = value.replace("${to.userID}", toAddr);
            }
        }

        return value;
    }
}
