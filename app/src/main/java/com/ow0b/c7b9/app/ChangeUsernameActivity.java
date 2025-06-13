package com.ow0b.c7b9.app;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class ChangeUsernameActivity extends AppCompatActivity {

    private EditText newUsernameInput;
    private Button submitButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_username);

        newUsernameInput = findViewById(R.id.new_username_input);
        submitButton = findViewById(R.id.submit_button);

        submitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Implement change username logic
                String newUsername = newUsernameInput.getText().toString();
                changeUsername(newUsername);
            }
        });
    }

    private void changeUsername(String newUsername) {
        // Implement your change username logic here
        Toast.makeText(this, "Username changed to " + newUsername, Toast.LENGTH_SHORT).show();
        // Navigate back to the account options activity
        Intent intent = new Intent(ChangeUsernameActivity.this, AccountOptionsActivity.class);
        startActivity(intent);
    }
}