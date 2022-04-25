package com.banuba.sdk.example.quickstart_c_api

import android.util.Log
import java.io.*
import java.util.zip.ZipFile
import android.content.res.AssetManager

class ResourcesExtractor {
    companion object {
        private val TAG = "ResourcesExtractor"

        fun prepare(assets: AssetManager, resPath: String) {
            prepareResources(assets, resPath);
            prepareEffects(assets, resPath);
        }

        private fun prepareResources(assets: AssetManager, resPath: String) = try {
            val zipFile = File("$resPath.zip")

            /* copy zip file from assets, if zip file do not exist */
            if (!zipFile.exists()) {
                val asset = assets.open("bnb-resources.zip")
                val fout = zipFile.outputStream()
                asset.copyTo(fout)
                fout.close()
                asset.close()
            }

            val destDir = File("$resPath")
            /* exstract all files in zip to destDir, if destDir do not exist */
            if (!destDir.exists()) {
                destDir.mkdirs()
                unzip(destDir, zipFile);
            } else {
            }
        } catch (e: IOException) {
            Log.e(TAG, "prepareResources", e)
        }

        private fun unzip(destDir: File, zipFile: File) = try {
            ZipFile(zipFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val targetFile = File(destDir, entry.name)
                    if (entry.isDirectory) {
                        targetFile.mkdirs()
                    } else {
                        zip.getInputStream(entry).use { input ->
                            targetFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.w(TAG, "unzip error", e)
        }

        private fun prepareEffects(assets: AssetManager, resPath: String) = try {
            val resDir = File(resPath, "effects")
            if (!resDir.exists()) {
                resDir.mkdirs()
                copyAssets(assets, File(resPath), "", listOf("effects"))
            } else {
            }
        } catch (e: IOException) {
            Log.e(TAG, "prepareEffects", e)
        }

        private fun copyAssets(assets: AssetManager, baseFolder: File, path: String, assetsToCopy: List<String?>) {
            val fileList = assets.list(path)
            if (fileList?.isEmpty() == true) {
                val file = File(baseFolder, path)
                val parent = file.parentFile
                if (!parent.exists() && !file.parentFile.mkdirs()) {
                    throw IOException(
                        "Failed to create $parent. Check if you have `write` permissions"
                    )
                }
                assets.open(path).use { inputStream ->
                    FileOutputStream(file).use { outputStream ->
                        processStreams(inputStream, outputStream)
                    }
                }
            } else {
                if (fileList != null) {
                    for (children in fileList) {
                        if (assetsToCopy.contains(children)) {
                            val fullPath = File(path, children).path
                            assets.list(fullPath)?.let {
                                copyAssets(assets, baseFolder, fullPath, it.toList())
                            }
                        }
                    }
                }
            }
        }

        @Throws(IOException::class)
        private fun processStreams(inputStream: InputStream, outputStream: OutputStream) {
            BufferedInputStream(inputStream).use { `in` ->
                BufferedOutputStream(outputStream).use { out ->
                    val buffer = ByteArray(1024)
                    var bytesRead: Int
                    while (`in`.read(buffer).also { bytesRead = it } >= 0) {
                        out.write(buffer, 0, bytesRead)
                    }
                }
            }
        }
    }
}
