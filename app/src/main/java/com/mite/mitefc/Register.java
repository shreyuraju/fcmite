package com.mite.mitefc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {

    public static final String TAG = Register.class.getSimpleName();
    private EditText registerText;
    private NfcAdapter nfcAdapter;
    private boolean isWrite = true;
    private final String regex = "";

    DatabaseReference reference;
    Pattern pattern;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle("REGISTER");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        registerText = findViewById(R.id.registerText);
        reference = FirebaseDatabase.getInstance().getReference();

        pattern = Pattern.compile(regex);


    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

        if (tag != null) {
            if (isWrite) {
                String messageToWrite = registerText.getText().toString().toUpperCase(Locale.ROOT);
                Matcher matcher = pattern.matcher(messageToWrite);
                //matcher.matches()
                    if (messageToWrite.length() < 10) {
                        registerText.setError("Enter Proper reg USN");
                    } else {
                        if (messageToWrite != null && (!TextUtils.equals(messageToWrite, "null")) && (!TextUtils.isEmpty(messageToWrite))) {
                            NdefRecord record = NdefRecord.createMime(messageToWrite, messageToWrite.getBytes());
                            NdefMessage message = new NdefMessage(new NdefRecord[]{record});
                            if (writeData(messageToWrite)) {
                                if (writeTag(tag, message)) {
                                    Boolean writeToDb = wirteToDB(messageToWrite);
                                    if(writeToDb)
                                        Toast.makeText(getApplicationContext(), "Successfully Registered", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(getApplicationContext(), "Error Registering", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Error Registering", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            registerText.setError("Please enter the text to write");
                        }
                    }
            }
        }
    }

    private boolean writeData(String messageToWrite) {
        DatabaseReference databaseReference = reference.child("users").child(messageToWrite).push();
        Map map = new HashMap();
        map.put("USN", messageToWrite);
        map.put("balance", 0);

        databaseReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if(task.isSuccessful()) {
                    Toast.makeText(Register.this, "Data is recorded in DB", Toast.LENGTH_SHORT).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ERROR :", e.getMessage());
            }
        });

        return true;
    }

    private Boolean wirteToDB(String messageToWrite) {
        return null;
    }

    private boolean writeTag(Tag tag, NdefMessage message) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    return false;
                }
                ndef.writeNdefMessage(message);
                return true;
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        format.format(message);
                        return true;
                    } catch (IOException e) {
                        return false;
                    }
                } else {
                    return false;
                }
            }
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter ndefDetected = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        IntentFilter[] nfcIntentFilter = new IntentFilter[]{techDetected, tagDetected, ndefDetected};

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }


}