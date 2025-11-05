
import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;

public class Server {

    public static void main(String[] args) {
        int port = 9000;
        Path receivePath = Paths.get("received_file.dat");
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server wait " + port + " for conect...");
            try {
                Socket clientSocket = serverSocket.accept();
                DataInputStream dataIn = new DataInputStream(clientSocket.getInputStream());
                FileChannel fileChannel = FileChannel.open(receivePath, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                System.out.println("Client connected");
                int transferMode = dataIn.readInt();
                long fileSize = dataIn.readLong();
                long startTime = System.currentTimeMillis();
                long bytesTransferred = 0;
                byte[] buffer = new byte[8192]; // 8KB
                int read;
                long remaining = fileSize;
                read = dataIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                FileOutputStream fileOut = new FileOutputStream(receivePath.toFile(), true);
                if (transferMode == 1) {
                    System.out.println("Start receive file by Standard I/O...");

                    read = dataIn.read(buffer);
                    while (read > 0) {
                        bytesTransferred += read;
                        remaining -= read;
                        fileOut.write(buffer, 0, read);
                        if (remaining == 0) {
                            break;
                        }
                    }
                } else {
                    System.out.println("start receive file by Standard I/O for zero copy");
                    while (remaining > 0 && read > 0) {
                        bytesTransferred += read;
                        remaining -= read;
                        fileOut.write(buffer, 0, read);
                    }
                }
                long endTime = System.currentTimeMillis();
                long duration = endTime - startTime;
                System.out.println("Receive Complete (" + bytesTransferred + " bytes)");
                System.out.println("Using Time " + duration + " ms");
            } catch (EOFException e) {
                System.err.println("Connection close by client before transfer success");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
