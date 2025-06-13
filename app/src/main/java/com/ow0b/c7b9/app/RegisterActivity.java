package com.ow0b.c7b9.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.JsonObject;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Encryption;
import com.ow0b.c7b9.app.util.Toast;

public class RegisterActivity extends AppCompatActivity
{
    private EditText usernameInput, passwordInput;
    private Button registerButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        registerButton = findViewById(R.id.register_button);

        registerButton.setOnClickListener(v ->
        {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            registerUser(username, password);
        });
    }

    private void registerUser(String username, String password)
    {
        JsonObject json = new JsonObject();
        json.addProperty("username", username);
        json.addProperty("password", Encryption.encryptMD5(password));

        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "register")
                .method("POST", json)
                .callback(new ApiCallback(this)
                {
                    @Override
                    public void onResponse(String response)
                    {
                        runOnUiThread(() ->
                        {
                            if(ApiClient.check(RegisterActivity.this, response).equals("info"))
                            {
                                Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                                Toast.showInfo(RegisterActivity.this, "注册成功");
                                startActivity(intent);
                            }
                            else Toast.showInfo(RegisterActivity.this, "注册失败，用户名已存在");
                        });
                    }
                })
                .enqueue();

    }
}