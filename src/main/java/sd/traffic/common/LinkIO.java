package sd.traffic.common;

import com.google.gson.Gson;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Classe utilitária para comunicação TCP confiável com retry e timeout.
 * Baseia-se em sockets e usa Gson para serialização JSON.
 * Inspirada nos exemplos de comunicação das fichas FP4.
 */
public class LinkIO {
    private final String host;
    private final int port;
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private final Gson gson = new Gson();

    public LinkIO(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Liga-se ao destino, com tentativas de reconexão. */
    public boolean connect() {
        int retries = 3;
        while (retries-- > 0) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), 2000);
                out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                System.out.println("[LinkIO] Ligado a " + host + ":" + port);
                return true;
            } catch (IOException e) {
                System.err.println("[LinkIO] Falha na ligação (" + e.getMessage() + "), retry...");
                try { Thread.sleep(500); } catch (InterruptedException ignored) { }
            }
        }
        return false;
    }

    /** Envia uma mensagem JSON. */
    public synchronized void send(Object msg) {
        if (out != null) out.println(gson.toJson(msg));
    }

    /** Lê uma linha JSON (bloqueante). */
    public synchronized String receive() throws IOException {
        if (in == null) return null;
        return in.readLine();
    }

    /** Fecha a ligação. */
    public void close() {
        try { if (socket != null) socket.close(); } catch (IOException ignore) { }
    }
}
