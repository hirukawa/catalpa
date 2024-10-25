package onl.oss.catalpa;

import org.mozilla.universalchardet.UniversalDetector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PushbackInputStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class AutoDetectReader extends Reader {

    public static List<String> readAllLines(Path file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (InputStream in = Files.newInputStream(file)) {
            try (AutoDetectReader adr = new AutoDetectReader(in)) {
                try (BufferedReader br = new BufferedReader(adr)) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        lines.add(line);
                    }
                }
            }
        }
        return lines;
    }

    private static final Charset UTF_8 = StandardCharsets.UTF_8;
    private static final Charset UTF_16BE = StandardCharsets.UTF_16BE;
    private static final Charset UTF_16LE = StandardCharsets.UTF_16LE;
    private static final Charset UTF_32BE = Charset.forName("UTF-32BE");
    private static final Charset UTF_32LE = Charset.forName("UTF-32LE");
    private static final int BUFFER_SIZE = 65535;

    private InputStream in;
    private PushbackInputStream pushbackInputStream;
    private boolean isDetected;
    private Charset charset;
    private Reader reader;

    public AutoDetectReader(InputStream in) {
        this.in = in;
        this.pushbackInputStream = new PushbackInputStream(in, BUFFER_SIZE);
    }

    public boolean isDetected() throws IOException {
        if(reader == null) {
            detect();
        }
        return isDetected;
    }

    public Charset getCharset() throws IOException {
        if(reader == null) {
            detect();
        }
        return charset;
    }

    private void detect() throws IOException {
        if(reader != null) {
            return;
        }

        UniversalDetector detector = new UniversalDetector(null);
        byte[] buf = new byte[BUFFER_SIZE];
        int offset = 0;
        int length = BUFFER_SIZE;
        int size;
        while((size = pushbackInputStream.read(buf, offset, length)) > 0) {
            detector.handleData(buf, offset, size);
            offset += size;
            length -= size;
            if(detector.isDone()) {
                break;
            }
            if(length <= 0) {
                break;
            }
        }

        pushbackInputStream.unread(buf, 0, offset);
        detector.dataEnd();
        if(detector.getDetectedCharset() != null) {
            charset = Charset.forName(detector.getDetectedCharset());
            isDetected = true;
        }
        if(charset == null) {
            charset = Charset.defaultCharset();
        }
        if(charset.equals(UTF_8) && offset >= 3) {
            byte[] bom = new byte[3];
            int n = pushbackInputStream.read(bom);
            if(n != 3 || bom[0] != (byte)0xEF || bom[1] != (byte)0xBB || bom[2] != (byte)0xBF) {
                pushbackInputStream.unread(bom, 0, n);
            }
        } else if(charset.equals(UTF_16BE) && offset >= 2) {
            byte[] bom = new byte[2];
            int n = pushbackInputStream.read(bom);
            if(n != 2 || bom[0] != (byte)0xFE || bom[1] != (byte)0xFF) {
                pushbackInputStream.unread(bom, 0, n);
            }
        } else if(charset.equals(UTF_16LE) && offset >= 2) {
            byte[] bom = new byte[2];
            int n = pushbackInputStream.read(bom);
            if(n != 2 || bom[0] != (byte)0xFF || bom[1] != (byte)0xFE) {
                pushbackInputStream.unread(bom, 0, n);
            }
        } else if(charset.equals(UTF_32BE) && offset >= 4) {
            byte[] bom = new byte[4];
            int n = pushbackInputStream.read(bom);
            if(n != 4 || bom[0] != (byte)0x00 || bom[1] != (byte)0x00 || bom[2] != (byte)0xFE || bom[3] != (byte)0xFF) {
                pushbackInputStream.unread(bom, 0, n);
            }
        } else if(charset.equals(UTF_32LE) && offset >= 4) {
            byte[] bom = new byte[4];
            int n = pushbackInputStream.read(bom);
            if(n != 4 || bom[0] != (byte)0xFF || bom[1] != (byte)0xFE || bom[2] != (byte)0x00 || bom[3] != (byte)0x00) {
                pushbackInputStream.unread(bom, 0, n);
            }
        }
        reader = new InputStreamReader(pushbackInputStream, charset);
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        if(charset == null) {
            getCharset();
        }
        return reader.read(cbuf, off, len);
    }

    @Override
    public void close() throws IOException {
        if(reader != null) {
            reader.close();
        }

        if(pushbackInputStream != null) {
            pushbackInputStream.close();
        }

        if(in != null) {
            in.close();
        }
    }
}
