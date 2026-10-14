package com.novalpie.nativeapp.feature.download

import android.content.Context
import com.novalpie.nativeapp.data.OwnedAtomicFile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID

internal data class PublicationFingerprint(val bytes:Long,val sha256:String)
internal enum class PublicationKind { MediaStore, ExternalFile, LocalFile }
internal enum class PublicationMilestone { IntentSaved, RowInserted, DestinationBound, BytesWritten, DestinationCommitted, ReceiptSaved }
internal data class DownloadPublicationRecord(
    val taskId:String,val accountId:Long,val bookId:Long,val format:DownloadFormat,val createdAt:Long,
    val ruleHash:String,val nonce:String,val name:String,val reservationName:String,
    val bytes:Long,val sha256:String,val kind:PublicationKind,val uri:String?=null,val published:Boolean=false,
) {
    override fun toString()="DownloadPublicationRecord(task=$taskId,kind=$kind,published=$published)"
    fun belongsTo(task:DownloadTask)=taskId==task.id&&accountId==task.accountId&&bookId==task.bookId&&format==task.format&&
        createdAt==task.createdAt&&ruleHash==publicationRuleHash(task)
}
internal fun publicationRuleHash(task:DownloadTask)=MessageDigest.getInstance("SHA-256")
    .digest(("${task.applyReplacement}:"+task.replacementSnapshot.orEmpty()).toByteArray()).joinToString(""){"%02x".format(it)}
internal suspend fun publicationFileDigest(file:File,paused:suspend()->Unit={}):String=
    file.inputStream().use {publicationStreamDigest(it,paused)}
internal suspend fun publicationStreamDigest(input:java.io.InputStream,paused:suspend()->Unit={}):String {
    val digest=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(64*1024)
    while(true){currentCoroutineContext().ensureActive();paused();val n=input.read(buffer);if(n<0)break;if(n>0)digest.update(buffer,0,n)}
    return digest.digest().joinToString(""){"%02x".format(it)}
}

/** Saved before creating a destination. Corrupt/foreign journals fail closed, never trigger a second publication. */
internal class DownloadPublicationJournal(context:Context,taskId:String,private val suffix:String="publisher") {
    val root=File(context.noBackupFilesDir,"download-publications").canonicalFile
    val file:File
    init {
        require(taskId.matches(Regex("[A-Za-z0-9][A-Za-z0-9_-]{0,95}")))
        require(suffix in setOf("publisher","runner"))
        require(root.parentFile==context.noBackupFilesDir.canonicalFile)
        file=File(root,"$taskId.$suffix.json")
    }
    fun readJson():JSONObject? {
        if(!file.exists()&&!File(file.path+".bak").exists())return null
        return try {
            val input=OwnedAtomicFile(file).openRead()
            input.use {require(it.channel.size()<=MAX_JSON_BYTES){"下载发布记录过大"};JSONObject(it.bufferedReader().readText())}
        } catch(error:Exception){throw IOException("下载发布记录无法确认，原文件已保留；不会重复发布",error)}
    }
    fun saveJson(data:JSONObject) {
        val bytes=data.toString().toByteArray(Charsets.UTF_8)
        require(bytes.size<=MAX_JSON_BYTES){"下载发布记录过大"}
        val atomic=OwnedAtomicFile(file);val out=atomic.startWrite()
        try {out.write(bytes);atomic.finishWrite(out)}
        catch(error:Throwable){runCatching{atomic.failWrite(out)};throw error}
    }
    fun validateJsonSize(data:JSONObject) {
        require(data.toString().toByteArray(Charsets.UTF_8).size<=MAX_JSON_BYTES){"下载发布记录过大，尚未保存到目标目录"}
    }
    companion object {
        // 100k chapter decisions need roughly 22MiB. Writer and recovery use the SAME bound.
        private const val MAX_JSON_BYTES=32*1024*1024
    }
    fun read(task:DownloadTask):DownloadPublicationRecord? {
        val j=readJson() ?: return null
        return try {
            require(j.getInt("schema")==1)
            val p=DownloadPublicationRecord(j.getString("task"),j.getLong("account"),j.getLong("book"),
                DownloadFormat.valueOf(j.getString("format")),j.getLong("created"),j.getString("rules"),
                j.getString("nonce"),j.getString("name"),j.getString("reservation"),j.getLong("bytes"),
                j.getString("sha256"),PublicationKind.valueOf(j.getString("kind")),j.optString("uri").takeIf{it.isNotBlank()},j.getBoolean("published"))
            require(p.belongsTo(task));require(p.nonce.matches(Regex("[a-f0-9]{32}")))
            require(p.sha256.matches(Regex("[a-f0-9]{64}"))&&p.bytes>0)
            requireSafePublicationName(p.name);requireSafePublicationName(p.reservationName)
            p
        } catch(error:Exception){throw IOException("下载发布记录与本任务不一致，原文件已保留；不会认领其他文件",error)}
    }
    fun save(p:DownloadPublicationRecord) {saveJson(JSONObject().put("schema",1).put("task",p.taskId).put("account",p.accountId)
        .put("book",p.bookId).put("format",p.format.name).put("created",p.createdAt).put("rules",p.ruleHash)
        .put("nonce",p.nonce).put("name",p.name).put("reservation",p.reservationName).put("bytes",p.bytes)
        .put("sha256",p.sha256).put("kind",p.kind.name).put("uri",p.uri.orEmpty()).put("published",p.published))}
    suspend fun prepare(task:DownloadTask,file:File,kind:PublicationKind,paused:suspend()->Unit,nameOverride:String?=null,verified:PublicationFingerprint?=null):DownloadPublicationRecord {
        read(task)?.let {record->
            if(file.isFile&&(record.bytes!=file.length()||record.sha256!=(verified?.sha256 ?: publicationFileDigest(file,paused))))
                throw IOException("下载结果与发布检查点不同，原文件已保留；不会覆盖或重复发布")
            return record
        }
        require(file.isFile&&file.length()>0){"下载结果为空"}
        if(verified!=null)require(verified.bytes==file.length()&&verified.sha256.matches(Regex("[a-f0-9]{64}"))){"打包文件校验指纹无效"}
        val nonce=UUID.randomUUID().toString().replace("-","")
        val extension=if(task.format==DownloadFormat.Epub)"epub"else "txt"
        val safe=task.title.replace(Regex("[^A-Za-z0-9\\p{L}\\p{N}._-]"),"_").trim {it=='_'||it=='.'}.ifBlank{"novalpie"}
        val title=buildString {for(c in safe){if((toString()+c).toByteArray().size>120)break;append(c)}}.trimEnd {it.isHighSurrogate()}
        val name=nameOverride ?: "${title}_${task.bookId}_${task.createdAt}_$nonce.$extension"
        val reservation="novalpie-${task.id}-$nonce.pending.$extension"
        requireSafePublicationName(name);requireSafePublicationName(reservation)
        return DownloadPublicationRecord(task.id,task.accountId,task.bookId,task.format,task.createdAt,
            publicationRuleHash(task),nonce,name,reservation,file.length(),verified?.sha256 ?: publicationFileDigest(file,paused),kind).also(::save)
    }
}
internal fun requireSafePublicationName(name:String) {
    require(name.isNotBlank()&&name!="."&&name!=".."&&name.none{it=='/'||it=='\\'||it=='\u0000'}&&name.toByteArray().size<=240){"下载文件名无效"}
}

/** One in-process publication at a time; no historical URL/task mutexes are retained. */
internal object DownloadPublicationLock {
    private val mutex=Mutex()
    suspend fun <T> withLock(action:suspend()->T):T=mutex.withLock {action()}
}
