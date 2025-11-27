package sd.traffic.crossing;

import com.google.gson.Gson;
import sd.traffic.common.LinkIO;
import sd.traffic.common.Message;
import sd.traffic.common.Vehicle;
import sd.traffic.coordinator.models.RegisterRequest;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * CrossingProcess - Fase 3:
 * - Liga-se ao Coordinator e envia REGISTER.
 * - Cria 4 semáforos (threads) e 4 filas (QueueManager).
 * - Alterna fases garantindo exclusão mútua.
 * - Envia telemetria detalhada.
 */
public class CrossingProcess {

    private final String id;
    private final int localPort;
    private final LinkIO linkCoordinator;
    private final Gson gson = new Gson();

    // Filas por direção
    private final QueueManager northQueue = new QueueManager();
    private final QueueManager southQueue = new QueueManager();
    private final QueueManager eastQueue = new QueueManager();
    private final QueueManager westQueue = new QueueManager();

    // Semáforos (threads)
    private SemaphoreThread semN;
    private SemaphoreThread semS;
    private SemaphoreThread semE;
    private SemaphoreThread semO;

    private final Object lock = new Object();

    public CrossingProcess(String id, int localPort) {
        this.id = id;
        this.localPort = localPort;
        this.linkCoordinator = new LinkIO("localhost", 6000);
    }

    public void start() {
        // Conectar ao Coordinator
        if (!linkCoordinator.connect()) {
            System.err.println("[Crossing " + id + "] Falha ao ligar ao Coordinator.");
            return;
        }

        // Enviar REGISTER
        sendRegister();

        // Iniciar servidor para receber veículos
        new Thread(this::startServerSocket, "Server-" + id).start();

        // Criar semáforos
        long tSemMillis = 1500;
        semN = new SemaphoreThread(id, "N", northQueue, linkCoordinator, lock, tSemMillis);
        semS = new SemaphoreThread(id, "S", southQueue, linkCoordinator, lock, tSemMillis);
        semE = new SemaphoreThread(id, "E", eastQueue, linkCoordinator, lock, tSemMillis);
        semO = new SemaphoreThread(id, "O", westQueue, linkCoordinator, lock, tSemMillis);

        semN.start();
        semS.start();
        semE.start();
        semO.start();

        // Alternância simples entre direções
        while (true) {
            semN.turnGreen();
            sleepPhase();
            semN.turnRed();

            semS.turnGreen();
            sleepPhase();
            semS.turnRed();

            semE.turnGreen();
            sleepPhase();
            semE.turnRed();

            semO.turnGreen();
            sleepPhase();
            semO.turnRed();
        }
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
                new Thread(() -> handleIncomingVehicles(clientSocket),
                        "Conn-" + clientSocket.getRemoteSocketAddress()).start();
            }
        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Erro no servidor: " + e.getMessage());
        }
    }

    private void handleIncomingVehicles(Socket clientSocket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {
            String line;
            while ((line = in.readLine()) != null) {
                Message<?> msg = gson.fromJson(line, Message.class);
                if ("VehicleTransfer".equals(msg.getType())) {
                    Vehicle v = gson.fromJson(gson.toJson(msg.getPayload()), Vehicle.class);
                    northQueue.enqueue(v);
                    System.out.println("[Crossing " + id + "] Veículo recebido: " + v.getId());
                }
            }
        } catch (IOException e) {
            System.err.println("[Crossing " + id + "] Conexão encerrada: " + e.getMessage());
        }
    }

    private void sleepPhase() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
