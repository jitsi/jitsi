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
 */
public class FacebookUser
{
    /*{"listChanged":true,
    "availableCount":2,
    
    "nowAvailableList":
    {"1355527894":{"i":false},
        "1386786477":{"i":false}},
        
            "wasAvailableIDs":[],
            
            "userInfos":{
                "1355527894":
                {"name":"Dai Zhiwei",
                    "firstName":"Dai",
                    "thumbSrc":"http:\/\/profile.ak.facebook.com\/v225\/1132\/119\/q1355527894_6497.jpg",
                    "status":null,
                    "statusTime":0,
                    "statusTimeRel":""},
                    "1386786477":
                    {"name":"\u5341\u4e00",
                        "firstName":"\u4e00",
                        "thumbSrc":"http:\/\/static.ak.fbcdn.net\/pics\/q_silhouette.gif",
                        "status":null,
                        "statusTime":0,
                        "statusTimeRel":""},
                        "1190346972":
                        {"name":"David Willer",
                            "firstName":"David",
                            "thumbSrc":"http:\/\/profile.ak.facebook.com\/profile5\/54\/96\/q1190346972_3586.jpg",
                            "status":null,
                            "statusTime":0,
                            "statusTimeRel":""}},
                            
                            "forcedRender":true,
                            "flMode":false,
                            "flData":{}}*/
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
     * @param id
     * @param user
     * @throws JSONException
     */
    public FacebookUser(String id, JSONObject user)
        throws JSONException
    {
        uid = id;
        isIdle = true;//default status is idle
        name = (String) user.get("name");
        firstName = (String) user.get("firstName");
        thumbSrc = (String) user.get("thumbSrc");
        Object temp = user.get("status");
        if(!temp.equals(org.json.JSONObject.NULL))
            status = (String)temp;
        else
            status = "";
        statusTime = (Number) user.get("statusTime");
        statusTimeRel = (String) user.get("statusTimeRel");

        lastSeen = new Date().getTime();
        isOnline = true;
    }

    /**
     * Copy the parameter's data to "this"
     * 
     * @param src
     */
    public void copy(FacebookUser src)
    {
        if (src == null)
            return;
        this.uid = src.uid;
        this.isIdle = src.isIdle;
        this.name = src.name;
        this.firstName = src.firstName;
        this.thumbSrc = src.thumbSrc;
        this.status = src.status;
        this.statusTime = src.statusTime;
        this.statusTimeRel = src.statusTimeRel;
        this.lastSeen = src.lastSeen;
        this.isOnline = src.isOnline;        
    }
}