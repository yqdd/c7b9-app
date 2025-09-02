package com.ow0b.c7b9.app.old;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.api.ApiClientImpl;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.util.ApiCallback;
import com.ow0b.c7b9.app.old.util.Encryption;
import com.ow0b.c7b9.app.old.util.Toast;

import okhttp3.Response;

public class RegistryActivity extends AppCompatActivity
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
            registryUser(username, password);
        });
    }

    private void registryUser(String username, String password)
    {
        ApiClientImpl.getInstance(this).url(getResources().getString(R.string.server) + "/registry")
                .parameter("username", username)
                .parameter("password", Encryption.encryptMD5(password))
                .get()
                .callback(new ApiCallback(this)
                {
                    @Override
                    public void onResponse(Response resp, String body)
                    {
                        runOnUiThread(() ->
                        {
                            ApiClientImpl.check(RegistryActivity.this, body);
                            if(resp.code() == 200)
                            {
                                Intent intent = new Intent(RegistryActivity.this, LoginActivity.class);
                                Toast.showInfo(RegistryActivity.this, "注册成功");
                                startActivity(intent);
                            }
                            else Toast.showInfo(RegistryActivity.this, "注册失败，用户名已存在");
                        });
                    }
                })
                .enqueue();

    }
}