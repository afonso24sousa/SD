package sd.traffic.common;

import com.google.gson.Gson;
import sd.traffic.coordinator.models.EventLogEntry;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * LinkIO — comunicação TCP fiável com JSON.
 * Esta versão corrige completamente o receiveNonBlocking()
 * e evita múltiplos BufferedReaders (BUG crítico anterior).
 */
public class LinkIO {

    private final String host;
    private final int port;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;

    private final Gson gson = new Gson();


    // ============================================================
    // CONSTRUCTOR
    // ============================================================

    public LinkIO(String host, int port) {
        this.host = host;
        this.port = port;
    }


    // ============================================================
    // CONNECT
    // ============================================================

    /** Liga-se ao destino, com tentativas de reconexão. */
    public boolean connect() {
        int retries = 3;

        while (retries-- > 0) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 2000);

                out = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8),
                        true
                );

                in = new BufferedReader(
                        new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
                );

                System.out.println("[LinkIO] Ligado a " + host + ":" + port);
                return true;

            } catch (IOException e) {
                System.err.println("[LinkIO] Falha na ligação (" + e.getMessage() + "), retry...");
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            }
        }

        return false;
    }


    // ============================================================
    // SEND
    // ============================================================

    /** Envia uma mensagem JSON. */
    public synchronized void send(Object msg) {
        if (out != null) {
            out.println(gson.toJson(msg));
        }
    }


    // ============================================================
    // RECEIVE (bloqueante)
    // ============================================================

    /** Lê uma linha JSON de forma bloqueante. */
    public synchronized String receive() throws IOException {
        if (in == null) return null;
        return in.readLine();
    }


    // ============================================================
    // RECEIVE NON BLOCKING (corrigido)
    // ============================================================

    /**
     * Lê uma mensagem sem bloquear.
     *
     * @return String com a linha recebida, ou null se:
     *         - não há dados,
     *         - ligação fechada,
     *         - ocorreu erro.
     */
    public String receiveNonBlocking() {
        try {
            if (socket == null || socket.isClosed() || in == null) return null;

            // ready() → TRUE = há dados disponíveis sem bloquear
            if (!in.ready()) return null;

            return in.readLine(); // usa SEMPRE o mesmo BufferedReader

        } catch (Exception e) {
            return null;
        }
    }


    // ============================================================
    // HELPERS
    // ============================================================

    /** Indica se a ligação está ativa. */
    public boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed();
    }


    /** Fecha a ligação TCP. */
    public void close() {
        try {
            if (socket != null)
                socket.close();
        } catch (IOException ignore) {
        } finally {
            socket = null;
            out = null;
            in = null;
        }
    }


    /** Envia evento de log ao Coordinator. */
    public void sendLog(String type, String node, String details, double simTime) {
        EventLogEntry ev = new EventLogEntry(type, simTime, node, details);
        send(new Message<>("EVENT_LOG", ev));
    }


    public String getHost() { return host; }
    public int getPort() { return port; }
}
