package sd.traffic.common;

public class LamportClock {

    private long time = 0;

    public synchronized long increment(){
        time++;
        return time;
    }

    public synchronized long update(long receivedTime){
        time = Math.max(time, receivedTime) +1;
        return time;
    }

    public synchronized long getTime(){
        return time;
    }
}
