import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Scanner;
import java.nio.channels.FileChannel; 
import java.nio.channels.Channels; 

// Threaddownload คลาสเดิม (ต้องนำมาใช้ซ้ำ 2 ครั้ง)

class Threaddownload extends Thread {
    String pathfile;
    String savefile;
    int indexthread;
    long startbyte;
    long endbyte;
    private final String serverIp;
    private final int serverPort;
    private final String mode; // ตัวแปรนี้ไว้เพื่อบอก Server ว่าใช้โหมดไหนในการส่งไฟล์

    public Threaddownload(String pathfile, String savefile, int indexthread, long startbyte, long endbyte, String serverIp, int serverPort, String mode) {
        this.pathfile = pathfile;
        this.savefile = savefile;
        this.indexthread = indexthread;
        this.startbyte = startbyte;
        this.endbyte = endbyte;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.mode = mode; 
    }

    @Override
    public void run() {
        try (Socket download = new Socket(serverIp, serverPort);
             OutputStream outd = download.getOutputStream();
             DataOutputStream outputdata = new DataOutputStream(outd);
             RandomAccessFile raf = new RandomAccessFile(savefile, "rw");) {

            // 1. ส่งคำขอในช่วงไบต์และโหมด
            // Server จะใช้โหมดนี้ในการเลือกวิธีส่งไฟล์
            String dataPacket = pathfile + "," + this.indexthread + "," + this.startbyte + "," + this.endbyte + "," + this.mode;
            outputdata.writeUTF(dataPacket);
            outputdata.flush();

            // 2. เตรียมรับข้อมูลและเขียนไฟล์ (ตรรกะการรับต้องแยกตามโหมดของ Server)
            raf.seek(startbyte);
            long threadfilesize = endbyte - startbyte + 1;
            long bytesRemaining = threadfilesize;
            long totalTransferred = 0;
            
            // ตรรกะการรับข้อมูล Client ต้องสอดคล้องกับโหมดที่ Server ใช้
            if (mode.equalsIgnoreCase("zerocopy")) {
                // --- โหมด ZERO-COPY: ใช้ FileChannel.transferFrom (NIO) ---
                try (FileChannel fileChannel = raf.getChannel();
                     java.nio.channels.ReadableByteChannel socketChannel = Channels.newChannel(download.getInputStream())) {

                    while (totalTransferred < threadfilesize) {
                        // transferFrom จะจัดการการเขียนลง RandomAccessFile/FileChannel ให้
                        long transferred = fileChannel.transferFrom(socketChannel, startbyte + totalTransferred, threadfilesize - totalTransferred);
                        if (transferred == 0 && totalTransferred < threadfilesize) {
                            Thread.sleep(10); 
                            continue;
                        }
                        totalTransferred += transferred;
                    }
                    bytesRemaining = threadfilesize - totalTransferred; 

                }
            } else {
                // --- โหมด BUFFERED (Default): ใช้ Stream I/O ---
                try (BufferedInputStream in = new BufferedInputStream(download.getInputStream())) {
                    byte[] bytes = new byte[1024 * 64]; 
                    int count;
                    
                    while (bytesRemaining > 0 && (count = in.read(bytes, 0, (int) Math.min(bytes.length, bytesRemaining))) != -1) {
                        raf.write(bytes, 0, count);
                        bytesRemaining -= count;
                        totalTransferred += count;
                    }
                }
            }

            // 4. ตรวจสอบความสมบูรณ์ของการรับข้อมูล
            if (bytesRemaining == 0) {
                // System.out.println("Thread " + indexthread + " finished. Mode: " + mode);
            } else {
                System.err.println("Thread " + indexthread + " Error: Received size mismatch. Missing: " + bytesRemaining + " bytes. Mode: " + mode);
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class Client {
    
    // เมธอดสำหรับสร้างและรันชุด Thread ดาวน์โหลด
    private static void runDownloadPhase(String serverIp, int serverPort, String mode, String send, long fileSize, int numberOfThreads) throws Exception {
        
        System.out.println("\n===== Starting Download in " + mode.toUpperCase() + " Mode =====");
        long startTime = System.currentTimeMillis();
        
        // สร้างชื่อไฟล์ปลายทางเฉพาะโหมด
        String savefile = mode + "_downloaded_" + send.substring(send.lastIndexOf('/') + 1);
        
        long partSize = fileSize / numberOfThreads;
        Threaddownload[] threads = new Threaddownload[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            long startbyte = i * partSize;
            long endbyte = (i == numberOfThreads - 1) ? fileSize - 1 : (startbyte + partSize - 1);
            // สร้าง Thread โดยส่งโหมดที่ต้องการ
            threads[i] = new Threaddownload(send, savefile, i, startbyte, endbyte, serverIp, serverPort, mode);
            threads[i].start();
        }
        
        // รอให้ทุก Thread เสร็จ
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].join();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Download completed in " + mode.toUpperCase() + " Mode: " + savefile);
        System.out.println("Time taken (" + mode + "): " + (endTime - startTime) + " ms");
    }


    public static void main(String[] args) {
        // *** 1. ตรวจสอบและรับ Argument 2 ตัว (IP และ Port เท่านั้น) ***
        if (args.length < 2) {
            System.out.println("Usage: java Client <Server IP> <Server Port>");
            System.out.println("Example: java Client 192.168.1.100 9000");
            return;
        }
        
        final String SERVER_IP = args[0];
        int SERVER_PORT;

        try {
            SERVER_PORT = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + args[1]);
            return;
        }

        Scanner scanner = new Scanner(System.in);
        String send = "";
        while (!send.equals("stop")) {
            System.out.print("Enter file path to download (or 'stop' to exit): ");
            send = scanner.nextLine();
            if (send.equals("stop")) {
                break;
            }
            long fileSize = -1;

            // 2. ติดต่อ Server เพื่อขอขนาดไฟล์ (ทำครั้งเดียว)
            try (Socket client = new Socket(SERVER_IP, SERVER_PORT);
                DataOutputStream outputdata = new DataOutputStream(client.getOutputStream());
                DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));) {

                outputdata.writeUTF(send);
                outputdata.flush();
                fileSize = in.readLong();
                System.out.println("File size to download: " + fileSize + " bytes");
            } catch (Exception e) {
                System.out.println("Error requesting file size: " + e.toString());
                continue;
            }
            
            if(fileSize <= 0){
                System.out.println("File not found on server or invalid file size.");
                continue;
            }
            int numberOfThreads = 2; 

            try {
                // *** 3. PHASE 1: ดาวน์โหลดและเปรียบเทียบโหมด ZEROCOPY ***
                runDownloadPhase(SERVER_IP, SERVER_PORT, "ZeroCopy", send, fileSize, numberOfThreads);
                
                // *** 4. PHASE 2: ดาวน์โหลดและเปรียบเทียบโหมด BUFFERED ***
                runDownloadPhase(SERVER_IP, SERVER_PORT, "Buffered", send, fileSize, numberOfThreads);
                
                System.out.println("\n--- Comparison Complete ---");

            } catch (Exception e) {
                System.err.println("An error occurred during download phases: " + e.getMessage());
            }
        }
    }
}