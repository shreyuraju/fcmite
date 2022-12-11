package com.mite.mitefc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class admin extends AppCompatActivity {
    EditText amtText;
    Button updateBtn;
    String amount;

    DatabaseReference reference;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin);
        amtText = findViewById(R.id.amtText);
        updateBtn = findViewById(R.id.updateBtn);



        reference = FirebaseDatabase.getInstance().getReference();

        updateBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                amount = amtText.getText().toString();
                updateMealAmt(amount);
            }
        });

    }
    private void updateMealAmt(String amount) {

        int intNewAmt = 0;
        try {
            intNewAmt = Integer.parseInt(amount);
        } catch (NumberFormatException e){
            Log.d("ERROR PARSEING", e.getMessage());
        }

        if (intNewAmt == 0) {
            amtText.setError("please enter proper number");
        } else {
            DatabaseReference databaseReference = reference.child("admin").child("mealsAmt");

            Map map = new HashMap();
            map.put("amount", intNewAmt);

            databaseReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
                @Override
                public void onComplete(@NonNull Task task) {
                    if (task.isSuccessful()) {
                        Toast.makeText(admin.this, "Updated", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(admin.this, Home.class));
                    } else {
                        Log.d("ERROR", task.getException().getMessage());
                    }
                }
            });
        }
    }
}