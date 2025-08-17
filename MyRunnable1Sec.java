public class MyRunnable1Sec implements Runnable {
    @Override
    public void run(){
        for(int i=1;i<=1;i++){

            try{
            Thread.sleep(1000);
            }
            catch(InterruptedException e){
                System.out.println("Thread interrupted");
            }

            if(i==1){
                System.out.println("time up");
                System.exit(0);
            }
        }  
    }
}
