package com.daniel.hw1;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {


    // Layout components
    private TextInputEditText inputEditText;
    private MaterialButton button;

    // Sensors related vars
    private SensorManager sensorManager;
    private Sensor proximitySensor;
    private SensorEventListener sensorEventListener;
    private float distance;

    // Permission related vars
    private static final int PERMISSION_CONTACTS_REQUEST_CODE = 1111;
    private static final int MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE = 1112;

    // Battery related vars
    private String percentage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findAll();

        this.registerReceiver(this.mBatInfoReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        sensorEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (sensorEvent.sensor.getType() == Sensor.TYPE_PROXIMITY) {
                    distance = sensorEvent.values[0];
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {

            }
        };

        sensorManager.registerListener(sensorEventListener, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        inputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                inputEditText.setText("");
            }
        });
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideKeyBoard();
                checkPassword();
            }
        });
    }


    private void findAll() {
        this.inputEditText = findViewById(R.id.main_input_txt);
        this.button = findViewById(R.id.main_btn_login);
    }

    private void hideKeyBoard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /**
     * Check all dependencies is applied
     */
    private void checkPassword() {

        if (!getContacts() || !getProximitySensor() || !getBatteryPercentage()) {
            Toast.makeText(this, "Wrong Password", Toast.LENGTH_LONG).show();
            inputEditText.clearFocus();
            inputEditText.setText("Oops.. Try Again");
        } else {
            Toast.makeText(this, " \uD83C\uDF89  \uD83C\uDF89  \uD83C\uDF89 ", Toast.LENGTH_LONG).show();

            inputEditText.clearFocus();
            inputEditText.setText("Great job !");
        }
    }

    /**
     * check that password contains the phone battery percentage & battery above 70% & input pass is no more than 8 characters
     *
     * @return boolean value if dependency is applied
     */
    private boolean getBatteryPercentage() {
        String input = inputEditText.getText().toString();

        if (!(input.length() > 8))
            if (Integer.parseInt(percentage) > 70)
                if (input.contains(percentage))
                    return true;
        return false;
    }

    /**
     * checks if proximity sensor is covered
     *
     * @return boolean value if dependency is applied
     */
    private boolean getProximitySensor() {
        if (distance == 0) {
            return true;
        }
        return false;
    }

    /**
     * get all contacts &
     * checks if phones contact contains specific name with a specific number
     *
     * @return boolean value if dependency is applied
     */
    private boolean getContacts() {
        boolean isGranted = checkForPermission(Manifest.permission.READ_CONTACTS);

        if (!isGranted) {
            requestPermission(Manifest.permission.READ_CONTACTS, PERMISSION_CONTACTS_REQUEST_CODE);
            return false;
        }
        Map<String, String> data = new HashMap<>();
        ContentResolver cr = getContentResolver();
        Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);

        if ((cur != null ? cur.getCount() : 0) > 0) {
            while (cur != null && cur.moveToNext()) {
                String id = cur.getString(cur.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cur.getString(cur.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));

                if (cur.getInt(cur.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)) > 0) {
                    Cursor pCur = cr.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[]{id}, null);
                    while (pCur.moveToNext()) {
                        String phoneNo = pCur.getString(pCur.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        data.put(name, phoneNo);
                    }
                    pCur.close();
                }
            }
        }
        if (cur != null) {
            cur.close();
        }
        return checkContacts(data);
    }

    /**
     * Broadcast Receiver inorder to get battery percentage
     */
    private BroadcastReceiver mBatInfoReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;
            percentage = String.valueOf((int) batteryPct);
        }
    };

    /**
     * cthe core check of the contacts map
     *
     * @param data - map object which contain all phone contacts
     * @return booelan value whether some conditions applied
     */
    private boolean checkContacts(Map<String, String> data) {
        try {
            String contactName = "Vlad";
            String phoneNumber = "050-011-2233";
            String actualPhoneNumberValue = data.get(contactName);
            if (!actualPhoneNumberValue.equals(phoneNumber))
                return false;
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    private void requestPermission(String permission, int code) {
        ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, code);
    }

    private boolean checkForPermission(String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }
        return false;
    }

    private void requestPermissionWithRationaleCheck(String permission, int code, int manualCode) {
        if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this, permission)) {
            ActivityCompat.requestPermissions(MainActivity.this, new String[]{permission}, code);
        } else {
            openPermissionSettingDialog(permission, code, manualCode);
        }
    }

    private void openPermissionSettingDialog(String permission, int code, int manualCode) {
        String message = "This app cannot work without those permissions \nPlease turn them on.";
        AlertDialog alertDialog =
                new AlertDialog.Builder(MainActivity.this)
                        .setMessage(message)
                        .setPositiveButton(getString(android.R.string.ok),
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        Intent intent = new Intent();
                                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                                        intent.setData(uri);
                                        startActivityForResult(intent, manualCode);
                                        dialog.cancel();
                                    }
                                }).show();
        alertDialog.setCanceledOnTouchOutside(true);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE) {
            getContacts();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_CONTACTS_REQUEST_CODE: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    getContacts();
                } else {
                    requestPermissionWithRationaleCheck(Manifest.permission.READ_CONTACTS, PERMISSION_CONTACTS_REQUEST_CODE, MANUALLY_CONTACTS_PERMISSION_REQUEST_CODE);
                    Toast.makeText(MainActivity.this, "Permission denied to read your External storage", Toast.LENGTH_SHORT).show();
                }
                return;
            }
        }
    }
}