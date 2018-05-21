package com.example.mobilitys.findaddress;

import android.Manifest;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Parcelable;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.zxing.Result;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class MainActivity extends AppCompatActivity{

    private LeDeviceListAdapter mLeDeviceListAdapter;

    //private static final int REQUEST_CAMERA = 1;

    BluetoothManager btManager;
    BluetoothAdapter btAdapter;
    BluetoothAdapter mBluetoothAdapter;     //  for classic BT
    BluetoothLeScanner btScanner;
    //TextView tvName, tvAddress;

    ListView lv;

    AlertDialog WriteTagDialog;

    Button startScanningButton, addButton, barcodeButton;

    //  check scanning status
    Boolean bIsScanning = false;

    private final static int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private BroadcastReceiver mReceiver;
    boolean mWriteMode = false;

    //  180323 sam, below is for NFC
    public static final String ERROR_DETECTED = "No NFC tag detected!";
    public static final String WRITE_SUCCESS = "Text written to the NFC tag successfully!";
    public static final String WRITE_ERROR = "Error during writing, is the NFC tag close enough to your device?";
    NfcAdapter mNfcAdapter;
    PendingIntent mNfcPendingIntent;
    IntentFilter writeTagFilters[];
    boolean writeMode;
    Tag myTag;
    //byte[] mimeData;
    String strDeviceName;
    String strMacAddress;
    String strShowTitle;

    private ZXingScannerView scannerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = btManager.getAdapter();
        btScanner = btAdapter.getBluetoothLeScanner();

        //  here's for classic
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        //mBluetoothAdapter.startDiscovery();

        if (btAdapter != null && !btAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            Log.i("BLE", "ACTION_REQUEST_ENABLE");
        }

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1001); //Any number
        //ActivityCompat.requestPermissions(reqStrs, 1001);

        if (! isLocationEnabled(this)) {
            Log.i("BLE", "location not enabled..");
        } else {
            Log.i("BLE", "location is enabled..");
        }
        // Create a BroadcastReceiver for ACTION_FOUND
        mReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                // When discovery finds a device
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    // Get the BluetoothDevice object from the Intent
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    // Add the name and address to an array adapter to show in a ListView
                    //mArrayAdapter.add(device.getName() + "\n" + device.getAddress());
                    Log.i("BLE", "device nme: " + device.getName() + " MAC: " + device.getAddress());
                    mLeDeviceListAdapter.addDevice(device);
                }
            }
        };
        // Register the BroadcastReceiver
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy

        startScanningButton = (Button) findViewById(R.id.button);
        addButton = (Button) findViewById(R.id.buttonAdd);

        //  180327 sam, for bar code scanner
        /*
        barcodeButton = (Button) findViewById(R.id.btnBarcode);

        barcodeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start the qr scan activity

                Intent i = new Intent(MainActivity.this,QrCodeActivity.class);
                startActivityForResult( i,REQUEST_CAMERA);

            }
        });
        */
        //tvName = (TextView) findViewById(R.id.textAddress);
        //tvAddress = (TextView) findViewById(R.id.textName);

        //  180326 sam, get from resources
        //strShowTitle = getResources().getString(R.string.strCloseTagToWrite);

        startScanningButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                btAdapter = btManager.getAdapter();
                if (btAdapter == null) {
                    showMessage("ERROR, please make sure Bluetooth is ON!!");
                    return;
                } else {
                    btScanner = btAdapter.getBluetoothLeScanner();
                }
                if (null == btScanner) {
                    btScanner = btAdapter.getBluetoothLeScanner();
                    if (null == btScanner) {
                        showMessage("ERROR, please make sure Bluetooth is ON!!");
                        return;
                    }
                }
                Log.i("BLE", "btScanner: " + btScanner);
                if (!bIsScanning)
                    startScanning();
                else
                    stopScanning();
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                addNewaddress();
            }
        });


        lv = (ListView) findViewById(R.id.listView);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        lv.setAdapter(mLeDeviceListAdapter);

        //  180322 sam, i need a item click listener
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Log.i("BLE", "you pressed " + id);
            //  180323 sam, now we nned retrive the data, if succeed then we can write to NFC tag
            ViewHolder vh = (ViewHolder) view.getTag();
            
            strDeviceName = vh.deviceName.getText().toString();
            strMacAddress = vh.deviceAddress.getText().toString();
            String s = "BB:F2";
            byte b[] = s.getBytes();
            
            String a = Arrays.toString(strMacAddress.getBytes());
            Log.i("BLE", a);
            //Log.i("BLE", new String(strMacAddress.getBytes()));
            Log.i("BLE", "name: " + strDeviceName + "  MAC: " + strMacAddress );
          
            mNfcAdapter = NfcAdapter.getDefaultAdapter(MainActivity.this);
            mNfcPendingIntent = PendingIntent.getActivity(MainActivity.this, 0,
                    new Intent(MainActivity.this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);

            enableTagWriteMode();
            //getResources().getString(R.string.strStopScan)
                //Context ctx;
                //String ss = activity.getResources().getString(R.string.strCloseTagToWrite);
            //new AlertDialog.Builder(MainActivity.this).setTitle(strShowTitle)
                //  180326 sam, user the dialog builder will convenient
                AlertDialog.Builder mBuilder = new AlertDialog.Builder(MainActivity.this);
                mBuilder.setTitle(R.string.strCloseTagToWrite);
                //mBuilder.setMessage("please close the device to write");
                mBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                /*
                        WriteTagDialog = new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.strCloseTagToWrite))
                                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                    @Override
                                    public void onCancel(DialogInterface dialog) {
                                        disableTagWriteMode();
                                    }

                                }).create();
                        */
                WriteTagDialog = mBuilder.create();
                WriteTagDialog.show();
                /*
                new AlertDialog.Builder(MainActivity.this).setTitle(getResources().getString(R.string.strCloseTagToWrite))
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            disableTagWriteMode();
                        }

                    }).create().show();
                    */


            }
        });

        /*
        if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("This app needs location access");
            builder.setMessage("Please grant location access so this app can detect peripherals.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
                }
            });
            builder.show();
        }
        */

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        //  180323 sam, below is for NFC
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            // Stop here, we definitely need NFC
            Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
        }
        readFromIntent(getIntent());

    }

    public void scanCode(View view) {
        scannerView = new ZXingScannerView(this);
        scannerView.setResultHandler(new ZXingScannerResultHandler());

        setContentView(scannerView);
        scannerView.startCamera();
    }

    class ZXingScannerResultHandler implements ZXingScannerView.ResultHandler {
        @Override
        public void handleResult(Result result) {
            String resultCode = result.getText();
            Toast.makeText(MainActivity.this, resultCode, Toast.LENGTH_LONG).show();

            setContentView(R.layout.activity_main);
            scannerView.stopCamera();
        }
    }



    private void enableTagWriteMode() {
        mWriteMode = true;
        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter[] mWriteTagFilters = new IntentFilter[] { tagDetected };
        mNfcAdapter.enableForegroundDispatch(this, mNfcPendingIntent, mWriteTagFilters, null);
    }

    private void disableTagWriteMode() {
        mWriteMode = false;
        mNfcAdapter.disableForegroundDispatch(this);
    }

    
    //int bbc = 187;
    public byte[] encodeData() {
        int i = 0;
        int j = 0;
        int k = 0;

        i = strDeviceName.length() + 10;
        byte[] mimeData = new byte[i];
        //{11, 0x00, (byte)bbc, 0x6F, (byte)0xF2, 0x6F, 12, 0x00, 0x08, 0x09,
                //0x42, 0x54, 0x48, 0x2D, 0x41, 0x41, 0x41};

        mimeData[0] = (byte)i;
        mimeData[1] = 0;

        /*
        mimeData[2] = (byte)187;
        mimeData[3] = (byte)111;
        mimeData[4] = (byte)242;;
        mimeData[5] = (byte)111;
        mimeData[6] = 18;
        mimeData[7] = 02;
        */

        //  180326 sam, here we need to convert MAC address to byte, let's do it step by step
        //  take F9:F4:1C:6B:6A:B7 as example, first byte of mineData should be 0xBF,
        //  how we convert it?  for example, BF is (byte)(16 * 11 + 15)

        for (i = 5;i >= 0; i--) {
            j = (int)strMacAddress.charAt(i * 3);
            k = (int)strMacAddress.charAt(i * 3 + 1);
            if (j >= 65 && j <= 70)   {       //  A ~ F
                j -= 55;
            }
            if (j >=48 && j <= 57) {        //  0 ~ 9
                j -= 48;
            }

            if (k >= 65 && k <= 70)   {       //  A ~ F
                k -= 55;
            }
            if (k >=48 && k <= 57) {        //  0 ~ 9
                k -= 48;
            }

            mimeData[7 - i] = (byte)(j * 16 + k);

            Log.i("BLE", "mimeData : " +  mimeData[7 - i]);

        }

        //
        mimeData[8] =(byte)(strDeviceName.length() + 1);
        mimeData[9] = 9;
        //  now the device name
        //strDeviceName
        //String ss = "BTH-AAA";
        for (i = 0;i < strDeviceName.length();i++) {
            //mimeData[10 + i] = (byte)((int)strDeviceName.charAt(i));
            int ii = strDeviceName.charAt(i);
            mimeData[10 + i] = (byte)(ii);
        }
        /*
        mimeData[10] = 65;
        mimeData[11] = 65;
        mimeData[12] = 65;
        mimeData[13] = 45;
        mimeData[14] = 65;
        mimeData[15] = 65;
        mimeData[16] = 65;
        */
        
        
        return mimeData;
    }
    @Override
    protected void onNewIntent(Intent intent) {
        // Tag writing mode
        if (mWriteMode && NfcAdapter.ACTION_TAG_DISCOVERED.equals(intent.getAction())) {
            Tag detectedTag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            NdefRecord record = NdefRecord.createMime("application/vnd.bluetooth.ep.oob", encodeData());
            //((TextView)findViewById(R.id.value)).getText().toString().getBytes());
            NdefMessage message = new NdefMessage(new NdefRecord[] { record });
            if (writeTag(message, detectedTag)) {
                //Toast.makeText(this, "Success: Wrote placeid to nfc tag", Toast.LENGTH_LONG)
                Toast.makeText(this, getResources().getString(R.string.strWriteTagOK), Toast.LENGTH_LONG)
                        .show();
            } else {
                Toast.makeText(this, getResources().getString(R.string.strWriteTagFail), Toast.LENGTH_LONG)
                        .show();
            }
            //  180326 sam, dismiss the alertdialog
            WriteTagDialog.dismiss();
        }
    }

    public boolean writeTag(NdefMessage message, Tag tag) {
        int size = message.toByteArray().length;
        try {
            Ndef ndef = Ndef.get(tag);
            if (ndef != null) {
                ndef.connect();
                if (!ndef.isWritable()) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag not writable",
                            Toast.LENGTH_SHORT).show();
                    return false;
                }
                if (ndef.getMaxSize() < size) {
                    Toast.makeText(getApplicationContext(),
                            "Error: tag too small",
                            Toast.LENGTH_SHORT).show();
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



    private void readFromIntent(Intent intent) {
        String action = intent.getAction();
        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
                || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
            Parcelable[] rawMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            NdefMessage[] msgs = null;
            if (rawMsgs != null) {
                msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
            //buildTagViews(msgs);
        }
    }





    public void showMessage(String str) {
        Toast.makeText(this, str, Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (btManager == null) {
            Log.i("BLE", "btManager is null");
        } else {
            Log.i("BLE", "btManager: " + btManager);
        }
        //btAdapter = btManager.getAdapter();
        if (btScanner == null) {
            Log.i("BLE", "btScanner is null");
        } else {
            Log.i("BLE", "btScanner: " + btScanner);
        }

        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        /*
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }
        */

        /*
        lv.post(new Runnable() {
                    @Override
                    public void run() {
                        lv.setAdapter(mLeDeviceListAdapter);
                    }
                }
        );
        */

        Log.i("BLE", "onResumescanning devices.. of scan");
        // Initializes list view adapter.

        //lv.setAdapter(mLeDeviceListAdapter);
        //lvsetListAdapter(mLeDeviceListAdapter);
        //scanLeDevice(true);
    }


    // Device scan callback.
    private ScanCallback leScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            //peripheralTextView.append("Device Name: " + result.getDevice().getName() + " rssi: " + result.getRssi() + "\n");

            // auto scroll for text view
            //final int scrollAmount = peripheralTextView.getLayout().getLineTop(peripheralTextView.getLineCount()) - peripheralTextView.getHeight();
            // if there is no need to scroll, scrollAmount will be <=0
            //if (scrollAmount > 0)
            //peripheralTextView.scrollTo(0, scrollAmount);
            Log.i("BLE", "add device in ScanCallback..");
            mLeDeviceListAdapter.addDevice(result.getDevice());
            //Log.i("BLE", "get count = " + mLeDeviceListAdapter.getCount());
            //Log.i("BLE", "name: " + result.getDevice().getName() + " address: " + result.getDevice().getAddress());
        }
    };

    @Override
    public void onPause() {
        super.onPause();
        //scannerView.stopCamera();
    }


    @Override
    public void onStart() {
        super.onStart();


    }

    @Override
    public void onStop() {
        super.onStop();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
    }

    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        Context context;

        public LeDeviceListAdapter(Context c) {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            context = c;
            //mInflator = MainActivity.this.getLayoutInflater();
            //mInflator = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //mInflator = MainActivity.this.getLayoutInflater();
            mInflator = LayoutInflater.from(c);
        }

        public void addDevice(BluetoothDevice device) {

            if (!mLeDevices.contains(device)) {
                Log.i("BLE", "device: " + device);
                String strDeviceName = device.getName();
                //tvName.setText(strDeviceName);
                //tvAddress.setText(device.getAddress());
                if (null != strDeviceName) {
                    //if (strDeviceName.contains("PTT")) {    //  search only for what i want
                    //  if found mPreviousConnectedAddress then connect it directly
                    //if (device.getAddress().equals(mPreviousConnectedAddress)) {
                    //connectDevice(device);
                    //} else
                    Log.i("BLE", "call mLeDevices add..");
                    mLeDevices.add(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                    //Log.i("BLE", "device array: " + mLeDevices.toArray());
                    Log.i("BLE", "device array: " + mLeDevices);

                    //connectDevice()
                    //Thread thread = new Thread(new backToHomeThread());
                    //thread.start();
                    //}

                }
            }
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {

            mLeDevices.clear();
            Log.i("BLE", "device list count: " + mLeDevices.size());
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ViewHolder viewHolder;
            Log.i("BLE", "getView?");
            // General ListView optimization code.
            if (view == null) {
                view = mInflator.inflate(R.layout.device_list, null);
                //view = mInflator.inflate(R.layout.device_list, viewGroup, false);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);

            final String deviceName = device.getName();
            Log.i("BLE", "device name: " + deviceName);
            if (deviceName != null && deviceName.length() > 0)
                viewHolder.deviceName.setText(deviceName);
            else
                viewHolder.deviceName.setText("unknown_device");
            viewHolder.deviceAddress.setText(device.getAddress());

            return view;
        }
    }   //  end of class LeDeviceListAdapter

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
    }

    public void startScanning() {
        Log.i("BLE", "start scanning");
        //peripheralTextView.setText("");
        //startScanningButton.setVisibility(View.INVISIBLE);
        //startScanningButton.setText("Stop Scan");
        startScanningButton.setText(getResources().getString(R.string.strStopScan));

        //stopScanningButton.setVisibility(View.VISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                //btScanner.startScan(leScanCallback);
                mBluetoothAdapter.startDiscovery();
                bIsScanning = true;
            }
        });
    }

    public void stopScanning() {
        System.out.println("stopping scanning");
        //peripheralTextView.append("Stopped Scanning");
        startScanningButton.setVisibility(View.VISIBLE);
        //startScanningButton.setText("Start Scan");
        startScanningButton.setText(getResources().getString(R.string.strStartScan));
        //startScanningButton.setText(getResources().getString(R.string.strStartScan));
        //stopScanningButton.setVisibility(View.INVISIBLE);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                //btScanner.stopScan(leScanCallback);
                mBluetoothAdapter.cancelDiscovery();
                bIsScanning = false;
            }
        });
    }

    public void addNewaddress() {
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    public static boolean isLocationEnabled(Context context) {
        int locationMode = 0;
        String locationProviders;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);

            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }

            return locationMode != Settings.Secure.LOCATION_MODE_OFF;

        }else{
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }


    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }



}
