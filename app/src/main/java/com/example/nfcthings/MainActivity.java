package com.example.nfcthings;


import android.Manifest;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcEvent;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements NfcAdapter.CreateNdefMessageCallback {

    private static final String TAG = "NFCImageTransfer";
    private static final int REQUEST_IMAGE_PICK = 1001;
    private static final int REQUEST_CAMERA = 1002;
    private static final int REQUEST_PERMISSIONS = 1003;
    private static final int REQUEST_ENABLE_BT = 1004;

    // UI Components
    private Button btnSelectImage, btnTakePhoto, btnSendImage;
    private ImageView imagePreview;
    private TextView statusText;

    // NFC Components
    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFilters;

    // Bluetooth Components
    private BluetoothAdapter bluetoothAdapter;

    // Current image data
    private Uri selectedImageUri;
    private String imageFileName;
    private File currentImageFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeNFC();
        initializeBluetooth();
        checkPermissions();

        // Handle incoming NFC intent
        handleNfcIntent(getIntent());
    }

    private void initializeViews() {
        btnSelectImage = findViewById(R.id.btnSelectImage);
        btnTakePhoto = findViewById(R.id.btnTakePhoto);
        btnSendImage = findViewById(R.id.btnSendImage);
        imagePreview = findViewById(R.id.imagePreview);
        statusText = findViewById(R.id.statusText);

        btnSelectImage.setOnClickListener(v -> selectImageFromGallery());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnSendImage.setOnClickListener(v -> enableNfcSending());

        btnSendImage.setEnabled(false);
        statusText.setText("Select an image to start sharing");
    }

    private void initializeNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            statusText.setText("NFC not supported on this device");
            Toast.makeText(this, "NFC not supported", Toast.LENGTH_LONG).show();
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            statusText.setText("Please enable NFC in settings");
            Toast.makeText(this, "Please enable NFC", Toast.LENGTH_LONG).show();
        }

        // Create pending intent for NFC
        pendingIntent = PendingIntent.getActivity(
                this, 0,
                new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
                PendingIntent.FLAG_MUTABLE
        );

        // Setup intent filters for NFC
        IntentFilter ndefFilter = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndefFilter.addDataType("text/plain");
        } catch (IntentFilter.MalformedMimeTypeException e) {
            Log.e(TAG, "Malformed MIME type", e);
        }

        intentFilters = new IntentFilter[]{ndefFilter};
    }

//    private void initializeBluetooth() {
//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//
//        if (bluetoothAdapter == null) {
//            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
//            return;
//        }
//
//        if (!bluetoothAdapter.isEnabled()) {
//            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
//        }
//    }

    private void initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_LONG).show();
            return;
        }

        // Request BLUETOOTH_CONNECT on Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        this,
                        new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                        REQUEST_PERMISSIONS
                );
                return;
            }
        }

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }


    private void checkPermissions() {
        String[] permissions = {
                Manifest.permission.NFC,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        boolean allGranted = true;
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(this, permissions, REQUEST_PERMISSIONS);
        }
    }

    private void selectImageFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void takePhoto() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {

            // Create file to save the image
            File photoFile = createImageFile();
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", photoFile);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                currentImageFile = photoFile;
                startActivityForResult(intent, REQUEST_CAMERA);
            }
        }
    }

    private File createImageFile() {
        String fileName = "IMG_" + System.currentTimeMillis() + ".jpg";
        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NFCTransfer");

        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }

        return new File(storageDir, fileName);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_IMAGE_PICK:
                    if (data != null) {
                        selectedImageUri = data.getData();
                        displaySelectedImage();
                    }
                    break;

                case REQUEST_CAMERA:
                    if (currentImageFile != null) {
                        selectedImageUri = FileProvider.getUriForFile(this,
                                getPackageName() + ".fileprovider", currentImageFile);
                        displaySelectedImage();
                    }
                    break;

                case REQUEST_ENABLE_BT:
                    Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    }

    private void displaySelectedImage() {
        if (selectedImageUri != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imagePreview.setImageBitmap(bitmap);

                // Extract filename
                imageFileName = "shared_image_" + System.currentTimeMillis() + ".jpg";

                btnSendImage.setEnabled(true);
                statusText.setText("Image ready to share. Tap 'Send via NFC' and bring devices together.");

                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error loading image", e);
                Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        }
    }

//    private void enableNfcSending() {
//        if (nfcAdapter != null && selectedImageUri != null) {
//            nfcAdapter.setNdefPushMessageCallback(this, this);
//            statusText.setText("NFC ready! Bring devices together to share image.");
//            Toast.makeText(this, "Bring devices together to share", Toast.LENGTH_SHORT).show();
//        }
//    }
//private void enableNfcSending() {
//    if (nfcAdapter != null && selectedImageUri != null) {
//        NdefMessage message = createNdefMessage(null); // Build your message
//        if (message != null) {
//
//            nfcAdapter.setNdefPushMessage(message, this);
//            Toast.makeText(this, "Bring devices together to share", Toast.LENGTH_SHORT).show();
//            statusText.setText("NFC ready! Bring devices together to share image.");
//        }
//    }
//}
private void enableNfcSending() {
    if (nfcAdapter != null && selectedImageUri != null) {
        Toast.makeText(this, "Bring devices together to share", Toast.LENGTH_SHORT).show();
        statusText.setText("NFC ready! Bring devices together to share image.");
        // No need to call setNdefPushMessage — just rely on foreground dispatch
    }
}


