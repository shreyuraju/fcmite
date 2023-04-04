package com.mite.mitefc;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mite.mitefc.Utility.NetworkChangeListener;
import com.mite.mitefc.transaction.MyAdapter;

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

    FirebaseFirestore firestoreDb;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference reference, userReference, adminReference, transReference;

    String NFCUSN, balText, newBal;
    int balInt = 0, transInt=0;
    ProgressDialog progressDialog, progressDialog1;

    RecyclerView recyclerView;
    MyAdapter myAdapter;
    ArrayList<Trans> list, transactionList;

    boolean isPressed = false, upiFlag=false;

    PendingIntent pendingIntent;

    String NFCUID=null;

    final int UPI_PAYMENT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);


        firestoreDb = FirebaseFirestore.getInstance();


        firebaseDatabase = FirebaseDatabase.getInstance();
        reference = FirebaseDatabase.getInstance().getReference();
        userReference = FirebaseDatabase.getInstance().getReference().child("users");
        adminReference = FirebaseDatabase.getInstance().getReference().child("admin");
        transReference = FirebaseDatabase.getInstance().getReference().child("transaction");

        nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        NFCText = findViewById(R.id.TAGdata);
        balData = findViewById(R.id.balanceData);
        amtData = findViewById(R.id.amountData);

        payBtn = findViewById(R.id.payBtn);
        payText = findViewById(R.id.payText);

        progressDialog = new ProgressDialog(this);

        recyclerView = findViewById(R.id.transList);
        recyclerView.setHasFixedSize(true);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setReverseLayout(true);
        linearLayoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(linearLayoutManager);

        list = new ArrayList<>();
        transactionList = new ArrayList<>();

        myAdapter = new MyAdapter(this,list);
        recyclerView.setAdapter(myAdapter);

        pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        payBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(NFCUSN == null) {
                    payText.setError("USN is Null\nTap ID card on the\nback of mobile");
                    return;
                }
                balText = payText.getText().toString();
                Log.d("Paytext: ", balText);
                try {
                    balInt = Integer.parseInt(balText);

                } catch (NumberFormatException e){
                    Log.d("ERROR PARSEING", e.getMessage());
                }
                if ((balInt%2) != 0  ) {
                    payText.setError("Amount should be in multiple of 10");
                    return;
                }
                if(balInt < 10 || balInt >200) {
                    payText.setError("Amount must be greatter\nthan or equal to 10 and\nless than or equal to 200");
                    return;
                }
                if ((balInt+transInt) > 200  ) {
                    payText.setError("Wallet Limit is only 200rs");
                    return;
                }
