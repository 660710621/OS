import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.Socket;
import java.util.Scanner;

class Threaddownload extends Thread {
    String pathfile;
    String savefile;
    int indexthread;
    long startbyte;
    long endbyte;

    public Threaddownload(String pathfile, String savefile, int indexthread, long startbyte, long endbyte) {
        this.pathfile = pathfile;
        this.savefile = savefile;
        this.indexthread = indexthread;
        this.startbyte = startbyte;
        this.endbyte = endbyte;
    }

    @Override
    public void run() {
        try (Socket download = new Socket("localhost", 9000);
            // สร้างช่องทางส่งข้อมูลไปยังเซิร์ฟเวอร์
            OutputStream outd = download.getOutputStream();
            DataOutputStream outputdata = new DataOutputStream(outd);
            // สร้างช่องทางรับข้อมูลจากเซิร์ฟเวอร์
            BufferedInputStream in = new BufferedInputStream(download.getInputStream());
            // เข้าถึงไฟล์แบบสุ่ม
            // savefile = ระบุถึง ไฟล์ ที่คุณต้องการเปิดหรือสร้างขึ้นมา
            // rw = read/write mode
            RandomAccessFile out = new RandomAccessFile(savefile, "rw");) {
            
            // 1. ส่งคำขอในช่วงไบต์
            String dataPacket = pathfile + "," + this.indexthread + "," + this.startbyte + "," + this.endbyte;
            outputdata.writeUTF(dataPacket);
            outputdata.flush();

            // 2. เตรียมรับข้อมูลและเขียนไฟล์
            byte[] bytes = new byte[1024 * 64]; //64KB buffer
            out.seek(startbyte); // เลื่อนไปยังตำแหน่ง byte ที่ระบุ
            long threadfilesize = endbyte - startbyte + 1;
            long bytesRemaining = threadfilesize;
            int count;
            
            // 3. รับข้อมูลจนกว่าจะครบตามขนาดที่ระบุ
            //เงื่อนไขการวนลูป: ตรวจสอบว่า bytesRemaining ยังมากกว่า 0 && count ที่อ่านได้จาก in.read() ยังไม่เท่ากับ -1 (ซึ่งหมายถึงจบการอ่าน)
            while (bytesRemaining > 0 && (count = in.read(bytes, 0, (int) Math.min(bytes.length, bytesRemaining))) != -1) {
                
                out.write(bytes, 0, count);
                bytesRemaining -= count;
            }
            // 4. ตรวจสอบความสมบูรณ์ของการรับข้อมูล
            if (bytesRemaining == 0) {
                System.out.println("Thread " + indexthread + " finished.");
            } else {
                System.err.println("Thread " + indexthread + " Error: Received size mismatch. Missing: " + bytesRemaining + " bytes.");
            }
        } catch (Exception e) {
            System.out.println(e);
        }
    }
}

class Client {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String send = "";
        while (!send.equals("stop")) {
            System.out.print("Enter file path to download (or 'stop' to exit): ");
            send = scanner.nextLine();
            if (send.equals("stop")) {
                break;
            }
            long fileSize = -1;
            String savefile = "downloaded_" + send.substring(send.lastIndexOf('/') + 1);

            try (Socket client = new Socket("localhost", 9000);
                // สร้างช่องทางส่งข้อมูลไปยังเซิร์ฟเวอร์
                DataOutputStream outputdata = new DataOutputStream(client.getOutputStream());

                // สร้างช่องทางรับข้อมูลจากเซิร์ฟเวอร์
                DataInputStream in = new DataInputStream(new BufferedInputStream(client.getInputStream()));) {

                // ส่งชื่อไฟล์ที่ต้องการดาวน์โหลดไปยังเซิร์ฟเวอร์
                outputdata.writeUTF(send);
                outputdata.flush();

                // รับขนาดไฟล์จากเซิร์ฟเวอร์
                fileSize = in.readLong();
                System.out.println("File size to download: " + fileSize + " bytes");
            } catch (Exception e) {
                System.out.println(e);
            }
            if(fileSize <= 0){
                System.out.println("File not found on server or invalid file size.");
                continue;
            }
            int numberOfThreads = 8; // กำหนดจำนวนเธรด
            long partSize = fileSize / numberOfThreads;
            Threaddownload[] threads = new Threaddownload[numberOfThreads];

            for (int i = 0; i < numberOfThreads; i++) {
                long startbyte = i * partSize;
                long endbyte = (i == numberOfThreads - 1) ? fileSize - 1 : (startbyte + partSize - 1);
                threads[i] = new Threaddownload(send, savefile, i, startbyte, endbyte);
                threads[i].start();
            }
            for (int i = 0; i < numberOfThreads; i++) {
                try {
                    threads[i].join();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
            System.out.println("Download completed: " + savefile);

        }
    }
}