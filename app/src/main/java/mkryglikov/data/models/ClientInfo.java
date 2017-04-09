package mkryglikov.data.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class ClientInfo {

    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("phone")
    @Expose
    private Long phone;
    @SerializedName("birthday")
    @Expose
    private String birthday;
    @SerializedName("status")
    @Expose
    private Integer status;
    @SerializedName("total_orders")
    @Expose
    private Integer totalOrders;
    @SerializedName("username")
    @Expose
    private String username;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Long getPhone() {
        return phone;
    }

    public String getBirthday() {
        return birthday;
    }

    public Integer getStatus() {
        return status;
    }

    public Integer getTotalOrders() {
        return totalOrders;
    }

    public String getUsername() {
        return username;
    }
}
