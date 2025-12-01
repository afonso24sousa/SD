package sd.traffic.coordinator;

public class CoordinatorEvent implements Comparable<CoordinatorEvent> {

    private final long lamport;
    private final String type;
    private final String node;
    private final String details;

    public CoordinatorEvent(long lamport, String type, String node, String details) {
        this.lamport = lamport;
        this.type = type;
        this.node = node;
        this.details = details;
    }

    public long getLamport() { return lamport; }
    public String getType() { return type; }
    public String getNode() { return node; }
    public String getDetails() { return details; }

    @Override
    public int compareTo(CoordinatorEvent o) {
        return Long.compare(this.lamport, o.lamport);
    }

    @Override
    public String toString() {
        return "CoordinatorEvent{" +
                "lamport=" + lamport +
                ", type='" + type + '\'' +
                ", node='" + node + '\'' +
                ", details='" + details + '\'' +
                '}';
    }
}
