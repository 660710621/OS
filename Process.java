public class Process {
    private int PID;
    private boolean isBoss;
    Process(int PID,boolean isBoss){
        this.PID = PID;
        this.isBoss = isBoss;
    }
    public int getPID() {
        return PID;
    }
    public boolean getisBoss(){
        return isBoss;
    }
    public void setBoss(boolean isBoss) {
        this.isBoss = isBoss;
    }
}
