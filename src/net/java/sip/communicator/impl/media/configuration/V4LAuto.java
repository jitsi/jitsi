/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 *
 * File based on:
 * @(#)V4LAuto.java	1.2 01/03/13
 * Copyright (c) 1999-2001 Sun Microsystems, Inc. All Rights Reserved.
 */
package net.java.sip.communicator.impl.media.configuration;

import java.util.Enumeration;
import java.util.Vector;

import javax.media.CaptureDeviceInfo;
import javax.media.CaptureDeviceManager;

import net.java.sip.communicator.util.Logger;

import com.sun.media.protocol.v4l.V4LDeviceQuery;

public class V4LAuto {
    
    private static final Logger logger = Logger.getLogger(V4LAuto.class);
    
    public V4LAuto() {
        Vector devices = (Vector) CaptureDeviceManager.getDeviceList(null).clone();
        Enumeration enumeration = devices.elements();
        while (enumeration.hasMoreElements()) {
            CaptureDeviceInfo cdi = (CaptureDeviceInfo) enumeration.nextElement();
            String name = cdi.getName();
            if (name.startsWith("v4l:"))
                CaptureDeviceManager.removeDevice(cdi);
        }
        
        autoDetect(0);
//        for (int i = 0; i < 10; i++) {	    
//            autoDetect(i);
//        }
    }
    
    protected CaptureDeviceInfo autoDetect(int cardNo) {
        CaptureDeviceInfo cdi = null;
        try {
            cdi = new V4LDeviceQuery(cardNo);
            if ( cdi != null && cdi.getFormats() != null &&
                    cdi.getFormats().length > 0) {
                // Commit it to disk. Its a new device
                if (CaptureDeviceManager.addDevice(cdi)) {
                    logger.info("Added device " + cdi);
                    CaptureDeviceManager.commit();
                }
                
            }
        } catch (Throwable t) {
            logger.error("Could not add device!", t);
            if (t instanceof ThreadDeath)
                throw (ThreadDeath)t;
        }
        
        return cdi;
    }
    
    public static void main(String [] args) {
        V4LAuto a = new V4LAuto();
        System.exit(0);
    }
}

