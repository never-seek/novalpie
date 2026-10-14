package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.*
import java.io.File
import java.io.IOException
import java.net.URLDecoder
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipFile

internal object EpubResourceSupport {
    private val token=Regex("\\[\\[epub-node:([a-f0-9]{32}):([a-f0-9]{32})]]")
    fun digest(value:String)=MessageDigest.getInstance("SHA-256").digest(value.toByteArray()).hex()
    fun digest(file:File):String {
        val md=MessageDigest.getInstance("SHA-256");val buffer=ByteArray(64*1024)
        file.inputStream().use{input->while(true){val n=input.read(buffer);if(n<0)break;if(n>0)md.update(buffer,0,n)}}
        return md.digest().hex()
    }
    fun capture(source:UploadFileSource,directory:File?):Pair<String,File> {
        val parent=directory?.canonicalFile ?: File(System.getProperty("java.io.tmpdir") ?: throw IOException("没有可用的EPUB临时目录")).canonicalFile
        if(!parent.isDirectory&&!parent.mkdirs())throw IOException("无法创建EPUB资源保存目录")
        val id=UUID.randomUUID().toString().replace("-","")
        val root=File(parent,"epub-$id").canonicalFile
        require(root.parentFile==parent);if(!root.mkdir())throw IOException("无法创建本次EPUB导入目录")
        val file=File(root,"source.epub")
        try {
            source.openStream().use{input->file.outputStream().use{out->
                val buffer=ByteArray(64*1024);while(true){val n=input.read(buffer);if(n<0)break;if(n>0)out.write(buffer,0,n)};out.fd.sync()
            }}
            if(file.length()==0L)throw IOException("EPUB文件为空")
            return id to file
        }catch(error:Throwable){discard(id,file);throw error}
    }
    fun discard(id:String,file:File) {
        require(id.matches(Regex("[a-f0-9]{32}")))
        val root=file.canonicalFile.parentFile!!
        require(file.name=="source.epub"&&root.name=="epub-$id")
        root.listFiles().orEmpty().filter {it.isFile&&(it.name=="source.epub"||it.name.matches(Regex("(?:chapter|node)-[a-f0-9]{32}\\.xhtml")))}.forEach(File::delete)
        root.delete() // Never recursively remove unexpected/user-owned files.
    }
    fun validate(bundle:EpubDocumentBundle):File {
        require(bundle.id.matches(Regex("[a-f0-9]{32}")))
        val file=File(bundle.archivePath).canonicalFile
        require(file.name=="source.epub"&&file.parentFile!!.name=="epub-${bundle.id}"){"EPUB资源路径无效"}
        if(!file.isFile||file.length()!=bundle.archiveBytes||digest(file)!=bundle.archiveSha256)
            throw IOException("EPUB原始资源包缺失或已改变；未导出缺图成品，编辑内容保留")
        return file
    }
    fun ownedMarkup(bundle:EpubDocumentBundle,name:String):File {
        require(name.matches(Regex("(?:chapter|node)-[a-f0-9]{32}\\.xhtml")))
        val root=File(bundle.archivePath).canonicalFile.parentFile!!
        require(root.name=="epub-${bundle.id}")
        return File(root,name).canonicalFile.also {require(it.parentFile==root)}
    }
    fun readMarkup(bundle:EpubDocumentBundle,name:String,sha256:String):String {
        val file=ownedMarkup(bundle,name)
        if(!file.isFile||sha256.isBlank()||digest(file)!=sha256)throw IOException("EPUB章节/插图结构文件缺失或校验失败；原稿保留，未生成损坏成品")
        return file.readText(Charsets.UTF_8)
    }
    fun writeMarkup(file:File,name:String,text:String):String {
        require(name.matches(Regex("(?:chapter|node)-[a-f0-9]{32}\\.xhtml")))
        File(file.parentFile,name).writeText(text,Charsets.UTF_8);return name
    }
    fun opaqueEditableMarkup(html:String,path:String,bundleId:String,archive:File,nodes:MutableList<EpubOpaqueNode>):String {
        // Non-text content stays opaque in the plain editor and can be moved/deleted as a marker.
        val paired=Regex("<(svg|math|audio|video|object|iframe)\\b[^>]*>[\\s\\S]*?</\\1\\s*>",RegexOption.IGNORE_CASE)
        val singles=Regex("<(?:img|image)\\b(?:[^>\"']|\"[^\"]*\"|'[^']*')*/?>",RegexOption.IGNORE_CASE)
        fun preserve(markup:String):String {
            val key=digest(path+"\n"+markup).take(32)
            if(nodes.none{it.id==key})nodes+=EpubOpaqueNode(key,writeMarkup(archive,"node-$key.xhtml",markup),path,digest(markup))
            return "[[epub-node:$bundleId:$key]]"
        }
        val pairedProtected=paired.replace(html){preserve(it.value)}
        return singles.replace(pairedProtected){preserve(it.value)}
    }
    fun expandEditedContent(content:String,bundles:List<EpubDocumentBundle>,relativeFrom:String):String {
        var cursor=0
        return buildString {
            token.findAll(content).forEach {match->
                append(plainXhtml(content.substring(cursor,match.range.first)))
                val bundle=bundles.singleOrNull {it.id==match.groupValues[1]} ?: throw IOException("正文中的EPUB资源标记没有对应原始包；编辑内容保留")
                val node=bundle.opaqueNodes.singleOrNull{it.id==match.groupValues[2]} ?: throw IOException("EPUB资源标记不存在；未生成缺图成品")
                val markup=readMarkup(bundle,node.markupFile,node.markupSha256)
                append(rewriteLinks(markup,node.sourcePath,bundle,relativeFrom,emptyMap()))
                cursor=match.range.last+1
            }
            append(plainXhtml(content.substring(cursor)))
        }
    }
    fun plainXhtml(text:String):String=text.replace("\r","").split(Regex("\\n\\s*\\n")).filter{it.isNotBlank()}
        .joinToString("\n"){"<p>${xml(it).replace("\n","<br/>")}</p>"}
    fun rewriteLinks(markup:String,sourcePath:String,bundle:EpubDocumentBundle,outputPath:String,chapters:Map<String,String>):String {
        val attr=Regex("""(?<![\w:-])(src|href|xlink:href|data|poster)\s*=\s*(["'])(.*?)\2""",setOf(RegexOption.IGNORE_CASE,RegexOption.DOT_MATCHES_ALL))
        return attr.replace(markup){m->
            val raw=decodeEntities(m.groupValues[3]);val part=raw.substringBefore('#').substringBefore('?')
            if(part.isBlank()||Regex("^[A-Za-z][A-Za-z0-9+.-]*:").containsMatchIn(part))m.value
            else {
                val target=resolve(sourcePath.substringBeforeLast('/',""),decodePath(part))
                val copied=chapters[target] ?: if(bundle.resources.any{it.path==target})resourceOutputPath(bundle,target)
                    else throw IOException("EPUB引用的本地资源不存在：$target；未生成丢图/断链成品")
                val suffix=raw.substring(part.length)
                m.groupValues[1]+"="+m.groupValues[2]+xml(relative(outputPath.substringBeforeLast('/',""),copied)+suffix)+m.groupValues[2]
            }
        }
    }
    fun resourceOutputPath(bundle:EpubDocumentBundle,path:String)="OEBPS/resources/${bundle.id}/${normalize(path)}"
    fun normalize(path:String):String {
        val result=mutableListOf<String>()
        path.replace('\\','/').split('/').forEach {p->when(p){"","."->Unit;".."->if(result.isEmpty())throw IOException("EPUB资源路径越出原始包")else result.removeAt(result.lastIndex);else->result+=p}}
        if(result.isEmpty())throw IOException("EPUB资源路径为空")
        return result.joinToString("/")
    }
    fun resolve(base:String,value:String)=normalize(if(value.startsWith('/'))value else listOf(base,value).filter{it.isNotBlank()}.joinToString("/"))
    fun decodePath(value:String)=try {URLDecoder.decode(value.replace("+","%2B"),"UTF-8")}catch(e:Exception){throw IOException("EPUB资源路径编码无效",e)}
    fun relative(base:String,target:String):String {
        val b=base.split('/').filter{it.isNotBlank()};val t=target.split('/').filter{it.isNotBlank()};var same=0
        while(same<minOf(b.size,t.size)&&b[same]==t[same])same++
        return (List(b.size-same){".."}+t.drop(same)).joinToString("/")
    }
    fun xml(v:String)=v.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;").replace("\"","&quot;").replace("'","&apos;")
    private fun decodeEntities(v:String)=com.novalpie.nativeapp.feature.reader.text.DerivedTextPipeline.decodeEntities(v)
    private fun ByteArray.hex()=joinToString(""){"%02x".format(it)}
}
