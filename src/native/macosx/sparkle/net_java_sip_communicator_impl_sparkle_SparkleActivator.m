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

/**
 * Init the Sparkle subsystem.
 *
 * To generate the .h file, compile SC, go to the 
 * classes/ directory, and execute:
 * javah -jni net.java.sip.communicator.impl.sparkle.SparkleActivator
 *
 * For compilation, this requires the Sparkle.framework 
 * installed in /Library/Frameworks/. This Framework is 
 * available at http://sparkle.andymatuschak.org/
 *
 * @author Romain Kuntz
 * @author Egidijus Jankauskas
 */

#include <Cocoa/Cocoa.h>
#include <Sparkle.h>
#include "net_java_sip_communicator_impl_sparkle_SparkleActivator.h"

/*
 * Class:     net_java_sip_communicator_impl_sparkle_SparkleActivator
 * Method:    initSparkle
 * Signature: (Ljava/lang/String;ZILjava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_net_java_sip_communicator_impl_sparkle_SparkleActivator_initSparkle
  (JNIEnv *env, jclass obj, jstring pathToSparkleFramework, 
   jboolean updateAtStartup, jint checkInterval, jstring downloadLink,
   jstring menuItemTitle)
{
    BOOL hasLaunchedBefore = [[NSUserDefaults standardUserDefaults] boolForKey:@"SCHasLaunchedBefore"];

    if(!hasLaunchedBefore)
    {
        [[NSUserDefaults standardUserDefaults] setBool:YES forKey:@"SCHasLaunchedBefore"];
        [[NSUserDefaults standardUserDefaults] synchronize];
    }

    // The below code was used to avoid to link the Sparkle framework
    // at comilation time. 
    //const char *path = (*env)->GetStringUTFChars(env, pathToSparkleFramework, 0); 
    //NSBundle* bundle = [NSBundle bundleWithPath:[NSString stringWithCString: path]];
    //Class suUpdaterClass = [bundle classNamed:@"SUUpdater"];
    //id suUpdater = [[suUpdaterClass alloc] init];
    //(*env)->ReleaseStringUTFChars(env, pathToSparkleFramework, path);

    SUUpdater *suUpdater = [SUUpdater updaterForBundle:[NSBundle mainBundle]];

    if(downloadLink)
    {
        const char* link = (*env)->GetStringUTFChars(env, downloadLink, 0);
        NSString* sLink = [NSString stringWithCString: link length: strlen(link)];
        NSURL* nsLink = [NSURL URLWithString: sLink];

        if(nsLink)
        {
            [suUpdater setFeedURL: nsLink];
        }
    }

    NSString* menuTitle;
    if(!menuItemTitle)
    {
        menuTitle = @"Check for Updates...";
    }
    else
    {
        const char* menuTitleChars =
            (const char *)(*env)->GetStringUTFChars(env, menuItemTitle, 0);
        menuTitle = [NSString stringWithUTF8String: menuTitleChars];
    }

    NSMenu* menu = [[NSApplication sharedApplication] mainMenu];
    NSMenu* applicationMenu = [[menu itemAtIndex:0] submenu];
    NSMenuItem* checkForUpdatesMenuItem = [[NSMenuItem alloc]
                                            initWithTitle:menuTitle
                                            action:@selector(checkForUpdates:)
                                            keyEquivalent:@""];

    [checkForUpdatesMenuItem setEnabled:YES];
    [checkForUpdatesMenuItem setTarget:suUpdater];

    // 0 => top, 1 => after "About..."
    [applicationMenu insertItem:checkForUpdatesMenuItem atIndex:1];

    // Update is launched only at the second startup
    if (hasLaunchedBefore && updateAtStartup == JNI_TRUE)
    {
        // This method needs to be executed on the main thread because it may result
        // in GUI showing up. Besides, Sparkle uses asynchronous URLConnection which
        // requires to be called from a thread which runs in a default run loop mode.
        [suUpdater performSelectorOnMainThread:@selector(checkForUpdatesInBackground)
                                withObject:nil 
                                waitUntilDone:NO];
    }
}
