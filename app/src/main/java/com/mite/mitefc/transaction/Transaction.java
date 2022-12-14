package com.mite.mitefc.transaction;

import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;

public class Transaction {

    public void addToMainTransaction(Map map) {
        DatabaseReference databaseReference = FirebaseDatabase.getInstance().getReference().child("alltransaction").push();
        databaseReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    Log.d("Suc added to allTrans", "USN : "+map.get("USN")+" amt :"+map.get("amount"));
                } else {
                    Log.d("ERROR", task.getException().getMessage());
                }
            }
        });
    }
}