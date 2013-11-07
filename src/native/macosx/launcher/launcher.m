/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

#include <jni.h>
#import <Cocoa/Cocoa.h>
#include <dlfcn.h>

#define JVM_JAVA_KEY "Java"
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
    for (NSString *path
            in @[@"Contents/Libraries/libjli.jnilib",
                 @"Contents/Home/jre/lib/jli/libjli.dylib",
                 @"Contents/Home/lib/jli/libjli.dylib"])
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

    for (NSString *jvmPath
            in @[[[NSBundle mainBundle] builtInPlugInsPath],
                 @"Library/Java/JavaVirtualMachines",
                 @"/Library/Java/JavaVirtualMachines",
                 @"/System/Library/Java/JavaVirtualMachines",
                 @"/Library/Internet Plug-Ins/JavaAppletPlugin.plugin"])
    {
        NSError *error = nil;
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
                }

                JLI_Launch_t jli_LaunchFxnPtr = getJLILaunch(bundlePath);

                if(jli_LaunchFxnPtr != NULL)
                {
                    return jli_LaunchFxnPtr;
                }
            }
        }
    }

    return NULL;
}

void launchJitsi(int argMainCount, char *argMainValues[])
{
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
            [NSMutableString stringWithFormat:@"%@/Contents/MacOS/%@_Launcher",
                                        [mainBundle bundlePath], pname];

        execv([oldLauncher fileSystemRepresentation], argMainValues);

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
    NSString *options = [javaDictionary objectForKey:@JVM_OPTIONS_KEY];

    // Get the system properties
    NSDictionary *sprops = [javaDictionary objectForKey:@JVM_PROPERTIES_KEY];

    // application arguments to be added, add them on first and second call
    // of the main, the first one starts the second one in new thread
    // no further calls
    int appArgc = 0;
    if(argsSupplied < 2 && jargc > 0)
        appArgc = jargc;

    // Initialize the arguments to JLI_Launch()
    int argc = 2 + [sprops count] + 1 + appArgc;
    if(options != NULL)
        argc++;

    char *argv[argc + appArgc];

    int i = 0;
    argv[i++] = argMainValues[0];
    argv[i++] = strdup([classPath UTF8String]);

    if(options != NULL)
    {
        NSString *op =
            [options stringByReplacingOccurrencesOfString:@JAVA_ROOT_PREFIX
                    withString:workingDirectory];
        op = [op stringByTrimmingCharactersInSet:
                    [NSCharacterSet whitespaceAndNewlineCharacterSet]];
        argv[i++] = strdup([op UTF8String]);
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

    argv[i++] = strdup([mainClassName UTF8String]);

    // copy the application parameters, the number we have saved
    // the params are last in the array of arguments
    for(int j = appArgc; j > 0; j--)
    {
        argv[i++] = strdup(argMainValues[argMainCount-j]);
    }
    argsSupplied++;

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
