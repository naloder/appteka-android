package com.tomclaw.appsend.screen.installed

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import com.avito.konveyor.ItemBinder
import com.avito.konveyor.adapter.AdapterPresenter
import com.avito.konveyor.adapter.SimpleRecyclerAdapter
import com.greysonparrelli.permiso.Permiso
import com.greysonparrelli.permiso.Permiso.IOnPermissionResult
import com.greysonparrelli.permiso.Permiso.IOnRationaleProvided
import com.tomclaw.appsend.Appteka
import com.tomclaw.appsend.R
import com.tomclaw.appsend.screen.details.createDetailsActivityIntent
import com.tomclaw.appsend.screen.installed.di.InstalledModule
import com.tomclaw.appsend.screen.permissions.createPermissionsActivityIntent
import com.tomclaw.appsend.util.Analytics
import com.tomclaw.appsend.util.ThemeHelper
import javax.inject.Inject

class InstalledActivity : AppCompatActivity(), InstalledPresenter.InstalledRouter {

    @Inject
    lateinit var presenter: InstalledPresenter

    @Inject
    lateinit var adapterPresenter: AdapterPresenter

    @Inject
    lateinit var binder: ItemBinder

    @Inject
    lateinit var analytics: Analytics

    @Inject
    lateinit var preferences: InstalledPreferencesProvider

    private val invalidateDetailsResultLauncher =
        registerForActivityResult(StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                presenter.invalidateApps()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val presenterState = savedInstanceState?.getBundle(KEY_PRESENTER_STATE)
        Appteka.getComponent()
            .installedComponent(InstalledModule(this, presenterState))
            .inject(activity = this)
        ThemeHelper.updateTheme(this)
        Permiso.getInstance().setActivity(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.installed_activity)

        val adapter = SimpleRecyclerAdapter(adapterPresenter, binder)
        val view = InstalledViewImpl(window.decorView, preferences, adapter)

        presenter.attachView(view)

        if (savedInstanceState == null) {
            analytics.trackEvent("open-installed-screen")
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        presenter.onBackPressed()
    }

    override fun onStart() {
        super.onStart()
        presenter.attachRouter(this)
    }

    override fun onResume() {
        super.onResume()
        Permiso.getInstance().setActivity(this)
    }

    override fun onStop() {
        presenter.detachRouter()
        super.onStop()
    }

    override fun onDestroy() {
        presenter.detachView()
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBundle(KEY_PRESENTER_STATE, presenter.saveState())
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Permiso.getInstance().onRequestPermissionResult(requestCode, permissions, grantResults)
    }

    override fun openAppScreen(appId: String, title: String) {
        val intent = createDetailsActivityIntent(
            context = this,
            appId = appId,
            label = title,
            moderation = false,
            finishOnly = true
        )
        invalidateDetailsResultLauncher.launch(intent)
    }

    override fun launchApp(packageName: String) {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        startActivity(intent)
        analytics.trackEvent("installed-launch-app")
    }

    override fun openPermissionsScreen(permissions: List<String>) {
        val intent = createPermissionsActivityIntent(context = this, permissions)
        startActivity(intent)
    }

    override fun removeApp(packageName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Permiso.getInstance().requestPermissions(object : IOnPermissionResult {
                override fun onPermissionResult(resultSet: Permiso.ResultSet) {
                    if (resultSet.isPermissionGranted(Manifest.permission.REQUEST_DELETE_PACKAGES)) {
                        onRemoveAppPermitted(packageName)
                    } else {
                        presenter.showSnackbar(getString(R.string.request_delete_packages))
                    }
                }

                override fun onRationaleRequested(
                    callback: IOnRationaleProvided,
                    vararg permissions: String
                ) {
                    val title: String = getString(R.string.app_name)
                    val message: String = getString(R.string.request_delete_packages)
                    Permiso.getInstance().showRationaleInDialog(title, message, null, callback)
                }
            }, Manifest.permission.REQUEST_DELETE_PACKAGES)
        } else {
            onRemoveAppPermitted(packageName)
        }
    }

    override fun leaveScreen() {
        finish()
    }

    private fun onRemoveAppPermitted(packageName: String) {
        val packageUri = Uri.parse("package:$packageName")
        val uninstallIntent = Intent(Intent.ACTION_DELETE, packageUri)
        startActivity(uninstallIntent)
        analytics.trackEvent("installed-delete-app")
    }

}

fun createInstalledActivityIntent(
    context: Context,
): Intent = Intent(context, InstalledActivity::class.java)

private const val KEY_PRESENTER_STATE = "presenter_state"
