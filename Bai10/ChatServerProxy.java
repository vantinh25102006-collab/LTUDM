package Bai10;
import java.util.HashSet;
import java.util.Set;

public class ChatServerProxy {
    static class Client {
        String id, name, ip;
        int udpPort;
        public String toString() { return name; }
    }

    static class Group {
        String id, name, ownerId, ip;
        int port, ttl;
        Set<String> members = new HashSet<>();
        public String toString() { return name; }
    }
}
