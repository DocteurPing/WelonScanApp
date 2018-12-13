package com.example.drping.welonscanapp

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.AsyncTask
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
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.json.GoogleJsonResponseException
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.AnnotateImageRequest
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest
import com.google.api.services.vision.v1.model.Feature
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.*
import kotlin.math.round

class MainActivity : AppCompatActivity() {
    lateinit var imageView: ImageView
    lateinit var textView: TextView
    var responseText = "test"
    var CLOUD_VISION_API_KEY = "AIzaSyD974zlD8-dlbxUF5w2Et1sD5f4unVOHLU"


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
        val filename = "Image-$n.jpg"
        val file = File(myDir, filename)
        if (file.exists())
            file.delete()
        try {
            val out = FileOutputStream(file)
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
            out.flush()
            out.close()
            Log.d("ok", "ok noice $root $filename")

        } catch (e: Exception) {
            e.printStackTrace()
        }
        uploadImage(finalBitmap)
    }

    private fun uploadImage(bitmap: Bitmap) {
        val base64EncodedImage = com.google.api.services.vision.v1.model.Image()
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        base64EncodedImage.encodeContent(imageBytes)
        class MyTask() : AsyncTask<Any, Void, String>() {

            override fun doInBackground(vararg params: Any): String {
                try {

                    val httpTransport = AndroidHttp.newCompatibleTransport()
                    val jsonFactory = GsonFactory.getDefaultInstance()

                    val requestInitializer = VisionRequestInitializer(CLOUD_VISION_API_KEY)

                    val builder = Vision.Builder(httpTransport, jsonFactory, null)
                    builder.setVisionRequestInitializer(requestInitializer)

                    val feature = Feature()
                    feature.type = "LABEL_DETECTION"
                    feature.maxResults = 10
                    val featureList : List<Feature> = listOf(feature)
                    val vision = builder.build()
                    val annotateImageReq = AnnotateImageRequest()
                    annotateImageReq.features = featureList
                    annotateImageReq.image = base64EncodedImage
                    val annotateImageRequests = listOf(annotateImageReq)

                    val batchAnnotateImagesRequest = BatchAnnotateImagesRequest()
                    batchAnnotateImagesRequest.requests = annotateImageRequests

                    val annotateRequest = vision.images().annotate(batchAnnotateImagesRequest)
                    annotateRequest.disableGZipContent = true
                    val response = annotateRequest.execute()

                    return response.toString()
                } catch (e: GoogleJsonResponseException) {
                    Log.d("ok", "failed to make API request because " + e.content)
                } catch (e: IOException) {
                    Log.d(
                        "ok",
                        "failed to make API request because of other IOException $e"
                    )
                }

                return "Cloud Vision API request failed. Check logs for details."
            }

            @SuppressLint("SetTextI18n")
            override fun onPostExecute(result: String) {
                val json = JSONObject(result)
                lateinit var textToDisplay : String
                for (i in 0..(json.getJSONArray("responses").getJSONObject(0).getJSONArray("labelAnnotations").length() - 1)) {
                    val labelAnnotation = json.getJSONArray("responses").getJSONObject(0).getJSONArray("labelAnnotations").getJSONObject(i).getString("description")
                    var percent = json.getJSONArray("responses").getJSONObject(0).getJSONArray("labelAnnotations").getJSONObject(i).getString("score").toDouble()
                    percent *= 100
                    percent = round(percent * 100) / 100
                    Log.d("Response", labelAnnotation.toString())
                    textToDisplay = if (i == 0) {
                        labelAnnotation.toString() + " " + percent + "%\n"
                    } else {
                        textToDisplay + labelAnnotation.toString() + " " + percent + "%\n"
                    }
                }
                textView.text = textToDisplay

            }
        }
        MyTask().execute()
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
