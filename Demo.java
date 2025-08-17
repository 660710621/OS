import java.util.Random;
import java.util.Scanner;

public class Demo {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int n = scan.nextInt();
        Process[] process = new Process[n];
        
        for(int i = 0; i < n; i++){
            Random random = new Random();
            int PID = random.nextInt(100); 
            process[i] = new Process(PID,false);
            System.out.println("Process " + i + " มี PID = " + PID);
        }
        // หา PID สูงสุด
        int maxPID = process[0].getPID(); 
        int maxIndex = 0;
        for(int i = 1; i < n; i++){
            if(process[i].getPID() > maxPID){
                maxPID = process[i].getPID();
                maxIndex = i;
            }
        }
        process[maxIndex].setBoss(true);
        System.out.println("PID สูงสุดคือ: " + maxPID);
    }
}