import java.net.*;

public class Receiver {
    public static void main(String[] args) {
        try{
            DatagramSocket socket = new DatagramSocket(5005);
            byte[] buffer = new byte[1024];
            System.out.println("Dang nhan tin nhan");
            while(true){
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String mess = new String(packet.getData(), 0, packet.getLength());
                System.out.println("Co tin Nhan" + mess);
            }
        }
        catch(Exception e){

        }
    }
}
