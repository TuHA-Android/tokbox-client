package com.example.tuha.tokboxclient

import com.google.gson.annotations.SerializedName

class ZoomData {
    @SerializedName("focus_x")
    var percentageFocusX: Float = -1F
    @SerializedName("focus_y")
    var percentageFocusY: Float = -1F
    @SerializedName("scale_factor")
    var scaleFactor: Float = -1F
}