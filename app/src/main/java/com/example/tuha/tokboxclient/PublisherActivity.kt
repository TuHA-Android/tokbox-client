package com.example.tuha.tokboxclient

import android.Manifest
import android.annotation.SuppressLint
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import com.bumptech.glide.Glide
import com.github.chrisbanes.photoview.OnScaleChangedListener
import com.github.chrisbanes.photoview.OnViewDragListener
import com.opentok.android.Connection
import com.opentok.android.OpentokError
import com.opentok.android.Session
import com.opentok.android.Stream
import io.reactivex.subjects.PublishSubject
import kotlinx.android.synthetic.main.activity_publisher.*
import org.json.JSONObject
import pub.devrel.easypermissions.AfterPermissionGranted
import pub.devrel.easypermissions.EasyPermissions
import java.util.concurrent.TimeUnit

class PublisherActivity : AppCompatActivity(), Session.SessionListener, Session.ConnectionListener {

    companion object {
        const val API_KEY = "46136012"
        const val SESSION_ID = "2_MX40NjEzNjAxMn5-MTUyODcwNDE4Mzk0NX40TVAxTjlNYTBWRFI0VEk2R1NrdmRCbFV-UH4"
        const val TOKEN = "T1==cGFydG5lcl9pZD00NjEzNjAxMiZzaWc9ODczOTJlNjM3MmRmZWM4NDkzMDM0MWFjNGJjYzBmZWZjODNiZTZmYzpzZXNzaW9uX2lkPTJfTVg0ME5qRXpOakF4TW41LU1UVXlPRGN3TkRFNE16azBOWDQwVFZBeFRqbE5ZVEJXUkZJMFZFazJSMU5yZG1SQ2JGVi1VSDQmY3JlYXRlX3RpbWU9MTUyODcwNjg2OSZub25jZT0wLjIwNTIxNTg0Mzc2ODU2Mjk0JnJvbGU9cHVibGlzaGVyJmV4cGlyZV90aW1lPTE1MzEyOTg4NjgmaW5pdGlhbF9sYXlvdXRfY2xhc3NfbGlzdD0="
        const val TAG = "PublisherActivity"
        const val RC_VIDEO_APP_PERM = 124
    }

    private var mSession: Session? = null
    private var myConnectionId: String? = null
    private val publishSubject: PublishSubject<String> = PublishSubject.create()

    private var onScale = OnScaleChangedListener { _, focusX, focusY ->
        val positionInsideImageView = photo_view.getBitmapPositionInsideImageView()!!
        val actualFocusX = focusX - positionInsideImageView[0]
        val actualFocusY = focusY - positionInsideImageView[1]
        val percentageFocusX = actualFocusX / photo_view.drawable.intrinsicWidth
        val percentageFocusY = actualFocusY / photo_view.drawable.intrinsicHeight
        val data = getZoomPackage(percentageFocusX, percentageFocusY, photo_view.scale)

        publishSubject.onNext(data)
    }

    private var onDrag = OnViewDragListener { translateX, translateY ->
        val percentageTranslateX = translateX / photo_view.drawable.intrinsicWidth
        val percentageTranslateY = translateY / photo_view.drawable.intrinsicHeight
        val data = getTranslatePackage(percentageTranslateX, percentageTranslateY)
        Log.e(TAG, data)

        mSession?.sendSignal(TRANSLATE, data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_publisher)

        initView()
        initSubject()
        requestPermissions()
    }

    @SuppressLint("CheckResult")
    private fun initSubject() {
        val share = publishSubject.share()
        val debounce = share.debounce(500L, TimeUnit.MILLISECONDS)
        share
                .buffer(debounce)
                .map { listData ->
                    val first = listData.first().toZoomData()
                    val last = listData.last().toZoomData()
                    return@map getZoomPackage(first.percentageFocusX, first.percentageFocusY, last.scaleFactor)
                }
                .subscribe({ data ->
                    Log.e(TAG, data)
                    mSession?.sendSignal(ZOOM, data)
                }, {
                    it.printStackTrace()
                })
    }

    private fun initView() {
        setOnScaleListener()
        Glide.with(this)
                .load("https://i.pinimg.com/736x/e2/b8/2a/e2b82aded815e80351b929a77519adaa--tropical-wallpapers-tropical-iphone-wallpaper.jpg")
                .into(photo_view)
    }

    private fun getZoomPackage(focusX: Float, focusY: Float, scaleFactor: Float): String {
        val jsonObject = JSONObject()
        jsonObject.put("focus_x", focusX)
        jsonObject.put("focus_y", focusY)
        jsonObject.put("scale_factor", scaleFactor)

        return jsonObject.toString()
    }

    private fun getTranslatePackage(translateX: Float, translateY: Float): String {
        val jsonObject = JSONObject()
        jsonObject.put("translate_x", translateX)
        jsonObject.put("translate_y", translateY)

        return jsonObject.toString()
    }

    override fun onResume() {
        super.onResume()

        if (mSession == null) {
            return
        }
        mSession?.onResume()
    }

    override fun onPause() {
        super.onPause()

        if (mSession == null) {
            return
        }
        mSession?.onPause()

        if (isFinishing) {
            disconnectSession()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, this)
    }

    @AfterPermissionGranted(RC_VIDEO_APP_PERM)
    private fun requestPermissions() {
        val perms = arrayOf(Manifest.permission.INTERNET, Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (EasyPermissions.hasPermissions(this, *perms)) {
            initializeSession()
        } else {
            EasyPermissions.requestPermissions(this, "This app needs access to your camera and mic to make video calls", RC_VIDEO_APP_PERM, *perms)
        }
    }

    //region implement tokbox interface
    override fun onConnected(session: Session) {
        Log.e(TAG, "Session Connected")

        myConnectionId = session.connection.connectionId
    }

    override fun onDisconnected(session: Session) {
        Log.e(TAG, "Session Disconnected")
    }

    override fun onStreamReceived(session: Session, stream: Stream) {
        Log.e(TAG, "Stream Received")
    }

    override fun onStreamDropped(session: Session, stream: Stream) {
        Log.e(TAG, "Stream Dropped")
    }

    override fun onError(session: Session, opentokError: OpentokError) {
        Log.e(TAG, "Session error: " + opentokError.message)
    }
    //endregion implement tokbox interface

    //  region OnConnectionListener
    override fun onConnectionDestroyed(p0: Session, p1: Connection) {
        Log.i(TAG, p1.data)
    }

    override fun onConnectionCreated(p0: Session, p1: Connection) {
        Log.i(TAG, p1.data)
    }
    //  endregion OnConnectionListener

    private fun initializeSession() {
        mSession = Session.Builder(this, API_KEY, SESSION_ID).build()
        mSession?.setSessionListener(this)
        mSession?.setConnectionListener(this)
        mSession?.connect(TOKEN)
    }

    private fun setOnScaleListener() {
        photo_view.setOnScaleChangeListener(onScale)
        photo_view.setOnViewDragListener(onDrag)
    }

    private fun disconnectSession() {
        if (mSession == null) {
            return
        }

        mSession?.disconnect()
    }
}
