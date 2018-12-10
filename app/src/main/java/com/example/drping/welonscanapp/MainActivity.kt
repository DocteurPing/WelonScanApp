package com.example.drping.welonscanapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.squareup.okhttp.*
import java.io.File
import java.io.FileOutputStream
import java.util.*


class MainActivity : AppCompatActivity() {

    lateinit var imageView: ImageView
    lateinit var textView: TextView


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        imageView = this.findViewById(R.id.imageView)
        textView = this.findViewById(R.id.textView)
    }

    private val REQUEST_IMAGE_CAPTURE = 1

    fun dispatchTakePictureIntent(view: View) {
        Intent(MediaStore.ACTION_IMAGE_CAPTURE).also { takePictureIntent ->
            takePictureIntent.resolveActivity(packageManager)?.also {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    private fun saveImage(finalBitmap: Bitmap) {
        val root = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/saved_images")

        myDir.mkdirs()
        val random = Random()
        var n = 10000
        n = random.nextInt(n)
        val filename = "Image-$n.png"
        val file = File(myDir, filename)
        if (file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            out.flush()
            out.close()
            Log.d("ok", "ok noice $root $filename")

        } catch (e: Exception) {
            e.printStackTrace()
        }
        uploadImage(file)
    }

    private fun uploadImage(file: File) {
        //try {
            val mediaTypePNG = MediaType.parse("image/png")
            val req : RequestBody =  MultipartBuilder()
                .type(MultipartBuilder.FORM)
                .addFormDataPart("body", "image.png", RequestBody.create(mediaTypePNG, file)).build()
            val request = Request.Builder()
                .url("https://southcentralus.api.cognitive.microsoft.com/customvision/v2.0/Prediction/fe93df1e-1dc3-467f-b772-9b2ea0a9ee11/url?iterationId=8ffb8e80-8290-4792-a5d3-bdcf4de23235")
                .header("Prediction-Key", "9eee5bb5cc83426c84c2c96d581e0d7b")
                .header("Content-Type", "application/json")
                .post(req)
                .build()

            val client = OkHttpClient()
            Log.d("lel", "oktamer")
            val response = client.newCall(request).execute()
            textView.text = response.toString()
        //} catch (e: java.lang.Exception) {
        //    Log.d("lel", "no")
        //}


    }

    private fun isStoragePermissionGranted(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            return if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED
            ) {
                Log.v("error", "Permission is granted")
                true
            } else {

                Log.v("error", "Permission is revoked")
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 2)
                false
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v("error", "Permission is granted")
            return true
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            val imageBitmap = data.extras.get("data") as Bitmap
            if (isStoragePermissionGranted())
                saveImage(imageBitmap)
            imageView.setImageBitmap(imageBitmap)
        }
    }
}
