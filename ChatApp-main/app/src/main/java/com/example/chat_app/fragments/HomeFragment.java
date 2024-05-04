package com.example.chat_app.fragments;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.example.chat_app.R;
import com.example.chat_app.activities.ChatActivity;
import com.example.chat_app.activities.ContainerFragmentActivity;
import com.example.chat_app.activities.SignInActivity;
import com.example.chat_app.adapters.RecentConversationsAdapter;
import com.example.chat_app.databinding.FragmentHomeBinding;
import com.example.chat_app.listeners.ConversationListener;
import com.example.chat_app.models.Conversation;
import com.example.chat_app.models.User;
import com.example.chat_app.utilities.Constants;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class HomeFragment extends Fragment implements ConversationListener {

    private FragmentHomeBinding binding;
    private ContainerFragmentActivity parentActivity;
    private List<Conversation> conversations;
    private RecentConversationsAdapter conversationsAdapter;
    private FirebaseFirestore database;

    public HomeFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        parentActivity = (ContainerFragmentActivity) requireActivity();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        init();
        getToken();
        listenConversations();
        return binding.getRoot();
    }

    private void init() {
        conversations = new ArrayList<>();
        conversationsAdapter =
                new RecentConversationsAdapter(conversations, this);
        binding.conversationRecyclerView.setAdapter(conversationsAdapter);
        binding.conversationRecyclerView.setItemAnimator(new RecyclerView.ItemAnimator() {
            @Override
            public boolean animateDisappearance(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull ItemHolderInfo preLayoutInfo, @Nullable ItemHolderInfo postLayoutInfo) {
                return false;
            }

            @Override
            public boolean animateAppearance(@NonNull RecyclerView.ViewHolder viewHolder, @Nullable ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
                return false;
            }

            @Override
            public boolean animatePersistence(@NonNull RecyclerView.ViewHolder viewHolder, @NonNull ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
                return false;
            }

            @Override
            public boolean animateChange(@NonNull RecyclerView.ViewHolder oldHolder, @NonNull RecyclerView.ViewHolder newHolder, @NonNull ItemHolderInfo preLayoutInfo, @NonNull ItemHolderInfo postLayoutInfo) {
                return false;
            }

            @Override
            public void runPendingAnimations() {

            }

            @Override
            public void endAnimation(@NonNull RecyclerView.ViewHolder item) {

            }

            @Override
            public void endAnimations() {

            }

            @Override
            public boolean isRunning() {
                return false;
            }
        });
        database = FirebaseFirestore.getInstance();
    }

    private void listenConversations() {
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null)
            return;
        if (value != null) {
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                String conversationId = documentChange.getDocument().getId();
                String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    Conversation conversation = new Conversation();
                    if (SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                        conversation.receiverImage = documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE);
                        conversation.receiverName = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME);
                        conversation.receiverEmail = documentChange.getDocument().getString(Constants.KEY_RECEIVER_EMAIL);
                        conversation.receiverToken = documentChange.getDocument().getString(Constants.KEY_RECEIVER_TOKEN);
                        conversation.receiverNumberPhone = documentChange.getDocument().getString(Constants.KEY_RECEIVER_NUMBER_PHONE);
                        conversation.receiverId = receiverId;
                    } else {
                        conversation.receiverImage = documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE);
                        conversation.receiverName = documentChange.getDocument().getString(Constants.KEY_SENDER_NAME);
                        conversation.receiverEmail = documentChange.getDocument().getString(Constants.KEY_SENDER_EMAIL);
                        conversation.receiverToken = documentChange.getDocument().getString(Constants.KEY_SENDER_TOKEN);
                        conversation.receiverNumberPhone = documentChange.getDocument().getString(Constants.KEY_SENDER_NUMBER_PHONE);
                        conversation.receiverId = senderId;
                    }
                    conversation.lastSender = documentChange.getDocument().getString(Constants.KEY_LAST_SENDER);
                    if(SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID).equals(conversation.lastSender))
                        conversation.lastMessage = "Bạn:  " + documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    else
                        conversation.lastMessage = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                    conversation.timestamp = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                    conversation.newMessageOf = documentChange.getDocument().getString(Constants.KEY_NEW_MESSAGE_OF);
                    conversation.conversationId = conversationId;
                    conversations.add(conversation);
                    database.collection(Constants.KEY_COLLECTION_USERS)
                            .document(conversation.receiverId)
                            .addSnapshotListener((value1, error1) -> {
                                if(error1 != null)
                                    return;
                                if(value1 != null){
                                    Boolean status = value1.getBoolean(Constants.KEY_STATUS);
                                    conversation.status = (status != null && status);
                                    conversationsAdapter.notifyItemChanged(conversations.indexOf(conversation));
                                }
                            });
                } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                    for (int i = 0; i < conversations.size(); i++) {
                        if (conversations.get(i).conversationId.equals(conversationId)) {
                            conversations.get(i).lastSender = documentChange.getDocument().getString(Constants.KEY_LAST_SENDER);
                            if(!SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID).equals(conversations.get(i).lastSender))
                                conversations.get(i).lastMessage = documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            else
                                conversations.get(i).lastMessage = "Bạn:  " + documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE);
                            conversations.get(i).timestamp = documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP);
                            conversations.get(i).newMessageOf = documentChange.getDocument().getString(Constants.KEY_NEW_MESSAGE_OF);
                            break;
                        }
                    }
                } else {
                    int index = -1;
                    for (int i = 0; i < conversations.size(); i++) {
                        if (conversations.get(i).conversationId.equals(conversationId)) {
                            index = i;
                            break;
                        }
                    }
                    if(index > -1)
                        conversations.remove(index);
                }
            }
            Collections.sort(conversations, (o1, o2) -> o2.timestamp.compareTo(o1.timestamp));
            conversationsAdapter.notifyDataSetChanged();
            binding.conversationRecyclerView.scrollToPosition(0);
            binding.conversationRecyclerView.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
        }
    };

    private void getToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateToken);
    }

    private void updateToken(String token) {
        SignInActivity.preferenceManager.putString(Constants.KEY_FCM_TOKEN, token);
        DocumentReference documentReference = database.collection(Constants.KEY_COLLECTION_USERS).document(
                SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID)
        );
        documentReference.update(Constants.KEY_FCM_TOKEN, token)
                .addOnFailureListener(
                        e -> Toast.makeText(parentActivity, "Không thể cập nhập token", Toast.LENGTH_SHORT).show()
                );
    }

    @Override
    public void onConversationClicked(User user,Conversation conversation) {
        if (SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID).equals(conversation.newMessageOf)) {
            database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                    .document(conversation.conversationId)
                    .update(Constants.KEY_NEW_MESSAGE_OF, "");
        }
        Intent intent = new Intent(parentActivity.getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        intent.putExtra(Constants.KEY_CONVERSATION_ID, conversation.conversationId);
        startActivity(intent);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onClickDeleteBottomSheet(Conversation conversation, BottomSheetDialog dialog) {
        dialog.cancel();
        AlertDialog.Builder builder = new AlertDialog.Builder(parentActivity);
        builder.setTitle("Xóa cuộc hội thoại")
                .setMessage("Bạn có muốn chắc chắn xóa cuộc hội thoại với \"" + conversation.receiverName + "\"")
                .setNegativeButton("Xác nhận xóa", (dialog1, which) -> {
                    database.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                            .document(conversation.conversationId)
                            .delete()
                            .addOnCompleteListener(task -> {
                                conversations.remove(conversation);
                                conversationsAdapter.notifyDataSetChanged();
                            }).addOnFailureListener(e ->
                                    Toast.makeText(parentActivity, "Xóa cuộc hột thoại không thành công", Toast.LENGTH_SHORT)
                                            .show());
                    dialog1.cancel();
                })
                .setPositiveButton("Hủy", (dialog1, which) -> dialog1.cancel());
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(Color.RED);
        alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.main_color_blue));
    }
}