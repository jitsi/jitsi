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
#include "Logger.h"
#include <tchar.h>
#include <cstdio>

#define LOGGER_DATE_STRING_LENGTH 25


/**
 * Constructs new Logger object.
 * @param pLogFile the filename of the log file.
 * @param pLogPath the path of the log file.
 */
Logger::Logger(LPCTSTR pLogFile, LPCTSTR pLogPath, int pLogLevel)
{
    logLevel = pLogLevel;
    canWriteInFile = false;
    if(pLogPath != nullptr && _tcslen(pLogPath) != 0)
    {
        logPath = new TCHAR[(_tcslen(pLogPath) + 1)];
        memcpy(logPath, pLogPath, _tcslen(pLogPath) + 1);
        if(pLogFile != nullptr && _tcslen(pLogFile) != 0)
        {
            logFile = new TCHAR[(_tcslen(pLogPath) + _tcslen(pLogFile) + 1)];
            _stprintf(logFile, _T("%s%s"), pLogPath, pLogFile);
            file = _tfsopen(logFile, _T("w"), _SH_DENYNO);
            canWriteInFile = file != nullptr;
        }
    }

    if(!canWriteInFile)
    {
        if (logPath)
        {
            delete[] logPath;
            logPath = nullptr;
        }

        if (logFile)
        {
            delete[] logFile;
            logFile = nullptr;
        }

        file = nullptr;
    }
}

Logger::~Logger()
{
    if (logPath)
    {
        delete[] logPath;
        logPath = nullptr;
    }

    if (logFile)
    {
        delete[] logFile;
        logFile = nullptr;
    }

    if(canWriteInFile)
        fclose(file);
}

LPCTSTR Logger::getCurrentFile()
{
    return _T("");
}

/**
 * Returns current timestamp string.
 */
void Logger::getCurrentTimeString(LPTSTR dateString)
{
    SYSTEMTIME systemTime;
    GetSystemTime(&systemTime);
    _stprintf(dateString,_T("%u-%02u-%02u-%02u-%02u-%02u.%u"),
        systemTime.wYear,
        systemTime.wMonth,
        systemTime.wDay,
        systemTime.wHour,
        systemTime.wMinute,
        systemTime.wSecond,
        systemTime.wMilliseconds);
}

/**
 * Logs a message
 * @param message the message.
 */
void Logger::log(LPCTSTR message)
{
    if(canWriteInFile && logLevel >= LOGGER_LEVEL_TRACE)
    {
        auto dateString = new TCHAR[LOGGER_DATE_STRING_LENGTH];
        getCurrentTimeString(dateString);
        _ftprintf(file, _T("%s %s: %s\n"), dateString, getCurrentFile(), message);
        fflush(file);
        delete[] dateString;
    }
}

/**
 * Logs a message
 * @param message the message.
 */
void Logger::logInfo(LPCTSTR message)
{
    if(canWriteInFile && logLevel >= LOGGER_LEVEL_INFO )
    {
        auto dateString = new TCHAR[LOGGER_DATE_STRING_LENGTH];
        getCurrentTimeString(dateString);
        _ftprintf(file, _T("%s %s: %s\n"), dateString, getCurrentFile(), message);
        fflush(file);
        delete[] dateString;
    }
}

/**
 * Returns the path of the log file.
 */
LPCTSTR Logger::getLogPath()
{
    return logPath;
}

/**
 * Returns the current log level.
 */
int Logger::getLogLevel() const
{
    return logLevel;
}


