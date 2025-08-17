public class HeartBeatSenderThread implements Runnable{
    int PID;
    HeartBeatSenderThread(int PID){
        this.PID=PID;
    }

    public void run(){
        sendBeat();
    }
    String sendBeat(){
        return PID+"is alive";
    }
    
}
