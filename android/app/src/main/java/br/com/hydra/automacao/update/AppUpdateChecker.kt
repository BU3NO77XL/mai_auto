package br.com.hydra.automacao.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject

/**
 * Verifica o version.json no R2, baixa o APK novo e dispara a instalação.
 * Uso: AppUpdateChecker.check(activity) no onCreate/onResume.
 */
object AppUpdateChecker {

    const val VERSION_URL = "https://pub-5311c34a59bf43819f0ed00a3e278737.r2.dev/version.json"

    data class RemoteVersion(
        val version: String,
        val build: Int,
        val url: String,
        val sha256: String,
    )

    sealed interface CheckResult {
        data class UpdateAvailable(val remote: RemoteVersion) : CheckResult
        data object UpToDate : CheckResult
        data class Failed(val reason: String) : CheckResult
    }

    fun fetchRemoteVersion(): RemoteVersion? {
        val conn = (URL(VERSION_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty("Cache-Control", "no-cache")
        }
        return try {
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().readText()
            val json = JSONObject(body)
            RemoteVersion(
                version = json.getString("version"),
                build = json.getInt("build"),
                url = json.getString("url"),
                sha256 = json.optString("sha256", ""),
            )
        } catch (_: Exception) {
            null
        } finally {
            conn.disconnect()
        }
    }

    fun check(context: Context, callback: (CheckResult) -> Unit) {
        Thread {
            val main = Handler(Looper.getMainLooper())
            val current = currentBuild(context)
            val remote = fetchRemoteVersion()
            main.post {
                when {
                    remote == null -> callback(CheckResult.Failed("Não foi possível verificar atualizações."))
                    remote.build > current -> callback(CheckResult.UpdateAvailable(remote))
                    else -> callback(CheckResult.UpToDate)
                }
            }
        }.start()
    }

    fun currentBuild(context: Context): Int {
        return try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).let { info ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.longVersionCode.toInt()
                else @Suppress("DEPRECATION") info.versionCode
            }
        } catch (_: Exception) {
            0
        }
    }

    /** Baixa o APK via DownloadManager e instala ao concluir. Retorna o ID do download. */
    @Volatile
    private var activeDownloadId: Long? = null

    @Volatile
    private var activeReceiver: BroadcastReceiver? = null

    fun downloadAndInstall(context: Context, remote: RemoteVersion): Long? {
        // Android 8+: sem "fontes desconhecidas" o instalador falha com
        // "problema ao analisar o pacote". Redireciona para a tela de permissão.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            return null
        }
        // Single-flight: cancela download anterior ainda pendente e remove o
        // receiver antigo — sem isso, cada toque em "Atualizar" empilha um
        // receiver e todos disparam installApk juntos ao concluir.
        val dm = context.getSystemService(DownloadManager::class.java)
        activeReceiver?.let {
            try {
                context.unregisterReceiver(it)
            } catch (_: Exception) {
            }
            activeReceiver = null
        }
        activeDownloadId?.let {
            try {
                dm.remove(it)
            } catch (_: Exception) {
            }
            activeDownloadId = null
        }
        // Nome único por build: evita que dois downloads concorram pelo mesmo
        // arquivo mai-0.2.0.apk e o instalador pegue um arquivo parcial.
        val fileName = "mai-${remote.version}-${remote.build}.apk"
        val request = DownloadManager.Request(Uri.parse(remote.url)).apply {
            setTitle("Mai v${remote.version}")
            setDescription("Baixando atualização…")
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setMimeType("application/vnd.android.package-archive")
            setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
        }
        val downloadId = dm.enqueue(request)
        activeDownloadId = downloadId

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
                if (id != downloadId) return
                activeReceiver = null
                activeDownloadId = null
                try {
                    c.unregisterReceiver(this)
                } catch (_: Exception) {
                }
                if (isDownloadSuccessful(c, downloadId)) {
                    installApk(c, downloadId)
                }
            }
        }
        activeReceiver = receiver
        context.registerReceiver(receiver, IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE))
        return downloadId
    }

    private fun isDownloadSuccessful(context: Context, downloadId: Long): Boolean {
        val dm = context.getSystemService(DownloadManager::class.java)
        val cursor: Cursor = dm.query(DownloadManager.Query().setFilterById(downloadId))
        return cursor.use {
            if (!it.moveToFirst()) return false
            val status = it.getInt(it.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))
            status == DownloadManager.STATUS_SUCCESSFUL
        }
    }

    private fun installApk(context: Context, downloadId: Long) {
        // A permissão pode ter sido revogada entre o download e a instalação.
        // Recheca aqui em vez de deixar o instalador falhar com erro genérico.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            !context.packageManager.canRequestPackageInstalls()
        ) {
            val intent = Intent(
                android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                Uri.parse("package:${context.packageName}"),
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            return
        }
        val dm = context.getSystemService(DownloadManager::class.java)
        // URI content://downloads/all_downloads/<id> — o instalador lê direto
        // do DownloadManager, sem depender de caminho de arquivo scoped storage.
        val uri = dm.getUriForDownloadedFile(downloadId) ?: return
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(intent)
    }
}
