package com.example.safeher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnRegister).setOnClickListener(v->{
            startActivity(new Intent(MainActivity.this,RegisterActivity.class));
        });

        findViewById(R.id.btnLogin).setOnClickListener(v->{startActivity(new Intent(MainActivity.this,LoginActivity.class));});


    }
}