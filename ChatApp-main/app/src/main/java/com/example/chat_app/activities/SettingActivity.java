package com.example.chat_app.activities;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import android.os.Bundle;

import com.example.chat_app.R;
import com.example.chat_app.utilities.Constants;

public class SettingActivity extends AppCompatActivity {
    SwitchCompat switchCompat;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        binding();
        setListener();
        configurationSettings();
    }
    private void binding(){
        switchCompat = findViewById(R.id.switchBlockSpam);
    }
    private void setListener(){
        switchCompat.setOnCheckedChangeListener((compoundButton, b) -> {
            SignInActivity.preferenceManager.putBoolean(Constants.KEY_CHECK_SPAM,b);
        });
    }
    private void configurationSettings(){
        boolean isBlockSpamOff = SignInActivity.preferenceManager.getBoolean(Constants.KEY_CHECK_SPAM);
        switchCompat.setChecked(isBlockSpamOff);
    }
}