package com.ow0b.c7b9.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.ow0b.c7b9.app.activity.main.DrawerFragment;
import com.ow0b.c7b9.app.activity.main.MainActivity;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Encryption;

public class LoginActivity extends AppCompatActivity
{
    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private TextView registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registerButton = findViewById(R.id.register_button);

        loginButton.setOnClickListener(v ->
        {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            loginUser(username, password);
        });

        registerButton.setOnClickListener(v ->
        {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String username, String password)
    {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", Encryption.encryptMD5(password));

        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "login")
                .method("POST", json)
                .callback(new ApiCallback(this)
                {
                    @Override
                    public void onResponse(String response)
                    {
                        runOnUiThread(() ->
                        {
                            if(ApiClient.check(LoginActivity.this, response).equals("info"))
                            {
                                DrawerFragment.INSTANCE.init();

                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                            }
                        });
                    }
                })
                .enqueue();
    }
}