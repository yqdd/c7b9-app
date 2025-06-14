package com.ow0b.c7b9.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Toast;

public class AccountOptionsActivity extends AppCompatActivity
{

    private SharedPreferences sharedPreferences;
    private Button changeUsernameButton;
    private Button logoutButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account_options);

        sharedPreferences = ApiClient.getSharedPreferences(this);
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