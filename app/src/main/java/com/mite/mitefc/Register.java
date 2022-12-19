package com.mite.mitefc;

import static com.mite.mitefc.R.id.alertText;

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
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Register extends AppCompatActivity {

    public static final String TAG = Register.class.getSimpleName();
    private EditText registerText;
    TextView alertTxt;
    private NfcAdapter nfcAdapter;
    private boolean isWrite = true;
    private final String sturegex = "[0-9][a-zA-Z][a-zA-Z][0-9][0-9][a-zA-Z][a-zA-Z][0-9][0-9][0-9]";
    private final String empregex = "[0-9][a-zA-Z][a-zA-Z][0-9][0-9][a-zA-Z][a-zA-Z][0-9][0-9][0-9]";

    DatabaseReference reference;
    Pattern pattern, pattern1;
    FirebaseFirestore db;
    boolean flag = false, flag1 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle("REGISTER");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        registerText = findViewById(R.id.registerText);
        alertTxt = findViewById(R.id.alertText);

        reference = FirebaseDatabase.getInstance().getReference();
        db = FirebaseFirestore.getInstance();
        pattern = Pattern.compile(sturegex);
        pattern1 = Pattern.compile(empregex);


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
                if (matcher.matches()) {
                    if (messageToWrite.length() < 10) {
                        registerText.setError("Enter Proper reg USN");
                    } else {
                        NdefRecord record = NdefRecord.createMime(messageToWrite, messageToWrite.getBytes());
                        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
                        if (checkUser(messageToWrite)) {
                            if (writeTag(tag, message)) {
                                if (writeData(messageToWrite)) {
                                    Toast.makeText(getApplicationContext(), "Successfully Registered", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(this, "Error Recording to DB", Toast.LENGTH_SHORT).show();
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Error Registering", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            //Toast.makeText(getApplicationContext(), "User rec already Exists\n Please ", Toast.LENGTH_SHORT).show();
                            alertTxt.setText("Record Already Exists");
                            Toast.makeText(getApplicationContext(), "Please Tap till it Register (3 times)\n if you are NEW USER", Toast.LENGTH_SHORT).show();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    alertTxt.setText("After Entering USN\nTap ID card on\nback of yourMobile");
                                }
                            }, 3000);
                        }
                    }
                } else {
                    registerText.setError("Enter Proper reg USN\n4MTXXXXXXX");
                }
            }
        }
    }

    private boolean checkUser(String text) {
        FirebaseFirestore.getInstance()
                .collection("users")
                .whereEqualTo("USN", text)
                .get()
                .addOnSuccessListener(new OnSuccessListener<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot queryDocumentSnapshots) {
                        Log.d("DATA :",queryDocumentSnapshots.getDocuments().toString());
                        if(queryDocumentSnapshots.isEmpty()) {
                            flag = true;
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        
                    }
                });
        return flag;
    }

    private boolean writeData(String usn) {
        
        Map map = new HashMap();
        map.put("USN", usn);

        db.collection("users").document(usn).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()) {
                    int bal =0;
                    DatabaseReference databaseReference = reference.child("users").child(usn);
                    Map map = new HashMap();
                    map.put("USN", usn);
                    map.put("balance", bal);
                    databaseReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
                        @Override
                        public void onComplete(@NonNull Task task) {
                            if(task.isSuccessful()) {
                                flag1 = true;
                                Log.d("Recorded to DB", map.toString());
                                //Toast.makeText(Register.this, "Recorded to DB", Toast.LENGTH_SHORT).show();
                            } else {
                                Log.d("ERROR :", task.getException().getMessage());
                            }
                        }
                    });

                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("ERROR :",e.toString());
            }
        });

        return flag1;
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