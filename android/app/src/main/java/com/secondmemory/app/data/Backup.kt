package com.secondmemory.app.data

import android.content.Context
import android.net.Uri
import com.secondmemory.app.domain.Settings
import com.secondmemory.app.domain.Thing
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object Backup {
    private const val MAGIC = "SM1"
    private const val ITERATIONS = 120_000

    fun snapshot(things: List<Thing>, settings: Settings): JSONObject {
        val arr = JSONArray()
        things.forEach { t ->
            arr.put(
                JSONObject()
                    .put("id", t.id)
                    .put("createdAt", t.createdAt)
                    .put("updatedAt", t.updatedAt)
                    .put("originalContent", t.originalContent)
                    .put("contentType", t.contentType.name)
                    .put("sourceUrl", t.sourceUrl)
                    .put("sourceApp", t.sourceApp)
                    .put("title", t.title)
                    .put("summary", t.summary)
                    .put("notes", t.notes)
                    .put("imageUri", t.imageUri)
                    .put("mimeType", t.mimeType)
                    .put("category", t.category.name)
                    .put("status", t.status.name)
                    .put("priority", t.priority.name)
                    .put("dueAt", t.dueAt)
                    .put("isPinned", t.isPinned)
                    .put("tags", t.tags.joinToString("|"))
                    .put("checklist", t.checklist)
                    .put("pinColor", t.pinColor)
                    .put("sortOrder", t.sortOrder)
                    .put("expiresAt", t.expiresAt)
                    .put("ogImageUrl", t.ogImageUrl)
                    .put("siteName", t.siteName)
                    .put("ocrText", t.ocrText),
            )
        }
        return JSONObject()
            .put("version", 1)
            .put("exportedAt", System.currentTimeMillis())
            .put("things", arr)
            .put(
                "settings",
                JSONObject()
                    .put("appearance", settings.appearance.name)
                    .put("automaticProcessing", settings.automaticProcessing)
                    .put("lockScreenPrivate", settings.lockScreenPrivate)
                    .put("pinExpiryHours", settings.pinExpiryHours)
                    .put("isPro", settings.isPro),
            )
    }

    fun pack(context: Context, json: JSONObject, password: String?): ByteArray {
        val zip = ByteArrayOutputStream()
        ZipOutputStream(zip).use { out ->
            out.putNextEntry(ZipEntry("snapshot.json"))
            out.write(json.toString().toByteArray(Charsets.UTF_8))
            out.closeEntry()
            json.optJSONArray("things")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val path = arr.getJSONObject(i).optString("imageUri")
                    if (path.isNullOrBlank()) continue
                    val file = java.io.File(path)
                    if (!file.exists()) continue
                    out.putNextEntry(ZipEntry("captures/${file.name}"))
                    file.inputStream().use { it.copyTo(out) }
                    out.closeEntry()
                }
            }
        }
        val plain = zip.toByteArray()
        if (password.isNullOrBlank()) return plain
        return encrypt(plain, password)
    }

    fun unpack(bytes: ByteArray, password: String?): Pair<JSONObject, Map<String, ByteArray>> {
        val zipBytes = if (password.isNullOrBlank()) bytes else decrypt(bytes, password)
        val files = mutableMapOf<String, ByteArray>()
        var snapshot = JSONObject()
        ZipInputStream(zipBytes.inputStream()).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val data = zip.readBytes()
                if (entry.name == "snapshot.json") {
                    snapshot = JSONObject(String(data, Charsets.UTF_8))
                } else if (entry.name.startsWith("captures/")) {
                    files[entry.name.removePrefix("captures/")] = data
                }
                entry = zip.nextEntry
            }
        }
        return snapshot to files
    }

    private fun encrypt(plain: ByteArray, password: String): ByteArray {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
        val packed = cipher.doFinal(plain)
        return MAGIC.toByteArray() + salt + iv + packed
    }

    private fun decrypt(data: ByteArray, password: String): ByteArray {
        val magic = MAGIC.toByteArray()
        require(data.size > magic.size + 16 + 12) { "Not an encrypted backup" }
        require(data.copyOfRange(0, magic.size).contentEquals(magic)) { "Wrong passphrase or file" }
        var i = magic.size
        val salt = data.copyOfRange(i, i + 16); i += 16
        val iv = data.copyOfRange(i, i + 12); i += 12
        val key = derive(password, salt)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
        return cipher.doFinal(data, i, data.size - i)
    }

    private fun derive(password: String, salt: ByteArray): SecretKeySpec {
        val spec = PBEKeySpec(password.toCharArray(), salt, ITERATIONS, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return SecretKeySpec(bytes, "AES")
    }

    fun write(context: Context, uri: Uri, bytes: ByteArray) {
        context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
    }

    fun read(context: Context, uri: Uri): ByteArray =
        context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
}
