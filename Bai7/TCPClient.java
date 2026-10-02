
package Bai7;

import java.io.*;
import java.net.*;
import java.util.Scanner;

public class TCPClient {

    public static void main(String[] args) {

        Scanner scanner = new Scanner(System.in);

        System.out.println("================================");
        System.out.println("       TCP CHAT CLIENT");
        System.out.println("================================");

        try {

            // Nhap IP Server
            System.out.print("Nhap IP Server: ");
            String host = scanner.nextLine();

            // Nhap Port
            System.out.print("Nhap Port: ");
            int port =
                    Integer.parseInt(scanner.nextLine());

            // Ket noi Server
            Socket socket =
                    new Socket(host, port);

            System.out.println(
                    "Da ket noi Server!"
            );

            // ==============================================
            // READER
            // Nhan du lieu tu Server
            // ==============================================

            BufferedReader reader =
                    new BufferedReader(
                            new InputStreamReader(
                                    socket.getInputStream()
                            )
                    );

            // ==============================================
            // WRITER
            // Gui du lieu den Server
            // ==============================================

            PrintWriter writer =
                    new PrintWriter(
                            socket.getOutputStream(),
                            true
                    );

            // ==============================================
            // THREAD NHAN MESSAGE
            // ==============================================

            Thread receiveThread =
                    new Thread(() -> {

                        try {

                            String message;

                            while (
                                    (message =
                                            reader.readLine())
                                            != null
                            ) {

                                System.out.println(
                                        "\n" + message
                                );

                                System.out.print(
                                        "> "
                                );
                            }

                        } catch (IOException e) {

                            System.out.println(
                                    "\nDa mat ket noi Server."
                            );
                        }
                    });

            receiveThread.start();

            // ==============================================
            // GUI MESSAGE
            // ==============================================

            while (true) {

                System.out.print("> ");

                String message =
                        scanner.nextLine();

                writer.println(message);

                // Thoat
                if (message.equalsIgnoreCase("/quit")) {

                    break;
                }
            }

            socket.close();

            System.out.println(
                    "Client da dong."
            );

        } catch (IOException e) {

            System.out.println(
                    "Khong the ket noi Server."
            );

            System.out.println(
                    "Chi tiet: "
                    + e.getMessage()
            );

        } finally {

            scanner.close();
        }
    }
}

