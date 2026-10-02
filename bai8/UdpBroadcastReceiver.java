import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UdpBroadcastReceiver {
    public static void main(String[] args) {
        try {
            DatagramSocket socket = new DatagramSocket(5005); // Lắng nghe trên cổng 5005
            byte[] buffer = new byte[1024];

            System.out.println("Đang chờ tin nhắn broadcast...");

            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);

                String message = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Nhận từ " + packet.getAddress() + ": " + message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
