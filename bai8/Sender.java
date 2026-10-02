import java.net.*;
import java.util.Scanner;

public class Sender {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        try {
            DatagramSocket socket = new DatagramSocket();
            socket.setBroadcast(true);

            InetAddress inet = InetAddress.getByName("192.168.33.255");
            while (true) {
                System.out.print("Moi nhap tin nhan: ");
                String message = sc.nextLine();
                byte[] buffer = message.getBytes();
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, inet, 5005);
                socket.send(packet);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
