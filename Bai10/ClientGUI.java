package Bai10;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class ClientGUI extends JFrame {
    DefaultListModel<String> serverModel = new DefaultListModel<>();
    DefaultListModel<String> onlineModel = new DefaultListModel<>();
    DefaultListModel<String> groupModel = new DefaultListModel<>();
    DefaultListModel<String> chatModel = new DefaultListModel<>();

    JList<String> serverList = new JList<>(serverModel);
    JList<String> onlineList = new JList<>(onlineModel);
    JList<String> groupList = new JList<>(groupModel);
    JList<String> chatList = new JList<>(chatModel);

    JTextField input = new JTextField();
    JLabel chatTitle = new JLabel("Chọn một phòng hoặc client");
    JLabel connection = new JLabel("ONLINE");

    String selectedType = "";
    String selectedId = "";

    Map<String, List<ChatCommon.ChatMessage>> history =
            new ConcurrentHashMap<>();

    JButton createBtn = new JButton("Tạo nhóm");
    JButton addBtn = new JButton("Thêm thành viên");
    JButton leaveBtn = new JButton("Rời nhóm");
    JButton disbandBtn = new JButton("Giải tán nhóm");
    JButton sendBtn = new JButton("Gửi");

    ClientGUI() {
        setTitle("Chat message");
        setSize(1180, 720);
        setMinimumSize(new Dimension(950, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.add(header(), BorderLayout.NORTH);

        JPanel left = leftPanel();
        JPanel right = rightPanel();

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, right);
        split.setDividerLocation(360);
        split.setResizeWeight(0.34);
        root.add(split, BorderLayout.CENTER);

        setContentPane(root);

        serverList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && serverList.getSelectedIndex() >= 0) {
                clearOtherSelections(serverList);
                selectedType = "SERVER";
                selectedId = "SERVER";
                chatTitle.setText("SERVER - Chat chung");
                showHistory("SERVER");
                updateButtons();
            }
        });

        onlineList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && onlineList.getSelectedIndex() >= 0) {
                clearOtherSelections(onlineList);
                String v = onlineList.getSelectedValue();
                String id = v.substring(0, v.indexOf(" |"));
                selectedType = "PRIVATE";
                selectedId = id;
                ChatServerProxy.Client c = ChatClient.clients.get(id);
                chatTitle.setText("Chat riêng - " + (c == null ? id : c.name));
                showHistory("PRIVATE:" + id);
                updateButtons();
            }
        });

        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && groupList.getSelectedIndex() >= 0) {
                clearOtherSelections(groupList);
                String v = groupList.getSelectedValue();
                String id = v.substring(0, v.indexOf(" |"));
                selectedType = "GROUP";
                selectedId = id;
                ChatServerProxy.Group g = ChatClient.groups.get(id);
                chatTitle.setText("Nhóm - " + (g == null ? id : g.name));
                showHistory("GROUP:" + id);
                updateButtons();
            }
        });

        sendBtn.addActionListener(e -> sendCurrent());
        input.addActionListener(e -> sendCurrent());
        createBtn.addActionListener(e -> showCreateDialog());
        addBtn.addActionListener(e -> showAddMembersDialog());
        leaveBtn.addActionListener(e -> leaveSelectedGroup());
        disbandBtn.addActionListener(e -> disbandSelectedGroup());
    }

    JPanel header() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(20,74,135));
        p.setBorder(new EmptyBorder(12,18,12,18));
        JLabel title = new JLabel("Chat message");
        title.setForeground(Color.WHITE);
        title.setFont(new Font("Arial", Font.BOLD, 24));
        JLabel name = new JLabel(ChatClient.myName);
        name.setForeground(Color.WHITE);
        name.setFont(new Font("Arial", Font.BOLD, 17));
        p.add(title, BorderLayout.WEST);
        p.add(name, BorderLayout.EAST);
        return p;
    }

    JPanel leftPanel() {
        JPanel p = new JPanel(new BorderLayout(6,6));
        p.setBorder(new EmptyBorder(8,8,8,5));
        p.setPreferredSize(new Dimension(360,0));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 3));
        styleButton(createBtn);
        styleButton(addBtn);
        styleButton(leaveBtn);
        styleButton(disbandBtn);
        top.add(createBtn);
        top.add(addBtn);
        top.add(leaveBtn);
        top.add(disbandBtn);
        p.add(top, BorderLayout.NORTH);

        JPanel lists = new JPanel();
        lists.setLayout(new BoxLayout(lists, BoxLayout.Y_AXIS));
        lists.add(section("SERVER", serverList, 70));
        lists.add(Box.createVerticalStrut(6));
        lists.add(section("CLIENT ĐANG ONLINE", onlineList, 180));
        lists.add(Box.createVerticalStrut(6));
        lists.add(section("NHÓM CỦA TÔI", groupList, 220));

        p.add(lists, BorderLayout.CENTER);
        return p;
    }

    void styleButton(JButton b) {
        b.setMargin(new Insets(5,7,5,7));
        b.setFocusable(false);
    }

    JPanel section(String title, JList<String> list, int h) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBorder(new TitledBorder(new LineBorder(new Color(60,100,160)), title));
        p.setPreferredSize(new Dimension(330,h));
        p.add(new JScrollPane(list), BorderLayout.CENTER);
        return p;
    }

    JPanel rightPanel() {
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(new EmptyBorder(10,5,10,10));

        JPanel top = new JPanel(new BorderLayout());
        chatTitle.setFont(new Font("Arial", Font.BOLD, 19));
        top.add(chatTitle, BorderLayout.WEST);
        connection.setForeground(new Color(0,130,60));
        top.add(connection, BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);

        ChatCellRenderer renderer = new ChatCellRenderer();
        chatList.setCellRenderer(renderer);
        p.add(new JScrollPane(chatList), BorderLayout.CENTER);

        JPanel composer = new JPanel(new BorderLayout(8,0));
        composer.setBorder(new EmptyBorder(5,0,0,0));
        input.setFont(new Font("Arial", Font.PLAIN, 15));
        composer.add(input, BorderLayout.CENTER);
        sendBtn.setPreferredSize(new Dimension(85,38));
        composer.add(sendBtn, BorderLayout.EAST);
        p.add(composer, BorderLayout.SOUTH);
        return p;
    }

    void refreshLists() {
        serverModel.clear();
        serverModel.addElement("SERVER | Tất cả client");

        String keepPrivate = selectedType.equals("PRIVATE") ? selectedId : null;
        String keepGroup = selectedType.equals("GROUP") ? selectedId : null;

        onlineModel.clear();
        for (ChatServerProxy.Client c : ChatClient.clients.values()) {
            if (!c.id.equals(ChatClient.myId))
                onlineModel.addElement(c.id + " | " + c.name);
        }

        groupModel.clear();
        for (ChatServerProxy.Group g : ChatClient.groups.values()) {
            if ("SERVER".equals(g.id)) continue;
            if (g.members.contains(ChatClient.myId))
                groupModel.addElement(g.id + " | " + g.name);
        }

        // Nếu nhóm đã bị giải tán hoặc client bị loại khỏi nhóm thì chuyển về SERVER.
        if ("GROUP".equals(selectedType) &&
                (!ChatClient.groups.containsKey(selectedId) ||
                 !ChatClient.groups.get(selectedId).members.contains(ChatClient.myId))) {
            selectedType = "SERVER";
            selectedId = "SERVER";
            serverList.setSelectedIndex(0);
            chatTitle.setText("SERVER - Chat chung");
            showHistory("SERVER");
        }
        updateButtons();
    }

    void clearOtherSelections(JList<?> current) {
        if (current != serverList) serverList.clearSelection();
        if (current != onlineList) onlineList.clearSelection();
        if (current != groupList) groupList.clearSelection();
    }

    void showHistory(String key) {
        chatModel.clear();
        for (ChatCommon.ChatMessage m : history.getOrDefault(key, List.of()))
            chatModel.addElement(m.displayHtml());
        if (chatModel.size() > 0)
            chatList.ensureIndexIsVisible(chatModel.size()-1);
    }

    void addMessage(ChatCommon.ChatMessage m) {
        String key;
        if ("PRIVATE".equals(m.scope)) {
            String other = m.senderId.equals(ChatClient.myId)
                    ? m.targetId : m.senderId;
            key = "PRIVATE:" + other;
        } else {
            key = "SERVER".equals(m.groupId) ? "SERVER" : "GROUP:" + m.groupId;
        }

        history.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(m);

        if (key.equals(currentKey())) {
            SwingUtilities.invokeLater(() -> {
                chatModel.addElement(m.displayHtml());
                if (chatModel.size() > 0)
                    chatList.ensureIndexIsVisible(chatModel.size()-1);
            });
        }
    }

    String currentKey() {
        if ("PRIVATE".equals(selectedType)) return "PRIVATE:" + selectedId;
        if ("GROUP".equals(selectedType)) return "GROUP:" + selectedId;
        return "SERVER";
    }

    void sendCurrent() {
        String text = input.getText().trim();
        if (text.isEmpty()) return;

        if ("SERVER".equals(selectedType)) {
            ChatClient.sendServer(text);
        } else if ("GROUP".equals(selectedType)) {
            ChatServerProxy.Group g = ChatClient.groups.get(selectedId);
            if (g != null && g.members.contains(ChatClient.myId))
                ChatClient.sendGroup(g, text);
        } else if ("PRIVATE".equals(selectedType)) {
            ChatServerProxy.Client c = ChatClient.clients.get(selectedId);
            if (c != null) {
                ChatCommon.ChatMessage local =
                        ChatClient.makeMessage("PRIVATE", "", "",
                                c.id, c.name, text);
                addMessage(local);
                ChatClient.sendPrivate(c, text);
            }
        }
        input.setText("");
        input.requestFocus();
    }

    void showCreateDialog() {
        JTextField nameField = new JTextField();
        JSpinner ttl = new JSpinner(new SpinnerNumberModel(8,1,32,1));

        JPanel top = new JPanel(new GridLayout(2,2,6,6));
        top.add(new JLabel("Tên nhóm:"));
        top.add(nameField);
        top.add(new JLabel("TTL:"));
        top.add(ttl);

        JPanel members = new JPanel();
        members.setLayout(new BoxLayout(members, BoxLayout.Y_AXIS));
        Map<String,JCheckBox> boxes = new LinkedHashMap<>();
        for (ChatServerProxy.Client c : ChatClient.clients.values()) {
            if (!c.id.equals(ChatClient.myId)) {
                JCheckBox cb = new JCheckBox(c.name + " [" + c.ip + "]");
                boxes.put(c.id, cb);
                members.add(cb);
            }
        }

        JPanel all = new JPanel(new BorderLayout(8,8));
        all.add(top, BorderLayout.NORTH);
        all.add(new JScrollPane(members), BorderLayout.CENTER);
        all.setPreferredSize(new Dimension(460,350));

        int result = JOptionPane.showConfirmDialog(this, all,
                "Tạo nhóm mới", JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            String name = nameField.getText().trim();
            if (name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Tên nhóm không được rỗng.");
                return;
            }
            List<String> ids = new ArrayList<>();
            for (Map.Entry<String,JCheckBox> e : boxes.entrySet())
                if (e.getValue().isSelected()) ids.add(e.getKey());
            ChatClient.createGroup(name, (Integer)ttl.getValue(), ids);
        }
    }

    void showAddMembersDialog() {
        if (!"GROUP".equals(selectedType)) return;
        ChatServerProxy.Group g = ChatClient.groups.get(selectedId);
        if (g == null || !g.ownerId.equals(ChatClient.myId)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ client tạo nhóm mới được thêm thành viên.");
            return;
        }

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        Map<String,JCheckBox> boxes = new LinkedHashMap<>();
        for (ChatServerProxy.Client c : ChatClient.clients.values()) {
            if (!g.members.contains(c.id) && !c.id.equals(ChatClient.myId)) {
                JCheckBox cb = new JCheckBox(c.name + " [" + c.ip + "]");
                boxes.put(c.id, cb);
                panel.add(cb);
            }
        }

        int result = JOptionPane.showConfirmDialog(this,
                new JScrollPane(panel), "Thêm thành viên",
                JOptionPane.OK_CANCEL_OPTION);
        if (result == JOptionPane.OK_OPTION) {
            List<String> ids = new ArrayList<>();
            for (Map.Entry<String,JCheckBox> e : boxes.entrySet())
                if (e.getValue().isSelected()) ids.add(e.getKey());
            ChatClient.addMembers(g.id, ids);
        }
    }

    void leaveSelectedGroup() {
        if (!"GROUP".equals(selectedType)) return;
        ChatServerProxy.Group g = ChatClient.groups.get(selectedId);
        if (g == null) return;

        int r = JOptionPane.showConfirmDialog(this,
                "Bạn có muốn rời nhóm \"" + g.name + "\"?",
                "Rời nhóm", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ChatClient.leaveGroup(g.id);
            selectedType = "SERVER";
            selectedId = "SERVER";
            serverList.setSelectedIndex(0);
        }
    }

    void disbandSelectedGroup() {
        if (!"GROUP".equals(selectedType)) return;
        ChatServerProxy.Group g = ChatClient.groups.get(selectedId);
        if (g == null) return;

        if (!g.ownerId.equals(ChatClient.myId)) {
            JOptionPane.showMessageDialog(this,
                    "Chỉ client tạo nhóm mới được giải tán nhóm.");
            return;
        }

        int r = JOptionPane.showConfirmDialog(this,
                "Giải tán nhóm \"" + g.name + "\" cho tất cả thành viên?",
                "Giải tán nhóm", JOptionPane.YES_NO_OPTION);
        if (r == JOptionPane.YES_OPTION) {
            ChatClient.disbandGroup(g.id);
            selectedType = "SERVER";
            selectedId = "SERVER";
            serverList.setSelectedIndex(0);
        }
    }

    void updateButtons() {
        boolean group = "GROUP".equals(selectedType);
        ChatServerProxy.Group g = group ? ChatClient.groups.get(selectedId) : null;
        boolean owner = g != null && g.ownerId.equals(ChatClient.myId);

        addBtn.setEnabled(owner);
        leaveBtn.setEnabled(group);
        disbandBtn.setEnabled(owner);
    }

    void appendSystem(String text) {
        SwingUtilities.invokeLater(() -> {
            // Hiển thị thông báo kết nối trong status/chat nếu chưa chọn nội dung.
            connection.setText(text);
        });
    }

    class ChatCellRenderer extends JPanel implements ListCellRenderer<String> {
        JLabel label = new JLabel();
        ChatCellRenderer() {
            setLayout(new BorderLayout());
            label.setBorder(new EmptyBorder(7,9,7,9));
            add(label, BorderLayout.CENTER);
        }

        public Component getListCellRendererComponent(JList<? extends String> list,
                String value, int index, boolean isSelected, boolean cellHasFocus) {
            label.setText(value.startsWith("<html>") ? value :
                    "<html>" + value.replace("&","&amp;")
                    .replace("<","&lt;").replace(">","&gt;") + "</html>");
            boolean mine = value.contains("] " + ChatClient.myName + ":") ||
                    value.contains("] <b>" + ChatClient.myName + "</b>:");
            label.setHorizontalAlignment(mine ? SwingConstants.RIGHT : SwingConstants.LEFT);
            setBorder(new EmptyBorder(3, 8, 3, 8));
            setBackground(isSelected ? new Color(220,235,250) : Color.WHITE);
            label.setOpaque(true);
            label.setBackground(isSelected ? new Color(220,235,250) : Color.WHITE);
            return this;
        }
    }
}
