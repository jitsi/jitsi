/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 * 
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.facebook;

import java.util.*;

import org.json.*;

/**
 * A data structure that store Facebook user's information
 * 
 * @author Dai Zhiwei
 * @author Edgar Poce
 */
public class FacebookUser
{
    public static String defaultThumbSrc = "http://static.ak.fbcdn.net/pics/q_silhouette.gif";

    public static String defaultAvatarSrc = "http://static.ak.fbcdn.net/pics/d_silhouette.gif";

    public String uid;

    public boolean isIdle;

    public String name;

    public String firstName;

    public String thumbSrc;

    public String status;

    public Number statusTime;

    public String statusTimeRel;

    public long lastSeen;

    public boolean isOnline;

    /**
     * Creat a facebook user according to the given id and JSONObject.
     * 
     * @param id
     * @param user
     * @throws JSONException
     */
    public FacebookUser(String id, JSONObject user) throws JSONException
    {
        uid = id;
        isIdle = true;// default status is idle
        name = (String) user.get("name");
        firstName = (String) user.get("firstName");
        thumbSrc = (String) user.get("thumbSrc");
        if (user.has("status"))
        {
            status = user.getString("status");
        }
        else
        {
            status = "";
        }
        if (user.has("statusTime"))
        {
            statusTime = (Number) user.get("statusTime");
        }
        else
        {
            statusTime = -1;
        }
        if (user.has("statusTimeRel"))
        {
            statusTimeRel = (String) user.get("statusTimeRel");
        }
        else
        {
            statusTimeRel = "";
        }
        lastSeen = new Date().getTime();
        isOnline = true;
    }

    @Override
    public String toString()
    {
        return "FacebookUser [firstName=" + firstName + ", isIdle=" + isIdle
                + ", isOnline=" + isOnline + ", lastSeen=" + lastSeen
                + ", name=" + name + ", status=" + status + ", statusTime="
                + statusTime + ", statusTimeRel=" + statusTimeRel
                + ", thumbSrc=" + thumbSrc + ", uid=" + uid + "]";
    }
}
