package ru.elegion_homework.homeworksix;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {
    private static final int PERMISSION_REQUEST = 6;
    private ImageView mDownloadedImage;
    private EditText mUrlInput;
    private Button mDownloadBtn;
    private Button mShowBtn;
    private String mErrorText;
    private String mPrimaryPermissionExplanation;
    private String mPermissionExplanation;
    private String mPositiveButtonText;
    private String mNegativeButtonText;
    private String mLoadingText;
    private String mImageExtension = null;
    private String mFileToShow;
    private String mPermissionNotGranted;
    private CustomBroadCasReceiver mReceiver;
    private IntentFilter mFilter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViewElements();
        initStrings();

        mReceiver = new CustomBroadCasReceiver();
        mFilter = new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE);

        if(!isPermissionGranted()) {
            requestForPermission();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(mReceiver, mFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();

        mShowBtn.setEnabled(false);
        mDownloadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(isPermissionGranted()) {
                    String rawUrl = mUrlInput.getText().toString();
                    if(mShowBtn.isEnabled()) {
                        mShowBtn.setEnabled(false);
                    }
                    if(!ifImage(rawUrl)) {
                        Toast.makeText(MainActivity.this, mErrorText, Toast.LENGTH_LONG).show();
                    } else {
                        downloadImage(rawUrl);
                    }
                } else {
                    Toast.makeText(MainActivity.this, mPermissionNotGranted, Toast.LENGTH_SHORT).show();
                }
            }
        });
        mShowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File image = new  File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" + mFileToShow);

                if(image.exists()){
                    Bitmap resultBitmap = BitmapFactory.decodeFile(image.getAbsolutePath());
                    mDownloadedImage.setImageBitmap(resultBitmap);
                }
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode != PERMISSION_REQUEST) {
            return;
        }
        if(permissions.length != 1) {
            return;
        }

        if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            new AlertDialog.Builder(this)
                    .setMessage(mPermissionExplanation)
                    .setPositiveButton(mPositiveButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            startActivity(new Intent(Settings.ACTION_APPLICATION_SETTINGS));
                        }
                    })
                    .setNegativeButton(mNegativeButtonText, null)
                    .show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(mReceiver);
    }

    private boolean ifImage(String rawUrl) {
        String[] imagesExtension = new String[] {".jpeg", ".bmp", ".png", ".jpg"};

        if(TextUtils.isEmpty(rawUrl)) {
            mErrorText = getString(R.string.empty_url_error);
            return false;
        }

        if(!URLUtil.isValidUrl(rawUrl)) {
            mErrorText = getString(R.string.bad_url_error);
            return false;
        }

        String extension = rawUrl.substring(rawUrl.length() - 4, rawUrl.length());
        if(extension.equals("jpeg")) {
            extension = "." + extension;
        }

        for(int step = 0; step < imagesExtension.length; step++) {
            if(extension.equals(imagesExtension[step])) {
                mImageExtension = imagesExtension[step];
                return true;
            }
        }

        mErrorText = getString(R.string.bad_format_error);

        return false;
    }

    private void initViewElements() {
        mDownloadedImage = findViewById(R.id.downloaded_image);
        mUrlInput = findViewById(R.id.url_input);
        mDownloadBtn = findViewById(R.id.download_btn);
        mShowBtn = findViewById(R.id.show_btn);
    }

    private void initStrings() {
        mPrimaryPermissionExplanation = getString(R.string.primary_permission_explanation);
        mPermissionExplanation = getString(R.string.permission_explanation);
        mPositiveButtonText = getString(R.string.positive_button_text);
        mNegativeButtonText = getString(R.string.negative_button_text);
        mLoadingText = getString(R.string.downloading_text);
        mPermissionNotGranted = getString(R.string.permission_not_granted_text);
    }

    private void downloadImage(String rawUri) {
        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(rawUri);
        DownloadManager.Request newRequest = new DownloadManager.Request(uri);
        newRequest.setAllowedNetworkTypes(DownloadManager.Request.NETWORK_WIFI | DownloadManager.Request.NETWORK_MOBILE);
        newRequest.setAllowedOverRoaming(false);
        newRequest.setTitle(mLoadingText);
        newRequest.setDescription(mLoadingText);
        newRequest.setVisibleInDownloadsUi(true);
        newRequest.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, getFileName());

        downloadManager.enqueue(newRequest);
    }

    private String getFileName() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_hhmmss", Locale.GERMANY);
        mFileToShow = dateFormat.format(System.currentTimeMillis()) + mImageExtension;
        return mFileToShow;
    }

    private boolean isPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestForPermission() {
        if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(this)
                    .setMessage(mPrimaryPermissionExplanation)
                    .setPositiveButton(mPositiveButtonText, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(MainActivity.this, 
                                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 
                                    PERMISSION_REQUEST);
                        }
                    })
                    .show();
        } else {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[] {Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    PERMISSION_REQUEST);
        }
    }

    private class CustomBroadCasReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mShowBtn.setEnabled(true);
        }
    }
}
