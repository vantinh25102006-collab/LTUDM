package Bai10;

import java.net.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;

public class ChatCommon {
    public static final int TCP_PORT = 5000;
    public static final String SERVER_GROUP = "SERVER";
    public static final String SERVER_MCAST_IP = "239.1.1.1";
    public static final int SERVER_MCAST_PORT = 8000;
    public static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static String b64(String s) {
        return Base64.getEncoder().encodeToString(
                s.getBytes(StandardCharsets.UTF_8));
    }

    public static String ub64(String s) {
        return new String(Base64.getDecoder().decode(s), StandardCharsets.UTF_8);
    }

    public static String now() {
        return LocalDateTime.now().format(TIME);
    }

    public static String safe(String s) {
        return s == null ? "" : s.replace("|", "/").replace("\n", " ");
    }

    public static class ChatMessage {
        public String scope;       // SERVER, GROUP, PRIVATE
        public String senderId;
        public String senderName;
        public String targetId;
        public String targetName;
        public String groupId;
        public String groupName;
        public String time;
        public String text;
        public String senderIp;
        public long seq;

        public String encode() {
            return String.join("|",
                    "CHAT",
                    safe(scope),
                    safe(senderId),
                    b64(senderName),
                    safe(targetId),
                    b64(targetName == null ? "" : targetName),
                    safe(groupId),
                    b64(groupName == null ? "" : groupName),
                    safe(time),
                    b64(text),
                    safe(senderIp),
                    String.valueOf(seq)
            );
        }

        public static ChatMessage decode(String line) {
            try {
                String[] p = line.split("\\|", -1);
                if (p.length < 12 || !"CHAT".equals(p[0])) return null;
                ChatMessage m = new ChatMessage();
                m.scope = p[1];
                m.senderId = p[2];
                m.senderName = ub64(p[3]);
                m.targetId = p[4];
                m.targetName = ub64(p[5]);
                m.groupId = p[6];
                m.groupName = ub64(p[7]);
                m.time = p[8];
                m.text = ub64(p[9]);
                m.senderIp = p[10];
                m.seq = Long.parseLong(p[11]);
                return m;
            } catch (Exception e) {
                return null;
            }
        }

        public String displayText() {
            return "[" + time + "] " + senderName + ": " + text;
        }
    }
}
