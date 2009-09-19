/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import net.java.sip.communicator.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

/**
 * An outgoing typing notification
 * 
 * @author Edgar Poce
 */
public class FacebookOutgoingTypingNotification
{
    private static Logger logger = Logger
            .getLogger(FacebookOutgoingTypingNotification.class);

    private final static String URL = "http://www.facebook.com/ajax/chat/typ.php";

    private final FacebookSession session;

    private int typingState;

    private String address;

    public FacebookOutgoingTypingNotification(FacebookSession session)
    {
        this.session = session;
    }

    public void send()
        throws IOException,
               BrokenFacebookProtocolException,
               FacebookErrorException
    {
        synchronized (this.session)
        {
            logger.debug("sending typing notification " + typingState);
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("typ", typingState + ""));
            nvps.add(new BasicNameValuePair("to", address));
            nvps
                    .add(new BasicNameValuePair("post_form_id", session
                            .getFormId()));
            HttpPost post = new HttpPost(URL);
            post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = this.session.getHttpClient().execute(post);
            String body = EntityUtils.toString(response.getEntity());
            new FacebookJsonResponse(this.session, body);
        }
    }

    public int getTypingState()
    {
        return typingState;
    }

    public void setTypingState(int typingState)
    {
        this.typingState = typingState;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }
}
