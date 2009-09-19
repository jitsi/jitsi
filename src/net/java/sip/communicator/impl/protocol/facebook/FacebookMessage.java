/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.util.*;

import org.json.*;

/**
 * Facebook meta message
 * 
 * @author Dai Zhiwei
 * @author Edgar Poce
 */
public class FacebookMessage
{
    private String text;

    private long time;

    private long clientTime;

    private String msgID;

    private String from;

    private String to;

    private String fromName;

    private String toName;

    private String fromFirstName;

    private String toFirstName;

    public FacebookMessage()
    {
    }

    public FacebookMessage(FacebookUser fromUser, JSONObject json)
        throws JSONException
    {
        this.from = json.getString("from");
        this.to = json.getString("to");
        this.time = json.getLong("time");
        this.clientTime = time;
        // some messages come without user time
        JSONObject msg = (JSONObject) json.get("msg");
        this.text = msg.getString("text");
        if (msg.has("msgID"))
        {
            this.msgID = msg.getString("msgID");
        }
        else
        {
            // assign a random if it's not included in the message
            this.msgID = UUID.randomUUID().toString();
        }

        if (!fromUser.uid.equals(this.from))
        {
            throw new IllegalArgumentException(
                    "the given message doesn't belong to the given user");
        }
        // envelope
        this.fromName = fromUser.name;
        this.fromFirstName = fromUser.firstName;
    }

    public FacebookMessage(JSONObject json)
        throws JSONException
    {
        // envelope
        this.from = json.getString("from");
        this.to = json.getString("to");
        this.fromName = json.getString("from_name");
        this.toName = json.getString("to_name");
        this.fromFirstName = json.getString("from_first_name");
        this.toFirstName = json.getString("to_first_name");

        // data
        JSONObject msg = (JSONObject) json.get("msg");
        this.text = msg.getString("text");
        this.time = msg.getLong("time");
        this.clientTime = msg.getLong("clientTime");
        this.msgID = msg.getString("msgID");
    }

    /**
     * Creat a facebook message with the given params.
     * 
     * @param txt
     *            message text
     * @param tm
     *            message received time(?)
     * @param ct
     *            client time(message sent time?)
     * @param id
     *            message id generated randomly.
     * @param f
     *            from uid
     * @param t
     *            to uid
     * @param fn
     *            from name
     * @param tn
     *            to name
     * @param ffn
     *            from first name
     * @param tfn
     *            to first name
     */
    public FacebookMessage(String txt, long tm, long ct, String id, String f,
            String t, String fn, String tn, String ffn, String tfn)
    {
        this.text = txt;
        this.time = tm;
        this.clientTime = ct;
        this.msgID = id;
        this.from = f;
        this.to = t;
        this.fromName = fn;
        this.toName = tn;
        this.fromFirstName = ffn;
        this.toFirstName = tfn;
    }

    public String getText()
    {
        return text;
    }

    public long getTime()
    {
        return time;
    }

    public long getClientTime()
    {
        return clientTime;
    }

    public String getMsgID()
    {
        return msgID;
    }

    public String getFrom()
    {
        return from;
    }

    public String getTo()
    {
        return to;
    }

    public String getFromName()
    {
        return fromName;
    }

    public String getToName()
    {
        return toName;
    }

    public String getFromFirstName()
    {
        return fromFirstName;
    }

    public String getToFirstName()
    {
        return toFirstName;
    }
}
