package com.example.chat_app.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.chat_app.adapters.ChatAdapter;
import com.example.chat_app.databinding.ActivityChatBinding;
import com.example.chat_app.models.Message;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Constants;
import com.example.chat_app.utilities.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class ChatActivity extends AppCompatActivity {
    private ActivityChatBinding binding;
    private User receiverUser;
    private List<Message> chatMessages;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore database;
    private String conversationId = null;
    private boolean isReceiverAvailable = false;
    private Boolean isWatching = false;

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null)
            return;
        if (value != null) {
            int size = chatMessages.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    Message chatMessage = new Message();
                    chatMessage.senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    chatMessage.receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    chatMessage.message = documentChange.getDocument().getString(Constants.KEY_MESSAGE);
                    chatMessage.dateTime = getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    chatMessage.dateObject = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    chatMessages.add(chatMessage);
                }
            }
            Collections.sort(chatMessages, (o1, o2) -> o1.dateObject.compareTo(o2.dateObject));
            if (size == 0) {
                chatAdapter.notifyDataSetChanged();
                binding.progressBar.setVisibility(View.GONE);
                binding.chatRecyclerView.setVisibility(View.VISIBLE);
            } else {
                chatAdapter.notifyItemRangeInserted(chatMessages.size(), chatMessages.size());
                binding.chatRecyclerView.scrollToPosition(chatMessages.size() - 1);
            }
        }
        if (conversationId == null)
            checkForConversion();
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListener();
        loadReceiverDetails();
        init();
        listenMessages();
        listenAvailabilityOfReceiver();
    }

    private void init() {
        preferenceManager = SignInActivity.preferenceManager;
        chatMessages = new ArrayList<>();
        chatAdapter = new ChatAdapter(
                chatMessages
                , getBitmapFromEncodedString(receiverUser.image)
                , preferenceManager.getString(Constants.KEY_USER_ID));
        binding.chatRecyclerView.setAdapter(chatAdapter);
        database = FirebaseFirestore.getInstance();
    }

    private void listenAvailabilityOfReceiver() {
        if (conversationId != null) {
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .addSnapshotListener(ChatActivity.this, (value, error) -> {
                        if (error != null)
                            return;
                        String messageOf = "";
                        if (value != null) {
                            Boolean res = value.getBoolean(receiverUser.id);
                            isReceiverAvailable = (res != null && res);
                            messageOf = value.getString(Constants.KEY_NEW_MESSAGE_OF);
                        }
                        if (isReceiverAvailable) {
                            binding.textAvailability.setVisibility(View.VISIBLE);
                        } else
                            binding.textAvailability.setVisibility(View.GONE);
                        if(conversationId != null && isWatching != null && isWatching && Objects.requireNonNull(messageOf).equals(preferenceManager.getString(Constants.KEY_USER_ID)))
                            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                                    .document(conversationId)
                                    .update(Constants.KEY_NEW_MESSAGE_OF,"");
                    });
        }
    }

    private void listenMessages() {
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverUser.id)
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CHAT)
                .whereEqualTo(Constants.KEY_SENDER_ID, receiverUser.id)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    private void sendMessage() {
        HashMap<String, Object> message = new HashMap<>();
        message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        message.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
        message.put(Constants.KEY_MESSAGE, binding.inputMessage.getText().toString());
        message.put(Constants.KEY_TIMESTAMP, new Date());
        database.collection(Constants.KEY_COLLECTION_CHAT).add(message);
        if (conversationId != null)
            updateConversation(binding.inputMessage.getText().toString(), receiverUser.id);
        else {
            HashMap<String, Object> conversation = new HashMap<>();
            conversation.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
            conversation.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_IMAGE));
            conversation.put(Constants.KEY_SENDER_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
            conversation.put(Constants.KEY_RECEIVER_ID, receiverUser.id);
            conversation.put(Constants.KEY_RECEIVER_NAME, receiverUser.name);
            conversation.put(Constants.KEY_RECEIVER_IMAGE, receiverUser.image);
            conversation.put(Constants.KEY_RECEIVER_TOKEN, receiverUser.token);
            conversation.put(Constants.KEY_LAST_MESSAGE, binding.inputMessage.getText().toString().trim());
            conversation.put(Constants.KEY_LAST_SENDER,preferenceManager.getString(Constants.KEY_USER_ID));
            conversation.put(Constants.KEY_TIMESTAMP, new Date());
            conversation.put(Constants.KEY_NEW_MESSAGE_OF, receiverUser.id);
            addConversation(conversation);
        }
        if (!isReceiverAvailable) {
            sendNotification(receiverUser.token, binding.inputMessage.getText().toString());
        }
        binding.inputMessage.setText("");
    }

    private void sendNotification(String toTokenFCM,String messageBody) {
        // Khởi tạo JSONObject chứa nội dung bạn muốn gửi
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("to", toTokenFCM);
            JSONObject notificationObject = new JSONObject();
            notificationObject.put("title", "Thông báo");
            notificationObject.put("body", messageBody);
            jsonObject.put("notification", notificationObject);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Khởi tạo RequestQueue bằng Volley
        RequestQueue requestQueue = Volley.newRequestQueue(this);

        // URL mà bạn muốn gửi POST request đến
        String url = "https://fcm.googleapis.com/fcm/send";

        // Tạo một POST request với JsonObjectRequest
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest
                (Request.Method.POST, url, jsonObject, response -> {

                }, error -> {

                }) {
            @Override
            public Map<String, String> getHeaders() {
                // Đặt các tiêu đề cần thiết cho yêu cầu (content-type và Authorization)
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "application/json");
                headers.put("Authorization", Constants.KEY_FCM);
                return headers;
            }
        };
        // Thêm request vào hàng đợi để thực thi
        requestQueue.add(jsonObjectRequest);
    }

    private Bitmap getBitmapFromEncodedString(String encodedImage) {
        if (encodedImage != null) {
            byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    private void loadReceiverDetails() {
        receiverUser = (User) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.textName.setText(receiverUser.name);
        binding.avatar.setImageBitmap(getImage(receiverUser.image));
        conversationId = getIntent().getStringExtra(Constants.KEY_CONVERSATION_ID);
    }

    private void setListener() {
        binding.imageBack.setOnClickListener(v -> onBackPressed());
        binding.layoutSend.setOnClickListener(v -> sendMessage());
        binding.inputMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0)
                    binding.layoutSend.setVisibility(View.VISIBLE);
                else
                    binding.layoutSend.setVisibility(View.GONE);
            }
            @Override
            public void afterTextChanged(Editable s) {

            }
        });
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("HH:mm, d MMMM", Locale.getDefault()).format(date);
    }

    private void addConversation(HashMap<String, Object> conversation) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .add(conversation)
                .addOnSuccessListener(documentReference -> setConversationId(documentReference.getId()));
    }

    private void updateConversation(String message, String newMessageOf) {
        DocumentReference documentReference =
                database.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversationId);
        documentReference.update(
                Constants.KEY_LAST_MESSAGE, message
                , Constants.KEY_TIMESTAMP, new Date()
                , Constants.KEY_NEW_MESSAGE_OF, newMessageOf
                ,Constants.KEY_LAST_SENDER, preferenceManager.getString(Constants.KEY_USER_ID));
    }

    private void checkForConversion() {
        if (chatMessages.size() != 0) {
            checkForConversionRemotely(
                    preferenceManager.getString(Constants.KEY_USER_ID)
                    , receiverUser.id
            );
            checkForConversionRemotely(
                    receiverUser.id
                    , preferenceManager.getString(Constants.KEY_USER_ID)
            );
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, senderId)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId)
                .get()
                .addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
            setConversationId(documentSnapshot.getId());
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (conversationId != null)
            statusSwitch(true);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (conversationId != null)
            statusSwitch(null);
    }

    private void statusSwitch(Boolean status) {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .update(preferenceManager.getString(Constants.KEY_USER_ID), status)
                .addOnSuccessListener(ChatActivity.this,unused -> isWatching = status);
    }

    private void setConversationId(String conversationId) {
        if (conversationId != null) {
            this.conversationId = conversationId;
            listenAvailabilityOfReceiver();
            statusSwitch(true);
        }
    }
    private Bitmap getImage(String encodedImage) {
        byte[] bytes = Base64.decode(encodedImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}