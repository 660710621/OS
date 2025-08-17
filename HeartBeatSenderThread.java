public class HeartBeatSenderThread implements Runnable{
    int PID;
    HeartBeatSenderThread(int PID){
        this.PID=PID;
    }
    @Override
    public void run(){
        while(true){
        sendBeat();
        }
    }
    String sendBeat(){
        return PID+"is alive";
    }
    
}
