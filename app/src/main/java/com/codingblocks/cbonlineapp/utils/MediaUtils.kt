package com.codingblocks.cbonlineapp.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.util.regex.Pattern


object MediaUtils {

    //Call this when you want to play a video and pass the return type to exoplayer
    fun getCourseVideoUri(videoUrl: String, context: Context): Uri {
        val file = context.getExternalFilesDir(Environment.getDataDirectory().absolutePath)
        val dataFile = File(file, "/$videoUrl/index.m3u8")
        return Uri.parse(dataFile.toURI().toString())
    }

    fun getCourseDownloadUrls(videoUrl: String, context: Context): ArrayList<String> {
        val videoNames = arrayListOf<String>()

        val file = context.getExternalFilesDir(Environment.getDataDirectory().absolutePath)
        val dataFile = File(file, "/$videoUrl/video.m3u8")

        dataFile.forEachLine {
            Log.i("fileName", it)
            if (it.contains(".ts")) {
                Log.i("fileName", it)
                videoNames.add(it)
            }
        }        //Read the file above and add the ts names to videoNames

        return videoNames
    }

    fun getYotubeVideoId(videoUrl: String): String {
        var vId = ""
        val pattern = Pattern.compile(
                "^https?://.*(?:youtu.be/|v/|u/\\w/|embed/|watch?v=)([^#&?]*).*$",
                Pattern.CASE_INSENSITIVE)
        val matcher = pattern.matcher(videoUrl)
        if (matcher.matches()) {
            vId = matcher.group(1)
        }
        return vId
    }

}