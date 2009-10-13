/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.io.*;
import java.util.*;

import org.apache.http.*;
import org.apache.http.client.entity.*;
import org.apache.http.client.methods.*;
import org.apache.http.message.*;
import org.apache.http.protocol.*;
import org.apache.http.util.*;

/**
 * An outgoing chat message
 * 
 * @author Edgar Poce
 */
public class FacebookOutgoingChatMessage {

    /**
     * The url of the login page
     */
    private static final String SEND_URL = "http://www.facebook.com/ajax/chat/send.php";

    private final FacebookSession session;

    private String address;

    private String content;

    private String messageUid;

    public FacebookOutgoingChatMessage(FacebookSession session)
    {
        this.session = session;
    }

    /**
     * Sends the message to the given address.<br>
     * Only one message can be sent at a time for a given
     * {@link FacebookSession}
     * 
     * @throws BrokenFacebookProtocolException
     * @throws IOException
     */
    public void send()
        throws BrokenFacebookProtocolException,
               IOException,
               FacebookErrorException
    {
        synchronized (this.session)
        {
            List<NameValuePair> nvps = new ArrayList<NameValuePair>();
            nvps.add(new BasicNameValuePair("msg_text", (content == null) ? ""
                    : content));
            nvps.add(new BasicNameValuePair("msg_id", messageUid));
            nvps.add(new BasicNameValuePair("client_time", Long.toString(System
                    .currentTimeMillis())));
            nvps.add(new BasicNameValuePair("to", address));
            nvps
                    .add(new BasicNameValuePair("post_form_id", session
                            .getFormId()));
            HttpPost post = new HttpPost(SEND_URL);
            post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
            HttpResponse response = this.session.getHttpClient().execute(post);
            String body = EntityUtils.toString(response.getEntity());
            if (response.getStatusLine().getStatusCode() != 200)
            {
                throw new BrokenFacebookProtocolException("CODE "
                        + response.getStatusLine().getStatusCode());
            }
            new FacebookJsonResponse(this.session, body);
        }
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getMessageUid()
    {
        return messageUid;
    }

    public void setMessageUid(String messageUid)
    {
        this.messageUid = messageUid;
    }
}
