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

#include <jni.h>
#import <Cocoa/Cocoa.h>
#include <dlfcn.h>

#ifdef _JITSI_USE_1_6_
#define JVM_JAVA_KEY "Java"
#else
#define JVM_JAVA_KEY "Javax" // Cannot be Java
                             // or OSX requests the old Apple JVM
#endif

#define JVM_WORKING_DIR_KEY "WorkingDirectory"
#define JVM_MAIN_CLASS_NAME_KEY "MainClass"
#define JVM_CLASSPATH_KEY "ClassPath"
#define JVM_OPTIONS_KEY "VMOptions"
#define JVM_PROPERTIES_KEY "Properties"

#define LAUNCH_ERROR "LaunchError"

#define APP_PACKAGE_PREFIX "$APP_PACKAGE"
#define JAVA_ROOT_PREFIX "$JAVAROOT"

typedef int (JNICALL *JLI_Launch_t)(int argc, char ** argv,
                                    int jargc, const char** jargv,
                                    int appclassc, const char** appclassv,
                                    const char* fullversion,
                                    const char* dotversion,
                                    const char* pname,
                                    const char* lname,
                                    jboolean javaargs,
                                    jboolean cpwildcard,
                                    jboolean javaw,
                                    jint ergo);

void launchJitsi(int, char **);

static int argsSupplied = 0;
// saving the original count of app arguments on the first main call
static int jargc;

int main(int argc, char *argv[])
{
    NSAutoreleasePool *pool = [[NSAutoreleasePool alloc] init];

    // on the first call save the arguments count, main will be called several
    // times while starting some threads and initializing jvm
    if(argsSupplied == 0)
    {
        jargc = argc - 1;
    }

    @try
    {
        launchJitsi(argc, argv);
        return 0;
    } @catch (NSException *exception)
    {
        NSAlert *alert = [[NSAlert alloc] init];
        [alert setAlertStyle:NSCriticalAlertStyle];
        [alert setMessageText:[exception reason]];
        [alert runModal];

        return 1;
    } @finally
    {
        [pool drain];
    }
}

JLI_Launch_t getJLILaunch(NSString *parentPath)
{
    void *libJLI = NULL;

    NSDictionary *paths = @{
        @"Contents/Libraries/libjli.jnilib"
            : @"Contents/Libraries/libsplashscreen.jnilib",
        @"Contents/Home/jre/lib/jli/libjli.dylib"
            : @"Contents/Home/jre/lib/libsplashscreen.dylib",
        @"Contents/Home/lib/jli/libjli.dylib"
            : @"Contents/Home/lib/libsplashscreen.dylib"
    };

    for (NSString *path in paths)
    {
        const char *libjliPath =
            [[parentPath stringByAppendingPathComponent:path]
                           fileSystemRepresentation];

        libJLI = dlopen(libjliPath, RTLD_LAZY);

        if(libJLI != NULL)
            break;
    }
    // if jre folder is deleted
    if(libJLI == NULL)
        return NULL;

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if (libJLI != NULL)
    {
        jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
    }

    return jli_LaunchFxnPtr;
}

BOOL satisfies(NSString *vmVersion, NSString *requiredVersion)
{
    if ([requiredVersion hasSuffix:@"+"])
    {
        requiredVersion =
            [requiredVersion substringToIndex:[requiredVersion length] - 1];
        return [requiredVersion compare:vmVersion options:NSNumericSearch] <= 0;
    }

    if ([requiredVersion hasSuffix:@"*"])
    {
        requiredVersion =
            [requiredVersion substringToIndex:[requiredVersion length] - 1];
    }

    return [vmVersion hasPrefix:requiredVersion];
}

