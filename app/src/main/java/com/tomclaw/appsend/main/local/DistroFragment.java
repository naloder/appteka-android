package com.tomclaw.appsend.main.local;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import com.tomclaw.appsend.main.adapter.files.FileViewHolderCreator;
import com.tomclaw.appsend.main.item.ApkItem;
import com.tomclaw.appsend.util.FileHelper;

import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.InstanceState;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static android.content.pm.PackageManager.GET_PERMISSIONS;

@EFragment
abstract class DistroFragment extends CommonItemFragment<ApkItem> {

    private static final CharSequence APK_EXTENSION = "apk";

    @InstanceState
    ArrayList<ApkItem> files;

    @Override
    protected List<ApkItem> getFiles() {
        return files;
    }

    @Override
    protected void setFiles(List<ApkItem> files) {
        if (files != null) {
            this.files = new ArrayList<>(files);
        } else {
            this.files = null;
        }
    }

    @Override
    protected FileViewHolderCreator<ApkItem> getViewHolderCreator() {
        return new ApkItemViewHolderCreator(getContext());
    }

    @Override
    List<ApkItem> loadItemsSync() {
        PackageManager packageManager = getContext().getPackageManager();
        ArrayList<ApkItem> itemList = new ArrayList<>();
        walkDir(packageManager, itemList, Environment.getExternalStorageDirectory());
        return itemList;
    }

    private void walkDir(PackageManager packageManager, List<ApkItem> itemList, File dir) {
        File listFile[] = dir.listFiles();
        if (listFile != null) {
            for (File file : listFile) {
                if (file.isDirectory()) {
                    walkDir(packageManager, itemList, file);
                } else {
                    String extension = FileHelper.getFileExtensionFromPath(file.getName());
                    if (TextUtils.equals(extension, APK_EXTENSION)) {
                        processApk(packageManager, itemList, file);
                    }
                }
            }
        }
    }

    private void processApk(PackageManager packageManager, List<ApkItem> itemList, File file) {
        if (file.exists()) {
            try {
                PackageInfo packageInfo = packageManager.getPackageArchiveInfo(
                        file.getAbsolutePath(), GET_PERMISSIONS);
                if (packageInfo != null) {
                    ApplicationInfo info = packageInfo.applicationInfo;
                    info.sourceDir = file.getAbsolutePath();
                    info.publicSourceDir = file.getAbsolutePath();
                    String label = packageManager.getApplicationLabel(info).toString();
                    String version = packageInfo.versionName;

                    String installedVersion = null;
                    try {
                        PackageInfo instPackageInfo = packageManager.getPackageInfo(info.packageName, 0);
                        if (instPackageInfo != null) {
                            installedVersion = instPackageInfo.versionName;
                        }
                    } catch (Throwable ignored) {
                        // No package, maybe?
                    }

                    ApkItem item = new ApkItem(label, info.packageName, version, file.getPath(),
                            file.length(), installedVersion, file.lastModified(), packageInfo);
                    itemList.add(item);
                }
            } catch (Throwable ignored) {
                // Bad package.
            }
        }
    }
}