package com.techexpert.indianvaarta.Notifications;

import com.techexpert.indianvaarta.Notifications.MyResponse;
import com.techexpert.indianvaarta.Notifications.Sender;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface APIService
{
    @Headers(
            {
                    "Content-Type:application/json",
                    "Authorization:key=AAAA00K_5y8:APA91bFl6ZTe_CrhOLOoncwaBIaxh8XpPlTpJPIWQDgyGolwjaTLWokNx6n4D6NVj1_8xTtydECeNnjEZrscrioIwvj-FtS2iveq46b3S4bQSC9hQfuSyRTH_TvsJAm6VvIaxGybgTz6"
            }
    )

    @POST("fcm/send")
    Call<MyResponse> sendNotification(@Body Sender body);
}
