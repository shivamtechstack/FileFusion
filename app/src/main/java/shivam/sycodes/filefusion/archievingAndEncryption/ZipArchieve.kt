package shivam.sycodes.filefusion.archievingAndEncryption

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ZipArchieve {

    fun zipFileorFolder(
        filesOrFolders: List<File>,
        outputZipPath: String,
        selectedCompression: String
    ) {
        val zipFile = File(outputZipPath)

        ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
            filesOrFolders.forEach { fileOrFolder ->
                val compressionLevel = when (selectedCompression) {
                    "No Compression" -> Deflater.NO_COMPRESSION
                    "Very Fast" -> 1
                    "Fast" -> 3
                    "Balanced" -> Deflater.DEFAULT_COMPRESSION
                    "High" -> 7
                    "Maximum Compression" -> Deflater.BEST_COMPRESSION
                    else -> Deflater.DEFAULT_COMPRESSION
                }

                zos.setLevel(compressionLevel)
                zipFileOrFolderRecursive(fileOrFolder, fileOrFolder.name, zos)
            }
        }
    }

    private fun zipFileOrFolderRecursive(fileOrFolder: File, parentDirPath: String, zos: ZipOutputStream) {
        if (fileOrFolder.isDirectory) {

            val folderName = "$parentDirPath/"
            zos.putNextEntry(ZipEntry(folderName))
            zos.closeEntry()

            fileOrFolder.listFiles()?.forEach { file ->
                zipFileOrFolderRecursive(file, "$parentDirPath/${file.name}", zos)
            }
        } else {
            FileInputStream(fileOrFolder).use { fis ->
                val zipEntry = ZipEntry(parentDirPath)
                zos.putNextEntry(zipEntry)
                fis.copyTo(zos, 1024)
                zos.closeEntry()
            }
        }
    }
    fun unZipFile(zipFile: File, outputDirPath: String) {
        val outputDir = File(outputDirPath)

        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var zipEntry: ZipEntry?

            while (zis.nextEntry.also { zipEntry = it } != null) {
                val outputFile = File(outputDir, zipEntry!!.name)

                if (zipEntry!!.isDirectory) {
                    outputFile.mkdirs()
                } else {
                    FileOutputStream(outputFile).use { fos ->
                        zis.copyTo(fos)
                    }
                }
                zis.closeEntry()
            }
        }
    }
}