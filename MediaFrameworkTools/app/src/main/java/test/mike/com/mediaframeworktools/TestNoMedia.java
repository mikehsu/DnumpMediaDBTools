package test.mike.com.mediaframeworktools;

import android.app.Activity;
import android.content.ContentValues;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import java.io.File;

public class TestNoMedia extends Activity {

    private final String TAG = "TestNoMedia_MediaProvider";
    private File mNoMedia, mTestNoMediaFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test_nomedia);

        mNoMedia = new File(Environment.getExternalStorageDirectory(), ".nomedia");
        mTestNoMediaFile = new File(Environment.getExternalStorageDirectory(), "/test/.nomedia");
        Log.d(TAG, "mNoMedia: " + mNoMedia.getPath());
        Log.d(TAG, "mTestNoMediaFile: " + mTestNoMediaFile.getPath());

        Button btnInsertByProvider = (Button) findViewById(R.id.btn_insert_direct);
        btnInsertByProvider.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertByProvider();
            }
        });

        Button btnInsertByConnection = (Button) findViewById(R.id.btn_insert_connect);
        btnInsertByConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertByConnection();
            }
        });

        Button btnUpdate = (Button) findViewById(R.id.btn_update);
        btnUpdate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateNoMedia();
            }
        });

        Button btnDelete = (Button) findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                deleteNoMedia();
            }
        });

        Button btnInsertTest = (Button) findViewById(R.id.btn_insert_test);
        btnInsertTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                insertTest();
            }
        });
    }

    private void insertTest() {
        Log.d(TAG, "insertTest");
        if(!mTestNoMediaFile.exists()) {
            mTestNoMediaFile.mkdirs();
        }

        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DATA, mTestNoMediaFile.getPath());
        getContentResolver().insert(MediaStore.Files.getContentUri("external"), cv);
    }

    private void insertByProvider() {
        Log.d(TAG, "insertByProvider");
        if(mNoMedia.exists()) {
            deleteNoMedia();
        }
        mNoMedia.mkdirs();
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DATA, mNoMedia.getPath());
        getContentResolver().insert(MediaStore.Files.getContentUri("external"), cv);
    }

    private void insertByConnection() {
        Log.d(TAG, "insertByConnection");
        if(mNoMedia.exists()) {
            deleteNoMedia();
        }
        mNoMedia.mkdirs();

        MediaScannerConnection.scanFile(this, new String[]{mNoMedia.getPath()}, null, new MediaScannerConnection.OnScanCompletedListener() {
            @Override
            public void onScanCompleted(String s, Uri uri) {
                Log.d(TAG, "scanFile: " + s + ", uri: " + uri);
            }
        });
    }

    private void updateNoMedia() {
        if(mNoMedia.exists()) {
            deleteNoMedia();
        }
        File test = new File(Environment.getExternalStorageDirectory(), "test");
        if(!test.exists()) {
            test.mkdirs();
        }
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.MediaColumns.DATA, test.getPath());
        Uri uri = getContentResolver().insert(MediaStore.Files.getContentUri("external"), cv);
        String testPath = test.getPath();
        Log.d(TAG, "insert test file: " + uri + ", " + testPath);

        test.renameTo(mNoMedia);
        cv.clear();
        cv.put(MediaStore.MediaColumns.DATA, mNoMedia.getPath());
        int count = getContentResolver().update(MediaStore.Files.getContentUri("external"), cv,
                MediaStore.MediaColumns.DATA + " = ?",
                new String[]{testPath});
        Log.d(TAG, "updateNoMedia: " + count);
    }

    private void deleteNoMedia() {
        if(mNoMedia.exists()) {
            mNoMedia.delete();
        }
        int count = getContentResolver().delete(MediaStore.Files.getContentUri("external"),
                MediaStore.MediaColumns.DATA + " = ?",
                new String[]{mNoMedia.getPath()});
        Log.d(TAG, "deleteNoMedia: " + count);
    }

}
