package com.odys.smartoffice;

import android.app.Activity;
import android.content.Context;
import android.support.design.widget.Snackbar;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ServerDataDownloader {

    private final String TAG = "DataThread";

    private static InetAddress serverAddr;
    private static PrintWriter output;
    private static BufferedReader input;
    private static final String SERVER_IP = "192.168.43.238";
    private static final int SERVER_PORT = 7131;
    private static final int TIMEOUT = 5000;
    private static Socket socket;

    private Context mContext;
    private LinearLayout view;
    private Activity activity;

    public ServerDataDownloader(Context context, LinearLayout view, Activity activity) {
        this.mContext = context;
        this.view = view;
        this.activity = activity;
    }

    public void getData() {
        final String[] data = {""};
        final String[] message = {""};
        new Thread(() -> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String login = AppConstants.Companion.getLogin();
                String testMessage = login+"$$$105$$$giveMeData\r\n\r\n";
                Log.e(TAG, "Send: " + testMessage);
                output.println(testMessage);
                output.flush();

                message[0] = input.readLine();
                if(message[0] != null){
                    Log.e(TAG, "Received: " + message[0]);
                    message[0] = message[0].substring(message[0].indexOf("$$$DATA$$$")+12);
                    message[0] = message[0].substring(0, message[0].length()-1);
                    Log.e(TAG, "Substring: " + message[0]);
                    message[0] = decryptMessage(message[0]);
                    Log.e(TAG, "Decrypted: " + message[0]);
                    if(message[0].equals("NO_ACCESS")) {
                        Snackbar.make(view, "Brak dostępu do danych. Skontaktuj się z Administratorem.", Snackbar.LENGTH_LONG).show();
                    } else {
                        data[0] = message[0];
                    }
                    endConnection();
                }
                activity.runOnUiThread(() -> AppConstants.Companion.setDataUpdate(data[0]));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void openDoorStartWork() {
        final String[] message = {""};
        new Thread(() -> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String login = AppConstants.Companion.getLogin();
                String testMessage = login+"$$$103$$$openDoorStartWork\r\n\r\n";
                Log.e(TAG, "Send: " + testMessage);
                output.println(testMessage);
                output.flush();

                message[0] = input.readLine();
                if(message[0] != null){
                    Log.e(TAG, "Received: " + message[0]);
                    message[0] = message[0].substring(message[0].indexOf("$$$DATA$$$")+12);
                    message[0] = message[0].substring(0, message[0].length()-1);
                    Log.e(TAG, "Substring: " + message[0]);
                    message[0] = decryptMessage(message[0]);
                    Log.e(TAG, "Decrypted: " + message[0]);
                    switch (message[0]) {
                        case "FAILED":
                            Snackbar.make(view, "Nie można otworzyć drzwi. Drzwi zablokowane.", Snackbar.LENGTH_LONG).show();
                            break;
                        case "ENTER":
                            activity.runOnUiThread(() -> AppConstants.Companion.setWork(true));
                            Snackbar.make(view, "Drzwi otwarte. Praca rozpoczęta.", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(view, "Nie można otworzyć drzwi - brak uprawnień. Skontaktuj się z Administratorem.", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                    endConnection();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void openDoorEndWork() {
        final String[] message = {""};
        new Thread(() -> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String login = AppConstants.Companion.getLogin();
                String testMessage = login+"$$$104$$$openDoorEndWork\r\n\r\n";
                Log.e(TAG, "Send: " + testMessage);
                output.println(testMessage);
                output.flush();

                message[0] = input.readLine();
                if(message[0] != null){
                    Log.e(TAG, "Received: " + message[0]);
                    message[0] = message[0].substring(message[0].indexOf("$$$DATA$$$")+12);
                    message[0] = message[0].substring(0, message[0].length()-1);
                    Log.e(TAG, "Substring: " + message[0]);
                    message[0] = decryptMessage(message[0]);
                    Log.e(TAG, "Decrypted: " + message[0]);
                    switch (message[0]) {
                        case "FAILED":
                            Snackbar.make(view, "Nie można otworzyć drzwi. Drzwi zablokowane.", Snackbar.LENGTH_LONG).show();
                            break;
                        case "EXIT":
                            activity.runOnUiThread(() -> AppConstants.Companion.setWork(false));
                            Snackbar.make(view, "Drzwi otwarte. Praca zakończona.", Snackbar.LENGTH_LONG).show();
                            break;
                        default:
                            Snackbar.make(view, "Nie można otworzyć drzwi - brak uprawnień. Skontaktuj się z Administratorem.", Snackbar.LENGTH_LONG).show();
                            break;
                    }
                    endConnection();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String decryptMessage(String ans) {
        byte[] decryptedData = Base64.decode(ans, Base64.DEFAULT);
        String decryptedString = "";
        Cipher decrypt;
        try {
            decrypt = Cipher.getInstance("RSA");
            String privateAndroidKeyString = PreferencesManager.Companion.getAndroidPrivateKey(mContext);
            byte [] keyEncodedBytes = privateAndroidKeyString.getBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(keyEncodedBytes, Base64.DEFAULT));
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateAndroidKey = kf.generatePrivate(keySpec);
            decrypt.init(Cipher.DECRYPT_MODE, privateAndroidKey);
            decryptedString = new String(decrypt.doFinal(decryptedData), StandardCharsets.UTF_8);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return decryptedString;
    }

    private void endConnection() {
        if(!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
