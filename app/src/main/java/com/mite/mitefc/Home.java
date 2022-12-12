package com.mite.mitefc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcManager;
import android.nfc.Tag;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.mite.mitefc.Utility.NetworkChangeListener;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;


public class Home extends AppCompatActivity {

    private NfcAdapter nfcAdapter, adapter;
    private TextView NFCText, balData, amtData;
    public static final String TAG = Home.class.getSimpleName();

    NetworkChangeListener networkChangeListener = new NetworkChangeListener();

    Button payBtn;
    EditText payText;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference, userReference, adminReference, transReference;

    String NFCUSN, balText, newBal;
    int balInt = 0, transInt=0;
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        context();
        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = FirebaseDatabase.getInstance().getReference();
        userReference = FirebaseDatabase.getInstance().getReference().child("users");
        adminReference = FirebaseDatabase.getInstance().getReference().child("admin");
        transReference = FirebaseDatabase.getInstance().getReference();

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        NFCText = findViewById(R.id.TAGdata);
        balData = findViewById(R.id.balanceData);
        amtData = findViewById(R.id.amountData);

        payBtn = findViewById(R.id.payBtn);
        payText = findViewById(R.id.payText);

        progressDialog = new ProgressDialog(this);

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                balText = payText.getText().toString();
                try {
                    balInt = Integer.parseInt(payText.getText().toString());
                    transInt = balInt;
                } catch (NumberFormatException e){
                    Log.d("ERROR PARSEING", e.getMessage());
                }
                if(balInt < 50) {
                    payText.setError("Amount must be greatter than 50");
                    return;
                }
                updateBalance(balInt, NFCUSN);
            }
        });


    }

    //Updateing the Balance

    private void updateBalance(int balText, String nfcusn) {

        int intNewBal = 0;
        try {
            balInt = Integer.parseInt(String.valueOf(balText));
            intNewBal = Integer.parseInt(newBal);
        } catch (NumberFormatException e){
            Log.d("ERROR PARSEING", e.getMessage());
        };

        intNewBal = balInt + intNewBal;

        Map map = new HashMap();
        map.put("USN", nfcusn);
        map.put("balance", intNewBal);
        try {
            userReference.child(nfcusn).setValue(map).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    //checkBalance(nfcusn);
                    addToTransaction(nfcusn, transInt);
                    NFCText.setText("Balance has been updated\nTap again to see\nupdated Balance");
                    balData.setText(null);
                    payText.setText(null);
                    NFCUSN = null;
                    balInt =0;
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d("ERROR updating", e.getMessage());
                }
            });
        } catch (Exception e) {
            Log.d("ERROR updating", e.getMessage());
            Toast.makeText(getApplicationContext(), "Failed To Update", Toast.LENGTH_SHORT).show();
        }

    }

    //Transaction details
    private void addToTransaction(String nfcusn, int transInt) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z");
        String currentDandT = sdf.format(new Date());
        String date= currentDandT.substring(15,22);
        String utr = currentDandT.substring(0,10);
        utr = utr.replaceAll("\\p{Punct}", "");
        date = date.replaceAll("\\p{Punct}","");
        utr = utr+date;
        Map map = new HashMap();
        map.put("mode","credit");
        map.put("USN", nfcusn);
        map.put("amount", transInt);
        map.put("utr", utr);
        map.put("date",currentDandT);
        DatabaseReference databaseReference = reference.child("transaction").push();
        databaseReference.updateChildren(map).addOnCompleteListener(new OnCompleteListener() {
            @Override
            public void onComplete(@NonNull Task task) {
                if (task.isSuccessful()) {
                    Log.d("Successfully added", "USN : "+map.get("USN")+" amt :"+map.get("amount"));
                } else {
                    Log.d("ERROR", task.getException().getMessage());
                }
            }
        });
    }

    //checking balance data from database
    private void checkUser(String text) {
        progressDialog.setTitle("Fetching");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();
        userReference.child(text).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()) {
                    Toast.makeText(getApplicationContext(), "User Rec Found", Toast.LENGTH_SHORT).show();
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Object balance = map.get("balance");
                    String USN1 = (String) map.get("USN");
                    NFCText.setText(USN1);
                    balData.setText("Balance : "+balance);
                    newBal = String.valueOf(balance);
                    progressDialog.dismiss();
                } else {
                    NFCText.setText("Tap ID card on the\nback of mobile");
                    balData.setText(null);
                    progressDialog.dismiss();
                    Toast.makeText(getApplicationContext(), "User Not Found\nPlease do Register", Toast.LENGTH_SHORT).show();

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //check balance

    private void checkBalance(String nfcusn) {
        
        userReference.child(nfcusn).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Object balance = map.get("balance");
                    String USN = (String) map.get("USN");
/*                  for(DataSnapshot data : snapshot.getChildren()) {
                        Log.d("DATA :" , data.toString());
                        USN = data.child("USN").getValue().toString();bal = data.child("balance").getValue().toString();
                    }*/
                    NFCText.setText(USN);
                    balData.setText("Balance : "+balance);
                } else {
                    Toast.makeText(Home.this, "ERROR retriveing Balance", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Display Meal Amount
    private void checkMealAmt() {

        adminReference.child("mealsAmt").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    String mealsamt = String.valueOf(map.get("amount"));
                    amtData.setText("Meals : "+mealsamt);
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("ERROR", error.getMessage());
            }
        });

    }

    //on pause and on resume of NFC Activity
    @Override
    protected void onPause() {
        super.onPause();
        if(nfcAdapter != null) nfcAdapter.disableForegroundDispatch(this);
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

    //reading
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        patchTag(tag);
        if (tag != null) {
            readFromNFC(tag, intent);
        }
    }

    //reading from NFC card

    private void readFromNFC(Tag tag, Intent intent) {
        progressDialog.setTitle("Reading");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                NdefMessage ndefMessage = ndef.getNdefMessage();

                if (ndefMessage != null) {

                    Parcelable[] messages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

                    if (messages != null) {
                        NdefMessage[] ndefMessages = new NdefMessage[messages.length];
                        for (int i = 0; i < messages.length; i++) {
                            ndefMessages[i] = (NdefMessage) messages[i];
                        }
                        NdefRecord record = ndefMessages[0].getRecords()[0];
                        byte[] payload = record.getPayload();
                        String text = new String(payload);
                       // NFCText.setText(text);
                        NFCUSN = text;
                        Log.e("tag", "vahid  -->  " + text);
                        progressDialog.dismiss();
                        checkUser(text);
                        ndef.close();
                    }
                } else {
                    Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();
                }
            } else {
                NdefFormatable format = NdefFormatable.get(tag);
                if (format != null) {
                    try {
                        format.connect();
                        NdefMessage ndefMessage = ndef.getNdefMessage();

                        if (ndefMessage != null) {
                            String message = new String(ndefMessage.getRecords()[0].getPayload());
                            Log.d(TAG, "NFC found.. " + "readFromNFC: " + message);
                         //   NFCText.setText(message);
                            NFCUSN = message;
                            progressDialog.dismiss();
                            checkUser(message);
                            ndef.close();
                        } else {
                            Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();

                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    Toast.makeText(this, "NFC is not readable", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private Tag patchTag(Tag oTag) {
        if (oTag == null)
            return null;

        String[] sTechList = oTag.getTechList();

        Parcel oParcel, nParcel;

        oParcel = Parcel.obtain();
        oTag.writeToParcel(oParcel, 0);
        oParcel.setDataPosition(0);

        int len = oParcel.readInt();
        byte[] id = null;
        if (len >= 0) {
            id = new byte[len];
            oParcel.readByteArray(id);
        }
        int[] oTechList = new int[oParcel.readInt()];
        oParcel.readIntArray(oTechList);
        Bundle[] oTechExtras = oParcel.createTypedArray(Bundle.CREATOR);
        int serviceHandle = oParcel.readInt();
        int isMock = oParcel.readInt();
        IBinder tagService;
        if (isMock == 0) {
            tagService = oParcel.readStrongBinder();
        } else {
            tagService = null;
        }
        oParcel.recycle();

        int nfca_idx = -1;
        int mc_idx = -1;

        for (int idx = 0; idx < sTechList.length; idx++) {
            if (sTechList[idx] == NfcA.class.getName()) {
                nfca_idx = idx;
            } else if (sTechList[idx] == MifareClassic.class.getName()) {
                mc_idx = idx;
            }
        }

        if (nfca_idx >= 0 && mc_idx >= 0 && oTechExtras[mc_idx] == null) {
            oTechExtras[mc_idx] = oTechExtras[nfca_idx];
        } else {
            return oTag;
        }

        nParcel = Parcel.obtain();
        nParcel.writeInt(id.length);
        nParcel.writeByteArray(id);
        nParcel.writeInt(oTechList.length);
        nParcel.writeIntArray(oTechList);
        nParcel.writeTypedArray(oTechExtras, 0);
        nParcel.writeInt(serviceHandle);
        nParcel.writeInt(isMock);
        if (isMock == 0) {
            nParcel.writeStrongBinder(tagService);
        }
        nParcel.setDataPosition(0);
        Tag nTag = Tag.CREATOR.createFromParcel(nParcel);
        nParcel.recycle();
        return nTag;
    }

    //POPUP option menu
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = new MenuInflater(this);
        inflater.inflate(R.menu.homemenu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.register:
                Intent i = new Intent(getBaseContext(), Register.class);
                startActivity(i);
                break;

            case R.id.admin:
                Intent i1 = new Intent(getBaseContext(), admin.class);
                startActivity(i1);
                break;

            case R.id.exit: System.exit(0); break;
        }
        return super.onOptionsItemSelected(item);
    }

    //check NFC hardware wheather iit is available or NOT

    private void checkForNFCHardware() {
        AlertDialog alertDialog = new AlertDialog.Builder(this).create();

        Context context=this;
        NfcManager nfcManager = (NfcManager) context.getSystemService(Context.NFC_SERVICE);
        adapter = nfcManager.getDefaultAdapter();
        if (adapter != null && !adapter.isEnabled()) {
            alertDialog.setTitle("Need to Enable NFC");
            alertDialog.setIcon(R.drawable.ic_baseline_nfc_36);
            alertDialog.setMessage("Please Enable Nfc");
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            // Toast.makeText(getApplicationContext(), "Need to Enable NFC", Toast.LENGTH_SHORT).show();
        } else if (adapter != null && adapter.isEnabled()) {
          //  Toast.makeText(getApplicationContext(), "NFC available", Toast.LENGTH_SHORT).show();
        } else {
            alertDialog.setTitle("HARDWARE ERROR");
            alertDialog.setIcon(R.drawable.ic_baseline_error_outline_54);
            alertDialog.setMessage("NFC Hardware not detected\nYou can't USE the Application");
            alertDialog.setCanceledOnTouchOutside(false);
            alertDialog.show();
            //Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT).show();
        }
    }



    //For Refreshing the lauyout for evry 30s

    private void context() {
        refresh(60000);
    }
    private void refresh(int i) {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                NFCUSN=null;
                balText=null;
                balData.setText(null);
                NFCText.setText("Tap ID card on the\nback of mobile");
                payText.setText(null);
                context();
            }
        };
        handler.postDelayed(runnable, i);
    }

    //on start
    @Override
    protected void onStart() {
        checkForNFCHardware();
        checkMealAmt();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        registerReceiver(networkChangeListener, filter);
        super.onStart();
    }

    @Override
    protected void onStop() {
        unregisterReceiver(networkChangeListener);
        super.onStop();
    }
}