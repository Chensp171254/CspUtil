package com.personal.csp.permission;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Created by csp on 2016/7/22.
 */
public abstract class AbstractCheckPermission {
    public static final int PERMISSIONS_GRANTED = 0; // 权限授权
    public static final int PERMISSIONS_DENIED = 1; // 权限拒绝
    private static final int PERMISSION_REQUEST_CODE = 1; // 系统权限管理页面的参数
    private static final String PACKAGE_URL_SCHEME = "package:"; // 方案
    private boolean isRequireCheck = true; // 是否需要系统权限检测, 防止和系统提示框重叠
    private Context context;
    private Activity activity;

    public AbstractCheckPermission(Activity activity) {
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    public abstract String[] getPermissions();

    public void checkPermission() {
        if (isRequireCheck) {
            String[] permissions = getPermissions();
            if (checkPermissions(permissions)) {
                //部分权限没有获取（只要有权限没被授予）
                requestPermissions(permissions); // 请求权限
            } else {
                onResultPermissionsGranted(); // 全部权限都已获取
            }
        } else {
            isRequireCheck = true;
        }
    }

    /**
     * 全部权限均已获取
     */
    private void onResultPermissionsGranted() {
        activity.setResult(PERMISSIONS_GRANTED);
    }

    /**
     * 权限分为允许、询问、拒绝；只有"询问"的权限会出现授权弹框
     * @param requestCode
     * @param grantResults
     */
    public void checkRequestPermissionsResult(int requestCode, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE && isAllPermissionsGranted(grantResults)) {
            isRequireCheck = true;
            onResultPermissionsGranted();
        } else {
            isRequireCheck = false;
            showPermissionDialog();
        }
    }

    /**
     * 含有全部的权限
     * @param grantResults
     * @return
     */
    private boolean isAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            //只要有一项权限被拒绝就返回false
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 显示缺失权限提示
     */
    private void showPermissionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("帮助");
        ///n/n最后点击两次后退按钮，即可返回。
        builder.setMessage("当前应用缺少必要权限。\n\n请点击\"设置\"-\"权限\"-打开所需权限。");
        // 拒绝, 退出应用
        builder.setNegativeButton("退出", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                activity.setResult(PERMISSIONS_DENIED);
                activity.finish();
            }
        });
        builder.setPositiveButton("设置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startAppSettings();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    /**
     * 启动应用的设置
     */
    private void startAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse(PACKAGE_URL_SCHEME + activity.getPackageName()));
        activity.startActivity(intent);
    }

    /**
     * 请求权限兼容低版本
     * @param permissions
     */
    public void requestPermissions(String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, PERMISSION_REQUEST_CODE);
    }

    /**
     * 判断权限集合
     * @param permissions
     * @return
     */
    public boolean checkPermissions(String... permissions) {
        for (String permission : permissions) {
            //有一个没授权就返回true
            if (checkPermission(context, permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断是否缺少权限
     * @param context
     * @param permission
     * @return
     */
    private boolean checkPermission(Context context, String permission) {
        //某一项权限没被授予，注意此处是PERMISSION_DENIED
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_DENIED;
    }
}
