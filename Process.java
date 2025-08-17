import java.util.Random;


public class Process {
    Random rand = new Random();
    int PID ;
    Boolean isBoss=false;
    Thread beatSender;
    Thread beatListener;
    Thread failureDetector;
    
    Process(){
        PID=processID;
        processID++;
    }
    
    
}
