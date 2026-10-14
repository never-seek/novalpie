package com.novalpie.nativeapp.model

/** Private, disk-backed import provenance. Binary resources are never inflated into Compose state. */
data class EpubResourceDescriptor(val path:String,val entryName:String,val mediaType:String,val properties:String="")
data class EpubOpaqueNode(val id:String,val markupFile:String,val sourcePath:String,val markupSha256:String="")
data class EpubChapterOrigin(val id:String,val title:String,val sourcePath:String,val markupFile:String,val contentSha256:String,
    val hierarchyLevel:Int=0,val sectionPath:List<String> = emptyList(),val markupSha256:String="")
data class EpubDocumentBundle(val id:String,val archivePath:String,val archiveBytes:Long,val archiveSha256:String,val packagePath:String,
    val resources:List<EpubResourceDescriptor> = emptyList(),val opaqueNodes:List<EpubOpaqueNode> = emptyList(),
    val origins:List<EpubChapterOrigin> = emptyList())
data class EpubChapterSource(val bundle:EpubDocumentBundle,val originId:String)
