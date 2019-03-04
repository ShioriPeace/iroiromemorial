package com.shiorin.iroiromemorial

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    private lateinit var textureView: TextureView
    private  var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private val previewSize: Size = Size(300,300)//カメラサイズ変更かもその２


    private lateinit var previewRequestBuilder : CaptureRequest.Builder
    private lateinit var previewRequest : CaptureRequest
    private lateinit var captureSession : CameraCaptureSession
    private var backgroundThread : HandlerThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.cameraView)
        textureView.surfaceTextureListener = surfaceTextureListener

    }

    private fun openCamera() {
        var manager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            //端末のどのカメラを使うか
            var cameraId: String = manager.cameraIdList[0]
            var permission = ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)

            if(permission != PackageManager.PERMISSION_GRANTED){
                requestCameraPermission()
                return
            }
            manager.openCamera(cameraId,stateCallback,null)
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    private val stateCallback = object : CameraDevice.StateCallback(){
        //カメラ接続完了
        override fun onOpened(camera: CameraDevice) {
            this@MainActivity.cameraDevice = camera
            createCameraPreviewSession()
        }

        //カメラ切断
        override fun onDisconnected(camera: CameraDevice) {
            cameraDevice?.close()
            this@MainActivity.cameraDevice = null
        }

        //カメラ接続エラー
        override fun onError(camera: CameraDevice, error: Int) {
            onDisconnected(camera)
            finish()

        }
    }

    private fun createCameraPreviewSession() {
        try {
            val texture = textureView.surfaceTexture
            texture.setDefaultBufferSize(previewSize.width,previewSize.height)
            val surface = Surface(texture)
            previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            cameraDevice?.createCaptureSession(Arrays.asList(surface,imageReader?.surface),
                object : CameraCaptureSession.StateCallback(){
                    override fun onConfigured(session: CameraCaptureSession) {
                        if(cameraDevice == null) return
                        captureSession = session
                        try {
                            previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)
                            previewRequest = previewRequestBuilder.build()
                            captureSession.setRepeatingRequest(previewRequest,null,Handler(backgroundThread?.looper))
                        }catch (e:CameraAccessException){

                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                },null)
        }catch (e:CameraAccessException){

        }

    }


    private fun requestCameraPermission() {

    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener{
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
            imageReader = ImageReader.newInstance(width,height,ImageFormat.JPEG,2)

            openCamera()
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {

        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {

        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
            return false
        }
    }



}