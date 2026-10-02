import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpBroadcastSender1 {
    public static void main(String[] args) {
        try {
            // Tạo socket UDP
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true); // Cho phép broadcast

            // Nội dung tin nhắn
            String message = "Xin chào mọi người trong mạng!";
            byte[] buffer = message.getBytes();

            // Địa chỉ broadcast toàn mạng cục bộ
            InetAddress broadcastAddress = InetAddress.getByName("255.255.255.255");

            // Tạo gói tin UDP
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 5005);

            // Gửi gói tin
            socket.send(packet);

            socket.close();
            System.out.println("Đã gửi broadcast tới 255.255.255.255: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
