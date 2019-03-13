package com.shiorin.iroiromemorial

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.MotionEvent
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import kotlinx.android.synthetic.main.activity_main.*
import java.io.*
import java.lang.ref.WeakReference
import java.util.*
import java.util.jar.Manifest

class MainActivity : AppCompatActivity() {
    companion object {
        const val PERMISSION_WRITE_STORAGE = 1000
        const val PERMISSION_READ_STORAGE = 1001
    }

    private lateinit var textureView: TextureView
    private  var imageReader: ImageReader? = null
    private var cameraDevice: CameraDevice? = null
    private val previewSize: Size = Size(300,300)//カメラサイズ変更かもその２



    private lateinit var previewRequestBuilder : CaptureRequest.Builder
    private lateinit var previewRequest : CaptureRequest
    private lateinit var captureSession : CameraCaptureSession
    private var backgroundThread : HandlerThread? = null
    private var backgroundHandler : Handler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textureView = findViewById(R.id.cameraView)
        textureView.surfaceTextureListener = surfaceTextureListener
        startBackgroundThread()

        val writePermission = ContextCompat.checkSelfPermission(this,android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val readPermission = ContextCompat.checkSelfPermission(this,android.Manifest.permission.READ_EXTERNAL_STORAGE)

        if ((writePermission != PackageManager.PERMISSION_GRANTED) || (readPermission != PackageManager.PERMISSION_GRANTED)){
            requestStoragePermission()
        }

    }



    private fun openCamera() {
        val manager: CameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            //端末のどのカメラを使うか
            val cameraId: String = manager.cameraIdList[0]
            val permission = ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA)

            if(permission != PackageManager.PERMISSION_GRANTED){
                requestCameraPermission()
                return
            }
            //カメラ起動
            manager.openCamera(cameraId,stateCallback,null)
        } catch (e:Exception){
            e.printStackTrace()
        }
    }

    //カメラを起動する時に接続できているかを確認するCallback
    private val stateCallback = object : CameraDevice.StateCallback(){
        //カメラ接続完了
        override fun onOpened(camera: CameraDevice) {
            this@MainActivity.cameraDevice = camera
            createCameraPreviewSession()
        }

        //カメラ切断
        override fun onDisconnected(camera: CameraDevice) {
            camera.close()
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
            previewRequestBuilder.addTarget(surface)

            //カメラのデータをTextureViewにプレビューしている
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
                            Log.e("erfs", e.toString())

                        }

                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {

                    }

                },null)
        }catch (e:CameraAccessException){
            Log.e("erfs", e.toString())

        }

    }

    //カメラ利用許可のダイヤログを表示
    private fun requestCameraPermission() {
        if(shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
            AlertDialog.Builder(baseContext)
                    .setMessage("Permission Check")
                    .setPositiveButton(android.R.string.ok){_, _ -> //dialog,which
                        requestPermissions(arrayOf(android.Manifest.permission.CAMERA),200)
                    }
                    .setNegativeButton(android.R.string.cancel){_,_ ->
                        finish()
                    }
        }else{
            requestPermissions(arrayOf(android.Manifest.permission.CAMERA),200)

        }

    }

    private val surfaceTextureListener = object : TextureView.SurfaceTextureListener{
        //TextureViewが有効になった
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

    private fun startBackgroundThread() {
        backgroundThread = HandlerThread("CameraBackground").also { it.start() }
        backgroundHandler = Handler(backgroundThread?.looper)

    }

    private fun requestStoragePermission() {
        //書き込み
        if(shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
            AlertDialog.Builder(baseContext)
                    .setMessage("Permission Check")
                    .setPositiveButton(android.R.string.ok){ _, _ ->
                        requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_STORAGE)
                    }
                    .setNegativeButton(android.R.string.cancel){ _, _ ->
                        finish()
                    }
                    .create()
        }else{
            requestPermissions(arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE), PERMISSION_WRITE_STORAGE)
        }

        //読み込み
        if(shouldShowRequestPermissionRationale(android.Manifest.permission.READ_EXTERNAL_STORAGE)){
            AlertDialog.Builder(baseContext)
                    .setMessage("Permission Check")
                    .setPositiveButton(android.R.string.ok){ _, _ ->
                        requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_READ_STORAGE)
                    }
                    .setNegativeButton(android.R.string.cancel){ _, _ ->
                        finish()
                    }
                    .create()
        }else{
            requestPermissions(arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), PERMISSION_READ_STORAGE)
        }

    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return super.onTouchEvent(event)
    }


}