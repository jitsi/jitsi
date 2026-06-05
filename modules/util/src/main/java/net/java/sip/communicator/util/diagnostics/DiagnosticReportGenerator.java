package net.java.sip.communicator.util.diagnostics;

import java.io.*;
import java.util.*;
import java.util.zip.*;

public class DiagnosticReportGenerator
{
    private final Map<String, byte[]> extraEntries = new LinkedHashMap<>();

    public void addTextEntry(String path, String content)
    {
        if (path == null || content == null)
            return;
        extraEntries.put(path, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    /**
     * Adds a binary entry to the report (for example an image).
     */
    public void addBinaryEntry(String path, byte[] data)
    {
        if (path == null || data == null)
            return;
        extraEntries.put(path, data);
    }

    public File generate(File destination, DiagnosticReportOptions options) throws IOException
    {
        if (destination == null)
            throw new IllegalArgumentException("destination");

        File parent = destination.getParentFile();
        if (parent != null && !parent.exists())
            parent.mkdirs();

        try (FileOutputStream fos = new FileOutputStream(destination);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             ZipOutputStream zos = new ZipOutputStream(bos))
        {
            // application-focused info (avoid including personal PC-identifiers)
            StringBuilder sys = new StringBuilder();
            sys.append("application.name=Jitsi").append('\n');
            // try to include implementation version when available
            String implVersion = DiagnosticReportGenerator.class.getPackage() != null
                ? DiagnosticReportGenerator.class.getPackage().getImplementationVersion()
                : null;
            if (implVersion == null)
                implVersion = System.getProperty("jitsi.version");
            if (implVersion != null)
                sys.append("application.version=").append(implVersion).append('\n');
            // minimal runtime info useful for debugging but not personal data
            sys.append("java.version=").append(System.getProperty("java.version")).append('\n');
            Runtime rt = Runtime.getRuntime();
            sys.append("availableProcessors=").append(rt.availableProcessors()).append('\n');
            sys.append("maxMemory=").append(rt.maxMemory()).append('\n');

            putEntry(zos, "application/jitsi-system.txt", sys.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            // thread dump
            if (options != null && options.isIncludeThreadDump())
            {
                StringBuilder td = new StringBuilder();
                Map<Thread, StackTraceElement[]> traces = Thread.getAllStackTraces();
                // Raw dump kept for experts
                for (Map.Entry<Thread, StackTraceElement[]> e : traces.entrySet())
                {
                    Thread t = e.getKey();
                    td.append("Thread: ").append(t.getName()).append(" id=").append(t.getId()).append(" state=").append(t.getState()).append('\n');
                    for (StackTraceElement st : e.getValue())
                    {
                        td.append('\t').append(st.toString()).append('\n');
                    }
                    td.append('\n');
                }
                putEntry(zos, "thread-dump/thread-dump.txt", td.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

                // Build a user-friendly thread summary
                StringBuilder friendly = new StringBuilder();
                friendly.append("Resumen del estado de hilos (versión amigable)\n");
                friendly.append("-----------------------------------------\n");
                int blocked = 0, waiting = 0, timed = 0, runnable = 0;
                for (Map.Entry<Thread, StackTraceElement[]> e : traces.entrySet())
                {
                    Thread t = e.getKey();
                    switch (t.getState())
                    {
                        case BLOCKED: blocked++; break;
                        case WAITING: waiting++; break;
                        case TIMED_WAITING: timed++; break;
                        case RUNNABLE: runnable++; break;
                        default: break;
                    }
                }
                friendly.append("Hilos en ejecución: ").append(runnable).append('\n');
                friendly.append("Hilos en espera: ").append(waiting).append('\n');
                friendly.append("Hilos en espera con timeout: ").append(timed).append('\n');
                friendly.append("Hilos bloqueados: ").append(blocked).append('\n');

                // Look for common user-facing problems: blocked event dispatch thread or many blocked threads
                Thread edt = null;
                for (Thread t : traces.keySet())
                {
                    if ("AWT-EventQueue-0".equals(t.getName())) { edt = t; break; }
                }
                if (edt != null && edt.getState() != Thread.State.RUNNABLE)
                {
                    friendly.append("Atención: la interfaz podría no responder (Event Dispatch Thread no está en RUNNABLE).\n");
                }

                // Add very-high-level user friendly suggestions
                friendly.append('\n');
                friendly.append("Sugerencias:").append('\n');
                if (blocked > 3)
                    friendly.append(" - Se detectaron múltiples hilos bloqueados. Esto puede indicar contenido bloqueante o problemas de E/S.\n");
                if (waiting + timed > 20)
                    friendly.append(" - Hay muchos hilos en espera; puede ser carga de fondo alta.\n");
                friendly.append(" - Si la aplicación no responde, intenta reiniciarla y reproduce el problema.\n");

                putEntry(zos, "report/thread-summary.txt", friendly.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            // extra entries provided by caller (bundles list etc)
            for (Map.Entry<String, byte[]> en : extraEntries.entrySet())
            {
                putEntry(zos, en.getKey(), en.getValue());
            }

            StringBuilder issuesSummary = new StringBuilder();

            // placeholder for logs/config: for now copy known files from user dirs
            if (options != null && options.isIncludeLogs())
            {
                // Try common locations
                List<File> candidates = new ArrayList<>();
                String userHome = System.getProperty("user.home");
                candidates.add(new File(userHome, "AppData\\Roaming\\jitsi\\logs"));
                candidates.add(new File(userHome, "AppData\\Local\\jitsi\\logs"));
                candidates.add(new File(userHome, "jitsi.log"));
                for (File c : candidates)
                {
                    if (c.exists())
                    {
                        if (c.isDirectory())
                        {
                            for (File f : c.listFiles())
                            {
                                if (f.isFile())
                                {
                                    addFileToZip(zos, f, "logs/" + f.getName(), options != null && options.isRedactSensitive());
                                    scanFileForIssues(f, issuesSummary);
                                }
                            }
                        }
                        else if (c.isFile())
                        {
                            addFileToZip(zos, c, "logs/" + c.getName(), options != null && options.isRedactSensitive());
                        }
                    }
                }
            }

            if (options != null && options.isIncludeConfig())
            {
                // copy a small set of config files if present
                String userHome = System.getProperty("user.home");
                File cfgDir = new File(userHome, "AppData\\Roaming\\jitsi");
                if (cfgDir.exists() && cfgDir.isDirectory())
                {
                    for (String name : new String[] {"accounts.properties", "jitsi-defaults.properties", "logback.xml"})
                    {
                        File f = new File(cfgDir, name);
                        if (f.exists() && f.isFile())
                        {
                            addFileToZip(zos, f, "config/" + f.getName(), options != null && options.isRedactSensitive());
                            scanFileForIssues(f, issuesSummary);
                        }
                    }
                }
            }

            // screenshot
            if (options != null && options.isIncludeScreenshot())
            {
                try
                {
                    java.awt.Robot r = new java.awt.Robot();
                    java.awt.Rectangle screen = new java.awt.Rectangle(java.awt.Toolkit.getDefaultToolkit().getScreenSize());
                    java.awt.image.BufferedImage img = r.createScreenCapture(screen);
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    javax.imageio.ImageIO.write(img, "png", baos);
                    putEntry(zos, "screenshot/screenshot.png", baos.toByteArray());
                }
                catch (Throwable t)
                {
                    // ignore screenshot errors
                }
            }

            // Build a short friendly summary for users
            StringBuilder summary = new StringBuilder();
            summary.append("Reporte de diagnóstico de Jitsi\n");
            summary.append("================================\n");
            summary.append("Incluye:\n");
            summary.append(" - Información de la aplicación (application/jitsi-system.txt)\n");
            if (options != null && options.isIncludeThreadDump())
                summary.append(" - Volcado de hilos (thread-dump/thread-dump.txt) y resumen amigable (report/thread-summary.txt)\n");
            if (options != null && options.isIncludeLogs())
                summary.append(" - Logs encontrados (logs/)\n");
            if (options != null && options.isIncludeConfig())
                summary.append(" - Archivos de configuración (config/)\n");
            if (options != null && options.isIncludeScreenshot())
                summary.append(" - Captura de la ventana de Jitsi (screenshot/jitsi-window.png)\n");
            summary.append('\n');
            summary.append("Notas para el usuario:\n");
            summary.append(" - Este archivo contiene información sobre la instalación de Jitsi y algunos datos técnicos útiles para soporte.\n");
            summary.append(" - No se incluyen datos personales identificables como su nombre de usuario del sistema.\n");
            summary.append(" - Los logs muy grandes han sido truncados a 200KB e identificados con [TRUNCATED].\n");
            summary.append(" - Si quieres más detalles, adjunta este ZIP al reportar un bug o compártelo con el soporte.\n");

            putEntry(zos, "report/summary.txt", summary.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));

            if (issuesSummary.length() > 0)
            {
                String header = "Resumen de problemas detectados (heurístico):\n\n";
                putEntry(zos, "report/issues-summary.txt", (header + issuesSummary.toString()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }

            zos.finish();
        }

        return destination;
    }

    private void putEntry(ZipOutputStream zos, String path, byte[] data) throws IOException
    {
        ZipEntry ze = new ZipEntry(path);
        zos.putNextEntry(ze);
        zos.write(data);
        zos.closeEntry();
    }

    private static final int MAX_BYTES_PER_FILE = 200 * 1024; // 200 KB

    private void addFileToZip(ZipOutputStream zos, File f, String entryName, boolean redact) throws IOException
    {
        // Decide if this is a text file we should attempt to redact
        String lname = f.getName().toLowerCase(Locale.ROOT);
        boolean likelyText = lname.endsWith(".log") || lname.endsWith(".txt") || lname.endsWith(".properties")
            || lname.endsWith(".xml") || lname.endsWith(".json") || lname.endsWith(".conf") || lname.endsWith(".yml") || lname.endsWith(".ini");

        if (likelyText)
        {
            // read last MAX_BYTES_PER_FILE bytes
            byte[] data = readLastBytes(f, MAX_BYTES_PER_FILE);

            // If data is not valid/mostly-text, fall back to binary handling
            if (!isProbablyText(data))
            {
                // treat as binary to avoid dumping gibberish into text files
                try (RandomAccessFile raf = new RandomAccessFile(f, "r"))
                {
                    long len = raf.length();
                    long toRead = Math.min(len, MAX_BYTES_PER_FILE);
                    byte[] buf = new byte[(int) toRead];
                    raf.seek(Math.max(0, len - toRead));
                    raf.readFully(buf);
                    if (len > toRead)
                    {
                        String info = "[TRUNCATED-BINARY] original_size=" + len + " bytes; included_last=" + toRead + " bytes\n";
                        putEntry(zos, entryName + ".readme.txt", info.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                    putEntry(zos, entryName, buf);
                }
                return;
            }

            String text = new String(data, java.nio.charset.StandardCharsets.UTF_8);
            boolean truncated = f.length() > data.length;

            if (redact)
                text = redactText(text);

            StringBuilder sb = new StringBuilder();
            if (truncated)
            {
                sb.append("[TRUNCATED] original_size=").append(f.length()).append(" bytes; included_last=").append(data.length).append(" bytes\n\n");
            }
            sb.append(text);
            putEntry(zos, entryName, sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        else
        {
            // binary or unknown: add up to MAX_BYTES_PER_FILE from the end (useful for huge files)
            try (RandomAccessFile raf = new RandomAccessFile(f, "r"))
            {
                long len = raf.length();
                long toRead = Math.min(len, MAX_BYTES_PER_FILE);
                byte[] buf = new byte[(int) toRead];
                raf.seek(Math.max(0, len - toRead));
                raf.readFully(buf);
                // If the file is larger, prefix a small info header as a separate entry
                if (len > toRead)
                {
                    String info = "[TRUNCATED-BINARY] original_size=" + len + " bytes; included_last=" + toRead + " bytes\n";
                    putEntry(zos, entryName + ".readme.txt", info.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                putEntry(zos, entryName, buf);
            }
        }
    }

    private byte[] readLastBytes(File f, int max) throws IOException
    {
        try (RandomAccessFile raf = new RandomAccessFile(f, "r"))
        {
            long len = raf.length();
            int toRead = (int) Math.min(len, max);
            byte[] buf = new byte[toRead];
            raf.seek(Math.max(0, len - toRead));
            raf.readFully(buf);
            return buf;
        }
    }

    private String redactText(String s)
    {
        if (s == null || s.length() == 0)
            return s;

        String out = s;

        // redact email addresses
        out = out.replaceAll("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}", "[REDACTED_EMAIL]");

        // redact common key/value patterns like password=..., passwd=..., token=..., secret=...
        out = out.replaceAll("(?i)(password|passwd|pwd|secret|token|apikey|api_key)\\s*[=:]\\s*[^\\s&\\r\\n]{4,}", "$1=[REDACTED]");

        // redact Authorization Bearer tokens
        out = out.replaceAll("(?i)Authorization:\\s*Bearer\\s+[A-Za-z0-9-._~+/=]{8,}", "Authorization: Bearer [REDACTED]");

        // redact long alphanumeric sequences that look like keys (>=20 chars)
        out = out.replaceAll("\\b[A-Za-z0-9_-]{20,}\\b", "[REDACTED_KEY]");

        return out;
    }

    /**
     * Heuristic: returns true if the byte array looks like UTF-8 text with a high
     * proportion of printable characters. Protects from decoding binary files as text.
     */
    private boolean isProbablyText(byte[] data)
    {
        if (data == null || data.length == 0) return true;

        // Quick check: try to decode as UTF-8 without throwing
        String decoded;
        try
        {
            decoded = new String(data, java.nio.charset.StandardCharsets.UTF_8);
        }
        catch (Throwable t)
        {
            return false;
        }

        int printable = 0;
        int total = Math.min(decoded.length(), 4096); // sample up to first chars
        for (int i = 0; i < total; i++)
        {
            char c = decoded.charAt(i);
            if (c == '\n' || c == '\r' || c == '\t') { printable++; continue; }
            if (c >= 0x20 && c <= 0x7E) printable++; // basic printable ASCII
            else if (Character.isLetterOrDigit(c) || Character.isSpaceChar(c) || Character.isIdeographic(c)) printable++;
        }

        // If less than 60% printable characters, treat as binary
        if (total == 0) return false;
        return ((double) printable / (double) total) > 0.60;
    }

    /**
     * Scan the tail of a file for common error patterns and append friendly messages to issuesSummary.
     */
    private void scanFileForIssues(File f, StringBuilder issuesSummary)
    {
        try
        {
            byte[] b = readLastBytes(f, 64 * 1024); // 64KB
            String txt = new String(b, java.nio.charset.StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);

            // look for generic error lines
            List<String> lines = Arrays.asList(txt.split("[\r\n]+"));
            for (String L : lines)
            {
                if (L.contains("exception") || L.contains("error") || L.contains("failed") || L.contains("unable to"))
                {
                    String friendly = null;
                    if (L.contains("account") || L.contains("auth") || L.contains("login"))
                        friendly = " - Problema relacionado con cuentas/autenticación detectado en logs: '" + summarizeLine(L) + "'\n";
                    else if (L.contains("audio") || L.contains("mic") || L.contains("playback") )
                        friendly = " - Posible problema de audio detectado: '" + summarizeLine(L) + "'\n";
                    else if (L.contains("network") || L.contains("dns") || L.contains("socket"))
                        friendly = " - Posible problema de red detectado: '" + summarizeLine(L) + "'\n";
                    else
                        friendly = " - Mensaje de error detectado: '" + summarizeLine(L) + "'\n";

                    issuesSummary.append(friendly);
                    // limit to avoid huge summaries
                    if (issuesSummary.length() > 8 * 1024) break;
                }
            }
        }
        catch (Throwable ignore) { }
    }

    private String summarizeLine(String l)
    {
        if (l == null) return "";
        String s = l.trim();
        if (s.length() > 200) s = s.substring(0, 200) + "...";
        return s.replaceAll("[\\\"]", "");
    }
}
