package com.bartovapps.audiorecorder.launcher

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Build.VERSION_CODES.M
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.util.Log
import android.widget.Toast
import com.bartovapps.audiorecorder.BasePresenter
import com.bartovapps.audiorecorder.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity(), MainContract.View {


    companion object {
        val TAG = MainActivity::class.java.simpleName
        val PERMISSIONS_REQUEST = 100
    }
    lateinit var presenter : MainPresenter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        checkPermissions()

        setPresenter(MainPresenter())
        Log.i(TAG, "onCreate")

        tbRecord.setOnClickListener({
            if(tbRecord.isChecked){
                presenter.startRecordClicked()
                tbPlayback.isEnabled = false
            }else{
                presenter.stopRecordClicked()
                tbPlayback.isEnabled = true
            }
        })


        tbPlayback.setOnClickListener({
            if(tbPlayback.isChecked){
                presenter.startPlayClicked()
                tbRecord.isEnabled = false
            }else{
                presenter.stopPlaybackClicked()
                tbRecord.isEnabled = true
            }
        })
    }


    override fun setPresenter(presenter: BasePresenter) {
        this.presenter = presenter as MainPresenter
    }

    override fun showPlaybackStopped() {
    }

    override fun showRecordingStopped() {
    }

    override fun initButtons() {
        tbPlayback.isEnabled = true
        tbRecord.isEnabled = true
    }


    private fun checkPermissions() {

        val permissions = arrayOf( Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO)

        val permissionCheck = hasPermissions(this, permissions)
        ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
        //        Log.i(TAG, "checkPermissions permissionCheck " + permissionCheck);
        if (!permissionCheck) {
            // User may have declined earlier, ask Android if we should show him a reason

            // request the permission.
            // CALLBACK_NUMBER is a integer constants
            ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_REQUEST)// The callback method gets the result of the request.

        }
    }

    fun hasPermissions(context: Activity?, permissions: Array<String>?): Boolean {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(context, permission)) {
                        // show an explanation to the user
                        // Good practise: don't block thread after the user sees the explanation, try again to request the permission.
                        Toast.makeText(context, "Asking again permission: " + permission, Toast.LENGTH_SHORT).show()
                    }
                    return false
                }
            }
        }
        return true
    }
}
