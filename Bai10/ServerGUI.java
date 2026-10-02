package Bai10;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ServerGUI extends JFrame {
    DefaultListModel<String> groupModel = new DefaultListModel<>();
    DefaultListModel<String> clientModel = new DefaultListModel<>();
    DefaultListModel<String> scopeModel = new DefaultListModel<>();
    DefaultListModel<String> chatModel = new DefaultListModel<>();

    JList<String> groupList = new JList<>(groupModel);
    JList<String> clientList = new JList<>(clientModel);
    JList<String> scopeList = new JList<>(scopeModel);
    JList<String> chatList = new JList<>(chatModel);

    Map<String, ChatServer.GroupInfo> groupSnapshot = new HashMap<>();
    Map<String, Long> receiveSequences = new HashMap<>();
    Map<String, java.util.List<ChatCommon.ChatMessage>> history = new ConcurrentHashMap<>();
    JLabel status = new JLabel("Server");
    JSpinner ttlSpinner = new JSpinner(new SpinnerNumberModel(8, 1, 32, 1));
    DefaultListModel<String> logModel = new DefaultListModel<>();

    ServerGUI() {
        setTitle("Chat message");
        setSize(1120, 700);
        setMinimumSize(new Dimension(900, 580));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header(), BorderLayout.NORTH);

        JPanel left = new JPanel(new BorderLayout(8,8));
        left.setBorder(new EmptyBorder(10,10,10,5));
        left.setPreferredSize(new Dimension(360, 0));
        left.add(leftContent(), BorderLayout.CENTER);

        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new EmptyBorder(10,5,10,10));
        right.add(chatHeader(), BorderLayout.NORTH);
        right.add(new JScrollPane(chatList), BorderLayout.CENTER);

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(360);
        split.setResizeWeight(0.35);

        root.add(split, BorderLayout.CENTER);
        root.add(footer(), BorderLayout.SOUTH);
        setContentPane(root);

        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) showSelectedGroup();
        });
    }

    JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(12,18,12,18));
        p.setBackground(new Color(20,74,135));
        JLabel title = new JLabel("Chat message");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel server = new JLabel("SERVER");
        server.setForeground(Color.WHITE);
        server.setFont(new Font("Arial", Font.BOLD, 17));
        p.add(title, BorderLayout.WEST);
        p.add(server, BorderLayout.EAST);
        return p;
    }

    JPanel leftContent() {
        JPanel all = new JPanel();
        all.setLayout(new BoxLayout(all, BoxLayout.Y_AXIS));

        all.add(section("NHÓM CHAT", groupList, 160));
        all.add(Box.createVerticalStrut(8));
        all.add(section("CLIENT ĐANG ONLINE", clientList, 130));
        all.add(Box.createVerticalStrut(8));
        all.add(section("PHẠM VI TIN NHẮN", scopeList, 170));
        return all;
    }

    JPanel section(String title, JList<String> list, int h) {
        JPanel p = new JPanel(new BorderLayout(3,3));
        p.setBorder(new TitledBorder(new LineBorder(new Color(60,100,160)),
                title));
        p.setPreferredSize(new Dimension(330,h));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    JPanel chatHeader() {
        JPanel p = new JPanel(new BorderLayout());
        JLabel label = new JLabel("Nội dung chat nhóm");
        label.setFont(new Font("Arial", Font.BOLD, 18));
        ttlSpinner.addChangeListener(e -> {
            ChatServer.defaultTTL = (Integer) ttlSpinner.getValue();
        });
        JPanel config = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        config.add(new JLabel("TTL mặc định:"));
        config.add(ttlSpinner);
        p.add(label, BorderLayout.WEST);
        p.add(config, BorderLayout.EAST);
        return p;
    }

    JPanel footer() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new EmptyBorder(5,10,8,10));
        status.setForeground(new Color(20,74,135));
        p.add(status, BorderLayout.WEST);
        JLabel info = new JLabel("Server chỉ quản lý/giám sát - không gửi tin nhắn");
        info.setForeground(new Color(120,120,120));
        p.add(info, BorderLayout.EAST);
        return p;
    }

    void refreshState() {
        SwingUtilities.invokeLater(() -> {
            String selected = groupList.getSelectedValue();
            groupModel.clear();
            groupSnapshot.clear();
            for (ChatServer.GroupInfo g : ChatServer.groups.values()) {
                groupModel.addElement(g.id + " | " + g.name +
                        " | TTL=" + g.ttl);
                groupSnapshot.put(g.id, g);
            }

            clientModel.clear();
            for (ChatServer.ClientInfo c : ChatServer.clients.values()) {
                clientModel.addElement(c.name + " [" + c.ip + ":" + c.udpPort + "]");
            }
            status.setText("Online: " + ChatServer.clients.size() +
                    " | Phòng: " + (ChatServer.groups.size()-1));
        });
    }

    void addChat(ChatCommon.ChatMessage m) {
        if ("GROUP".equals(m.scope)) {
            String key = m.groupId + "|" + m.senderId;
            long old = receiveSequences.getOrDefault(key, 0L);
            if (old > 0 && m.seq > old + 1) {
                addScopeWarning(m, "MAT DATAGRAM seq " + (old + 1) + ".." + (m.seq - 1));
            }
            if (m.seq > old) receiveSequences.put(key, m.seq);
        }
        history.computeIfAbsent(m.groupId, k -> new ArrayList<>()).add(m);
        if (groupList.getSelectedValue() != null &&
                groupList.getSelectedValue().startsWith(m.groupId + " | ")) {
            SwingUtilities.invokeLater(() -> chatModel.addElement(m.displayText()));
        }
    }

    void addScopeWarning(ChatCommon.ChatMessage m, String warning) {
        SwingUtilities.invokeLater(() -> scopeModel.addElement("[WARNING] " + warning + " | " + m.groupName + " | " + m.time));
    }

    void addScope(ChatCommon.ChatMessage m) {
        SwingUtilities.invokeLater(() -> {
            String target = "GROUP".equals(m.scope) ? m.groupName :
                    ("PRIVATE".equals(m.scope) ? m.targetName : "ALL CLIENT");
            scopeModel.addElement("[" + m.scope + "] " + m.senderName +
                    " -> " + target + " | " + m.time + " | " + m.text);
            while (scopeModel.size() > 300) scopeModel.remove(0);
        });
    }

    void showSelectedGroup() {
        String v = groupList.getSelectedValue();
        if (v == null) return;
        String id = v.split(" \\|")[0];
        chatModel.clear();
        for (ChatCommon.ChatMessage m : history.getOrDefault(id, List.of()))
            chatModel.addElement(m.displayText());
    }

    void appendSystem(String s) {
        if (logModel != null) {
            logModel.addElement("[" + ChatCommon.now() + "] " + s);
        }
        SwingUtilities.invokeLater(() ->
                status.setText(s));
    }

    void joinMonitor(ChatServer.GroupInfo g) {
        try {
            InetAddress addr = InetAddress.getByName(g.mcastIp);
            MulticastSocket socket = new MulticastSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(g.port));
            socket.joinGroup(addr);
            byte[] buf = new byte[65535];
            appendSystem("Server dang giam sat " + g.name);
            while (ChatServer.groups.containsKey(g.id)) {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);
                String line = new String(packet.getData(), packet.getOffset(),
                        packet.getLength(), StandardCharsets.UTF_8);
                ChatCommon.ChatMessage m = ChatCommon.ChatMessage.decode(line);
                if (m != null) ChatServer.monitorChat(m);
            }
            socket.leaveGroup(addr);
            socket.close();
        } catch (Exception e) {
            appendSystem("Monitor " + g.name + ": " + e.getMessage());
        }
    }
}