JLI_Launch_t getLauncher(NSDictionary *javaDictionary)
{
    // lets find all jre/jdk we can discover and use the preferred one
    // will search for environment variable JITSI_JRE
    NSString *required =
        [javaDictionary valueForKey:@"JVMVersion"];
    if(required == NULL)
        required = @"1.7*";

    NSString *overridenJVM =
        [[[NSProcessInfo processInfo] environment] objectForKey:@"JITSI_JRE"];

    if (overridenJVM != NULL)
    {
        JLI_Launch_t jli_LaunchFxnPtr = getJLILaunch(overridenJVM);

        if(jli_LaunchFxnPtr != NULL)
            return jli_LaunchFxnPtr;
    }

    // first test builtInPlugins folder, if we have embedded one we want it
    NSError *error = nil;
    NSString *pluginsFolder = [[NSBundle mainBundle] builtInPlugInsPath];
    NSArray *pluginsFolderContents = [[NSFileManager defaultManager]
        contentsOfDirectoryAtPath:pluginsFolder error:&error];
    if (pluginsFolderContents != nil)
    {
        for (NSString *pFolderName in pluginsFolderContents)
        {
            NSString *bundlePath
                = [pluginsFolder stringByAppendingPathComponent:pFolderName];

            if ([pFolderName hasSuffix:@".jdk"]
                || [pFolderName hasSuffix:@".jre"])
            {
                NSBundle *bundle = [NSBundle bundleWithPath:bundlePath];

                NSDictionary *jdict =
                    [bundle.infoDictionary valueForKey:@"JavaVM"];

                if(jdict == NULL)
                    continue;

                NSString *jvmVersion = [jdict valueForKey:@"JVMVersion"];

                if (jvmVersion == NULL
                    || !satisfies(jvmVersion, required))
                    continue;

                JLI_Launch_t jli_LaunchFxnPtr = getJLILaunch(bundlePath);

                if(jli_LaunchFxnPtr != NULL)
                {
                    return jli_LaunchFxnPtr;
                }
            }
        }
    }

    // Now let's try some other common locations
    NSMutableDictionary *foundVersions = [NSMutableDictionary new];
    for (NSString *jvmPath
            in @[@"Library/Java/JavaVirtualMachines",
                 @"/Library/Java/JavaVirtualMachines",
                 @"/System/Library/Java/JavaVirtualMachines",
                 @"/Library/Internet Plug-Ins/JavaAppletPlugin.plugin"])
    {

        NSArray *vms =
            [[NSFileManager defaultManager]
                contentsOfDirectoryAtPath:jvmPath error:&error];

        if (vms != nil)
        {
            for (NSString *vmFolderName in vms)
            {
                NSString *bundlePath =
                    [jvmPath stringByAppendingPathComponent:vmFolderName];

                if ([vmFolderName hasSuffix:@".jdk"]
                    || [vmFolderName hasSuffix:@".jre"])
                {
                    NSBundle *bundle = [NSBundle bundleWithPath:bundlePath];

                    NSDictionary *jdict =
                        [bundle.infoDictionary valueForKey:@"JavaVM"];

                    if(jdict == NULL)
                        continue;

                    NSString *jvmVersion = [jdict valueForKey:@"JVMVersion"];

                    if (jvmVersion == NULL
                        || !satisfies(jvmVersion, required))
                        continue;

                    [foundVersions setObject:bundlePath forKey:jvmVersion];
                }
            }
        }
    }

    NSArray *sortedKeys =
        [foundVersions keysSortedByValueUsingComparator:
           ^NSComparisonResult(id obj1, id obj2) {
               return [obj2 compare:obj1];
           }];

    for (id key in sortedKeys)
    {
        JLI_Launch_t jli_LaunchFxnPtr =
            getJLILaunch([foundVersions objectForKey:key]);

        if(jli_LaunchFxnPtr != NULL)
        {
            return jli_LaunchFxnPtr;
        }
    }

    return NULL;
}

