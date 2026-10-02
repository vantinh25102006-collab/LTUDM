package Bai10;

import javax.swing.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatClient {
    static final String SERVER_HOST = "127.0.0.1"; // đổi thành IP server khi chạy khác máy
    static ClientGUI gui;
    static Socket socket;
    static BufferedReader in;
    static PrintWriter out;

    static String myId, myName;
    static int udpPort;
    static DatagramSocket privateSocket;
    static volatile boolean running = true;

    static final Map<String, ChatServerProxy.Client> clients = new ConcurrentHashMap<>();
    static final Map<String, ChatServerProxy.Group> groups = new ConcurrentHashMap<>();
    static final Map<String, MulticastReceiver> receivers = new ConcurrentHashMap<>();
    static final Map<String, ChatServerProxy.Group> previousGroups = new ConcurrentHashMap<>();
    static final Map<String, Long> sendSequences = new ConcurrentHashMap<>();
    static final Map<String, Long> receiveSequences = new ConcurrentHashMap<>();
    static final Map<String, Map<Long, byte[]>> sentPackets = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        myName = askName();
        if (myName == null) return;

        try {
            privateSocket = new DatagramSocket(0);
            udpPort = privateSocket.getLocalPort();

            socket = new Socket(SERVER_HOST, ChatCommon.TCP_PORT);
            in = new BufferedReader(new InputStreamReader(
                    socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.UTF_8), true);

            send("HELLO|" + ChatCommon.b64(myName) + "|" + udpPort);

            String welcome = in.readLine();
            if (welcome == null || !welcome.startsWith("WELCOME|"))
                throw new IOException("Server khong gui WELCOME.");

            String[] w = welcome.split("\\|");
            myId = w[1];

            SwingUtilities.invokeAndWait(() -> {
                gui = new ClientGUI();
                gui.setVisible(true);
                gui.appendSystem("Da ket noi server.");
            });

            new Thread(ChatClient::readServer, "TCP-Reader").start();
            new Thread(ChatClient::readPrivate, "UDP-Private").start();

        } catch (Exception e) {
            JOptionPane.showMessageDialog(null,
                    "Khong ket noi duoc server: " + e.getMessage(),
                    "Loi", JOptionPane.ERROR_MESSAGE);
            System.exit(1);
        }
    }

    static String askName() {
        while (true) {
            String s = JOptionPane.showInputDialog(null,
                    "Nhap ten hien thi:",
                    "Chat Multicast - Dang nhap",
                    JOptionPane.QUESTION_MESSAGE);
            if (s == null) return null;
            s = s.trim();
            if (!s.isEmpty()) return s;
            JOptionPane.showMessageDialog(null, "Ten hien thi khong duoc rong.");
        }
    }

    static void send(String s) {
        synchronized (out) {
            out.println(s);
            out.flush();
        }
    }

    static void readServer() {
        try {
            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("STATE")) {
                    readState();
                } else if (line.startsWith("ERROR|")) {
                    String msg = ChatCommon.ub64(line.substring(6));
                    SwingUtilities.invokeLater(() ->
                            JOptionPane.showMessageDialog(gui, msg, "Thong bao",
                                    JOptionPane.WARNING_MESSAGE));
                } else if (line.startsWith("TTL|")) {
                    // TTL server gui rieng neu client yeu cau
                } else if (line.startsWith("RESEND|")) {
                    String[] p = line.split("\\|", -1);
                    if (p.length >= 3) resendPacket(p[1], Long.parseLong(p[2]));
                }
            }
        } catch (IOException e) {
            if (running) SwingUtilities.invokeLater(() ->
                    gui.appendSystem("Mat ket noi server."));
        }
    }

    static void readState() throws IOException {
        Map<String, ChatServerProxy.Client> newClients = new HashMap<>();
        Map<String, ChatServerProxy.Group> newGroups = new HashMap<>();

        String line;
        while ((line = in.readLine()) != null && !"END".equals(line)) {
            String[] p = line.split("\\|", -1);
            if (p[0].equals("CLIENT")) {
                ChatServerProxy.Client c = new ChatServerProxy.Client();
                c.id = p[1];
                c.name = ChatCommon.ub64(p[2]);
                c.ip = p[3];
                c.udpPort = Integer.parseInt(p[4]);
                newClients.put(c.id, c);
            } else if (p[0].equals("GROUP")) {
                ChatServerProxy.Group g = new ChatServerProxy.Group();
                g.id = p[1];
                g.name = ChatCommon.ub64(p[2]);
                g.ownerId = p[3];
                g.ip = p[4];
                g.port = Integer.parseInt(p[5]);
                g.ttl = Integer.parseInt(p[6]);
                if (!p[7].isEmpty())
                    g.members.addAll(Arrays.asList(p[7].split(",")));
                newGroups.put(g.id, g);
            }
        }

        detectMembershipChanges(newGroups);
        clients.clear();
        clients.putAll(newClients);
        previousGroups.clear();
        previousGroups.putAll(newGroups);
        groups.clear();
        groups.putAll(newGroups);

        updateReceivers();
        if (gui != null) SwingUtilities.invokeLater(() -> gui.refreshLists());
    }

    static void detectMembershipChanges(Map<String, ChatServerProxy.Group> newGroups) {
        for (Map.Entry<String, ChatServerProxy.Group> oldEntry : previousGroups.entrySet()) {
            String gid = oldEntry.getKey();
            ChatServerProxy.Group oldG = oldEntry.getValue();
            ChatServerProxy.Group newG = newGroups.get(gid);
            if (newG != null) {
                if (oldG.members.contains(myId) && !newG.members.contains(myId)) {
                    addLocalSystem("GROUP:" + gid, "Ban da bi roi/kick khoi nhom " + oldG.name + ".");
                }
                for (String id : oldG.members) {
                    if (!newG.members.contains(id) && id.equals(myId)) continue;
                }
            } else if (!"SERVER".equals(gid) && oldG.members.contains(myId)) {
                addLocalSystem("GROUP:" + gid, "Phong \"" + oldG.name + "\" da duoc giai tan.");
            }
        }
        for (Map.Entry<String, ChatServerProxy.Group> e : newGroups.entrySet()) {
            ChatServerProxy.Group oldG = previousGroups.get(e.getKey());
            ChatServerProxy.Group newG = e.getValue();
            if (oldG == null) continue;
            for (String id : oldG.members) {
                if (!newG.members.contains(id)) {
                    ChatServerProxy.Client c = clients.get(id);
                    String name = c == null ? id : c.name;
                    if (newG.members.contains(myId) && !id.equals(myId))
                        addLocalSystem("GROUP:" + newG.id, name + " da roi khoi phong.");
                }
            }
        }
    }

    static void addLocalSystem(String key, String text) {
        ChatCommon.ChatMessage m = new ChatCommon.ChatMessage();
        m.scope = "GROUP";
        m.senderId = "SYSTEM";
        m.senderName = "SYSTEM";
        m.groupId = key.startsWith("GROUP:") ? key.substring(6) : "";
        ChatServerProxy.Group g = groups.get(m.groupId);
        m.groupName = g == null ? "" : g.name;
        m.time = ChatCommon.now();
        m.text = text;
        historyAddFromState(key, m);
    }

    static void historyAddFromState(String key, ChatCommon.ChatMessage m) {
        if (gui != null) gui.addMessage(m);
    }

    static synchronized void updateReceivers() {
        Set<String> wanted = new HashSet<>();
        for (ChatServerProxy.Group g : groups.values()) {
            if (g.members.contains(myId)) wanted.add(g.id);
        }

        for (String id : new HashSet<>(receivers.keySet())) {
            if (!wanted.contains(id)) {
                MulticastReceiver r = receivers.remove(id);
                if (r != null) r.closeReceiver();
            }
        }

        for (String id : wanted) {
            if (!receivers.containsKey(id)) {
                ChatServerProxy.Group g = groups.get(id);
                if (g != null) {
                    MulticastReceiver r = new MulticastReceiver(g);
                    receivers.put(id, r);
                    new Thread(r, "MCAST-" + id).start();
                }
            }
        }
    }

    static void readPrivate() {
        byte[] buf = new byte[65535];
        while (running) {
            try {
                DatagramPacket p = new DatagramPacket(buf, buf.length);
                privateSocket.receive(p);
                String line = new String(p.getData(), p.getOffset(),
                        p.getLength(), StandardCharsets.UTF_8);
                ChatCommon.ChatMessage m = ChatCommon.ChatMessage.decode(line);
                if (m != null) {
                    if (!m.time.equals("")) gui.addMessage(m);
                }
            } catch (IOException e) {
                if (running) break;
            }
        }
    }

    static void checkSequence(ChatCommon.ChatMessage m) {
        if (!"GROUP".equals(m.scope) || m.senderId.equals(ChatClient.myId)) return;
        String key = m.groupId + "|" + m.senderId;
        long previous = receiveSequences.getOrDefault(key, 0L);
        if (m.seq > previous + 1 && previous > 0) {
            gui.appendSystem("Canh bao: mat datagram seq " + (previous + 1) + ".." +
                    (m.seq - 1) + " tu " + m.senderName + ". Dang yeu cau gui lai...");
            for (long seq = previous + 1; seq < m.seq; seq++) {
                send("RESEND_REQUEST|" + m.groupId + "|" + m.senderId + "|" + seq);
            }
        }
        if (m.seq > previous) receiveSequences.put(key, m.seq);
    }

    static void resendPacket(String groupId, long seq) {
        ChatServerProxy.Group g = ChatClient.groups.get(groupId);
        Map<Long, byte[]> cache = sentPackets.get(groupId);
        if (g == null || cache == null) return;
        byte[] data = cache.get(seq);
        if (data == null) return;
        try {
            InetAddress addr = InetAddress.getByName(g.ip);
            try (MulticastSocket ms = new MulticastSocket()) {
                ms.setTimeToLive(g.ttl);
                ms.setLoopbackMode(false);
                ms.send(new DatagramPacket(data, data.length, addr, g.port));
            }
        } catch (Exception e) {
            gui.appendSystem("Khong gui lai duoc seq " + seq + ": " + e.getMessage());
        }
    }

    static void sendGroup(ChatServerProxy.Group g, String text) {
        try {
            ChatCommon.ChatMessage m = makeMessage("GROUP", g.id, g.name, null, null, text);
            m.seq = sendSequences.merge(g.id, 1L, Long::sum);
            byte[] data = m.encode().getBytes(StandardCharsets.UTF_8);
            sentPackets.computeIfAbsent(g.id, k -> new ConcurrentHashMap<>()).put(m.seq, data);
            InetAddress addr = InetAddress.getByName(g.ip);
            try (MulticastSocket ms = new MulticastSocket()) {
                ms.setTimeToLive(g.ttl);
                ms.setLoopbackMode(false);
                ms.send(new DatagramPacket(data, data.length, addr, g.port));
            }
        } catch (Exception e) {
            gui.appendSystem("Loi gui group: " + e.getMessage());
        }
    }

    static void sendServer(String text) {
        ChatServerProxy.Group g = groups.get("SERVER");
        if (g != null) sendGroup(g, text);
    }

    static void sendPrivate(ChatServerProxy.Client target, String text) {
        try {
            ChatCommon.ChatMessage m = makeMessage("PRIVATE", "",
                    "", target.id, target.name, text);
            m.senderIp = InetAddress.getLocalHost().getHostAddress();

            byte[] data = m.encode().getBytes(StandardCharsets.UTF_8);
            InetAddress addr = InetAddress.getByName(target.ip);
            DatagramPacket p = new DatagramPacket(
                    data, data.length, addr, target.udpPort);
            privateSocket.send(p);

            // Server khong relay. Chi nhan ban sao de giam sat pham vi/private log.
            send("PRIVATE_MONITOR|" + m.encode());
        } catch (Exception e) {
            gui.appendSystem("Loi gui private: " + e.getMessage());
        }
    }

    static ChatCommon.ChatMessage makeMessage(String scope, String gid, String gname,
                                               String tid, String tname, String text) {
        ChatCommon.ChatMessage m = new ChatCommon.ChatMessage();
        m.scope = scope;
        m.senderId = myId;
        m.senderName = myName;
        m.groupId = gid;
        m.groupName = gname;
        m.targetId = tid == null ? "" : tid;
        m.targetName = tname == null ? "" : tname;
        m.time = ChatCommon.now();
        m.text = text;
        m.seq = 0;
        try { m.senderIp = InetAddress.getLocalHost().getHostAddress(); }
        catch (Exception e) { m.senderIp = ""; }
        return m;
    }

    static void leaveGroup(String gid) {
        if ("SERVER".equals(gid)) return;
        send("LEAVE_GROUP|" + gid);
    }

    static void disbandGroup(String gid) {
        if ("SERVER".equals(gid)) return;
        send("DISBAND_GROUP|" + gid);
    }

    static void createGroup(String name, int ttl, List<String> ids) {
        send("CREATE_GROUP|" + ChatCommon.b64(name) + "|" + ttl + "|" +
                String.join(",", ids));
    }

    static void addMembers(String gid, List<String> ids) {
        send("ADD_MEMBERS|" + gid + "|" + String.join(",", ids));
    }

    static class MulticastReceiver implements Runnable {
        final ChatServerProxy.Group group;
        volatile boolean active = true;
        MulticastSocket socket;
        InetAddress addr;

        MulticastReceiver(ChatServerProxy.Group group) {
            this.group = group;
        }

        public void run() {
            try {
                addr = InetAddress.getByName(group.ip);
                socket = new MulticastSocket(null);
                socket.setReuseAddress(true);
                socket.bind(new InetSocketAddress(group.port));
                socket.joinGroup(addr);

                byte[] buf = new byte[65535];
                while (active) {
                    DatagramPacket p = new DatagramPacket(buf, buf.length);
                    socket.receive(p);
                    String line = new String(p.getData(), p.getOffset(),
                            p.getLength(), StandardCharsets.UTF_8);
                    ChatCommon.ChatMessage m = ChatCommon.ChatMessage.decode(line);
                    if (m != null) {
                        checkSequence(m);
                        gui.addMessage(m);
                    }
                }
            } catch (Exception e) {
                if (active && gui != null)
                    gui.appendSystem("Receiver " + group.name + ": " + e.getMessage());
            } finally {
                try {
                    if (socket != null && addr != null) socket.leaveGroup(addr);
                } catch (Exception ignored) {}
                if (socket != null) socket.close();
            }
        }

        void closeReceiver() {
            active = false;
            if (socket != null) socket.close();
        }
    }
}
