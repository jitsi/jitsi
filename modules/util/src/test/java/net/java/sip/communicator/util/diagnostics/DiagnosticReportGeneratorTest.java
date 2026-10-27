package net.java.sip.communicator.util.diagnostics;

import org.junit.*;
import java.io.*;
import java.util.zip.*;

public class DiagnosticReportGeneratorTest
{
    @Test
    public void testGenerateCreatesZip() throws Exception
    {
        File tmp = File.createTempFile("diag-test", ".zip");
        tmp.delete();
        DiagnosticReportGenerator gen = new DiagnosticReportGenerator();
        gen.addTextEntry("hello.txt", "world");
        DiagnosticReportOptions opts = new DiagnosticReportOptions();
        opts.setIncludeThreadDump(true);
        File out = gen.generate(tmp, opts);
        Assert.assertTrue(out.exists());

        // check that hello.txt is present
        boolean found = false;
        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(out)))
        {
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null)
            {
                if (ze.getName().equals("hello.txt"))
                {
                    found = true; break;
                }
            }
        }
        Assert.assertTrue("hello.txt should be present in zip", found);
        out.delete();
    }
}