//    @Override
//    public NdefMessage createNdefMessage(NfcEvent event) {
//        if (selectedImageUri == null || imageFileName == null) {
//            return null;
//        }
//
//        // Create message with image metadata
//        String message = "IMAGE_TRANSFER:" + imageFileName + ":" + selectedImageUri.toString();
//        NdefRecord record = NdefRecord.createTextRecord("en", message);
//
//        return new NdefMessage(new NdefRecord[]{record});
//    }

    @Override
    public NdefMessage createNdefMessage(NfcEvent event) {
        if (selectedImageUri == null || imageFileName == null) {
            return null;
        }

        // Create properly formatted NDEF message
        String payload = "IMAGE_TRANSFER:" + imageFileName + ":" + selectedImageUri.toString();
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        NdefRecord record = new NdefRecord(
                NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT,
                new byte[0],
                payloadBytes
        );

        return new NdefMessage(new NdefRecord[]{record});
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null) {
            nfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleNfcIntent(intent);
    }

    private void handleNfcIntent(Intent intent) {
        String action = intent.getAction();

        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action) ||
                NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)) {

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if (rawMessages != null && rawMessages.length > 0) {
                NdefMessage message = (NdefMessage) rawMessages[0];
                String receivedData = new String(message.getRecords()[0].getPayload(), StandardCharsets.UTF_8);

                // Skip language code (first 3 bytes for text record)
                if (receivedData.length() > 3) {
                    receivedData = receivedData.substring(3);
                }

                Log.d(TAG, "Received NFC data: " + receivedData);

                if (receivedData.startsWith("IMAGE_TRANSFER:")) {
                    handleImageTransferRequest(receivedData);
                }
            }
        }
    }

    private void handleImageTransferRequest(String data) {
        String[] parts = data.split(":");
        if (parts.length >= 3) {
            String fileName = parts[1];
            String imageUriString = data.substring(data.indexOf(":", data.indexOf(":") + 1) + 1);

            statusText.setText("Image transfer request received: " + fileName);
            Toast.makeText(this, "Receiving image via Bluetooth...", Toast.LENGTH_SHORT).show();

            // Initiate Bluetooth transfer
            initiateBluetoothReceive(fileName);
        }
    }

    private void initiateBluetoothTransfer() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "No image selected", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Copy image to a shareable location
            File shareFile = copyImageToShareableLocation();

            if (shareFile != null) {
                Uri shareUri = FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", shareFile);

                Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("image/*");
                shareIntent.putExtra(Intent.EXTRA_STREAM, shareUri);
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                // Try to send via Bluetooth
                shareIntent.setPackage("com.android.bluetooth");

                if (shareIntent.resolveActivity(getPackageManager()) != null) {
                    startActivity(shareIntent);
                    statusText.setText("Sharing image via Bluetooth...");
                } else {
                    // Fallback to general sharing
                    startActivity(Intent.createChooser(shareIntent, "Share Image"));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initiating Bluetooth transfer", e);
            Toast.makeText(this, "Error sharing image", Toast.LENGTH_SHORT).show();
        }
    }

//    private void initiateBluetoothReceive(String fileName) {
//        // For receiving, we'll show a notification that an image is being received
//        statusText.setText("Ready to receive: " + fileName + "\nAccept the Bluetooth file transfer.");
//
//        // Make Bluetooth discoverable for incoming transfers
//        if (bluetoothAdapter != null) {
//            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
//            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
//            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) != PackageManager.PERMISSION_GRANTED) {
//                // TODO: Consider calling
//                //    ActivityCompat#requestPermissions
//                // here to request the missing permissions, and then overriding
//                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
//                //                                          int[] grantResults)
//                // to handle the case where the user grants the permission. See the documentation
//                // for ActivityCompat#requestPermissions for more details.
//                return;
//            }
//            startActivity(discoverableIntent);
//        }
//    }

    private void initiateBluetoothReceive(String fileName) {
        statusText.setText("Ready to receive: " + fileName + "\nAccept the Bluetooth file transfer.");

        if (bluetoothAdapter != null) {
            // On Android 12+ (API 31+), need runtime permission for BLUETOOTH_ADVERTISE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE)
                        != PackageManager.PERMISSION_GRANTED) {

                    ActivityCompat.requestPermissions(
                            this,
                            new String[]{Manifest.permission.BLUETOOTH_ADVERTISE},
                            REQUEST_PERMISSIONS
                    );
                    return;
                }
            }

            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private File copyImageToShareableLocation() {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);

            File shareDir = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Shared");
            if (!shareDir.exists()) {
                shareDir.mkdirs();
            }

            File shareFile = new File(shareDir, imageFileName);
            FileOutputStream outputStream = new FileOutputStream(shareFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            inputStream.close();
            outputStream.close();

            return shareFile;

        } catch (IOException e) {
            Log.e(TAG, "Error copying image", e);
            return null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_PERMISSIONS) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }

            if (!allGranted) {
                Toast.makeText(this, "Permissions required for full functionality", Toast.LENGTH_LONG).show();
            }
        }
    }
}