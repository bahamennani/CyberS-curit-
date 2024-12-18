package com.example.convertpng;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PublicKey;
import java.security.PrivateKey;
import javax.crypto.Cipher;

public class MainActivity extends AppCompatActivity {
    private static final int PICK_IMAGE = 1;
    private ImageView imagePreview;
    private Bitmap selectedBitmap;
    private KeyPair keyPair;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }

        imagePreview = findViewById(R.id.imagePreview);
        Toast.makeText(this, "Welcome to Convert Png", Toast.LENGTH_LONG).show();

        findViewById(R.id.chooseImageButton).setOnClickListener(v -> chooseImage());
        findViewById(R.id.convertImageButton).setOnClickListener(v -> convertToPng());
        findViewById(R.id.saveImageButton).setOnClickListener(v -> saveImage());

        try {
            keyPair = generateKeyPair();  // Generate the key pair for encryption/decryption
            byte[] encryptedData = encrypt("Sample data to encrypt".getBytes(), keyPair.getPublic()); // Encrypt sample data
            byte[] decryptedData = decrypt(encryptedData, keyPair.getPrivate()); // Decrypt the data
            Toast.makeText(this, new String(decryptedData), Toast.LENGTH_LONG).show(); // Show decrypted data
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, "AndroidKeyStore");
        keyPairGenerator.initialize(new KeyGenParameterSpec.Builder(
                "myKeyAlias",
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)
                .setKeySize(2048)
                .build());
        return keyPairGenerator.generateKeyPair();
    }

    private byte[] encrypt(byte[] data, PublicKey publicKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);
        return cipher.doFinal(data);
    }

    private byte[] decrypt(byte[] data, PrivateKey privateKey) throws Exception {
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, privateKey);
        return cipher.doFinal(data);
    }

    private void chooseImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri imageUri = data.getData();
            try {
                selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                imagePreview.setImageBitmap(selectedBitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void convertToPng() {
        if (selectedBitmap != null) {
            imagePreview.setImageBitmap(selectedBitmap);
            Toast.makeText(this, "Image converted to PNG!", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Please select an image first.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImage() {
        if (selectedBitmap != null) {
            File imagesDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images");
            if (!imagesDirectory.exists()) {
                if (!imagesDirectory.mkdir()) {
                    Toast.makeText(this, "Failed to create the Images directory!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            File convertDirectory = new File(imagesDirectory, "Convert");
            if (!convertDirectory.exists()) {
                if (!convertDirectory.mkdir()) {
                    Toast.makeText(this, "Failed to create the Convert directory!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            File file = new File(convertDirectory, System.currentTimeMillis() + ".png");
            try (FileOutputStream out = new FileOutputStream(file)) {
                selectedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                MediaScannerConnection.scanFile(this, new String[]{file.getAbsolutePath()}, null, (path, uri) -> runOnUiThread(() -> Toast.makeText(this, "Image saved and added to gallery: " + path, Toast.LENGTH_LONG).show()));
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error saving the image.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Please select and convert an image first.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.open_folder) {
            File directory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Images/Convert");
            if (!directory.exists()) {
                Toast.makeText(this, "No converted images yet!", Toast.LENGTH_SHORT).show();
                return true;
            }

            Intent intent = new Intent(Intent.ACTION_VIEW);
            Uri folderUri = Uri.fromFile(directory);
            intent.setDataAndType(folderUri, "*/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
