
package Bai7;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class TCPServer {

    private static final int PORT = 5000;

    // Tu dong tao ID: 1, 2, 3, ...
    private static final AtomicInteger clientCounter = new AtomicInteger(0);

    // Luu danh sach Client dang ket noi
    // clientId -> ClientHandler
    private static final ConcurrentHashMap<Integer, ClientHandler> clients
            = new ConcurrentHashMap<>();

    // File luu lich su chat
    private static final String HISTORY_FILE = "chat_history.txt";

    public static void main(String[] args) {

        System.out.println("======================================");
        System.out.println("       TCP CHAT SERVER - JAVA");
        System.out.println("======================================");

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {

            System.out.println("Server dang chay...");
            System.out.println("IP: " + InetAddress.getLocalHost().getHostAddress());
            System.out.println("Port: " + PORT);
            System.out.println("Dang cho Client ket noi...\n");

            while (true) {

                // Cho Client ket noi
                Socket socket = serverSocket.accept();

                // Tao ID cho Client
                int clientId = clientCounter.incrementAndGet();

                System.out.println(
                        "Client" + clientId
                        + " da ket noi: "
                        + socket.getInetAddress().getHostAddress()
                );

                // Tao Thread xu ly Client
                ClientHandler clientHandler =
                        new ClientHandler(socket, clientId);

                // Luu Client vao ConcurrentHashMap
                clients.put(clientId, clientHandler);

                // Chay Thread
                clientHandler.start();
            }

        } catch (IOException e) {
            System.out.println("Loi Server: " + e.getMessage());
        }
    }

    // =========================================================
    // BROADCAST
    // Gui tin nhan den tat ca Client
    // =========================================================
    public static void broadcast(String message) {

        for (ClientHandler client : clients.values()) {

            client.sendMessage(message);
        }

        // Luu lich su
        saveHistory(message);

        // Hien thi tren Server
        System.out.println(message);
    }

    // =========================================================
    // GUI TIN NHAN RIENG
    // =========================================================
    public static void privateMessage(
            int senderId,
            int receiverId,
            String message) {

        ClientHandler receiver = clients.get(receiverId);

        if (receiver != null) {

            String senderName =
                    clients.get(senderId).getDisplayName();

            String text =
                    "[Private] "
                    + senderName
                    + " -> "
                    + receiver.getDisplayName()
                    + ": "
                    + message;

            // Gui cho nguoi nhan
            receiver.sendMessage(text);

            // Gui lai cho nguoi gui de ho thay
            ClientHandler sender = clients.get(senderId);

            if (sender != null) {
                sender.sendMessage(text);
            }

            saveHistory(text);

            System.out.println(text);

        } else {

            ClientHandler sender = clients.get(senderId);

            if (sender != null) {
                sender.sendMessage(
                        "Server: Client" + receiverId
                        + " khong ton tai hoac da offline."
                );
            }
        }
    }

    // =========================================================
    // HIEN THI DANH SACH CLIENT ONLINE
    // =========================================================
    public static String getOnlineList() {

        StringBuilder result = new StringBuilder();

        result.append("===== DANH SACH ONLINE =====\n");

        for (ClientHandler client : clients.values()) {

            result.append(
                    "Client"
                    + client.getClientId()
                    + " - "
                    + client.getDisplayName()
                    + "\n"
            );
        }

        result.append("============================");

        return result.toString();
    }

    // =========================================================
    // XOA CLIENT KHI NGAT KET NOI
    // =========================================================
    public static void removeClient(int clientId) {

        ClientHandler client = clients.remove(clientId);

        if (client != null) {

            String message =
                    "Server: "
                    + client.getDisplayName()
                    + " da roi phong chat.";

            broadcast(message);

            System.out.println(
                    "Client" + clientId + " da ngat ket noi."
            );
        }
    }

    // =========================================================
    // LUU LICH SU CHAT
    // =========================================================
    public static synchronized void saveHistory(String message) {

        try (FileWriter fw =
                     new FileWriter(HISTORY_FILE, true);

             BufferedWriter bw =
                     new BufferedWriter(fw)) {

            String time =
                    LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern(
                                    "dd/MM/yyyy HH:mm:ss"
                            )
                    );

            bw.write("[" + time + "] " + message);
            bw.newLine();

        } catch (IOException e) {

            System.out.println(
                    "Khong the luu lich su: "
                    + e.getMessage()
            );
        }
    }

    // =========================================================
    // CLASS CLIENT HANDLER
    // MOI CLIENT = 1 THREAD
    // =========================================================
    static class ClientHandler extends Thread {

        private Socket socket;

        private int clientId;

        private BufferedReader reader;

        private PrintWriter writer;

        // Ten hien thi
        private String nickname;

        public ClientHandler(Socket socket, int clientId) {

            this.socket = socket;
            this.clientId = clientId;

            // Mac dinh
            this.nickname = "Client" + clientId;
        }

        @Override
        public void run() {

            try {

                // Nhan du lieu
                reader = new BufferedReader(
                        new InputStreamReader(
                                socket.getInputStream()
                        )
                );

                // Gui du lieu
                writer = new PrintWriter(
                        socket.getOutputStream(),
                        true
                );

                // Thong bao cho Client
                sendMessage(
                        "Server: Ban da ket noi voi Server."
                );

                sendMessage(
                        "Server: ID cua ban la Client"
                        + clientId
                );

                sendMessage(
                        "Server: Nhap /help de xem cac lenh."
                );

                // Thong bao Client moi
                broadcast(
                        "Server: "
                        + getDisplayName()
                        + " da tham gia phong chat."
                );

                String message;

                // Doc tin nhan lien tuc
                while ((message = reader.readLine()) != null) {

                    message = message.trim();

                    if (message.isEmpty()) {
                        continue;
                    }

                    // Xu ly lenh
                    handleCommand(message);
                }

            } catch (IOException e) {

                System.out.println(
                        "Client" + clientId
                        + " mat ket noi."
                );

            } finally {

                // Xoa Client
                removeClient(clientId);

                try {
                    socket.close();
                } catch (IOException e) {
                    // Khong lam gi
                }
            }
        }

        // =====================================================
        // XU LY LENH
        // =====================================================
        private void handleCommand(String message) {

            // /help
            if (message.equalsIgnoreCase("/help")) {

                sendMessage(
                        "===== DANH SACH LENH =====\n"
                        + "/name ten - Doi nickname\n"
                        + "/online - Xem Client online\n"
                        + "/msg ID message - Gui tin rieng\n"
                        + "/all message - Gui tat ca\n"
                        + "/me message - Tin nhan hanh dong\n"
                        + "/quit - Thoat\n"
                        + "=========================="
                );

                return;
            }

            // /name
            if (message.toLowerCase().startsWith("/name ")) {

                String newName =
                        message.substring(6).trim();

                if (newName.isEmpty()) {

                    sendMessage(
                            "Server: Nickname khong duoc rong."
                    );

                    return;
                }

                String oldName = nickname;

                nickname = newName;

                sendMessage(
                        "Server: Ban da doi ten thanh "
                        + nickname
                );

                broadcast(
                        "Server: "
                        + oldName
                        + " da doi ten thanh "
                        + nickname
                );

                return;
            }

            // /online
            if (message.equalsIgnoreCase("/online")) {

                sendMessage(getOnlineList());

                return;
            }

            // /msg ID message
            if (message.toLowerCase().startsWith("/msg ")) {

                String[] parts =
                        message.split(" ", 3);

                if (parts.length < 3) {

                    sendMessage(
                            "Cu phap: /msg ID noi_dung"
                    );

                    return;
                }

                try {

                    int receiverId =
                            Integer.parseInt(parts[1]);

                    String privateText =
                            parts[2];

                    privateMessage(
                            clientId,
                            receiverId,
                            privateText
                    );

                } catch (NumberFormatException e) {

                    sendMessage(
                            "Server: ID phai la so."
                    );
                }

                return;
            }

            // /all
            if (message.toLowerCase().startsWith("/all ")) {

                String text =
                        message.substring(5).trim();

                broadcast(
                        getDisplayName()
                        + ": "
                        + text
                );

                return;
            }

            // /me
            if (message.toLowerCase().startsWith("/me ")) {

                String text =
                        message.substring(4).trim();

                broadcast(
                        "* "
                        + getDisplayName()
                        + " "
                        + text
                );

                return;
            }

            // /quit
            if (message.equalsIgnoreCase("/quit")) {

                sendMessage(
                        "Server: Ban da thoat."
                );

                try {
                    socket.close();
                } catch (IOException e) {
                    // Khong lam gi
                }

                return;
            }

            // Tin nhan binh thuong
            broadcast(
                    getDisplayName()
                    + ": "
                    + message
            );
        }

        // =====================================================
        // GUI MESSAGE CHO CLIENT
        // =====================================================
        public synchronized void sendMessage(String message) {

            if (writer != null) {

                writer.println(message);
            }
        }

        public int getClientId() {

            return clientId;
        }

        public String getDisplayName() {

            return nickname;
        }
    }
}

