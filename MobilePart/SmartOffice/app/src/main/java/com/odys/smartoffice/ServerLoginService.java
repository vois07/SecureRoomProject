package com.odys.smartoffice;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPrivateKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class ServerLoginService extends Service {
    
    private final String TAG = "LoginThread";

    private static InetAddress serverAddr;
    private static PrintWriter output;
    private static BufferedReader input;
    private static final String SERVER_IP = "192.168.43.238";
    private static final int SERVER_PORT = 7131;
    private static final int TIMEOUT = 5000;
    private static Socket socket;

    private static volatile boolean threadEnd = false;

    public static int status = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        boolean firstLaunch = PreferencesManager.Companion.isFirstLaunched(this);

        testConnection();
        while(!threadEnd){}
        endConnection();
        threadEnd = false;

        if(firstLaunch) {
            downloadPublicKey();
            while (!threadEnd) {}
            endConnection();
            threadEnd = false;

            sendAndroidPublicKey();
            while(!threadEnd){}
            endConnection();
            threadEnd = false;
        }

        testEncryption();
        while(!threadEnd){}
        endConnection();
        threadEnd = false;

        status = -1;

        return START_STICKY;
    }

    private void testEncryption() {
        new Thread(()-> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String login = AppConstants.Companion.getLogin();

                String secret = "MOBILE_OK";
                String publicKey = PreferencesManager.Companion.getServerPublicKeySaved(this);
                byte[] publicKeyByte = publicKey.getBytes();
                X509EncodedKeySpec spec = new X509EncodedKeySpec(Base64.decode(publicKeyByte, Base64.DEFAULT));
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                PublicKey publicServerKey = keyFactory.generatePublic(spec);
                byte[] msgByte = encrypt(publicServerKey, secret);
                String msgSecret = Base64.encodeToString(msgByte, Base64.DEFAULT);

                String message = login+"$$$202$$$" + msgSecret +"\r\n\r\n";
                Log.e(TAG, "Send: " + message);
                output.println(message);
                output.flush();

                message = input.readLine();
                if(message != null) {
                    Log.e(TAG, "Received: " + message);
                    String m = message.substring(message.indexOf("$$$DATA$$$")+12);
                    m = m.substring(0, m.length()-1);
                    Log.e(TAG, "got encrypted: " + m);
                    decryptMessage(m);
                }
                threadEnd = true;
            } catch (IOException | NoSuchAlgorithmException | InvalidKeySpecException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void downloadPublicKey() {
        new Thread(()-> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String login = AppConstants.Companion.getLogin();
                String testMessage = login+"$$$101$$$giveMePubKey\r\n\r\n";
                Log.e(TAG, "Send: " + testMessage);
                output.println(testMessage);
                output.flush();

                String message = input.readLine();
                if(message != null){
                    Log.e(TAG, "Received: " + message);
                    String serverPubKey = message.substring(message.indexOf("$$$DATA$$$")+10);
                    Log.e(TAG, "serv pub key: "+serverPubKey);
                    saveServerKey(serverPubKey);
                }
                threadEnd = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void sendAndroidPublicKey() {
        new Thread(()-> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());

                KeyPairGenerator kpg;
                try {
                    String publicAndroidKeyString;
                    if(!PreferencesManager.Companion.areAndroidKeysSaved(this)) {
                        kpg = KeyPairGenerator.getInstance("RSA");
                        kpg.initialize(4096);
                        KeyPair kp = kpg.generateKeyPair();
                        PrivateKey privateAndroidKey = kp.getPrivate();
                        PublicKey publicAndroidKey = kp.getPublic();
                        publicAndroidKeyString = Base64.encodeToString(publicAndroidKey.getEncoded(), Base64.DEFAULT);
                        String privateAndroidKeyString = Base64.encodeToString(privateAndroidKey.getEncoded(), Base64.DEFAULT);
                        PreferencesManager.Companion.saveAndroidKeys(this, publicAndroidKeyString, privateAndroidKeyString);
                    } else {
                        publicAndroidKeyString = PreferencesManager.Companion.getAndroidPublicKey(this);
                    }
                    String login = AppConstants.Companion.getLogin();
                    String message = login+"$$$102$$$"+ publicAndroidKeyString + "\r\n\r\n";
                    Log.e(TAG, "Send: " + message);
                    output.println(message);
                    output.flush();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                }

                String message = input.readLine();
                if(message != null){
                    Log.e(TAG, "Received: " + message);
                    Log.e(TAG, "server got ur key? "+message.substring(message.length()-2, message.length()));
                }
                threadEnd = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();
    }

    private void testConnection() {
        new Thread(()-> {
            try {
                //init connection
                serverAddr = InetAddress.getByName(SERVER_IP);
                socket = new Socket();
                socket.connect(new InetSocketAddress(serverAddr, SERVER_PORT), TIMEOUT);
                input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                Log.e(TAG, "Connected");

                output = new PrintWriter(socket.getOutputStream());
                String testMessage = "TEST$$$201$$$test\r\n\r\n";
                Log.e(TAG, "Send: " + testMessage);
                output.println(testMessage);
                output.flush();

                String message = input.readLine();
                if(message != null) {
                    Log.e(TAG, "Received: " + message);
//                    String serverPubKey = message.substring(message.indexOf("$$$DATA$$$") + 10);
                }
                threadEnd = true;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void decryptMessage(String ans) {
        byte[] decryptedData = Base64.decode(ans, Base64.DEFAULT);
        Cipher decrypt;
        try {
            decrypt = Cipher.getInstance("RSA");
            String privateAndroidKeyString = PreferencesManager.Companion.getAndroidPrivateKey(this);
            byte [] keyEncodedBytes = privateAndroidKeyString.getBytes();
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(Base64.decode(keyEncodedBytes, Base64.DEFAULT));

            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey privateAndroidKey = kf.generatePrivate(keySpec);
            decrypt.init(Cipher.DECRYPT_MODE, privateAndroidKey);
            String decryptedString = new String(decrypt.doFinal(decryptedData), StandardCharsets.UTF_8);
            Log.e(TAG, "decrypted: " + decryptedString);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeySpecException | InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    private void saveServerKey(String serverPubKey) {
        PreferencesManager.Companion.saveServerPublicKey(ServerLoginService.this, serverPubKey);
    }

    private static byte[] encrypt(Key pubkey, String text) {
        Cipher rsa;
        try {
            rsa = Cipher.getInstance("RSA");
            rsa.init(Cipher.ENCRYPT_MODE, pubkey);
            return rsa.doFinal(text.getBytes());
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | BadPaddingException | IllegalBlockSizeException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void endConnection() {
        if(!socket.isClosed()) {
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroy() {
        endConnection();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
