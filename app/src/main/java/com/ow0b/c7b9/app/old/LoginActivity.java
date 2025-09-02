package com.ow0b.c7b9.app.old;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.ow0b.c7b9.app.api.ApiClientImpl;
import com.ow0b.c7b9.app.R;
import com.ow0b.c7b9.app.old.activity.main.DrawerFragment;
import com.ow0b.c7b9.app.MainActivity;
import com.ow0b.c7b9.app.old.util.ApiCallback;
import com.ow0b.c7b9.app.old.util.Encryption;

import okhttp3.Response;

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
            Intent intent = new Intent(LoginActivity.this, RegistryActivity.class);
            startActivity(intent);
        });
    }

    private void loginUser(String username, String password)
    {
        ApiClientImpl.getInstance(this).url(getResources().getString(R.string.server) + "/login")
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
                            ApiClientImpl.check(LoginActivity.this, body);
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
}