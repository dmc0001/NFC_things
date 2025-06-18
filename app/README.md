# NFC Image Transfer App for Android

A modern Android application that enables peer-to-peer image sharing using NFC handshake and Bluetooth file transfer. This app serves as an alternative to the deprecated Android Beam functionality.

## 🚀 Features

- **NFC Handshake**: Uses NFC to detect proximity and initiate transfer
- **Bluetooth Transfer**: Actual image transfer happens via Bluetooth for faster speeds
- **Image Selection**: Pick images from gallery or capture new photos
- **Secure File Sharing**: Uses FileProvider for secure file operations
- **Offline Operation**: Works completely offline without internet connection
- **Modern UI**: Clean, intuitive interface with Material Design principles

## 🛠️ Tech Stack

- **Language**: Java
- **Platform**: Android (API 21+)
- **Core Technologies**:
    - NFC API (`NfcAdapter`, `NdefMessage`, `NdefRecord`)
    - Bluetooth API (`BluetoothAdapter`)
    - FileProvider for secure file sharing
    - Material Design Components

## 📁 Project Structure

```
app/
├── src/main/java/com/example/nfcimagetransfer/
│   ├── MainActivity.java          # Main activity with NFC/Bluetooth logic
│   └── TransferUtils.java         # Utility classes for file operations
├── src/main/res/
│   ├── layout/
│   │   └── activity_main.xml      # Main UI layout
│   ├── drawable/                  # Button styles and icons
│   ├── values/
│   │   ├── strings.xml           # App strings
│   │   ├── colors.xml            # Color definitions
│   │   └── styles.xml            # UI styles
│   └── xml/
│       └── file_paths.xml        # FileProvider configuration
└── AndroidManifest.xml           # App manifest with permissions
```

## 🔧 Setup Instructions

### Prerequisites

1. **Android Studio** (Latest version recommended)
2. **Two physical Android devices** with:
    - NFC capability
    - Bluetooth capability
    - Android 5.0 (API 21) or higher
3. **Developer Options** enabled on both devices

### Installation Steps

1. **Clone/Download the project files**
2. **Open in Android Studio**
3. **Create the following directories in `res/` if they don't exist**:
    - `res/drawable/`
    - `res/xml/`
    - `res/values/`

4. **Add the provided files to their respective locations**:
   ```
   src/main/java/com/example/nfcimagetransfer/MainActivity.java
   src/main/java/com/example/nfcimagetransfer/TransferUtils.java
   src/main/res/layout/activity_main.xml
   src/main/res/drawable/*.xml (button styles and icons)
   src/main/res/values/strings.xml
   src/main/res/values/colors.xml
   src/main/res/values/styles.xml
   src/main/res/xml/file_paths.xml
   src/main/AndroidManifest.xml
   build.gradle (Module: app)
   ```

5. **Sync the project** (Gradle sync should happen automatically)
6. **Build the project** (`Build > Make Project`)

### Generating APK

1. **For Debug APK**:
   ```
   Build > Build Bundle(s) / APK(s) > Build APK(s)
   ```

2. **For Release APK**:
   ```
   Build > Generate Signed Bundle / APK > APK
   ```
   (You'll need to create a keystore first)

## 📱 Testing Instructions

### Required Hardware

- **Two NFC-enabled Android devices**
- Both devices must have **Bluetooth** enabled
- Both devices must have **NFC** enabled

### Step-by-Step Testing

#### Device Setup (Both Devices)

1. **Enable NFC**:
    - Go to `Settings > Connected devices > Connection preferences > NFC`
    - Turn ON NFC
    - Enable "Android Beam" if available

2. **Enable Bluetooth**:
    - Go to `Settings > Connected devices > Bluetooth`
    - Turn ON Bluetooth
    - Make sure device is discoverable

3. **Install the APK** on both devices
4. **Grant required permissions** when prompted:
    - Camera
    - Storage
    - Location (for Bluetooth)

#### Testing Procedure

**On Sender Device:**

1. **Launch the app**
2. **Select an image**:
    - Tap "Select Image" to choose from gallery, OR
    - Tap "Take Photo" to capture a new image
3. **Verify image preview** appears
4. **Tap "Send via NFC + Bluetooth"**
5. **Wait for "NFC ready!" message**

**On Receiver Device:**

1. **Launch the app**
2. **Keep the app open** and ready

**Transfer Process:**

1. **Bring devices together** (back-to-back, NFC antennas aligned)
2. **Wait for NFC detection** (usually 1-2 seconds)
3. **On sender device**: Look for NFC success message
4. **On receiver device**: You should see "Image transfer request received"
5. **Accept Bluetooth transfer**:
    - A Bluetooth file transfer dialog should appear
    - Tap "Accept" to receive the file
6. **Verify transfer completion**

### Troubleshooting

#### NFC Issues

- **"NFC not supported"**: Device doesn't have NFC hardware
- **"Please enable NFC"**: Go to Settings and enable NFC
- **NFC not detecting**:
    - Try different positions (center of devices)
    - Ensure no thick cases are blocking NFC
    - Check if NFC antennas are properly aligned

#### Bluetooth Issues

- **Bluetooth transfer not starting**:
    - Ensure Bluetooth is enabled on both devices
    - Make receiver device discoverable
    - Check if devices are paired (may help but not required)

- **Transfer fails**:
    - Check available storage space
    - Verify file permissions
    - Try making both devices discoverable

#### App Issues

- **Crashes on image selection**:
    - Check storage permissions
    - Verify external storage is available

- **Image not displaying**:
    - Check if image file is corrupted
    - Try with different image formats (JPG,