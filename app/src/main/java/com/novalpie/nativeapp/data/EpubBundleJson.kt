package com.novalpie.nativeapp.data
import com.novalpie.nativeapp.model.*
import org.json.JSONArray
import org.json.JSONObject

/** Import provenance is private archive metadata, not a website upload field or credential. */
internal object EpubBundleJson {
 fun encode(values:List<EpubDocumentBundle>)=JSONArray().apply {values.forEach {b->put(JSONObject()
  .put("id",b.id).put("archive",b.archivePath).put("bytes",b.archiveBytes).put("sha256",b.archiveSha256).put("package",b.packagePath)
  .put("resources",JSONArray().apply{b.resources.forEach{r->put(JSONObject().put("path",r.path).put("entry",r.entryName).put("mime",r.mediaType).put("properties",r.properties))}})
  .put("nodes",JSONArray().apply{b.opaqueNodes.forEach{n->put(JSONObject().put("id",n.id).put("file",n.markupFile).put("source",n.sourcePath).put("sha",n.markupSha256))}})
  .put("origins",JSONArray().apply{b.origins.forEach{o->put(JSONObject().put("id",o.id).put("title",o.title).put("path",o.sourcePath)
     .put("file",o.markupFile).put("textSha",o.contentSha256).put("depth",o.hierarchyLevel).put("parents",JSONArray(o.sectionPath)).put("sha",o.markupSha256))}}))}}
 fun decode(array:JSONArray?):List<EpubDocumentBundle> {
  if(array==null)return emptyList()
  return (0 until array.length()).map {index->val b=array.getJSONObject(index)
   val resources=b.getJSONArray("resources").let {a->(0 until a.length()).map {i->val r=a.getJSONObject(i);EpubResourceDescriptor(r.getString("path"),r.getString("entry"),r.getString("mime"),r.optString("properties"))}}
   val nodes=b.getJSONArray("nodes").let {a->(0 until a.length()).map {i->val n=a.getJSONObject(i);EpubOpaqueNode(n.getString("id"),n.getString("file"),n.getString("source"),n.optString("sha"))}}
   val origins=b.getJSONArray("origins").let {a->(0 until a.length()).map {i->val o=a.getJSONObject(i);EpubChapterOrigin(o.getString("id"),o.getString("title"),o.getString("path"),o.getString("file"),o.getString("textSha"),o.optInt("depth"),o.getJSONArray("parents").let{p->(0 until p.length()).map{p.getString(it)}},o.optString("sha"))}}
   EpubDocumentBundle(b.getString("id"),b.getString("archive"),b.getLong("bytes"),b.getString("sha256"),b.getString("package"),resources,nodes,origins)
  }
 }
}