//                else {
//                    //paymentGateWay(NFCUSN, balText);
//                }

                if (true) {

                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
                    String currentDandT = sdf.format(new Date());
                    String date= currentDandT.substring(11,19);
                    String utr = currentDandT.substring(0,10);
                    utr = utr.replaceAll("\\p{Punct}", "");
                    date = date.replaceAll("\\p{Punct}","");
                    utr = NFCUSN+utr+date;

                    updateBalance(balInt, NFCUSN, utr, currentDandT);

                    balData.setText(null);
                    payText.setText(null);
                    NFCUSN = null;
                    balInt = 0;
                    recyclerView.setAdapter(null);
                } else {
                    Toast.makeText(Home.this, "Payment Failed", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private boolean paymentGateWay(String nfcusn, String balText) {

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        String currentDandT = sdf.format(new Date());
        String date= currentDandT.substring(15,19);
        String utr = currentDandT.substring(0,10);
        utr = utr.replaceAll("\\p{Punct}", "");
        date = date.replaceAll("\\p{Punct}","");
        utr = nfcusn+utr+date;


        Uri uri = Uri.parse("upi://pay").buildUpon()
                .appendQueryParameter("pa", "9206581025@ybl")
                .appendQueryParameter("pn", "Shreyas M")
                .appendQueryParameter("tn", utr)
                .appendQueryParameter("am", balText)
                .appendQueryParameter("cu", "INR")
                .build();

        Intent upiPayIntent = new Intent(Intent.ACTION_VIEW);
        upiPayIntent.setData(uri);

        Intent chooser = Intent.createChooser(upiPayIntent, "Pay With");

        // check if intent resolves
        if(null != chooser.resolveActivity(getPackageManager())) {
            startActivityForResult(chooser, UPI_PAYMENT);
        } else {
            Toast.makeText(getBaseContext(),"No UPI app found, please install one to continue",Toast.LENGTH_SHORT).show();
        }

        if(upiFlag){
            updateBalance(balInt, NFCUSN, utr, currentDandT);
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case UPI_PAYMENT:
                if ((RESULT_OK == resultCode) || (resultCode == 11)) {
                    if (data != null) {
                        String trxt = data.getStringExtra("response");
                        Log.d("UPI", "onActivityResult: " + trxt);
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add(trxt);
                        upiPaymentDataOperation(dataList);
                    } else {
                        Log.d("UPI", "onActivityResult: " + "Return data is null");
                        ArrayList<String> dataList = new ArrayList<>();
                        dataList.add("nothing");
                        upiPaymentDataOperation(dataList);
                    }
                } else {
                    Log.d("UPI", "onActivityResult: " + "Return data is null"); //when user simply back without payment
                    ArrayList<String> dataList = new ArrayList<>();
                    dataList.add("nothing");
                    upiPaymentDataOperation(dataList);
                }
                break;
        }
    }

    private void upiPaymentDataOperation(ArrayList<String> data) {
        if (isConnectionAvailable(getBaseContext())) {
            String str = data.get(0);
            Log.d("UPIPAY", "upiPaymentDataOperation: "+str);
            String paymentCancel = "";
            if(str == null) str = "discard";
            String status = "";
            String approvalRefNo = "";
            String response[] = str.split("&");
            for (int i = 0; i < response.length; i++) {
                String equalStr[] = response[i].split("=");
                if(equalStr.length >= 2) {
                    if (equalStr[0].toLowerCase().equals("Status".toLowerCase())) {
                        status = equalStr[1].toLowerCase();
                    }
                    else if (equalStr[0].toLowerCase().equals("ApprovalRefNo".toLowerCase()) || equalStr[0].toLowerCase().equals("txnRef".toLowerCase())) {
                        approvalRefNo = equalStr[1];
                    }
                }
                else {
                    paymentCancel = "Payment cancelled by user.";
                }
            }

            if (status.equals("success")) {
                //Code to handle successful transaction here.
                upiFlag=true;
                Toast.makeText(getBaseContext(), "Transaction successful.", Toast.LENGTH_SHORT).show();
                Log.d("UPI", "responseStr: "+approvalRefNo);
            }
            else if("Payment cancelled by user.".equals(paymentCancel)) {
                Toast.makeText(getBaseContext(), "Payment cancelled by user.", Toast.LENGTH_SHORT).show();
            }
            else {
                Toast.makeText(getBaseContext(), "Transaction failed.Please try again", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getBaseContext(), "Internet connection is not available. Please check and try again", Toast.LENGTH_SHORT).show();
        }
    }

    public static boolean isConnectionAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
            if (netInfo != null && netInfo.isConnected()
                    && netInfo.isConnectedOrConnecting()
                    && netInfo.isAvailable()) {
                return true;
            }
        }
        return false;
    }

    //Updateing the Balance

    private void updateBalance(int balint, String nfcusn , String utr, String currentDandT) {

        int intNewBal = 0;
        try {
            balInt = Integer.parseInt(String.valueOf(balint));
            intNewBal = Integer.parseInt(newBal);
        } catch (NumberFormatException e){
            Log.d("ERROR PARSEING", e.getMessage());
        };

        intNewBal = balInt + intNewBal;

        Map map = new HashMap();
        //map.put("USN", nfcusn);
        //map.put("NFCUID", NFCUID);
        map.put("balance", intNewBal);
        try {
            userReference.child(nfcusn).updateChildren(map).addOnSuccessListener(new OnSuccessListener() {
                @Override
                public void onSuccess(Object o) {
                    addToTransaction(nfcusn, balint, utr, currentDandT);
                    NFCText.setText("New Balance has been updated\nTap again to see\nUpdated Transaction");
                    balData.setText(null);
                    payText.setText(null);
                    NFCUSN = null;
                    balInt = 0;
                    recyclerView.setAdapter(null);
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
    private void addToTransaction(String nfcusn, int transInt, String utr, String currentDandT) {

        Map map = new HashMap();
        map.put("mode","credit");
        map.put("USN", nfcusn);
        map.put("amount", transInt);
        map.put("utr", utr);
        map.put("date",currentDandT);
        DatabaseReference databaseReference = adminReference.child("alltransaction").push();
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

    private void checkUser(String nfcuid) {
        progressDialog.setTitle("Fetching");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();

        DocumentReference document = firestoreDb.collection("users").document(nfcuid);

        document.get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if(documentSnapshot.exists()) {
                    Toast.makeText(Home.this, "user registered", Toast.LENGTH_SHORT).show();
                    NFCUSN = documentSnapshot.getString("USN");
                    progressDialog.dismiss();
                    checkData(NFCUSN);

                } else {
                    progressDialog.dismiss();
                    Toast.makeText(getBaseContext(),"data not found" , Toast.LENGTH_SHORT).show();
                }

            }
        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(getBaseContext(),"user not found" , Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void checkData(String nfcusn) {

        progressDialog.setTitle("Fetching");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();

        userReference.child(nfcusn).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.exists()) {
                    Toast.makeText(getApplicationContext(), "User Rec Found", Toast.LENGTH_SHORT).show();
                    Map<String, Object> map = (Map<String, Object>) snapshot.getValue();
                    Object balance = map.get("balance");
                    String USN1 = (String) map.get("USN");
                    //NFCUSN=USN1;
                    NFCText.setText(USN1);
                    balData.setText("Balance : "+balance);

                    newBal = String.valueOf(balance);
                    try {
                        transInt = Integer.parseInt(newBal);
                    } catch (Exception e) {
                        Log.d("Error Parsing", e.getMessage());
                    }

                    showAllTransaction(USN1);
                    progressDialog.dismiss();
                    context();
                } else {
                    progressDialog.dismiss();
                    recyclerView.setAdapter(null);
                    NFCText.setText("Tap ID card on the\nback of mobile");
                    balData.setText(null);
                    Toast.makeText(getApplicationContext(), "User Not Found\nPlease do Register", Toast.LENGTH_SHORT).show();

                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("DataBase ERROR :", error.getMessage());
            }
        });
    }

//DISPLAYING TRANSACTION MADE BY USER
    private void showAllTransaction(String usn1) {
        list.clear();
        recyclerView.setAdapter(myAdapter);
        progressDialog1 = new ProgressDialog(this);
        progressDialog.setTitle("Fetching Transaction");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();
        adminReference.child("alltransaction").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {
                    list.clear();

                    for (DataSnapshot data : snapshot.getChildren()) {
                        //Log.d("DATA", data.getValue().toString());
                        String USN, amount, date, mode, utr;
                        USN = data.child("USN").getValue().toString();
                        amount = data.child("amount").getValue().toString();
                        date = data.child("date").getValue().toString();
                        mode = data.child("mode").getValue().toString();
                        utr = data.child("utr").getValue().toString();
                        if (USN.equals(usn1)) {
                            USN = "USN :"+USN;
                            if (mode.equals("credit")) {
                                amount = "+"+amount+" rs";
                            } else {
                                amount = "-"+amount+" rs";
                            }
                            date = "Date :"+date.substring(0,16);
                            utr = "utr no :"+utr;

                            Trans trans = new Trans();
                            trans.setUSN(USN);
                            trans.setAmount(amount);
                            trans.setDate(date);
                            trans.setMode(mode);
                            trans.setUtr(utr);
                            list.add(trans);
                        }

                        progressDialog1.dismiss();
                    }
                    checkBalance(usn1);
                    progressDialog1.dismiss();
                    myAdapter.notifyDataSetChanged();
                } else {
                    recyclerView.setAdapter(null);
                    //  Toast.makeText(Home.this, "No Transaction Exist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.d("DataBase ERROR :", error.getMessage());
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
                        USN = data.child("USN").getValue().toString();
                        bal = data.child("balance").getValue().toString();
                    }*/
                    //NFCText.setText(USN);
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


        if (nfcAdapter != null)
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, nfcIntentFilter, null);
    }

    //reading

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            NFCUID = ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            Log.d("NFC TAG UID","NFC Tag UID :" + ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID)));
        }
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        patchTag(tag);
        try {
            if (tag != null) {
                readFromNFC(tag, intent);
            }
        } catch (Exception e) {
            Toast.makeText(this, "NO Data Found Please Register", Toast.LENGTH_SHORT).show();
            Log.d("ERROR : ", e.getMessage());
        }

    }

    private String ByteArrayToHexString(byte[] byteArrayExtra) {
        int i, j, in;
        String [] hex = {"0","1","2","3","4","5","6","7","8","9","A","B","C","D","E","F"};
        String out= "";

        for(j = 0 ; j < byteArrayExtra.length ; ++j)
        {
            in = (int) byteArrayExtra[j] & 0xff;
            i = (in >> 4) & 0x0f;
            out += hex[i];
            i = in & 0x0f;
            out += hex[i];
        }
        return out;
    }

    //reading from NFC card

    private void readFromNFC(Tag tag, Intent intent) {
        progressDialog.setTitle("Reading");
        progressDialog.setMessage("Please Wait");
        progressDialog.setCanceledOnTouchOutside(true);
        progressDialog.show();
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef == null) {
                progressDialog.dismiss();
                Toast.makeText(this, "NO Data Found Please Register", Toast.LENGTH_SHORT).show();
            } else if (ndef != null) {
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
                        Log.d("DATA : ", text);
                       // NFCText.setText(text);
                        //NFCUSN = text;
                        Log.e("tag", "vahid  -->  " + text);
                        progressDialog.dismiss();
                        checkUser(NFCUID);
                        ndef.close();
                    }

                } else {
                    Toast.makeText(this, "Not able to read from NFC, Please try again...", Toast.LENGTH_LONG).show();
                }
            }
            else {
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

                            recyclerView.setAdapter(null);
                            Log.d("DATA : ", message);
                            progressDialog.dismiss();
                            checkUser(NFCUID);
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

            //case R.id.exit: System.exit(0); break;
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



    //For Refreshing the lauyout for evry 50s

    private void context() {
        refresh(60000);
    }
    private void refresh(int i) {
        final Handler handler = new Handler();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                NFCUID=null;
                NFCUSN=null;
                balText=null;
                balData.setText(null);
                NFCText.setText("Tap ID card on the\nback of mobile");
                payText.setText(null);
                recyclerView.setAdapter(null);
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

    @Override
    public void onBackPressed() {
        if(isPressed) {
            finishAffinity();
            System.exit(0);
        } else {
            Toast.makeText(this, "Press again to Exit", Toast.LENGTH_SHORT).show();
            isPressed = true;
        }
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                isPressed = false;
            }
        };
        new Handler().postDelayed(runnable,2000);
    }
}