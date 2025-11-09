import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class ServerHandler implements Runnable {
    private final Socket clientSocket;
    private final File sharedDir = Server.SHARED_DIR;
    

    public ServerHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (Socket socket = this.clientSocket;
             DataInputStream input = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
             DataOutputStream output = new DataOutputStream(socket.getOutputStream())) {

            // 1. รับคำสั่งแรกจากไคลเอนต์
            String receivedData = input.readUTF();
            
            // ตรวจสอบว่าคำสั่งนี้เป็นการขอขนาดไฟล์ หรือเป็นการขอส่วนไฟล์ย่อย
            if (receivedData.contains(",")) {
                // *** A. จัดการคำขอส่วนไฟล์ย่อย (จาก Threaddownload) ***
                handleChunkRequest(receivedData, output);
            } else {
                // *** B. จัดการคำขอขนาดไฟล์ (จาก Main Client) ***
                handleFileSizeRequest(receivedData, output);
            }

        } catch (EOFException e) {
            // การเชื่อมต่อปิดลงอย่างปกติ
            System.out.println("Client disconnected normally.");
        } catch (IOException e) {
            System.err.println("Handler exception: " + e.getMessage());
        }
    }
    
    // --- ฟังก์ชัน A: จัดการคำขอส่วนไฟล์ย่อย ---
    private void handleChunkRequest(String dataPacket, DataOutputStream output) throws IOException {
        String[] parts = dataPacket.split(",");
        if (parts.length != 4) {
            System.err.println("Invalid data packet format: " + dataPacket);
            return;
        }

        String fileName = parts[0];
        // parts[1] คือ indexthread (ไม่จำเป็นสำหรับเซิร์ฟเวอร์)
        long startByte = Long.parseLong(parts[2]);
        long endByte = Long.parseLong(parts[3]);

        File fileToServe = new File(sharedDir, fileName);
        if (!fileToServe.exists()) {
            System.err.println("File not found: " + fileName);
            // ไม่มีวิธีมาตรฐานในการตอบกลับ error ใน DataOutputStream สำหรับกรณีนี้
            // เซิร์ฟเวอร์ทำได้แค่ปิด Socket หรือไม่ส่งข้อมูล
            return; 
        }

        // ใช้ RandomAccessFile เพื่อเข้าถึงและอ่านไฟล์เฉพาะช่วง
        try (RandomAccessFile raf = new RandomAccessFile(fileToServe, "r")) {
            long bytesToSend = endByte - startByte + 1;
            long bytesRemaining = bytesToSend;
            
            // เลื่อน Pointer ไปยังจุดเริ่มต้นที่ร้องขอ
            raf.seek(startByte);
            
            byte[] buffer = new byte[64 * 1024]; // 64KB buffer
            int bytesRead;

            System.out.println("Sending chunk " + startByte + "-" + endByte + " for " + fileName);

            // วนลูปเพื่ออ่านและส่งข้อมูลตามจำนวนไบต์ที่เหลือที่ต้องส่ง
            while (bytesRemaining > 0 && 
                   (bytesRead = raf.read(buffer, 0, (int) Math.min(buffer.length, bytesRemaining))) > 0) {
                
                output.write(buffer, 0, bytesRead);
                bytesRemaining -= bytesRead;
            }
            output.flush();
            System.out.println("Finished sending chunk. Bytes sent: " + (bytesToSend - bytesRemaining));

        } catch (IOException e) {
            System.err.println("Error reading/sending file chunk: " + e.getMessage());
        }
    }
    
    // --- ฟังก์ชัน B: จัดการคำขอขนาดไฟล์ ---
    private void handleFileSizeRequest(String fileName, DataOutputStream output) throws IOException {
        File file = new File(sharedDir, fileName);
        long fileSize;
        
        if (file.exists() && file.isFile()) {
            fileSize = file.length();
            System.out.println("Client requested file size for: " + fileName + ". Size: " + fileSize);
        } else {
            fileSize = -1; // ส่ง -1 กลับไปเพื่อระบุว่าไฟล์ไม่พบ
            System.out.println("Client requested file size for non-existent file: " + fileName);
        }

        // เซิร์ฟเวอร์ส่งขนาดไฟล์กลับไปยัง Main Client
        output.writeLong(fileSize);
        output.flush();
    }
}
public class Server {
    
    static final File SHARED_DIR = new File("SharedFiles");
    static final ExecutorService threadPool = Executors.newFixedThreadPool(16); // ใช้ Thread Pool 10 ตัว

    public static void main(String[] args) {
        int PORT;
        try {
            PORT = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[0]);
            return;
        }
        if (!SHARED_DIR.exists()) {
            SHARED_DIR.mkdir();
            System.out.println("Created shared directory at: " + SHARED_DIR.getAbsolutePath());
        }

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("File Server running on port " + PORT + "...");
            
            while (true) {
                // รอรับการเชื่อมต่อใหม่จากไคลเอนต์ (Main Client หรือ Thread ย่อย)
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());

                // ส่งการเชื่อมต่อนี้ไปให้ ServerHandler จัดการใน Thread Pool
                threadPool.execute(new ServerHandler(clientSocket));
            }
        } catch (IOException e) {
            System.err.println("Server exception: " + e.getMessage());
        } finally {
            threadPool.shutdown();
        }
    }
}