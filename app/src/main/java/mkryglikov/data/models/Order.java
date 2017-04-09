package mkryglikov.data.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.io.Serializable;

public class Order implements Serializable {
    public static final String ORDER = "order";

    @SerializedName("id")
    @Expose
    private Integer id;

    public Order(Integer id, String tobacco, String extra, String date, String inTime) {
        this.id = id;
        this.tobacco = tobacco;
        this.extra = extra;
        this.date = date;
        this.inTime = inTime;
    }

    @SerializedName("client_id")
    @Expose
    private String clientId;
    @SerializedName("desk")
    @Expose
    private Integer desk;
    @SerializedName("tobacco")
    @Expose
    private String tobacco;
    @SerializedName("extra")
    @Expose
    private String extra;
    @SerializedName("date")
    @Expose
    private String date;
    @SerializedName("in_time")
    @Expose
    private String inTime;
    @SerializedName("out_time")
    @Expose
    private String outTime;
    @SerializedName("total")
    @Expose
    private Integer total;
    @SerializedName("completed")
    @Expose
    private Integer completed;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getDesk() {
        return desk;
    }

    public void setDesk(Integer desk) {
        this.desk = desk;
    }

    public String getTobacco() {
        return tobacco;
    }

    public void setTobacco(String tobacco) {
        this.tobacco = tobacco;
    }

    public String getExtra() {
        return extra;
    }

    public void setExtra(String extra) {
        this.extra = extra;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getInTime() {
        return inTime;
    }

    public void setInTime(String inTime) {
        this.inTime = inTime;
    }

    public String getOutTime() {
        return outTime;
    }

    public void setOutTime(String outTime) {
        this.outTime = outTime;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Integer getCompleted() {
        return completed;
    }

    public void setCompleted(Integer completed) {
        this.completed = completed;
    }
}
