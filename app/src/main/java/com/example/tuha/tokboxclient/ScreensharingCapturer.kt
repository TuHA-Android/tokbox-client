package com.example.tuha.tokboxclient

import android.os.Handler
import android.util.Log
import android.view.View
import com.opentok.android.BaseVideoCapturer

class ScreensharingCapturer(private val mContentView: View) : BaseVideoCapturer() {

    companion object {
        const val FPS = 60
        const val INTERVAL = 100L
        const val TAG = "PublisherActivity"
    }

    private val mFrameProducerHandler = Handler()
    private lateinit var mCapturerSettings: CaptureSettings
    private var mCapturerHasStarted = false
    private var mCapturerIsPaused = false
    private var mWidth: Int = 0
    private var mHeight: Int = 0
    private var frameBuffer : IntArray? = null
    private var mFrameProducerIntervalMillis: Long = INTERVAL

    private val mFrameProducer = object : Runnable {
        override fun run() {
            val width = mContentView.width
            val height = mContentView.height

            if (frameBuffer == null || mWidth != width || mHeight != height) {
                mWidth = width
                mHeight = height
                frameBuffer = IntArray(mWidth * mHeight)
            }

            mContentView.isDrawingCacheEnabled = true
            mContentView.buildDrawingCache()
            val bmp = mContentView.drawingCache
            if (bmp != null) {
                bmp.getPixels(frameBuffer, 0, width, 0, 0, width, height)
                mContentView.isDrawingCacheEnabled = false
                provideIntArrayFrame(frameBuffer, BaseVideoCapturer.ARGB, width, height, 0, false)
            }

            if (mCapturerHasStarted && !mCapturerIsPaused) {
                mFrameProducerHandler.postDelayed(this, mFrameProducerIntervalMillis)
            }
        }
    }

    override fun init() {
        mCapturerSettings = BaseVideoCapturer.CaptureSettings()
        mCapturerSettings.fps = FPS
        mCapturerSettings.width = mWidth
        mCapturerSettings.height = mHeight
        mCapturerSettings.format = BaseVideoCapturer.ARGB
    }

    override fun onResume() {
        Log.e(TAG, "resume capturer")

        mCapturerIsPaused = false
        mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis)
    }

    override fun stopCapture(): Int {
        Log.i(TAG,"stop capture")
        mCapturerHasStarted = false
        mFrameProducerHandler.removeCallbacks(mFrameProducer)
        return 0
    }

    override fun getCaptureSettings(): CaptureSettings {
        return mCapturerSettings
    }

    override fun startCapture(): Int {
        mCapturerHasStarted = true
        mFrameProducerHandler.postDelayed(mFrameProducer, mFrameProducerIntervalMillis)
        return 0
    }

    override fun destroy() {
        Log.e(TAG, "Destroy capturer")
    }

    override fun onPause() {
        Log.e(TAG, "Pause capturer")
        mCapturerIsPaused = true
    }

    override fun isCaptureStarted(): Boolean {
        return mCapturerHasStarted
    }
}