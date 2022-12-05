package com.mite.mitefc;

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
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.util.Locale;

public class Register extends AppCompatActivity {

    public static final String TAG = Register.class.getSimpleName();
    private EditText registerText;
    private NfcAdapter nfcAdapter;
    private boolean isWrite = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        getSupportActionBar().setTitle("REGISTER");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        registerText = findViewById(R.id.registerText);

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if (tag != null) {
            if (isWrite) {
                String messageToWrite = registerText.getText().toString().toUpperCase(Locale.ROOT);

                if (messageToWrite != null && (!TextUtils.equals(messageToWrite, "null")) && (!TextUtils.isEmpty(messageToWrite))) {
                    NdefRecord record = NdefRecord.createMime(messageToWrite, messageToWrite.getBytes());
                    NdefMessage message = new NdefMessage(new NdefRecord[]{record});

                    if (writeTag(tag, message)) {
                        Boolean writeToDb = wirteToDB(messageToWrite);
                        Toast.makeText(getApplicationContext(), "Successfully Registered", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error Registering", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    registerText.setError("Please enter the text to write");
                }
            }
        }
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