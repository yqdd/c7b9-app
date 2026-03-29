package com.ow0b.c7b9.app;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.activity.main.DrawerFragment;
import com.ow0b.c7b9.app.activity.main.MainActivity;
import com.ow0b.c7b9.app.util.ApiCallback;
import com.ow0b.c7b9.app.util.ApiClient;
import com.ow0b.c7b9.app.util.Encryption;
import com.ow0b.c7b9.app.util.Toast;

import okhttp3.Response;

public class LoginActivity extends AppCompatActivity
{
    private EditText usernameInput, passwordInput;
    private Button loginButton;
    private TextView registryButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        usernameInput = findViewById(R.id.username_input);
        passwordInput = findViewById(R.id.password_input);
        loginButton = findViewById(R.id.login_button);
        registryButton = findViewById(R.id.registry_button);

        findViewById(R.id.back).setOnClickListener(v -> finish());
        loginButton.setOnClickListener(v ->
        {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            loginUser(username, password);
        });

        registryButton.setOnClickListener(v ->
        {
            String username = usernameInput.getText().toString();
            String password = passwordInput.getText().toString();
            registryUser(username, password);
        });
    }

    private void loginUser(String username, String password)
    {
        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "/login")
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
                            ApiClient.check(LoginActivity.this, body);
                            if(resp.code() == 200)
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
    private void registryUser(String username, String password)
    {
        ApiClient.getInstance(this).url(getResources().getString(R.string.server) + "/registry")
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
                            ApiClient.check(LoginActivity.this, body);
                            if(resp.code() == 200)
                            {
                                Toast.showInfo(LoginActivity.this, "注册成功");
                                loginUser(username, password);
                            }
                            else Toast.showInfo(LoginActivity.this, "注册失败，用户名已存在");
                        });
                    }
                })
                .enqueue();
    }
}