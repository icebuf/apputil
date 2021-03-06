package com.icebuf.util;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The type Package util.
 */
public class PackageUtil {

    private static final String TAG = PackageUtil.class.getSimpleName();

    /**
     * The constant ACTION_INSTALL_APK.
     */
    public static final String ACTION_INSTALL_APK = "com.kty.sdk.action.INSTALL_APK";
    /**
     * The constant ACTION_INSTALL_COMMIT.
     */
    public static final String ACTION_INSTALL_COMMIT =
            "com.android.packageinstaller.ACTION_INSTALL_COMMIT";

    /**
     * The constant REQUEST_CODE_INSTALL.
     */
    public static final int REQUEST_CODE_INSTALL = 1;
    /**
     * The constant REQUEST_CODE_UNINSTALL.
     */
    public static final int REQUEST_CODE_UNINSTALL = 2;


    /**
     * Install.
     *
     * @param context  the context
     * @param apkPath  the apk path
     * @param receiver the receiver
     */
    public static void install(Context context, String apkPath, Class<?> receiver) {
        PackageManager pm = context.getPackageManager();
        PackageInstaller packageInstaller = pm.getPackageInstaller();

        PackageInstaller.SessionParams params = new PackageInstaller.SessionParams(
                PackageInstaller.SessionParams.MODE_FULL_INSTALL);

        File apkFile = new File(apkPath);
        params.setSize(apkFile.length());

        int sessionId = -1;
        try {
            sessionId = packageInstaller.createSession(params);
        } catch (IOException e) {
            Log.e(TAG, "create session failed!");
            return;
        }

        PackageInstaller.Session session = installApkFile(pm, sessionId, apkFile);
        if(session != null) {
            Intent broadcastIntent = new Intent(context, receiver);
            broadcastIntent.setAction(ACTION_INSTALL_APK);
            broadcastIntent.setFlags(Intent.FLAG_RECEIVER_FOREGROUND);
//            broadcastIntent.setPackage(
//                    pm.getPermissionControllerPackageName());
//            broadcastIntent.putExtra(EventResultPersister.EXTRA_ID, mInstallId);
//            broadcastIntent.setPackage(context.getPackageName());

            // 创建一个pendingIntent
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    REQUEST_CODE_INSTALL,
                    broadcastIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
            // 调用session的commit方法将intent发送出去
            session.commit(pendingIntent.getIntentSender());

        } else {
            packageInstaller.abandonSession(sessionId);
        }
    }

    private static PackageInstaller.Session installApkFile(PackageManager pm, int sessionId, File file) {
        PackageInstaller.Session session;
        try {
            // 根据sessionId创建要给session
            session = pm.getPackageInstaller().openSession(sessionId);
        } catch (IOException e) {
            return null;
        }
        // 设置安装进度条
        session.setStagingProgress(0);
        try {
            // 读取文件
            try (InputStream in = new FileInputStream(file)) {
                long sizeBytes = file.length();
                // 下面的操作是将文件中的内容写到PackageInstaller的session中
                try (OutputStream out = session
                        .openWrite("Installer", 0, sizeBytes)) {
                    byte[] buffer = new byte[512 * 1024];
                    while (true) {
                        int numRead = in.read(buffer);
                        if (numRead == -1) {
                            session.fsync(out);
                            break;
                        }
                        out.write(buffer, 0, numRead);
                    }
                }
            }
            return session;
        } catch (IOException | SecurityException e) {
            Log.e(TAG, "Could not write package", e);
            session.close();
            return null;
        }
    }

    /**
     * Uninstall.
     *
     * @param context     the context
     * @param packageName the package name
     * @param receiver    the receiver
     */
    public void uninstall(Context context, String packageName, Class<?> receiver) {
        uninstall(context, packageName, REQUEST_CODE_UNINSTALL, receiver);
    }

    /**
     * Uninstall.
     *
     * @param context     the context
     * @param packageName the package name
     * @param requestCode the request code
     * @param receiver    the receiver
     */
    public void uninstall(Context context, String packageName, int requestCode, Class<?> receiver) {
        Intent intent = new Intent(context, receiver);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, requestCode,
                intent, PendingIntent.FLAG_UPDATE_CURRENT);
        PackageInstaller pi = context.getPackageManager().getPackageInstaller();
        pi.uninstall(packageName, pendingIntent.getIntentSender());
    }


    /**
     * Gets app uid.
     *
     * @param context the context
     * @return the app uid
     */
    public static int getAppUid(Context context) {
        return getAppUid(context, context.getPackageName());
    }

    /**
     * 获取指定包名app的uid
     *
     * @param context the context
     * @param pkgName the pkg name
     * @return app uid
     */
    public static int getAppUid(Context context, String pkgName) {
        PackageManager pm = context.getPackageManager();
        try {
            ApplicationInfo applicationInfo = pm.getApplicationInfo(pkgName, 0);
            return applicationInfo.uid;
        }catch (Exception e){
            e.printStackTrace();
        }
        return 0;
    }
}
