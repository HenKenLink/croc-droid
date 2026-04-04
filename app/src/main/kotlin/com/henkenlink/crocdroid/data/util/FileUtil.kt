package com.henkenlink.crocdroid.data.util

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object FileUtil {

    /**
     * Copy a single file URI to a destination File
     */
    fun copyUriToFile(context: Context, uri: android.net.Uri, destFile: File): Boolean {
        return try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Recursively copy a DocumentTree URI to a destination directory
     */
    fun copyDirectoryToFolder(context: Context, treeUri: android.net.Uri, destDir: File): Boolean {
        val root = DocumentFile.fromTreeUri(context, treeUri) ?: return false
        return copyDocumentFileRecursive(context, root, destDir)
    }

    private fun copyDocumentFileRecursive(context: Context, source: DocumentFile, destDir: File): Boolean {
        if (source.isDirectory) {
            val newDir = File(destDir, source.name ?: "unknown_dir")
            if (!newDir.exists()) newDir.mkdirs()
            source.listFiles().forEach { child ->
                copyDocumentFileRecursive(context, child, newDir)
            }
        } else {
            val destFile = File(destDir, source.name ?: "unknown_file")
            copyUriToFile(context, source.uri, destFile)
        }
        return true
    }

    /**
     * Move (copy + delete source) files from internal cache to a Scoped Storage URI
     */
    fun copyFilesToUri(context: Context, sourceFile: File, destFolderUri: android.net.Uri): Boolean {
        return try {
            val destFolder = DocumentFile.fromTreeUri(context, destFolderUri) ?: return false
            if (sourceFile.isDirectory) {
                val newFolder = destFolder.createDirectory(sourceFile.name) ?: return false
                sourceFile.listFiles()?.forEach { child ->
                    copyRecursiveToDocument(context, child, newFolder)
                }
            } else {
                val newFile = destFolder.createFile("*/*", sourceFile.name) ?: return false
                context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                    sourceFile.inputStream().use { input ->
                        input.copyTo(output)
                    }
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    private fun copyRecursiveToDocument(context: Context, source: File, destFolder: DocumentFile) {
        if (source.isDirectory) {
            val newFolder = destFolder.createDirectory(source.name) ?: return
            source.listFiles()?.forEach { child ->
                copyRecursiveToDocument(context, child, newFolder)
            }
        } else {
            val newFile = destFolder.createFile("*/*", source.name) ?: return
            context.contentResolver.openOutputStream(newFile.uri)?.use { output ->
                source.inputStream().use { input ->
                    input.copyTo(output)
                }
            }
        }
    }

    fun formatSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$bytes B"
        }
    }
}
