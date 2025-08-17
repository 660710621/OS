import java.util.Random;
import java.util.Scanner;
import java.util.UUID;

public class Demo {
    public static void main(String[] args) {
        Scanner scan = new Scanner(System.in);
        int n = scan.nextInt();
        Process[] process = new Process[n];
        
        for(int i = 0; i < n; i++){
            Random random = new Random();
            int PID = random.nextInt(100); 
            process[i] = new Process(PID);
            System.out.println("Process " + i + " มี PID = " + PID);
        }
    }
}