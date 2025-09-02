package com.ow0b.c7b9.app.old;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.api.ApiClientImpl;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.MainActivity;
import com.ow0b.c7b9.app.old.util.Toast;

public class AccountOptionsActivity extends AppCompatActivity
{

    private SharedPreferences sharedPreferences;
    private TextView changeUsernameButton, logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_options);

        sharedPreferences = ApiClientImpl.getSharedPreferences(this);
        changeUsernameButton = findViewById(R.id.change_username_button);
        logoutButton = findViewById(R.id.logout_button);

        changeUsernameButton.setOnClickListener(v ->
        {
            Toast.showInfo(this, "暂不支持此功能");
            //Intent intent = new Intent(AccountOptionsActivity.this, ChangeUsernameActivity.class);
            //startActivity(intent);
        });

        logoutButton.setOnClickListener(v ->
        {
            logout();
        });
    }


    private void logout()
    {
        sharedPreferences.edit().clear().apply();
        Intent intent = new Intent(AccountOptionsActivity.this, MainActivity.class);
        startActivity(intent);
    }
}