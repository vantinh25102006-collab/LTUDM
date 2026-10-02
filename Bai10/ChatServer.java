package Bai10;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatServer {
    static final Map<String, ClientInfo> clients = new ConcurrentHashMap<>();
    static final Map<String, GroupInfo> groups = new ConcurrentHashMap<>();
    static final List<ServerConnection> connections =
            Collections.synchronizedList(new ArrayList<>());
    static final AtomicInteger clientSeq = new AtomicInteger(0);
    static final AtomicInteger groupSeq = new AtomicInteger(1);
    static volatile int defaultTTL = 8;

    static ServerGUI gui;
    static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            SwingUtilities.invokeAndWait(() -> {
                gui = new ServerGUI();
                gui.setVisible(true);
                gui.appendSystem("Server dang khoi dong...");
            });
        } catch (Exception e) {
            return;
        }

        groups.put("SERVER", new GroupInfo(
                "SERVER", "SERVER", "SERVER",
                ChatCommon.SERVER_MCAST_IP,
                ChatCommon.SERVER_MCAST_PORT,
                1
        ));

        try {
            new Thread(() -> gui.joinMonitor(groups.get("SERVER")), "Monitor-SERVER").start();
            serverSocket = new ServerSocket(ChatCommon.TCP_PORT);
            gui.appendSystem("TCP Server: 0.0.0.0:" + ChatCommon.TCP_PORT);
            gui.appendSystem("Nhom SERVER: " + ChatCommon.SERVER_MCAST_IP +
                    ":" + ChatCommon.SERVER_MCAST_PORT);
            while (true) {
                Socket s = serverSocket.accept();
                ServerConnection c = new ServerConnection(s);
                connections.add(c);
                new Thread(c, "Client-" + s.getRemoteSocketAddress()).start();
            }
        } catch (IOException e) {
            if (gui != null) gui.appendSystem("Loi server: " + e.getMessage());
        }
    }

    static synchronized String nextClientId() {
        return "C" + clientSeq.incrementAndGet();
    }

    static synchronized String nextGroupId() {
        return "G" + groupSeq.getAndIncrement();
    }

    static synchronized int nextGroupNumber() {
        return groupSeq.get() + 1;
    }

    static void broadcastState() {
        String state = buildState();
        synchronized (connections) {
            for (ServerConnection c : connections) {
                if (c.client != null) c.send(state);
            }
        }
        if (gui != null) gui.refreshState();
    }

    static String buildState() {
        StringBuilder sb = new StringBuilder("STATE\n");
        for (ClientInfo c : clients.values()) {
            sb.append("CLIENT|")
              .append(c.id).append("|")
              .append(ChatCommon.b64(c.name)).append("|")
              .append(c.ip).append("|")
              .append(c.udpPort).append("\n");
        }
        for (GroupInfo g : groups.values()) {
            sb.append("GROUP|")
              .append(g.id).append("|")
              .append(ChatCommon.b64(g.name)).append("|")
              .append(g.ownerId).append("|")
              .append(g.mcastIp).append("|")
              .append(g.port).append("|")
              .append(g.ttl).append("|")
              .append(String.join(",", g.members))
              .append("\n");
        }
        sb.append("END\n");
        return sb.toString();
    }

    static ClientInfo findClient(String id) {
        return clients.get(id);
    }

    static boolean isOwner(String clientId, String groupId) {
        GroupInfo g = groups.get(groupId);
        return g != null && g.ownerId.equals(clientId);
    }

    static void sendError(ServerConnection c, String message) {
        c.send("ERROR|" + ChatCommon.b64(message));
    }

    static synchronized void createGroup(ServerConnection c, String groupName,
                                          int ttl, List<String> memberIds) {
        if (c.client == null) return;
        groupName = groupName.trim();
        if (groupName.isEmpty()) {
            sendError(c, "Ten phong khong duoc rong.");
            return;
        }
        ttl = Math.max(1, Math.min(32, ttl));

        String id = nextGroupId();
        int n = Integer.parseInt(id.substring(1)) + 1;
        String ip = "239.1.1." + Math.min(254, Math.max(2, n));
        int port = 8000 + Math.min(900, n);

        GroupInfo g = new GroupInfo(id, groupName, c.client.id, ip, port, ttl);
        g.members.add(c.client.id);

        for (String mid : memberIds) {
            if (clients.containsKey(mid)) g.members.add(mid);
        }
        groups.put(id, g);

        gui.appendSystem(c.client.name + " da tao phong " + groupName +
                " (" + ip + ":" + port + ", TTL=" + ttl + ")");
        broadcastState();

        // Server monitor join group.
        new Thread(() -> gui.joinMonitor(g), "Monitor-" + g.id).start();
    }

    static synchronized void addMembers(ServerConnection c, String groupId,
                                         List<String> ids) {
        GroupInfo g = groups.get(groupId);
        if (g == null) {
            sendError(c, "Khong tim thay phong.");
            return;
        }
        if (!g.ownerId.equals(c.client.id)) {
            sendError(c, "Chi client tao phong moi duoc them thanh vien.");
            return;
        }
        for (String id : ids) {
            if (clients.containsKey(id)) g.members.add(id);
        }
        gui.appendSystem(c.client.name + " da them thanh vien vao " + g.name);
        broadcastState();
    }

    static synchronized void leaveGroup(String clientId, String groupId) {
        GroupInfo g = groups.get(groupId);
        if (g == null || "SERVER".equals(groupId)) return;
        if (g.members.contains(clientId)) {
            ClientInfo c = clients.get(clientId);
            String name = c == null ? clientId : c.name;
            g.members.remove(clientId);
            gui.appendSystem(name + " da roi phong " + g.name);
            broadcastState();
        }
    }

    static synchronized void disbandGroup(String clientId, String groupId) {
        GroupInfo g = groups.get(groupId);
        if (g == null || "SERVER".equals(groupId)) return;
        if (!g.ownerId.equals(clientId)) {
            ServerConnection c = findConnection(clientId);
            if (c != null) sendError(c, "Chi chu phong moi duoc giai tan phong.");
            return;
        }
        groups.remove(groupId);
        gui.appendSystem("Phong " + g.name + " da duoc giai tan.");
        broadcastState();
    }

    static ServerConnection findConnection(String clientId) {
        synchronized (connections) {
            for (ServerConnection c : connections)
                if (c.client != null && c.client.id.equals(clientId)) return c;
        }
        return null;
    }

    static void monitorChat(ChatCommon.ChatMessage m) {
        if (m == null) return;
        if ("GROUP".equals(m.scope) || "SERVER".equals(m.scope)) {
            gui.addChat(m);
        }
        gui.addScope(m);
    }

    static class ClientInfo {
        String id, name, ip;
        int udpPort;
        ClientInfo(String id, String name, String ip, int udpPort) {
            this.id = id; this.name = name; this.ip = ip; this.udpPort = udpPort;
        }
    }

    static class GroupInfo {
        String id, name, ownerId, mcastIp;
        int port, ttl;
        Set<String> members = ConcurrentHashMap.newKeySet();
        GroupInfo(String id, String name, String ownerId,
                  String mcastIp, int port, int ttl) {
            this.id=id; this.name=name; this.ownerId=ownerId;
            this.mcastIp=mcastIp; this.port=port; this.ttl=ttl;
        }
    }

    static class ServerConnection implements Runnable {
        Socket socket;
        BufferedReader in;
        PrintWriter out;
        ClientInfo client;

        ServerConnection(Socket socket) {
            this.socket = socket;
            try {
                in = new BufferedReader(new InputStreamReader(
                        socket.getInputStream(), StandardCharsets.UTF_8));
                out = new PrintWriter(new OutputStreamWriter(
                        socket.getOutputStream(), StandardCharsets.UTF_8), true);
            } catch (IOException e) {}
        }

        synchronized void send(String s) {
            out.print(s);
            out.flush();
        }

        public void run() {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    handle(line);
                }
            } catch (IOException ignored) {
            } finally {
                disconnect();
            }
        }

        void handle(String line) {
            try {
                String[] p = line.split("\\|", -1);
                if (p[0].equals("HELLO")) {
                    String name = ChatCommon.ub64(p[1]);
                    int udpPort = Integer.parseInt(p[2]);
                    String id = nextClientId();
                    String ip = socket.getInetAddress().getHostAddress();
                    client = new ClientInfo(id, name, ip, udpPort);
                    clients.put(id, client);
                    groups.get("SERVER").members.add(id);
                    send("WELCOME|" + id + "|" +
                            ChatCommon.SERVER_MCAST_IP + "|" +
                            ChatCommon.SERVER_MCAST_PORT + "\n");
                    gui.appendSystem(name + " da ket noi (" + ip + ":" + udpPort + ")");
                    broadcastState();
                } else if (client == null) {
                    return;
                } else if (p[0].equals("CREATE_GROUP")) {
                    String name = ChatCommon.ub64(p[1]);
                    int ttl = Integer.parseInt(p[2]);
                    List<String> ids = p.length > 3 && !p[3].isEmpty()
                            ? Arrays.asList(p[3].split(",")) : List.of();
                    createGroup(this, name, ttl, ids);
                } else if (p[0].equals("ADD_MEMBERS")) {
                    List<String> ids = p.length > 2 && !p[2].isEmpty()
                            ? Arrays.asList(p[2].split(",")) : List.of();
                    addMembers(this, p[1], ids);
                } else if (p[0].equals("LEAVE_GROUP")) {
                    leaveGroup(client.id, p[1]);
                } else if (p[0].equals("DISBAND_GROUP")) {
                    disbandGroup(client.id, p[1]);
                } else if (p[0].equals("PRIVATE_MONITOR")) {
                    ChatCommon.ChatMessage m = ChatCommon.ChatMessage.decode(line.substring(
                            "PRIVATE_MONITOR|".length()));
                    if (m != null) monitorChat(m);
                } else if (p[0].equals("RESEND_REQUEST")) {
                    if (p.length >= 4) {
                        String senderId = p[2];
                        long seq = Long.parseLong(p[3]);
                        ServerConnection sender = findConnection(senderId);
                        if (sender != null) sender.send("RESEND|" + p[1] + "|" + seq + "\n");
                    }
                } else if (p[0].equals("GET_TTL")) {
                    send("TTL|" + defaultTTL + "\n");
                }
            } catch (Exception e) {
                sendError(this, "Du lieu khong hop le: " + e.getMessage());
            }
        }

        void disconnect() {
            if (client == null) return;
            String id = client.id;
            String name = client.name;
            clients.remove(id);
            for (GroupInfo g : groups.values()) {
                if (g.members.remove(id) && !"SERVER".equals(g.id)) {
                }
            }
            synchronized (connections) {
                connections.remove(this);
            }
            gui.appendSystem(name + " da offline.");
            broadcastState();
            client = null;
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
