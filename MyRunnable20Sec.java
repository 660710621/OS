public class MyRunnable20Sec implements Runnable {
    @Override
    public void run(){
        for(int i=1;i<=20;i++){

            try{
            Thread.sleep(1000);
            }
            catch(InterruptedException e){
                System.out.println("Thread interrupted");
            }

            if(i==20){
                System.out.println("time up");
                System.exit(0);
            }
        }  
    }
}
