package sd.traffic.coordinator.models;

import java.util.List;
import sd.traffic.common.NodeId;
import sd.traffic.common.VehicleType;

public class VehicleTransfer {

    private String vehicleId;
    private String from;
    private String to;
    private double time;

    private List<NodeId> path;   // NOVO
    private int index;           // NOVO
    private VehicleType type;    // NOVO

    public VehicleTransfer() {}

    public VehicleTransfer(
            String vehicleId,
            String from,
            String to,
            double time,
            List<NodeId> path,
            int index,
            VehicleType type
    ) {
        this.vehicleId = vehicleId;
        this.from = from;
        this.to = to;
        this.time = time;
        this.path = path;
        this.index = index;
        this.type = type;
    }

    @Override
    public String toString() {
        return "VehicleTransfer{" +
                "vehicleId='" + vehicleId + '\'' +
                ", from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", time=" + time +
                ", index=" + index +
                ", type=" + type +
                ", path=" + path +
                '}';
    }

    // GETTERS NECESSÁRIOS
    public String getVehicleId() { return vehicleId; }
    public String getFrom() { return from; }
    public String getTo() { return to; }
    public double getTime() { return time; }

    public List<NodeId> getPath() { return path; }
    public int getIndex() { return index; }
    public VehicleType getType() { return type; }
}
