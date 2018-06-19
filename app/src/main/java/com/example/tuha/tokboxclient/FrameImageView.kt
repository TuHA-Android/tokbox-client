package com.example.tuha.tokboxclient

import android.content.Context
import android.graphics.Matrix
import android.graphics.Paint
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import com.github.chrisbanes.photoview.PhotoView


class FrameImageView : PhotoView {

    constructor(context: Context?) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        mPaint.style = Paint.Style.STROKE
        mPaint.strokeWidth = mStrokeWidth.toFloat()
        mPaint.color = ContextCompat.getColor(context, R.color.colorAccent)
    }

    private val mStrokeWidth = 10

    private val mPaint: Paint = Paint()

    private var mImageDimens: IntArray? = null

    /**
     * Returns the bitmap position inside an image
     * @return 0: left, 1: top, 2: width, 3: height
     */
    fun getBitmapPositionInsideImageView(): IntArray? {
        val ret = IntArray(4)

        // Get image dimensions
        // Get image matrix values and place them in an array
        val f = FloatArray(9)
        imageMatrix.getValues(f)

        // Extract the scale values using the constants (if aspect ratio maintained, scaleX == scaleY)
        val scaleX = f[Matrix.MSCALE_X]
        val scaleY = f[Matrix.MSCALE_Y]

        // Get the drawable (could also get the bitmap behind the drawable and getWidth/getHeight)
        val origW = drawable.intrinsicWidth
        val origH = drawable.intrinsicHeight

        // Calculate the actual dimensions
        val actW = Math.round(origW * scaleX)
        val actH = Math.round(origH * scaleY)

        ret[2] = actW
        ret[3] = actH

        // Get image position
        // We assume that the image is centered into ImageView
        val imgViewW = width
        val imgViewH = height

        val top = (imgViewH - actH) / 2
        val left = (imgViewW - actW) / 2

        ret[0] = left
        ret[1] = top

        return ret
    }
}