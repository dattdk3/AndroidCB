package com.example.chat_app.firebase;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.example.chat_app.MyApplication;
import com.example.chat_app.R;
import com.example.chat_app.activities.SignInActivity;
import com.example.chat_app.utilities.Constants;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MessagingService extends FirebaseMessagingService {
    //Gửi request  url
    private final String _url = "https://block-sms-spam-f7664e46fbe0.herokuapp.com/predict";
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
    }
    //Tích Hợp Model
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        String title = Objects.requireNonNull(remoteMessage.getNotification()).getTitle();
        String body = remoteMessage.getNotification().getBody();
        //Khi người dùng bật chức năng, người dùng sẽ request lên
        if(SignInActivity.preferenceManager.getBoolean(Constants.KEY_CHECK_SPAM)){
            StringRequest stringRequest = new StringRequest(Request.Method.POST, _url,
                    //khi response ve, nếu kqua 0 thì chặn k show lên
                    response -> {
                        try {
                            JSONObject jsonObject = new JSONObject(response);
                            String data = jsonObject.getString("result");
                            if(data.equals("0")){
                                showNotification(title,body);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    },
                    error -> Toast.makeText(MessagingService.this,error.getMessage(),Toast.LENGTH_SHORT).show()){

                @Override
                protected Map<String,String> getParams(){
                    Map<String,String> params = new HashMap<>();
//                    params.put("text",body);
                    return params;
                }
            };
            RequestQueue queue = Volley.newRequestQueue(this);
            queue.add(stringRequest);
        }else{
            showNotification(title,body);
        }
    }

    private void showNotification(String title, String body) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(),R.mipmap.ic_launcher);
        Notification notification = new NotificationCompat.Builder(this,MyApplication.CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(body)
                .setSmallIcon(R.drawable.ic_notification)
                .setLargeIcon(bitmap)
                .build();

        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if(notificationManager != null){
            notificationManager.notify(1,notification);
        }
    }
}
