package mkryglikov.shisha;

import java.util.List;

import mkryglikov.data.models.AuthResponse;
import mkryglikov.data.models.ClientInfo;
import mkryglikov.data.models.Order;
import mkryglikov.data.models.TobaccosExtrasResponse;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.DELETE;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;

public interface Api {

    @FormUrlEncoded
    @POST("/login")
    Call<AuthResponse> auth(@Field("grant_type") String grantType,
                            @Field("client_id") String clientId,
                            @Field("client_secret") String clientSecret,
                            @Field("username") String username,
                            @Field("password") String password);

    @FormUrlEncoded
    @POST("/login")
    Call<AuthResponse> refreshToken(@Field("grant_type") String grantType,
                                    @Field("client_id") String clientId,
                                    @Field("client_secret") String clientSecret,
                                    @Field("refresh_token") String RefreshToken);

    @FormUrlEncoded
    @POST("/client")
    Call<ResponseBody> register(@Field("username") String username,
                                @Field("password") String password,
                                @Field("name") String name,
                                @Field("phone") long phone,
                                @Field("birthday") String birthday);

    @GET("/verifytoken")
    Call<List> verifyToken(@Header("Authorization") String token);

    @GET("/client")
    Call<ClientInfo> getClientInfo(@Header("Authorization") String token);

    @FormUrlEncoded
    @PUT("/client")
    Call<ResponseBody> updateUserInfo(@Header("Authorization") String token,
                                      @Field("name") String name,
                                      @Field("phone") long phone,
                                      @Field("birthday") String birthday);

    @GET("/orders")
    Call<List<Order>> getOrders(@Header("Authorization") String token);

    @GET("/orders/active")
    Call<List<Order>> getActiveOrders(@Header("Authorization") String token);

    @FormUrlEncoded
    @POST("/orders")
    Call<ResponseBody> newOrder(@Header("Authorization") String token,
                                @Field("desk") int desk,
                                @Field("tobacco") String tobacco,
                                @Field("extra") String extra,
                                @Field("date") String date,
                                @Field("time") String time);

    @FormUrlEncoded
    @PUT("/orders/{id}")
    Call<ResponseBody> updateOrder(@Header("Authorization") String token,
                                   @Path("id") int id,
                                   @Field("desk") int desk,
                                   @Field("tobacco") String tobacco,
                                   @Field("extra") String extra,
                                   @Field("date") String date,
                                   @Field("time") String time);

    @DELETE("/orders/{id}")
    Call<ResponseBody> deleteOrder(@Header("Authorization") String token,
                                   @Path("id") int id);

    @GET("/tobaccos")
    Call<List<TobaccosExtrasResponse>> getTobaccos(@Header("Authorization") String token);

    @GET("/extras")
    Call<List<TobaccosExtrasResponse>> getExtras(@Header("Authorization") String token);

    @Multipart
    @POST("/clientAvatar")
    Call<ResponseBody> setAvatar(@Header("Authorization") String token,
                                 @Part MultipartBody.Part image);
}
