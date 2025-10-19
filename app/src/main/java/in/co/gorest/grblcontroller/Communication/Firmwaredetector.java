package in.co.gorest.grblcontroller.communication;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FirmwareDetector {

    public static void sendProbe(OutputStream out) throws IOException {
        // kirim 0x87 (full status report)
        out.write(new byte[]{(byte) 0x87});
        out.flush();
        // kirim $I+ (info grblHAL)
        out.write("$I+\n".getBytes(StandardCharsets.US_ASCII));
        out.flush();
    }

    public static Optional<String> parseFirmwareLine(String line) {
        if (line.contains("FW:")) {
            int i = line.indexOf("FW:");
            int j = line.indexOf('|', i);
            return Optional.of(line.substring(i + 3, j == -1 ? line.length() : j));
        }
        if (line.startsWith("[FIRMWARE:")) {
            int a = line.indexOf(':') + 1;
            int b = line.indexOf(']', a);
            if (b > a) return Optional.of(line.substring(a, b));
        }
        return Optional.empty();
    }
}
