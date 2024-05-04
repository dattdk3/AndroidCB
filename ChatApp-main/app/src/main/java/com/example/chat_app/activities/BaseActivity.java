package com.example.chat_app.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chat_app.broadcast.MyReceive;
import com.example.chat_app.utilities.Constants;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public abstract class BaseActivity extends AppCompatActivity {
    private DocumentReference documentReference;
    private MyReceive myReceive;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        documentReference = database.collection(Constants.KEY_COLLECTION_USERS)
                .document(SignInActivity.preferenceManager.getString(Constants.KEY_USER_ID));
        registerReceiver(myReceive = new MyReceive(), new IntentFilter(Intent.ACTION_CLOSE_SYSTEM_DIALOGS), Context.RECEIVER_NOT_EXPORTED);
    }

    @Override
    protected void onResume() {
        super.onResume();
        documentReference.update(Constants.KEY_STATUS, Boolean.TRUE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(myReceive);
    }
}
