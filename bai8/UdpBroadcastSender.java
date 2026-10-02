
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class UdpBroadcastSender {
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true); // Cho phép broadcast

            String message = "Xin chào mọi người trong mạng!";
            byte[] buffer = message.getBytes();

            // Địa chỉ broadcast của subnet, ví dụ 192.168.1.255
            InetAddress broadcastAddress = InetAddress.getByName("192.168.1.255");

            DatagramPacket packet = new DatagramPacket(buffer, buffer.length, broadcastAddress, 5005);
            socket.send(packet);

            socket.close();
            System.out.println("Đã gửi broadcast: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
