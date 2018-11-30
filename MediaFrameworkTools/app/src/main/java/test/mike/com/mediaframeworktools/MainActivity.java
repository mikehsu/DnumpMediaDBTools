package test.mike.com.mediaframeworktools;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.ExifInterface;
import android.media.MediaScannerConnection;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import test.mike.com.mediaframeworktools.DocumentUtilis.ImageDocUriUtility;
import test.mike.com.mediaframeworktools.ExifUtils.ExifUtility;
import test.mike.com.mediaframeworktools.permission.PermissionUtils;

public class MainActivity extends AppCompatActivity implements MediaScannerConnection.OnScanCompletedListener {

    private final String TAG = "MediaFrameworkTool";
    private final int REQUEST_CODE_IMAGE = 1;
    private final int REQUEST_CODE_VIDEO = 2;
    private final int REQUEST_CODE_AUDIO = 3;
    private final int REQUEST_CODE_SETTINGS = 4;

    private TextView mTextViewInfo;
    private StringBuilder mStringBuilder;

    private final String[] mKeys = new String[] {
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED,
            MediaStore.Files.FileColumns.DATE_MODIFIED,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.Files.FileColumns.TITLE,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            "format",
            "bucket_id",
            "bucket_display_name",
            "parent",
            "description",
            "picasa_id",
            "orientation",
            "latitude",
            "longitude",
            "mini_thumb_magic",
            "isprivate",
            "title_key",
            "artist_id",
            "album_id",
            "composer",
            "track",
            "year",
            "is_ringtone",
            "is_music",
            "is_alarm",
            "is_notification",
            "is_podcast",
            "album_artist",
            "duration",
            "bookmark",
            "artist",
            "album",
            "resolution",
            "tags",
            "category",
            "language",
            "mini_thumb_data",
            "name",
            "old_id",
            "is_drm",
            "width",
            "height",
            //"title_resource_uri",
            "volume_id",
    };

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    dumpExternalFileDone((String) msg.obj);
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(!PermissionUtils.hasExternalStoragePermission(getApplicationContext())) {
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    0x01);
        } else {
            dumpExternalStorage();
        }

        mTextViewInfo = (TextView) findViewById(R.id.txt_info);

        Button btnDump = (Button) findViewById(R.id.btn_dump_table);
        btnDump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "dump external storage table");
                dumpExternalFile();
            }
        });

        Button btnDumpImage = (Button) findViewById(R.id.btn_dump_image);
        btnDumpImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("image/*");
                startActivityForResult(chooseFile, REQUEST_CODE_IMAGE);
            }
        });

        Button btnDumpVideo = (Button) findViewById(R.id.btn_dump_video);
        btnDumpVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("video/*");
                startActivityForResult(chooseFile, REQUEST_CODE_VIDEO);
            }
        });

        Button btnDumpAudio = (Button) findViewById(R.id.btn_dump_audio);
        btnDumpAudio.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                final Intent chooseFile = new Intent(Intent.ACTION_GET_CONTENT);
                chooseFile.setType("audio/*");
                startActivityForResult(chooseFile, REQUEST_CODE_AUDIO);
            }
        });

        Button btnTestNoMedia = (Button) findViewById(R.id.btn_test_nomedia);
        btnTestNoMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, TestNoMedia.class);
                startActivity(intent);
            }
        });

        Button btnTestRingtoneMgr = (Button) findViewById(R.id.btn_test_ringtone_mgr);
        btnTestRingtoneMgr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Intent intent = new Intent(MainActivity.this, TestRingtoneManager.class);
                //startActivity(intent);
                testRingtoneManager();
            }
        });

        Button btnScanOTGFile = (Button) findViewById(R.id.btn_scan_otg_file);
        btnScanOTGFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                scanOTGFile();
            }
        });

        btnTestNoMedia.setVisibility(View.GONE);
        btnTestRingtoneMgr.setVisibility(View.GONE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 0x01) {
            if(grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(MainActivity.this, "No permission to access!", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                dumpExternalStorage();
            }
        }
    }

    private boolean hasSettingPermission() {
        return Settings.System.canWrite(getApplicationContext());
    }
    private void checkSettingPermission() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivityForResult(intent, REQUEST_CODE_SETTINGS);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK) {
            switch (requestCode) {
                case REQUEST_CODE_SETTINGS:
                    break;
                case REQUEST_CODE_AUDIO:
                case REQUEST_CODE_IMAGE:
                case REQUEST_CODE_VIDEO:
                    dumpMediaInfo(data.getData(), requestCode);
                    break;
            }
        }
    }

    private void dumpMediaInfo(Uri uri, int type) {
        String filePath = ImageDocUriUtility.getPath(getApplicationContext(), uri);
        String fileId = ImageDocUriUtility.queryFileIDByPath(getApplicationContext(), filePath);
        Log.e(TAG, "[dumpMediaInfo]: fileId: " + fileId + ", filePath: " + filePath);

        String[] projection = new String[] {
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATE_MODIFIED,
                "datetaken",
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.TITLE
        };

        Cursor c = null;
        try {
            c = getContentResolver().query(MediaStore.Files.getContentUri("external"), null,
                    MediaStore.Files.FileColumns._ID + " = ?", new String[]{fileId}, null);
            if(c != null) {
                Log.d(TAG, "found item # " + c.getCount());
            }
            c.moveToFirst();

            mStringBuilder = new StringBuilder();
            mStringBuilder.append("File info").append("\n");
            mStringBuilder.append("ID: ").append(fileId).append("\n");
            mStringBuilder.append("Path: ").append(filePath).append("\n");
            mStringBuilder.append("DATE_ADDED: ").append(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)).append("\n");
            mStringBuilder.append("DATE_MODIFIED: ").append(c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED))).append("\n");
            mStringBuilder.append("DATA_TAKEN: ").append(c.getString(c.getColumnIndexOrThrow("datetaken"))).append("\n");
            mStringBuilder.append("DISPLAY_NAME: ").append(c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))).append("\n");
            mStringBuilder.append("TITLE: ").append(c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE))).append("\n");
            mStringBuilder.append("MimeType: ").append(c.getString(c.getColumnIndexOrThrow("mime_type"))).append("\n");
            mStringBuilder.append("latitude: ").append(c.getString(c.getColumnIndexOrThrow("latitude"))).append("\n");
            mStringBuilder.append("longitude: ").append(c.getString(c.getColumnIndexOrThrow("longitude"))).append("\n");
            mStringBuilder.append("resolution: ").append(c.getString(c.getColumnIndexOrThrow("resolution"))).append("\n");
            mStringBuilder.append("width: ").append(c.getString(c.getColumnIndexOrThrow("width"))).append("\n");
            mStringBuilder.append("height: ").append(c.getString(c.getColumnIndexOrThrow("height"))).append("\n");

            if(c.getInt(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)) == MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) {
                mStringBuilder.append("\n");
                dumpImageInfo(filePath);
            } else {
                Log.d(TAG, "file info: " + mStringBuilder.toString());
                mTextViewInfo.setText(mStringBuilder.toString());
            }
        } catch (Exception e) {
            Log.e(TAG, "Exception dumpMediaInfo", e);
        } finally {
            if(c != null) {
                c.close();
            }
        }
    }

    private void dumpImageInfo(String filePath) {
        mStringBuilder.append("Image info").append("\n");
        ExifInterface exif = null;
        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            Log.e(TAG, "IOException dumpImageInfo", e);
        }
        if(exif != null) {
            mStringBuilder.append("Exif GPS time: ").append(ExifUtility.getGpsDateTime(exif)).append("\n");
            mStringBuilder.append("Exif Date time: ").append(ExifUtility.getDateTime(exif)).append("\n");
        }

        File file = new File(filePath);
        if(file != null && file.exists()) {
            mStringBuilder.append("File Modified: ").append(file.lastModified()/1000).append("\n");
        }

        Log.d(TAG, "image info: " + mStringBuilder.toString());
        mTextViewInfo.setText(mStringBuilder.toString());
    }

    private void testRingtoneManager() {
        if(!hasSettingPermission()) {
            checkSettingPermission();
            return;
        }

        Log.d(TAG, "[testRingtoneManager]");

        Uri uri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
        Log.d(TAG, "[testRingtoneManager] 1 TYPE_RINGTONE:" + uri);

        //content://media/internal/audio/media/68
        Uri test = Uri.parse("content://0@media/internal/audio/media/68");
        Log.d(TAG, "test uri: " + test);
        RingtoneManager.setActualDefaultRingtoneUri(getApplicationContext(),
                RingtoneManager.TYPE_RINGTONE, test);

        uri = RingtoneManager.getActualDefaultRingtoneUri(getApplicationContext(), RingtoneManager.TYPE_RINGTONE);
        Log.d(TAG, "[testRingtoneManager] 2 TYPE_RINGTONE:" + uri);

        //Ringtone ringtone = new Ringtone();

        Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), uri);
        r.play();
    }

    private void dumpExternalStorage() {
        StorageManager storageManager = (StorageManager) getSystemService(Context.STORAGE_SERVICE);
        List<StorageVolume> volumes = storageManager.getStorageVolumes();

        for(StorageVolume volume : volumes) {
            Log.d(TAG, "dumpExternalStorage: " + volume.toString() + ", UUID: " + volume.getUuid());
        }
    }

    private void dumpExternalFile() {
        Cursor c = null;
        FileWriter writer = null;
        File outFile = new File(Environment.getExternalStorageDirectory(), "files_DB.txt");
        if(outFile.exists()) {
            outFile.delete();
        }
        try {
            c = getContentResolver().query(
                    MediaStore.Files.getContentUri("external"),
                    null, null, null, null);
            if(c.moveToFirst()) {
                writer = new FileWriter(outFile);
                do {
                    StringBuilder sb = new StringBuilder();
                    for(String key : mKeys) {
                        sb.append("[").append(key).append("=")
                        .append(c.getString(c.getColumnIndexOrThrow(key)))
                        .append("]; ");
                    }
                    sb.append("\n");
                    /*
                    sb.append(MediaStore.Files.FileColumns._ID).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.DATA).append(": ").append(
                            c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.SIZE).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE))).append(", ");
                    sb.append("format").append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow("format"))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.PARENT).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.PARENT))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.DATE_ADDED).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.DATE_MODIFIED).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.MIME_TYPE).append(": ").append(
                            c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.TITLE).append(": ").append(
                            c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.TITLE))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.DISPLAY_NAME).append(": ").append(
                            c.getString(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME))).append(", ");
                    sb.append(MediaStore.Images.ImageColumns.DATE_TAKEN).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN))).append(", ");
                    sb.append("bucket_id").append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow("bucket_id"))).append(", ");
                    sb.append("bucket_display_name").append(": ").append(
                            c.getString(c.getColumnIndexOrThrow("bucket_display_name"))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.MEDIA_TYPE).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.WIDTH).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.WIDTH))).append(", ");
                    sb.append(MediaStore.Files.FileColumns.HEIGHT).append(": ").append(
                            c.getLong(c.getColumnIndexOrThrow(MediaStore.Files.FileColumns.HEIGHT))).append(", ");
                    sb.append("\n");
*/
                    writer.write(sb.toString());

                } while(c.moveToNext());

                MediaScannerConnection.scanFile(getApplicationContext(),
                        new String[]{outFile.getPath()}, new String[]{"text/plain"}, this);
            }
        } catch (Exception e) {
            Log.e(TAG, "[dumpExternalFile] " + e);
        } finally {
            if(c != null) {
                c.close();
            }
            try {
                if (writer != null) {
                    writer.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "[dumpExternalFile] IOException: " + e);
            }
        }
    }

    @Override
    public void onScanCompleted(String path, Uri uri) {
        Log.d(TAG, "[onScanCompleted]: " + path + ", " + uri);
        Message.obtain(mHandler, 1, path).sendToTarget();
//        if(path.endsWith("files_DB.txt")) {
//
//        } else if(path.endsWith("test.mp3")) {
//            Message.obtain(mHandler, 1, path).sendToTarget();
//        }
    }

    private void dumpExternalFileDone(String path) {
        Toast.makeText(getApplicationContext(), "Dump finish: " + path, Toast.LENGTH_SHORT).show();
    }

    private void scanOTGFile() {
        //File otg = new File("/storage/C0BA-12E2/test.mp3");
        File otg = new File("/mnt/media_rw/C0BA-12E2/test.mp3");
        if(!otg.exists()) {
            Log.d(TAG, "[scanOTGFile] otg file not existed");
            //return;
        }
        Log.d(TAG, "[scanOTGFile]: " + otg.getPath());
        MediaScannerConnection.scanFile(getApplicationContext(), new String[]{otg.getPath()}, null, this);
    }
}
