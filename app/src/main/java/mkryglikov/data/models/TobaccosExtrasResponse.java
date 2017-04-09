package mkryglikov.data.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class TobaccosExtrasResponse {
    @SerializedName("id")
    @Expose
    private Integer id;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("price")
    @Expose
    private Integer price;
    @SerializedName("balances")
    @Expose
    private Integer balances;


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Integer getPrice() {
        return price;
    }

    public Integer getBalances() {
        return balances;
    }
}
