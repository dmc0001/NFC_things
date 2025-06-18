package com.example.nfcthings;


import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Environment;
import android.util.Log;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

public class TransferUtils {
    private static final String TAG = "TransferUtils";

    public static class NFCHelper {
        /**
         * Check if NFC is available and enabled on the device
         */
        public static boolean isNFCAvailable(Context context) {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
            return nfcAdapter != null && nfcAdapter.isEnabled();
        }

        /**
         * Check if NFC is supported on the device
         */
        public static boolean isNFCSupported(Context context) {
            NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);
            return nfcAdapter != null;
        }

        /**
         * Create a message for NFC transfer containing image metadata
         */
        public static String createImageTransferMessage(String fileName, String imageUri) {
            return "IMAGE_TRANSFER:" + fileName + ":" + imageUri;
        }

        /**
         * Parse received NFC message to extract image metadata
         */
        public static ImageTransferData parseImageTransferMessage(String message) {
            if (message.startsWith("IMAGE_TRANSFER:")) {
                String[] parts = message.split(":", 3);
                if (parts.length >= 3) {
                    return new ImageTransferData(parts[1], parts[2]);
                }
            }
            return null;
        }
    }

    public static class BluetoothHelper {
        /**
         * Check if Bluetooth is available and enabled
         */
        public static boolean isBluetoothAvailable() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null && adapter.isEnabled();
        }

        /**
         * Check if Bluetooth is supported
         */
        public static boolean isBluetoothSupported() {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            return adapter != null;
        }

        /**
         * Get paired Bluetooth devices
         */
//        public static Set<BluetoothDevice> getPairedDevices() {
//            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
//            if (adapter != null) {
//                return adapter.getBondedDevices();
//            }
//            return null;
//        }
        public static Set<BluetoothDevice> getPairedDevices(Context context) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

            if (adapter != null) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    if (context.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        Log.w(TAG, "Missing BLUETOOTH_CONNECT permission");
                        return null; // Or request permission via activity
                    }
                }
                return adapter.getBondedDevices();
            }
            return null;
        }

        /**
         * Make device discoverable for incoming connections
         */
        public static Intent makeDiscoverable(int duration) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, duration);
            return discoverableIntent;
        }
    }

    public static class FileHelper {
        /**
         * Create a unique filename for image transfer
         */
        public static String createUniqueImageFileName() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
            return "IMG_" + sdf.format(new Date()) + ".jpg";
        }

        /**
         * Create directory for storing transferred images
         */
        public static File createTransferDirectory(Context context) {
            File transferDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "NFCTransfer");
            if (!transferDir.exists()) {
                transferDir.mkdirs();
            }
            return transferDir;
        }

        /**
         * Create directory for sharing images
         */
        public static File createShareDirectory(Context context) {
            File shareDir = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Shared");
            if (!shareDir.exists()) {
                shareDir.mkdirs();
            }
            return shareDir;
        }

        /**
         * Copy file from source URI to destination file
         */
        public static boolean copyFile(Context context, Uri sourceUri, File destFile) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(sourceUri);
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                if (inputStream == null) {
                    return false;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error copying file", e);
                return false;
            }
        }

        /**
         * Copy file from source file to destination file
         */
        public static boolean copyFile(File sourceFile, File destFile) {
            try (FileInputStream inputStream = new FileInputStream(sourceFile);
                 FileOutputStream outputStream = new FileOutputStream(destFile)) {

                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                return true;
            } catch (IOException e) {
                Log.e(TAG, "Error copying file", e);
                return false;
            }
        }

        /**
         * Get file size in bytes
         */
        public static long getFileSize(Context context, Uri uri) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                if (inputStream != null) {
                    long size = 0;
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        size += bytesRead;
                    }
                    return size;
                }
            } catch (IOException e) {
                Log.e(TAG, "Error getting file size", e);
            }
            return 0;
        }

        /**
         * Get formatted file size string
         */
        public static String getFormattedFileSize(long bytes) {
            if (bytes < 1024) {
                return bytes + " B";
            } else if (bytes < 1024 * 1024) {
                return String.format(Locale.getDefault(), "%.1f KB", bytes / 1024.0);
            } else {
                return String.format(Locale.getDefault(), "%.1f MB", bytes / (1024.0 * 1024.0));
            }
        }

        /**
         * Create a FileProvider URI for sharing
         */
        public static Uri getFileProviderUri(Context context, File file) {
            return FileProvider.getUriForFile(context,
                    context.getPackageName() + ".fileprovider", file);
        }

        /**
         * Clean up old transferred files (older than 7 days)
         */
        public static void cleanupOldFiles(Context context) {
            File transferDir = createTransferDirectory(context);
            File shareDir = createShareDirectory(context);

            long cutoffTime = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000); // 7 days

            cleanupDirectory(transferDir, cutoffTime);
            cleanupDirectory(shareDir, cutoffTime);
        }

        private static void cleanupDirectory(File directory, long cutoffTime) {
            if (directory.exists() && directory.isDirectory()) {
                File[] files = directory.listFiles();
                if (files != null) {
                    for (File file : files) {
                        if (file.lastModified() < cutoffTime) {
                            boolean deleted = file.delete();
                            Log.d(TAG, "Cleaned up old file: " + file.getName() + " (deleted: " + deleted + ")");
                        }
                    }
                }
            }
        }
    }

    public static class ValidationHelper {
        /**
         * Check if the image file is valid
         */
        public static boolean isValidImageFile(File file) {
            if (!file.exists() || !file.isFile()) {
                return false;
            }

            String fileName = file.getName().toLowerCase();
            return fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                    fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                    fileName.endsWith(".bmp") || fileName.endsWith(".webp");
        }

        /**
         * Check if the image URI is valid
         */
        public static boolean isValidImageUri(Context context, Uri uri) {
            try (InputStream inputStream = context.getContentResolver().openInputStream(uri)) {
                return inputStream != null;
            } catch (Exception e) {
                return false;
            }
        }

        /**
         * Check if file size is within reasonable limits (max 10MB)
         */
        public static boolean isFileSizeValid(long sizeInBytes) {
            return sizeInBytes > 0 && sizeInBytes <= 10 * 1024 * 1024; // 10MB limit
        }
    }

    /**
     * Data class for image transfer information
     */
    public static class ImageTransferData {
        public final String fileName;
        public final String imageUri;

        public ImageTransferData(String fileName, String imageUri) {
            this.fileName = fileName;
            this.imageUri = imageUri;
        }
    }

    /**
     * Interface for transfer status callbacks
     */
    public interface TransferStatusListener {
        void onTransferStarted();
        void onTransferProgress(int progress);
        void onTransferCompleted(boolean success, String message);
        void onTransferError(String error);
    }
}