void launchJitsi(int argMainCount, char *argMainValues[])
{
    // special psn args that we will skip,
    // those args are added when application is started from finder
    int psnArgsCount = 0;
    for(int i = 0; i < argMainCount; i++)
    {
        if(memcmp(argMainValues[i], "-psn_", 4) == 0)
            psnArgsCount++;
    }

    NSBundle *mainBundle = [NSBundle mainBundle];

    NSDictionary *infoDictionary = [mainBundle infoDictionary];

    // The java options
    NSDictionary *javaDictionary = [infoDictionary objectForKey:@JVM_JAVA_KEY];

    // Get the working directory options
    NSString *workingDirectory =
        [[javaDictionary objectForKey:@JVM_WORKING_DIR_KEY]
            stringByReplacingOccurrencesOfString:@APP_PACKAGE_PREFIX
            withString:[mainBundle bundlePath]];
    if (workingDirectory == NULL)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
                      reason:@"Working directory not set"
                      userInfo:NULL] raise];
    }
    else
    {
        chdir([workingDirectory UTF8String]);
    }

    JLI_Launch_t jli_LaunchFxnPtr = getLauncher(javaDictionary);

    NSString *pname = [infoDictionary objectForKey:@"CFBundleName"];

    if(jli_LaunchFxnPtr == NULL)
    {
        NSString *oldLauncher =
            [NSMutableString stringWithFormat:@"%@/Contents/MacOS/%@_OldLauncher",
                                        [mainBundle bundlePath], pname];

        execv([oldLauncher fileSystemRepresentation], argMainValues);

        NSLog(@"No java found!");
        exit(-1);
    }

    NSString *mainClassName =
        [javaDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == NULL)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
            reason:@"Missing main class name"
            userInfo:NULL] raise];
    }

    NSMutableString *classPath = [NSMutableString
        stringWithFormat:@"-Djava.class.path=%@", workingDirectory];

    NSArray *jvmcp = [javaDictionary objectForKey:@JVM_CLASSPATH_KEY];
    if (jvmcp == NULL)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
            reason:@"Missing class path entry"
            userInfo:NULL] raise];
    }
    else
    {
        for (NSString *cpe in jvmcp)
        {
            [classPath appendFormat:@":%@",
                [cpe stringByReplacingOccurrencesOfString:@JAVA_ROOT_PREFIX
                     withString:workingDirectory]];
        }
    }

    // Get the VM options
    NSString *optionsStrValue = [javaDictionary objectForKey:@JVM_OPTIONS_KEY];
    NSArray *options = NULL;
    if(optionsStrValue != NULL)
    {
        optionsStrValue = [optionsStrValue
            stringByReplacingOccurrencesOfString:@JAVA_ROOT_PREFIX
            withString:workingDirectory];
        optionsStrValue = [optionsStrValue stringByTrimmingCharactersInSet:
            [NSCharacterSet whitespaceAndNewlineCharacterSet]];
        options = [optionsStrValue componentsSeparatedByString:@" "];
    }

    // Get the system properties
    NSDictionary *sprops = [javaDictionary objectForKey:@JVM_PROPERTIES_KEY];

    // application arguments to be added, add them on first and second call
    // of the main, the first one starts the second one in new thread
    // no further calls
    int appArgc = 0;
    if(argsSupplied < 2 && jargc > 0)
        appArgc = jargc;

    // Initialize the arguments to JLI_Launch()
    // 2 for argMainValues and classPath
    // 1 + 1 - the dock.name + mainclass
    int argc = 2 + [sprops count] + 1 + 1 + appArgc - psnArgsCount;
    if(options != NULL)
        argc += [options count];

    char *argv[argc];

    int i = 0;
    argv[i++] = argMainValues[0];
    argv[i++] = strdup([classPath UTF8String]);

    if(options != NULL)
    {
        for ( NSString *op in options)
        {
            argv[i++] = strdup([op UTF8String]);
        }
    }

    for( NSString *sPropKey in sprops )
    {
        NSString *sPropValue = [sprops objectForKey:sPropKey];
        sPropValue = [sPropValue stringByTrimmingCharactersInSet:
                        [NSCharacterSet whitespaceAndNewlineCharacterSet]];
        argv[i++] = strdup(
            [[NSMutableString stringWithFormat:@"-D%@=%@", sPropKey,
                [sPropValue
                    stringByReplacingOccurrencesOfString:@JAVA_ROOT_PREFIX
                    withString:workingDirectory]]
            UTF8String]);
    }

    argv[i++] = strdup(
        [[NSString stringWithFormat:@"-Xdock:name=%@", pname] UTF8String]);

    argv[i++] = strdup([mainClassName UTF8String]);

    // copy the application parameters, the number we have saved
    // the params are last in the array of arguments
    for(int j = appArgc; j > 0; j--)
    {
        // skip -psn args
        if (memcmp(argMainValues[argMainCount-j], "-psn_", 4) != 0)
            argv[i++] = strdup(argMainValues[argMainCount-j]);
    }
    argsSupplied++;

    // once psn args are filtered, no more count them as they will not be
    // supplied to the launch function
    jargc = jargc - psnArgsCount;

    // Invoke JLI_Launch()
    jli_LaunchFxnPtr(argc, argv,
                     0, NULL,
                     0, NULL,
                     "",
                     "",
                     [pname UTF8String],
                     [pname UTF8String],
                     FALSE,
                     FALSE,
                     FALSE,
                     0);
}
