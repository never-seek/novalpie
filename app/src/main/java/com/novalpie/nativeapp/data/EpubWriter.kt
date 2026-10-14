package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.*
import java.io.OutputStream
import java.io.IOException
import java.io.StringWriter
import java.util.UUID
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import java.util.zip.ZipFile
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Element

/** Native editor export retains source resources, exact unchanged markup and editable media markers. */
object EpubWriter {
    private const val MIMETYPE="application/epub+zip"
    private const val OPF="http://www.idpf.org/2007/opf"
    private const val DC="http://purl.org/dc/elements/1.1/"
    private data class ChapterOutput(val chapter:UploadChapter,val path:String,val source:EpubChapterSource?,val origin:EpubChapterOrigin?)
    fun write(output:OutputStream,metadata:EditorBookMetadata,chapters:List<UploadChapter>) {
        require(metadata.title.isNotBlank()){ "书名不能为空" };require(metadata.author.isNotBlank()){ "作者不能为空" };require(chapters.isNotEmpty()){ "至少需要一个章节" }
        val bundles=(metadata.epubBundles+chapters.mapNotNull{it.epubSource?.bundle}).distinctBy{it.id}
        val files=bundles.associate {it.id to EpubResourceSupport.validate(it)}
        val resolved=chapters.mapIndexed {index,ch->
            val source=ch.epubSource ?: bundles.asSequence().flatMap {b->b.origins.asSequence().filter {o->o.title==ch.title&&o.contentSha256==EpubResourceSupport.digest(ch.content)}.map {EpubChapterSource(b,it.id)}}.singleOrNull()
                ?: bundles.asSequence().flatMap {b->b.origins.asSequence().filter {o->o.contentSha256==EpubResourceSupport.digest(ch.content)}.map {EpubChapterSource(b,it.id)}}.singleOrNull()
                ?: bundles.asSequence().flatMap {b->b.origins.asSequence().filter {o->o.title==ch.title}.map {EpubChapterSource(b,it.id)}}.singleOrNull()
            val origin=source?.bundle?.origins?.singleOrNull{it.id==source.originId}
            val path=if(origin!=null) {
                val dir=origin.sourcePath.substringBeforeLast('/',"")
                "OEBPS/resources/${source!!.bundle.id}/"+listOf(dir,"novalpie-chapter-${index+1}-${origin.id}.xhtml").filter{it.isNotBlank()}.joinToString("/")
            }else "OEBPS/chapter-${index+1}.xhtml"
            ChapterOutput(ch,path,source,origin)
        }
        val maps=bundles.associate {bundle->bundle.id to resolved.filter{it.source?.bundle?.id==bundle.id&&it.origin!=null}.groupBy{it.origin!!.sourcePath}.mapValues{it.value.first().path}}
        val resources=bundles.flatMap{bundle->bundle.resources.map{bundle to it}}
        ZipOutputStream(output.buffered()).use {zip->
            writeStoredMimetype(zip)
            writeText(zip,"META-INF/container.xml","""<?xml version="1.0" encoding="UTF-8"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
            bundles.forEach {bundle->ZipFile(files.getValue(bundle.id)).use {archive->
                bundle.resources.forEach {resource->
                    val entry=archive.getEntry(resource.entryName) ?: throw IOException("EPUB原始资源缺失：${resource.path}；未保存缺图成品")
                    val target=EpubResourceSupport.resourceOutputPath(bundle,resource.path)
                    zip.putNextEntry(ZipEntry(target))
                    var bytes=0L
                    archive.getInputStream(entry).use{input->val buffer=ByteArray(64*1024);while(true){val n=input.read(buffer);if(n<0)break;if(n>0){zip.write(buffer,0,n);bytes+=n}}}
                    if(entry.size>=0&&bytes!=entry.size)throw IOException("EPUB资源复制不完整：${resource.path}")
                    zip.closeEntry()
                }
            }}
            resolved.forEach {item->writeText(zip,item.path,chapterXhtml(metadata,item,bundles,maps))}
            writeText(zip,"OEBPS/nav.xhtml",navigationXhtml(metadata,resolved))
            writeText(zip,"OEBPS/content.opf",packageXml(metadata,resolved,resources,files))
        }
    }
    private fun chapterXhtml(metadata:EditorBookMetadata,item:ChapterOutput,bundles:List<EpubDocumentBundle>,maps:Map<String,Map<String,String>>):String {
        val source=item.source;val origin=item.origin
        if(source!=null&&origin!=null&&origin.contentSha256==EpubResourceSupport.digest(item.chapter.content)) {
            val file=EpubResourceSupport.ownedMarkup(source.bundle,origin.markupFile)
            if(!file.isFile)throw IOException("EPUB原章节格式文件缺失，原编辑内容保留")
            val raw=EpubResourceSupport.readMarkup(source.bundle,origin.markupFile,origin.markupSha256)
            val safe=EpubResourceSupport.rewriteLinks(raw,origin.sourcePath,source.bundle,item.path,maps[source.bundle.id].orEmpty())
            return safe.replace(Regex("<title\\b[^>]*>[\\s\\S]*?</title\\s*>",RegexOption.IGNORE_CASE),"<title>${xml(item.chapter.title)}</title>")
        }
        val head=if(source!=null&&origin!=null) {
            val file=EpubResourceSupport.ownedMarkup(source.bundle,origin.markupFile)
            if(!file.isFile)throw IOException("EPUB原章节资源引用文件缺失")
            val raw=EpubResourceSupport.readMarkup(source.bundle,origin.markupFile,origin.markupSha256)
            val original=Regex("<head\\b[^>]*>([\\s\\S]*?)</head\\s*>",RegexOption.IGNORE_CASE).find(raw)?.groupValues?.get(1).orEmpty()
            EpubResourceSupport.rewriteLinks(original,origin.sourcePath,source.bundle,item.path,maps[source.bundle.id].orEmpty())
                .replace(Regex("<title\\b[^>]*>[\\s\\S]*?</title\\s*>",RegexOption.IGNORE_CASE),"")
        }else ""
        val body=EpubResourceSupport.expandEditedContent(item.chapter.content,bundles,item.path)
        return """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>${xml(item.chapter.title)}</title>$head</head><body><h1>${xml(item.chapter.title)}</h1>$body</body></html>"""
    }
    private fun packageXml(metadata:EditorBookMetadata,chapters:List<ChapterOutput>,resources:List<Pair<EpubDocumentBundle,EpubResourceDescriptor>>,files:Map<String,java.io.File>):String {
        val factory=DocumentBuilderFactory.newInstance().apply{isNamespaceAware=true;isExpandEntityReferences=false
            runCatching{setFeature("http://apache.org/xml/features/disallow-doctype-decl",true)};runCatching{setFeature("http://xml.org/sax/features/external-general-entities",false)};runCatching{setFeature("http://xml.org/sax/features/external-parameter-entities",false)}}
        val doc=factory.newDocumentBuilder().newDocument();val pkg=doc.createElementNS(OPF,"package");doc.appendChild(pkg)
        pkg.setAttribute("version","3.0");pkg.setAttribute("unique-identifier","book-id")
        val sourceBundle=resources.firstOrNull()?.first ?: chapters.firstNotNullOfOrNull{it.source?.bundle}
        var original:org.w3c.dom.Document?=null
        if(sourceBundle!=null)ZipFile(files.getValue(sourceBundle.id)).use {archive->
            val e=archive.getEntry(sourceBundle.packagePath) ?: throw IOException("EPUB原元数据文件缺失")
            archive.getInputStream(e).use{original=factory.newDocumentBuilder().parse(it)}
        }
        val sourceMeta=original?.getElementsByTagNameNS("*","metadata")?.item(0)
        val meta=if(sourceMeta!=null)doc.importNode(sourceMeta,true) as Element else doc.createElementNS(OPF,"metadata")
        meta.setAttribute("xmlns:dc",DC);pkg.appendChild(meta)
        fun remove(local:String,namespace:String?=null) {
            val nodes=(0 until meta.childNodes.length).map{meta.childNodes.item(it)}.filter{it is Element&&(it.localName ?: it.nodeName.substringAfter(':'))==local&&(namespace==null||it.namespaceURI==namespace)}
            nodes.forEach(meta::removeChild)
        }
        fun field(name:String,value:String) {
            val nodes=meta.getElementsByTagNameNS(DC,name)
            val first=nodes.item(0) as? Element
            if(first!=null)first.textContent=value else meta.appendChild(doc.createElementNS(DC,"dc:$name").also{it.textContent=value})
        }
        val identifier=(meta.getElementsByTagNameNS("*","identifier").item(0) as? Element)
        if(identifier!=null)identifier.setAttribute("id","book-id") else meta.appendChild(doc.createElementNS(DC,"dc:identifier").also{it.setAttribute("id","book-id");it.textContent="urn:uuid:${UUID.randomUUID()}"})
        field("title",metadata.title);field("creator",metadata.author);field("language",metadata.language.ifBlank{"zh"});field("description",metadata.description)
        if(metadata.tags.isNotBlank()) {remove("subject",DC);metadata.tags.split(Regex("[,，\\s]+")).filter{it.isNotBlank()}.distinct().forEach{tag->meta.appendChild(doc.createElementNS(DC,"dc:subject").also{it.textContent=tag})}}
        if(metadata.sourceUrl.isNotBlank())field("source",metadata.sourceUrl)else if(metadata.source.isNotBlank())field("source",metadata.source)
        val obsolete=(0 until meta.childNodes.length).map{meta.childNodes.item(it)}.filterIsInstance<Element>().filter{it.getAttribute("property")=="dcterms:modified"||it.getAttribute("name")=="cover"}
        obsolete.forEach(meta::removeChild)
        val utc=java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'",java.util.Locale.US).apply{timeZone=java.util.TimeZone.getTimeZone("UTC")}.format(java.util.Date())
        meta.appendChild(doc.createElementNS(OPF,"meta").also{it.setAttribute("property","dcterms:modified");it.textContent=utc})
        val manifest=doc.createElementNS(OPF,"manifest");pkg.appendChild(manifest)
        fun item(id:String,path:String,mime:String,props:String="") {manifest.appendChild(doc.createElementNS(OPF,"item").also{it.setAttribute("id",id);it.setAttribute("href",EpubResourceSupport.relative("OEBPS",path));it.setAttribute("media-type",mime);if(props.isNotBlank())it.setAttribute("properties",props)})}
        item("nav","OEBPS/nav.xhtml","application/xhtml+xml","nav")
        chapters.forEachIndexed{index,ch->item("chapter-${index+1}",ch.path,"application/xhtml+xml")}
        resources.forEach{(bundle,r)->
            val id="resource-${bundle.id}-${EpubResourceSupport.digest(r.path).take(24)}"
            val props=r.properties.split(Regex("\\s+")).filter{it.isNotBlank()&&it!="nav"}.joinToString(" ")
            item(id,EpubResourceSupport.resourceOutputPath(bundle,r.path),r.mediaType,props)
            if("cover-image" in props.split(' '))meta.appendChild(doc.createElementNS(OPF,"meta").also{it.setAttribute("name","cover");it.setAttribute("content",id)})
        }
        val spine=doc.createElementNS(OPF,"spine");pkg.appendChild(spine)
        chapters.forEachIndexed{index,_->spine.appendChild(doc.createElementNS(OPF,"itemref").also{it.setAttribute("idref","chapter-${index+1}")})}
        val out=StringWriter();TransformerFactory.newInstance().newTransformer().transform(DOMSource(doc),StreamResult(out));return out.toString()
    }
    private fun navigationXhtml(metadata:EditorBookMetadata,chapters:List<ChapterOutput>):String {
        val links=chapters.joinToString(""){ch->"<li><a href=\"${xml(EpubResourceSupport.relative("OEBPS",ch.path))}\">${xml(ch.chapter.title)}</a></li>"}
        return """<?xml version="1.0" encoding="UTF-8"?><html xmlns="http://www.w3.org/1999/xhtml"><head><title>${xml(metadata.title)}</title></head><body><nav epub:type="toc" xmlns:epub="http://www.idpf.org/2007/ops"><h1>目录</h1><ol>$links</ol></nav></body></html>"""
    }
    private fun writeStoredMimetype(zip:ZipOutputStream){val bytes=MIMETYPE.toByteArray(Charsets.US_ASCII);val crc=CRC32().apply{update(bytes)}
        zip.putNextEntry(ZipEntry("mimetype").apply{method=ZipEntry.STORED;size=bytes.size.toLong();compressedSize=size;this.crc=crc.value});zip.write(bytes);zip.closeEntry()}
    private fun writeText(zip:ZipOutputStream,path:String,text:String){zip.putNextEntry(ZipEntry(path));zip.write(text.toByteArray(Charsets.UTF_8));zip.closeEntry()}
    private fun xml(value:String)=EpubResourceSupport.xml(value)
}
