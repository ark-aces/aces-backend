package example.ark_smartbridge_listener;

import lombok.Data;

@Data
public class ServiceInfoView {

    public final static String STATUS_UP = "Up";
    public final static String STATUS_DOWN = "Down";

    private String capacity;
    private String flatFeeArk;
    private String percentFee;
    private String status;
}
