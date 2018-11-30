package test.mike.com.mediaframeworktools.permission;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v4.content.ContextCompat;

public class PermissionUtils {

    public static boolean hasPermission(Context ctx, String requestPermission) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return ContextCompat.checkSelfPermission(ctx, requestPermission) == PackageManager.PERMISSION_GRANTED;
        } else {
            return true;
        }
    }

    public static boolean hasExternalStoragePermission(Context ctx) {
        return hasPermission(ctx, Manifest.permission.READ_EXTERNAL_STORAGE)
                && hasPermission(ctx, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    }

}
