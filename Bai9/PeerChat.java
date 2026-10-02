package Bai9;

import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class PeerChat {

    // Luu thong tin cac Peer
    // Ten Peer -> InetSocketAddress
    private static final ConcurrentHashMap<String, InetSocketAddress> peers
            = new ConcurrentHashMap<>();

    // Socket UDP
    private static DatagramSocket socket;

    // Ten hien thi cua Peer hien tai
    private static String nickname;

    // Port cua Peer hien tai
    private static int localPort;

    // Trang thai chuong trinh
    private static volatile boolean running = true;

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        try {

            // ==============================================
            // NHAP THONG TIN PEER
            // ==============================================

            System.out.println("======================================");
            System.out.println("       UDP CHAT PEER-TO-PEER");
            System.out.println("======================================");

            System.out.print("Nhap ten cua ban: ");
            nickname = scanner.nextLine().trim();

            if (nickname.isEmpty()) {
                nickname = "Peer";
            }

            System.out.print("Nhap Local Port: ");
            localPort = Integer.parseInt(scanner.nextLine());

            // Tao DatagramSocket voi local port
            socket = new DatagramSocket(localPort);

            System.out.println();
            System.out.println("Peer da khoi dong.");
            System.out.println("Ten: " + nickname);
            System.out.println("Local Port: " + localPort);
            System.out.println();

            // ==============================================
            // LUONG NHAN
            // ==============================================

            Thread receiveThread = new Thread(() -> receiveMessages());

            receiveThread.start();

            // ==============================================
            // THEM PEER BAN DAU
            // ==============================================

            System.out.println("Nhap thong tin Peer muon chat.");
            System.out.println("Neu khong muon them Peer, nhap Enter.");

            System.out.print("Peer IP: ");
            String peerIP = scanner.nextLine().trim();

            if (!peerIP.isEmpty()) {

                System.out.print("Peer Port: ");
                int peerPort =
                        Integer.parseInt(scanner.nextLine());

                addPeer(
                        "Peer1",
                        peerIP,
                        peerPort
                );

                System.out.println(
                        "Da them Peer1: "
                        + peerIP
                        + ":"
                        + peerPort
                );
            }

            System.out.println();
            System.out.println("Nhap /help de xem cac lenh.");
            System.out.println();

            // ==============================================
            // LUONG GUI
            // ==============================================

            while (running) {

                System.out.print("> ");

                String message = scanner.nextLine();

                if (message.trim().isEmpty()) {
                    continue;
                }

                // Xu ly lenh
                if (message.startsWith("/")) {

                    handleCommand(message);

                } else {

                    // Gui tin nhan binh thuong
                    sendToAllPeers(message);
                }
            }

            socket.close();
            scanner.close();

        } catch (Exception e) {

            System.out.println(
                    "Loi: " + e.getMessage()
            );

        } finally {

            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }

    // =========================================================
    // LUONG NHAN MESSAGE
    // =========================================================

    private static void receiveMessages() {

        while (running) {

            try {

                byte[] buffer = new byte[4096];

                DatagramPacket packet =
                        new DatagramPacket(
                                buffer,
                                buffer.length
                        );

                // Cho nhan Datagram
                socket.receive(packet);

                String message =
                        new String(
                                packet.getData(),
                                0,
                                packet.getLength()
                        );

                String senderIP =
                        packet.getAddress().getHostAddress();

                int senderPort =
                        packet.getPort();

                System.out.println();

                System.out.println(
                        message
                );

                System.out.print("> ");

                // Luu Peer vao danh sach neu chua co
                addPeerIfNotExists(
                        senderIP,
                        senderPort
                );

            } catch (SocketException e) {

                if (running) {

                    System.out.println(
                            "Socket da dong."
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "Loi khi nhan tin: "
                        + e.getMessage()
                );
            }
        }
    }

    // =========================================================
    // GUI MESSAGE CHO TAT CA PEER
    // =========================================================

    private static void sendToAllPeers(String message) {

        if (peers.isEmpty()) {

            System.out.println(
                    "Chua co Peer nao de gui."
            );

            return;
        }

        String time = getCurrentTime();

        String fullMessage =
                "[" + time + "] "
                + nickname
                + ": "
                + message;

        for (Map.Entry<String, InetSocketAddress> entry
                : peers.entrySet()) {

            sendMessage(
                    entry.getValue(),
                    fullMessage
            );
        }
    }

    // =========================================================
    // GUI MESSAGE DEN MOT PEER
    // =========================================================

    private static void sendMessage(
            InetSocketAddress address,
            String message) {

        try {

            byte[] data =
                    message.getBytes();

            DatagramPacket packet =
                    new DatagramPacket(
                            data,
                            data.length,
                            address.getAddress(),
                            address.getPort()
                    );

            socket.send(packet);

        } catch (Exception e) {

            System.out.println(
                    "Khong the gui den "
                    + address.getAddress().getHostAddress()
                    + ":"
                    + address.getPort()
            );
        }
    }

    // =========================================================
    // THEM PEER
    // =========================================================

    private static void addPeer(
            String name,
            String ip,
            int port) {

        try {

            InetAddress address =
                    InetAddress.getByName(ip);

            InetSocketAddress peerAddress =
                    new InetSocketAddress(
                            address,
                            port
                    );

            peers.put(
                    name,
                    peerAddress
            );

        } catch (Exception e) {

            System.out.println(
                    "IP khong hop le."
            );
        }
    }

    // =========================================================
    // TU DONG THEM PEER KHI NHAN MESSAGE
    // =========================================================

    private static void addPeerIfNotExists(
            String ip,
            int port) {

        for (InetSocketAddress address
                : peers.values()) {

            if (address.getAddress()
                    .getHostAddress()
                    .equals(ip)
                    && address.getPort() == port) {

                return;
            }
        }

        String name =
                "Peer"
                + (peers.size() + 1);

        try {

            peers.put(
                    name,
                    new InetSocketAddress(
                            InetAddress.getByName(ip),
                            port
                    )
            );

        } catch (Exception e) {

            // Khong lam gi
        }
    }

    // =========================================================
    // XU LY CAC LENH
    // =========================================================

    private static void handleCommand(String command) {

        // ==============================================
        // /help
        // ==============================================

        if (command.equalsIgnoreCase("/help")) {

            System.out.println();
            System.out.println("========== DANH SACH LENH ==========");
            System.out.println("/name ten");
            System.out.println("    Doi nickname");

            System.out.println("/list");
            System.out.println("    Xem danh sach Peer");

            System.out.println("/add IP PORT");
            System.out.println("    Them mot Peer");

            System.out.println("/msg TEN message");
            System.out.println("    Gui tin rieng cho mot Peer");

            System.out.println("/exit");
            System.out.println("    Thoat chuong trinh");

            System.out.println("/help");
            System.out.println("    Xem danh sach lenh");

            System.out.println("====================================");
            System.out.println();

            return;
        }

        // ==============================================
        // /name
        // ==============================================

        if (command.toLowerCase().startsWith("/name ")) {

            String newName =
                    command.substring(6).trim();

            if (newName.isEmpty()) {

                System.out.println(
                        "Ten khong duoc rong."
                );

                return;
            }

            nickname = newName;

            System.out.println(
                    "Da doi ten thanh: "
                    + nickname
            );

            return;
        }

        // ==============================================
        // /list
        // ==============================================

        if (command.equalsIgnoreCase("/list")) {

            showPeerList();

            return;
        }

        // ==============================================
        // /add IP PORT
        // ==============================================

        if (command.toLowerCase().startsWith("/add ")) {

            String[] parts =
                    command.split(" ");

            if (parts.length != 3) {

                System.out.println(
                        "Cu phap: /add IP PORT"
                );

                return;
            }

            try {

                String ip = parts[1];

                int port =
                        Integer.parseInt(parts[2]);

                String name =
                        "Peer"
                        + (peers.size() + 1);

                addPeer(
                        name,
                        ip,
                        port
                );

                System.out.println(
                        "Da them "
                        + name
                        + ": "
                        + ip
                        + ":"
                        + port
                );

            } catch (Exception e) {

                System.out.println(
                        "Port phai la so."
                );
            }

            return;
        }

        // ==============================================
        // /msg TEN message
        // ==============================================

        if (command.toLowerCase().startsWith("/msg ")) {

            String[] parts =
                    command.split(" ", 3);

            if (parts.length < 3) {

                System.out.println(
                        "Cu phap: /msg TEN message"
                );

                return;
            }

            String peerName = parts[1];

            String message = parts[2];

            InetSocketAddress address =
                    peers.get(peerName);

            if (address == null) {

                System.out.println(
                        "Khong tim thay Peer: "
                        + peerName
                );

                return;
            }

            String time =
                    getCurrentTime();

            String fullMessage =
                    "[" + time + "] "
                    + nickname
                    + " -> "
                    + peerName
                    + ": "
                    + message;

            sendMessage(
                    address,
                    fullMessage
            );

            return;
        }

        // ==============================================
        // /exit
        // ==============================================

        if (command.equalsIgnoreCase("/exit")) {

            running = false;

            System.out.println(
                    "Dang thoat chuong trinh..."
            );

            if (socket != null) {
                socket.close();
            }

            return;
        }

        // ==============================================
        // LENH KHONG TON TAI
        // ==============================================

        System.out.println(
                "Lenh khong hop le."
                + " Nhap /help de xem huong dan."
        );
    }

    // =========================================================
    // HIEN THI DANH SACH PEER
    // =========================================================

    private static void showPeerList() {

        System.out.println();
        System.out.println("========== PEER ONLINE ==========");

        if (peers.isEmpty()) {

            System.out.println(
                    "Chua co Peer nao."
            );

        } else {

            for (Map.Entry<String, InetSocketAddress> entry
                    : peers.entrySet()) {

                InetSocketAddress address =
                        entry.getValue();

                System.out.println(
                        entry.getKey()
                        + " -> "
                        + address.getAddress()
                                   .getHostAddress()
                        + ":"
                        + address.getPort()
                );
            }
        }

        System.out.println(
                "================================"
        );
        System.out.println();
    }

    // =========================================================
    // LAY THOI GIAN HIEN TAI
    // =========================================================

    private static String getCurrentTime() {

        SimpleDateFormat formatter =
                new SimpleDateFormat(
                        "HH:mm:ss"
                );

        return formatter.format(
                new Date()
        );
    }
}


