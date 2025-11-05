import java.io.*;
import java.net.*;
import java.nio.channels.*;
import java.nio.file.*;

public class Client {

    private static final int SERVER_PORT = 9000;
    
    // โหมด 1: Standard I/O | โหมดอื่นๆ: Zero-Copy
    public void transferFile(String serverIp, Path filePath, int transferMode) throws IOException {
        Socket socket = null;
        DataOutputStream dataOut = null;
        FileChannel fileChannel = null;
        FileInputStream fileIn = null;

        try {
            // 1. เชื่อมต่อ Socket
            socket = new Socket(serverIp, SERVER_PORT);
            dataOut = new DataOutputStream(socket.getOutputStream());
            long fileSize = Files.size(filePath);

            // 2. ส่ง Header (Mode, Size) ให้ Server
            dataOut.writeInt(transferMode);
            dataOut.writeLong(fileSize);
            dataOut.flush();
            
            long startTime = System.currentTimeMillis();
            String modeName;

            if (transferMode == 1) {
                // --- โหมด 1: Standard I/O ---
                modeName = "Standard I/O";
                fileIn = new FileInputStream(filePath.toFile());
                byte[] buffer = new byte[8192];
                int read;

                while ((read = fileIn.read(buffer)) > 0) {
                    dataOut.write(buffer, 0, read);
                }
                dataOut.flush();
                
            } else {
                // --- โหมดอื่นๆ: Zero-Copy ---
                modeName = "Zero-Copy";
                fileChannel = FileChannel.open(filePath, StandardOpenOption.READ);
                
                // ใช้ Zero-Copy: transferTo สั่งให้ Kernel โอนข้อมูลจากไฟล์ไปยัง Socket โดยตรง
                fileChannel.transferTo(0, fileSize, socket.getChannel());
            }

            socket.shutdownOutput(); // แจ้งให้ Server ทราบว่าการส่งเสร็จสิ้น

            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            System.out.println("โอนไฟล์สำเร็จ! (" + modeName + ") ใช้เวลา: " + duration + " ms");

        } finally {
            // 3. ปิด Resource ใน finally Block
            try { if (fileChannel != null) fileChannel.close(); } catch (IOException e) {}
            try { if (fileIn != null) fileIn.close(); } catch (IOException e) {}
            try { if (dataOut != null) dataOut.close(); } catch (IOException e) {}
            try { if (socket != null) socket.close(); } catch (IOException e) {}
        }
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.out.println("การใช้งาน: java FileTransferClient <Server IP> <File Path> <Mode (1=Normal, อื่นๆ=Zero-Copy)>");
            return;
        }
        
        try {
            String serverIp = args[0];
            Path filePath = Paths.get(args[1]);
            int transferMode = Integer.parseInt(args[2]);

            if (!Files.exists(filePath)) {
                System.err.println("ไม่พบไฟล์: " + filePath);
                return;
            }

            Client client = new Client();
            client.transferFile(serverIp, filePath, transferMode);

        } catch (IOException e) {
            System.err.println("เกิดข้อผิดพลาดในการโอนไฟล์/เชื่อมต่อ:");
            e.printStackTrace();
        } catch (NumberFormatException e) {
             System.err.println("Mode ต้องเป็นตัวเลขเท่านั้น (1 หรือ 2)");
        }
    }
}
