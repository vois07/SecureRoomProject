package com.odys.smartoffice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import butterknife.BindView;
import butterknife.ButterKnife;

public class RegisterActivity extends AppCompatActivity {

    @BindView(R.id.nameEditText)
    EditText nameEditText;
    @BindView(R.id.surnameEditText)
    EditText surnameEditText;
    @BindView(R.id.registerButton)
    Button registerButton;
    @BindView(R.id.registerLayout)
    LinearLayout registerLayout;

    private final int PERMISSION = 666;
    private static ProgressDialog progressDialog;

    @SuppressLint("HardwareIds")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        ButterKnife.bind(this);

        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Nawiązywanie połączenia..."); // Setting Message
        progressDialog.setTitle("Łączenie z serwerem"); // Setting Title
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER); // Progress Dialog Style Spinner
        progressDialog.setCancelable(false);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    PERMISSION);
        }

        if(!PreferencesManager.Companion.isFirstLaunched(this)) {
            String s = PreferencesManager.Companion.getUsername(this);
            s = s.substring(0, s.length()-4);
            String[] r = s.split("(?=\\p{Upper})");
            if(r.length > 2) {
                nameEditText.setText(r[1]);
                surnameEditText.setText(r[2]);
            }
            connection();
        }

        registerButton.setOnClickListener(view -> {
            if (!nameEditText.getText().toString().equals("") && !surnameEditText.getText().toString().equals("")) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.READ_PHONE_STATE},
                            PERMISSION);
                } else {
                    String login = getImei();
                    AppConstants.Companion.setLogin(login);
                    PreferencesManager.Companion.setUsername(this, login);
                    connection();
                }
            } else {
                Snackbar.make(registerLayout, "Musisz wypełnić oba pola!", Snackbar.LENGTH_SHORT).show();
            }
        });
    }

    private void connection() {
        progressDialog.show();

        new Thread(() -> {
            startService(new Intent(this, ServerLoginService.class));
            try {
                Thread.sleep(1000);
                while(ServerLoginService.status > 0){}
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            progressDialog.dismiss();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }).start();
    }

    @SuppressLint("HardwareIds")
    private String getImei() {
        TelephonyManager telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String imei = "000000";
        if (telephonyManager != null) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                imei = telephonyManager.getDeviceId();
            }
        }
        return nameEditText.getText().toString() + surnameEditText.getText().toString()
                + imei.substring(0,4);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSION: {
                if (grantResults.length <= 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Snackbar.make(registerLayout, "Brak niezbędnych uprawnień!", Snackbar.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }
}
