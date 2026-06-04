package net.java.sip.communicator.util.diagnostics;

public class DiagnosticReportOptions
{
    private boolean includeLogs = true;
    private boolean includeConfig = true;
    private boolean includeThreadDump = true;
    private boolean includeScreenshot = false;
    private boolean redactSensitive = true;

    public boolean isIncludeLogs() { return includeLogs; }
    public void setIncludeLogs(boolean includeLogs) { this.includeLogs = includeLogs; }
    public boolean isIncludeConfig() { return includeConfig; }
    public void setIncludeConfig(boolean includeConfig) { this.includeConfig = includeConfig; }
    public boolean isIncludeThreadDump() { return includeThreadDump; }
    public void setIncludeThreadDump(boolean includeThreadDump) { this.includeThreadDump = includeThreadDump; }
    public boolean isIncludeScreenshot() { return includeScreenshot; }
    public void setIncludeScreenshot(boolean includeScreenshot) { this.includeScreenshot = includeScreenshot; }
    public boolean isRedactSensitive() { return redactSensitive; }
    public void setRedactSensitive(boolean redactSensitive) { this.redactSensitive = redactSensitive; }
}
