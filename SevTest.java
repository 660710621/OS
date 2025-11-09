import java.io.*;
import java.net.*;
import java.nio.file.*;

public class SevTest {
    private static final int SERVER_PORT = 9000;
    private static final Path RECEIVED_PATH = Paths.get("received_file.dat"); 

    public static void main(String[] args) {
        // ประกาศตัวแปร Resource
        ServerSocket serverSocket = null;
        Socket clientSocket = null;
        DataInputStream dataIn = null;
        FileOutputStream fileOut = null;
        try {
            // 1. สร้าง ServerSocket และรอการเชื่อมต่อ
            serverSocket = new ServerSocket(SERVER_PORT);
            System.out.println("Server รอการเชื่อมต่อที่พอร์ต " + SERVER_PORT + "...");

            clientSocket = serverSocket.accept();
            System.out.println("Client เชื่อมต่อสำเร็จ: " + clientSocket.getInetAddress());

            dataIn = new DataInputStream(clientSocket.getInputStream());
            
            // 2. อ่าน Header (Mode, Size)
            int transferMode = dataIn.readInt();
            long fileSize = dataIn.readLong();
            long startTime = System.currentTimeMillis();
            
            // เตรียมไฟล์ปลายทาง
            fileOut = new FileOutputStream(RECEIVED_PATH.toFile());

            // 3. เริ่มรับไฟล์ด้วย Standard I/O
            long bytesTransferred = 0;
            long remaining = fileSize;
            String modeName = (transferMode == 1) ? "Standard I/O" : "Standard I/O (Client Zero-Copy)";
            System.out.println("เริ่มรับไฟล์ด้วย " + modeName + " ขนาด: " + fileSize + " bytes");

            byte[] buffer = new byte[8192]; //8KB buffer
            int read ;
            // ลูปรับข้อมูลจนกว่าจะครบตามขนาดไฟล์ที่ Header บอก
            while (remaining > 0 ) {
                read =dataIn.read(buffer, 0, (int) Math.min(buffer.length, remaining));
                System.out.println("`Read:"+ read);
                if(read<0){
                    break;
                }
                
                bytesTransferred += read;
                remaining -= read;
                System.out.println("`Remaining:"+ remaining);
                fileOut.write(buffer, 0, read);
            }
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;

            if (bytesTransferred == fileSize) {
                System.out.println("รับไฟล์สำเร็จ (" + bytesTransferred + " bytes)");
            } else {
                System.err.println("การโอนถ่ายไฟล์ไม่สมบูรณ์! ได้รับ " + bytesTransferred + " จาก " + fileSize + " bytes");
            }
            System.out.println("ใช้เวลา: " + duration + " ms");

        } catch (EOFException e) {
            System.err.println("การเชื่อมต่อถูกปิดโดย Client ก่อนโอนถ่ายไฟล์เสร็จสมบูรณ์");
        } catch (IOException e) {
            System.err.println("เกิดข้อผิดพลาด I/O: " + e.getMessage());
        } finally {
            // 4. ปิด Resource ใน finally Block
            // try { if (fileOut != null) fileOut.close(); } catch (IOException e) {}
            // try { if (dataIn != null) dataIn.close(); } catch (IOException e) {}
            // try { if (clientSocket != null) clientSocket.close(); } catch (IOException e) {}
            // try { if (serverSocket != null) serverSocket.close(); } catch (IOException e) {}
            System.out.println("Server ปิดการทำงาน");
        }
    }
}