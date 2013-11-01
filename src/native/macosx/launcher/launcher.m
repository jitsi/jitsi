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
#define JVM_RUNTIME_KEY "JVMRuntime"
#define JVM_MAIN_CLASS_NAME_KEY "MainClass"
#define JVM_CLASSPATH_KEY "ClassPath"
#define JVM_OPTIONS_KEY "VMOptions"
#define JVM_PROPERTIES_KEY "Properties"

#define LAUNCH_ERROR "LaunchError"

#define APP_PACKAGE_PREFIX "$APP_PACKAGE"
#define JAVA_ROOT_PREFIX "$JAVAROOT"

#define LIBJLI_DYLIB "/Library/Internet Plug-Ins/JavaAppletPlugin.plugin/Contents/Home/lib/jli/libjli.dylib"

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
    if (workingDirectory == nil)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
                      reason:@"Working directory not set"
                      userInfo:nil] raise];
    }
    else
    {
        chdir([workingDirectory UTF8String]);
    }

    // Locate the JLI_Launch() function
    NSString *runtime = [javaDictionary objectForKey:@JVM_RUNTIME_KEY];

    const char *libjliPath = NULL;
    if (runtime != nil)
    {
        NSString *runtimePath =
            [[[NSBundle mainBundle] builtInPlugInsPath]
                stringByAppendingPathComponent:runtime];
        libjliPath =
            [[runtimePath stringByAppendingPathComponent:@"Contents/Home/jre/lib/jli/libjli.dylib"]
                          fileSystemRepresentation];
    } else {
        libjliPath = LIBJLI_DYLIB;
    }

    void *libJLI = dlopen(libjliPath, RTLD_LAZY);

    // if jre folder is deleted
    if(libJLI == NULL)
    {
        libJLI = dlopen(LIBJLI_DYLIB, RTLD_LAZY);
    }

    JLI_Launch_t jli_LaunchFxnPtr = NULL;
    if (libJLI != NULL)
    {
        jli_LaunchFxnPtr = dlsym(libJLI, "JLI_Launch");
    }

    if (jli_LaunchFxnPtr == NULL)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
            reason:@"JRE load error"
            userInfo:nil] raise];
    }

    NSString *mainClassName =
        [javaDictionary objectForKey:@JVM_MAIN_CLASS_NAME_KEY];
    if (mainClassName == nil)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
            reason:@"Missing main class name"
            userInfo:nil] raise];
    }

    NSMutableString *classPath =
        [NSMutableString stringWithFormat:@"-Djava.class.path=%@", workingDirectory];

    NSArray *jvmcp = [javaDictionary objectForKey:@JVM_CLASSPATH_KEY];
    if (jvmcp == nil)
    {
        [[NSException exceptionWithName:@LAUNCH_ERROR
            reason:@"Missing class path entry"
            userInfo:nil] raise];
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
    if(options != nil)
        argc++;

    char *argv[argc + appArgc];

    int i = 0;
    argv[i++] = argMainValues[0];
    argv[i++] = strdup([classPath UTF8String]);

    if(options != nil)
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
                [sPropValue stringByReplacingOccurrencesOfString:@JAVA_ROOT_PREFIX
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

    NSString *pname = [infoDictionary objectForKey:@"CFBundleName"];

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
