/*
 * SIP Communicator, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */
#include <windows.h>
#include <stdio.h>

int main(int argc, char **argv)
{
    // Local variables
    PROCESS_INFORMATION pi;
    STARTUPINFO si;

    // Argument count
    if (argc != 2)
        return EXIT_FAILURE;

    // Initialize
    memset(&si,0,sizeof(STARTUPINFO));
    si.cb = sizeof(STARTUPINFO);

    // Execute
    if(!CreateProcess(NULL, argv[1], NULL, NULL, FALSE, 0, NULL,NULL,&si,&pi)) {
        // Failed
        //cout << "Could not run the program." << endl;
        char msg[255];
        DWORD dwError = GetLastError();
        FormatMessage(FORMAT_MESSAGE_FROM_SYSTEM,0,dwError,0,msg,sizeof(msg),0);
        printf("Could not start installer %s\n", msg);
        return EXIT_FAILURE;
    }

    // Finished
    return EXIT_SUCCESS;
}
