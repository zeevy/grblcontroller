package in.co.gorest.grblcontroller.communication;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public class TcpConnection implements Closeable {
    private Socket socket;
    private BufferedReader reader;
    private BufferedWriter writer;
    private Thread readerThread;
    private volatile boolean running = false;
    private Consumer<String> onLineReceived;

    public TcpConnection(String host, int port, Consumer<String> onLineReceived) throws IOException {
        this.onLineReceived = onLineReceived;
        socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), 3000);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
        writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.US_ASCII));
        startReader();
    }

    private void startReader() {
        running = true;
        readerThread = new Thread(() -> {
            String line;
            try {
                while (running && (line = reader.readLine()) != null) {
                    onLineReceived.accept(line);
                }
            } catch (IOException ignored) {}
        }, "grbl-tcp-reader");
        readerThread.start();
    }

    public synchronized void writeLine(String s) throws IOException {
        writer.write(s + "\n");
        writer.flush();
    }

    public synchronized void writeBytes(byte[] b) throws IOException {
        socket.getOutputStream().write(b);
        socket.getOutputStream().flush();
    }

    @Override
    public void close() throws IOException {
        running = false;
        if (socket != null) socket.close();
    }
}
