package com.henkenlink.crocdroid.data.util

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object CompressionUtil {

    /**
     * Zips files/folders. 
     * If multiple paths are provided, they are added to the root of the zip.
     * If a folder is provided, it is added recursively with its base name as root.
     */
    fun zipFiles(files: List<File>, destination: File, onProgress: (String) -> Unit = {}) {
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { zos ->
            // Use NO_COMPRESSION (0) for speed since croc does its own compression
            zos.setLevel(0) 
            
            files.forEach { file ->
                if (file.isDirectory) {
                    zipDirectory(zos, file, file.name, onProgress)
                } else {
                    zipFile(zos, file, file.name, onProgress)
                }
            }
        }
    }

    private fun zipDirectory(zos: ZipOutputStream, folder: File, parentPath: String, onProgress: (String) -> Unit) {
        val files = folder.listFiles() ?: return
        
        // Add entry for the directory itself
        val dirEntry = ZipEntry("$parentPath/")
        zos.putNextEntry(dirEntry)
        zos.closeEntry()
        
        for (file in files) {
            val path = "$parentPath/${file.name}"
            if (file.isDirectory) {
                zipDirectory(zos, file, path, onProgress)
            } else {
                zipFile(zos, file, path, onProgress)
            }
        }
    }

    private fun zipFile(zos: ZipOutputStream, file: File, path: String, onProgress: (String) -> Unit) {
        onProgress(file.name)
        val entry = ZipEntry(path)
        zos.putNextEntry(entry)
        FileInputStream(file).use { fis ->
            fis.copyTo(zos)
        }
        zos.closeEntry()
    }
}
