package sd.traffic.coordinator.models;

public class Policy {

    private int min_green;
    private int max_green;
    private int quota;
    private int yellow;
    private int clearance;
    private int queue_threshold;
    private int max_extensions;

    public int getMin_green() { return min_green; }
    public int getMax_green() { return max_green; }
    public int getQuota() { return quota; }
    public int getYellow() { return yellow; }
    public int getClearance() { return clearance; }
    public int getQueue_threshold() { return queue_threshold; }
    public int getMax_extensions() { return max_extensions; }

    @Override
    public String toString() {
        return "Policy{" +
                "min_green=" + min_green +
                ", max_green=" + max_green +
                ", quota=" + quota +
                ", yellow=" + yellow +
                ", clearance=" + clearance +
                ", queue_threshold=" + queue_threshold +
                ", max_extensions=" + max_extensions +
                '}';
    }
}
