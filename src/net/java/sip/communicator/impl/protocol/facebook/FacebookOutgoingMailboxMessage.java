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
 * FIXME not working
 * 
 * @author Edgar Poce
 */
public class FacebookOutgoingMailboxMessage
{
    private final static String URL = "http://www.facebook.com/ajax/inbox/ajax.php";

    private final FacebookSession session;

    private String uid;

    private String subject;

    private String content;

    private String address;

    public FacebookOutgoingMailboxMessage(FacebookSession session)
    {
        this.session = session;
    }

    public void send()
        throws IOException,
               BrokenFacebookProtocolException,
               FacebookErrorException
    {
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        nvps.add(new BasicNameValuePair("post_form_id", session.getFormId()));
        nvps.add(new BasicNameValuePair("message", (this.content == null) ? ""
                : this.content));
        nvps.add(new BasicNameValuePair("subject", (this.subject == null) ? ""
                : this.subject));
        nvps.add(new BasicNameValuePair("ids[0]", address));
        nvps.add(new BasicNameValuePair("action", "compose"));

        HttpPost post = new HttpPost(URL);
        post.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        HttpResponse response = this.session.getHttpClient().execute(post);
        String body = EntityUtils.toString(response.getEntity());
        new FacebookJsonResponse(this.session, body);
    }

    public String getUid()
    {
        return uid;
    }

    public void setUid(String uid)
    {
        this.uid = uid;
    }

    public String getSubject()
    {
        return subject;
    }

    public void setSubject(String subject)
    {
        this.subject = subject;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
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
