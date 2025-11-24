package sd.traffic.crossing;

import com.google.gson.Gson;
import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.coordinator.models.RegisterRequest;
import sd.traffic.coordinator.models.TelemetryPayload;
import sd.traffic.coordinator.models.VehicleTransfer;
import sd.traffic.model.LightColor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Representa um cruzamento como um processo TCP independente.
 */
public class CrossingProcess {

    private final String id;
    private final int localPort;
    private final LinkIO linkCoordinator;
    private final BlockingQueue<VehicleTransfer> queue = new LinkedBlockingQueue<>(); // agora guarda objetos
    private final Gson gson = new Gson();

    // Configurações simples
    private final long telemetryIntervalMs = 2000; // envio de telemetria a cada 2s
    private volatile LightColor currentLight = LightColor.GREEN; // estado inicial do semáforo

    public CrossingProcess(String id, int localPort) {
        this.id = id;
        this.localPort = localPort;
        this.linkCoordinator = new LinkIO("localhost", 6000); // Coordinator na porta 6000
    }

    public void start() {
        // 1. Conectar ao Coordinator
        if (!linkCoordinator.connect()) {
            System.err.println("[Crossing " + id + "] Falha ao ligar ao Coordinator.");
            return;
        }

        // 2. Enviar REGISTER
        sendRegister();

        // 3. Iniciar servidor para receber mensagens (VehicleTransfer)
        new Thread(this::startServerSocket, "Server-" + id).start();

        // 4. Iniciar envio de telemetria
        new Thread(this::sendTelemetryLoop, "Telemetry-" + id).start();

        // 5. Iniciar escuta de mensagens do Coordinator (POLICY_UPDATE futuro)
        new Thread(this::handleMessages, "CoordinatorListener-" + id).start();
    }

    private void sendRegister() {
        RegisterRequest req = new RegisterRequest();
        req.setNodeId(id);
        req.setRole("CROSSING");
        linkCoordinator.send(new Message<>("REGISTER", req));
        System.out.println("[Crossing " + id + "] REGISTER enviado ao Coordinator.");
    }

    private void startServerSocket() {
        try (ServerSocket serverSocket = new ServerSocket(localPort)) {
            System.out.println("[Crossing " + id + "] A ouvir na porta " + localPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> handleIncoming(clientSocket), "Conn-" + clientSocket.getRemoteSocketAddress()).start();
            }
        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Erro no servidor: " + e.getMessage());
        }
    }

    private void handleIncoming(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[Crossing " + id + "] Mensagem recebida: " + line);

                // Parse da mensagem para VehicleTransfer
                try {
                    Message<?> msg = gson.fromJson(line, Message.class);
                    if ("VehicleTransfer".equalsIgnoreCase(msg.getType())) {
                        VehicleTransfer vt = gson.fromJson(gson.toJson(msg.getPayload()), VehicleTransfer.class);
                        queue.add(vt);
                        System.out.println("[Crossing " + id + "] VehicleTransfer adicionado à fila: " + vt);
                    } else {
                        System.out.println("[Crossing " + id + "] Tipo de mensagem não suportado: " + msg.getType());
                    }
                } catch (Exception e) {
                    System.err.println("[Crossing " + id + "] Erro ao processar mensagem: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Conexão encerrada: " + e.getMessage());
        }
    }

    private void sendTelemetryLoop() {
        while (true) {
            try {
                TelemetryPayload tel = new TelemetryPayload();
                tel.setCrossing(id);
                tel.setQueue(queue.size());
                tel.setAvg(0.0); // placeholder
                tel.setLightState(currentLight);

                linkCoordinator.send(new Message<>("TELEMETRY", tel));
                Thread.sleep(telemetryIntervalMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void handleMessages() {
        try {
            while (true) {
                String line = linkCoordinator.receive();
                if (line == null) break;
                System.out.println("[Crossing " + id + "] Mensagem do Coordinator: " + line);
            }
        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Erro ao ler do Coordinator: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Uso: CrossingProcess <ID> <PORT>");
            return;
        }
        String id = args[0];
        int port = Integer.parseInt(args[1]);
        CrossingProcess cp = new CrossingProcess(id, port);
        cp.start();
    }
}
