package com.novalpie.nativeapp.feature.reader.pagination

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextIndent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.novalpie.nativeapp.model.ReaderContent
import com.novalpie.nativeapp.model.ReaderViewportAnchor
import com.novalpie.nativeapp.ui.ReaderChapterEntryPosition
import com.novalpie.nativeapp.ui.ReaderContentBlock
import com.novalpie.nativeapp.ui.ReaderUiOptions
import com.novalpie.nativeapp.ui.longPressOnly
import com.novalpie.nativeapp.ui.readerTextLayout
import com.novalpie.nativeapp.ui.LocalChineseVariant
import com.novalpie.nativeapp.ui.convertChineseVariantText
import com.novalpie.nativeapp.feature.reader.text.readerAnnotatedTextWithWordSpacing
import com.novalpie.nativeapp.feature.reader.text.readerSpokenTextRange
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Isolated, measured-page renderer. The parent retains navigation, comments and existing chrome. */
@Composable
internal fun NativePagedReader(
    bookId:Long,
    chapterId:Long,
    original:ReaderContent,
    derived:ReaderContent,
    options:ReaderUiOptions,
    entry:ReaderChapterEntryPosition,
    legacyAnchor:ReaderViewportAnchor?,
    fontFamily:FontFamily,
    textColor:Color,
    background:Color,
    hasPrevious:Boolean,
    hasNext:Boolean,
    onTap:(Float,Float)->Unit,
    onBoundary:(Int)->Unit,
    registerTurn:(((Int)->Unit)?)->Unit,
    onAnchor:(ReaderViewportAnchor)->Unit,
    onPreview:(ReaderContentBlock.Image,String)->Unit,
    modifier:Modifier=Modifier,
    followText:String?=null,
    highlightText:String?=null,
    backgroundImageUri:String?=null,
    comments:@Composable ()->Unit,
) {
    val context=LocalContext.current
    val density=LocalDensity.current
    val chineseVariant=LocalChineseVariant.current
    val store=remember(context){ReaderAnchorStore(context)}
    val textLayout=readerTextLayout(options)
    val measurer=rememberTextMeasurer(cacheSize=0)
    var anchor by remember(bookId,chapterId){mutableStateOf<ReaderAnchor?>(null)}
    var measured by remember(bookId,chapterId){mutableStateOf<MeasuredChapterDocument?>(null)}
    var navigator by remember(bookId,chapterId){mutableStateOf<ReaderPageNavigator?>(null)}
    var pageIndex by remember(bookId,chapterId){mutableIntStateOf(0)}
    var initialized by remember(bookId,chapterId){mutableStateOf(false)}
    var awaitingChapter by remember(bookId,chapterId){mutableStateOf(false)}
    var showComments by remember(bookId,chapterId){mutableStateOf(false)}
    var turnBusy by remember(bookId,chapterId){mutableStateOf(false)}
    var measuring by remember(bookId,chapterId){mutableStateOf(true)}
    var error by remember(bookId,chapterId){mutableStateOf<String?>(null)}
    var images by remember(bookId,chapterId){mutableStateOf(emptyMap<String,ImageDimensions>())}
    val generation=remember(bookId,chapterId){PageLayoutGeneration()}
    val revision=remember(derived){"${derived.content.hashCode()}:${derived.title.hashCode()}:${derived.illustrations.hashCode()}"}
    val latestTap by rememberUpdatedState(onTap)
    val latestBoundary by rememberUpdatedState(onBoundary)
    val latestAnchor by rememberUpdatedState(onAnchor)
    val touchSlop=LocalViewConfiguration.current.touchSlop

    fun move(direction:Int) {
        val current=measured ?: return
        if(awaitingChapter||turnBusy||measuring)return
        if(showComments) {
            if(direction<0){showComments=false;pageIndex=current.plan.pages.lastIndex;return}
        } else {
            val target=pageIndex+if(direction<0)-1 else 1
            if(target in current.plan.pages.indices) {
                val move=if(direction<0)navigator?.previous()else navigator?.next()
                if(move !is PageMove.Page)return
                pageIndex=move.index
                anchor=current.plan.pages[move.index].startAnchor
                turnBusy=options.pageTurnEffect!="none"
                return
            }
            if(direction>0&&options.showComments){showComments=true;return}
        }
        if((direction<0&&hasPrevious)||(direction>0&&hasNext)) {
            val move=if(direction<0)navigator?.previous()else navigator?.next()
            if(move !is PageMove.Chapter)return
            awaitingChapter=true
            latestBoundary(direction)
        }
    }
    val latestMove by rememberUpdatedState<(Int)->Unit>(::move)
    LaunchedEffect(options.showComments){if(!options.showComments)showComments=false}
    LaunchedEffect(pageIndex) {
        if(turnBusy)kotlinx.coroutines.delay(if(options.pageTurnEffect=="simulated")240 else 180)
        turnBusy=false
    }
    DisposableEffect(bookId,chapterId) {
        registerTurn { latestMove(it) }
        onDispose {generation.invalidate();registerTurn(null)}
    }

    BoxWithConstraints(modifier=modifier) {
        val width=with(density){maxWidth.toPx().toInt()}
        val height=with(density){maxHeight.toPx().toInt()}
        val bodyStyle=TextStyle(fontSize=textLayout.fontSizeSp.sp,lineHeight=textLayout.lineHeightSp.sp,
            fontFamily=fontFamily,fontWeight=FontWeight(textLayout.fontWeight),letterSpacing=textLayout.letterSpacingSp.sp,
            textIndent=TextIndent(firstLine=textLayout.firstLineIndentSp.sp),textAlign=TextAlign.Justify)
        val titleStyle=bodyStyle.copy(fontSize=textLayout.titleFontSizeSp.sp,lineHeight=(textLayout.titleFontSizeSp*1.4f).sp,
            textIndent=TextIndent.None,fontWeight=FontWeight.Bold,textAlign=TextAlign.Center)
        val key=PageLayoutKey(bookId,chapterId,revision,width.coerceAtLeast(1),height.coerceAtLeast(1),
            "$bodyStyle:${density.density}:${density.fontScale}:${options.emptyLine}:${options.wordSpacing}:${options.showImages}:${options.removeDuplicateLines}:$chineseVariant",images.hashCode().toString())
        LaunchedEffect(key) {
            if(width<=0||height<=0)return@LaunchedEffect
            val ticket=generation.request(key)
            measuring=true
            error=null
            try {
                val oldAnchor=anchor ?: if(!initialized&&legacyAnchor!=null)withContext(Dispatchers.IO){store.load(bookId,chapterId)}else null
                val oldDocument=measured?.document
                val result=withContext(Dispatchers.Default) {
                    val document=chapterDocumentFromContent(bookId,chapterId,original,derived,revision,options.showImages,options.removeDuplicateLines,chineseVariant,options.wordSpacing)
                    measureChapterDocument(document,key,measurer,bodyStyle,titleStyle,
                        paragraphSpacingPx=with(density){textLayout.paragraphSpacingDp.dp.toPx()},
                        headingSpacingPx=with(density){textLayout.titleBottomSpacingDp.dp.toPx()},imageDimensions=images)
                }
                if(!generation.isCurrent(ticket))return@LaunchedEffect
                val restored=oldAnchor?.let{remapReaderAnchor(it,oldDocument,result.document)} ?: legacyAnchor?.let { legacy ->
                    result.document.blocks.getOrNull(legacy.itemIndexWithinChapter)?.let { block ->
                        val text=result.textLayouts[block.id]
                        val line=if(text!=null)text.getLineForVerticalPosition(legacy.itemScrollOffsetPx.toFloat()) else 0
                        ReaderAnchor(bookId,chapterId,block.id,text?.getLineStart(line) ?: 0)
                    }
                }
                pageIndex=if(!initialized&&entry==ReaderChapterEntryPosition.End)result.plan.pages.lastIndex
                    else restored?.let(result.plan::pageForAnchor) ?: 0
                measured=result
                anchor=result.plan.pages[pageIndex].startAnchor
                navigator=ReaderPageNavigator(result.plan,anchor)
                initialized=true
            } catch(cancelled:kotlinx.coroutines.CancellationException){throw cancelled}
            catch(failure:Exception){if(generation.isCurrent(ticket))error=failure.message ?: "分页失败，请调整字号后重试"}
            finally {if(generation.isCurrent(ticket))measuring=false}
        }
        LaunchedEffect(followText,measured) {
            val query=followText?.takeIf(String::isNotBlank)?.let{convertChineseVariantText(it,chineseVariant)} ?: return@LaunchedEffect
            val current=measured ?: return@LaunchedEffect
            val block=current.document.blocks.filterIsInstance<ChapterDocumentBlock.Paragraph>().firstNotNullOfOrNull {paragraph->
                readerSpokenTextRange(paragraph.text.text,query)?.let{paragraph to it.first}
            } ?: return@LaunchedEffect
            val target=ReaderAnchor(bookId,chapterId,block.first.id,block.second)
            pageIndex=current.plan.pageForAnchor(target)
            anchor=current.plan.pages[pageIndex].startAnchor
            navigator?.jump(current.plan,anchor)
            showComments=false
        }
        LaunchedEffect(anchor) {
            val value=anchor ?: return@LaunchedEffect
            val document=measured ?: return@LaunchedEffect
            withContext(Dispatchers.IO){store.save(value)}
            val index=document.document.blocks.indexOfFirst {it.id==value.blockId}
            val layout=document.textLayouts[value.blockId]
            val offset=layout?.let {it.getLineTop(it.getLineForOffset(value.textOffset.coerceIn(0,it.layoutInput.text.length))).toInt()} ?: 0
            if(index>=0)latestAnchor(ReaderViewportAnchor(chapterId,index,offset))
        }
        when {
            error!=null -> Text(error.orEmpty(),color=textColor,modifier=Modifier.align(Alignment.Center))
            measured==null -> CircularProgressIndicator(modifier=Modifier.align(Alignment.Center))
            showComments -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween) {
                    TextButton(onClick={move(-1)}){Text("返回末页")}
                    TextButton(onClick={latestTap(.5f,.5f)}){Text("阅读菜单")}
                    TextButton(onClick={move(1)},enabled=hasNext){Text("下一章")}
                }
                comments()
            }
            else -> {
                val current=measured!!
                AnimatedContent(targetState=pageIndex,modifier=Modifier.fillMaxSize(),label="reader-page",
                    transitionSpec={
                        val direction=if(targetState>initialState)1 else -1
                        when(options.pageTurnEffect) {
                            "none"->EnterTransition.None togetherWith ExitTransition.None
                            "slide"->slideInHorizontally(tween(180)){direction*it} togetherWith slideOutHorizontally(tween(180)){-direction*it}
                            "cover"->slideInHorizontally(tween(180)){direction*it} togetherWith fadeOut(tween(180))
                            "simulated"->(slideInHorizontally(tween(240)){direction*it}+scaleIn(tween(240),initialScale=.94f)) togetherWith
                                (slideOutHorizontally(tween(240)){-direction*it/3}+scaleOut(tween(240),targetScale=.94f))
                            else->fadeIn(tween(150)) togetherWith fadeOut(tween(150))
                        }
                    }) { visiblePage ->
                    Box(Modifier.fillMaxSize().background(background)) {
                        backgroundImageUri?.let{uri->AsyncImage(model=uri,contentDescription=null,contentScale=ContentScale.Crop,
                            modifier=Modifier.fillMaxSize().graphicsLayer{alpha=.14f})}
                    PagedChapterCanvas(current,visiblePage,textColor,
                        Modifier.fillMaxSize().pointerInput(bookId,chapterId,touchSlop) {
                            var distance=0f
                            detectHorizontalDragGestures(
                                onDragStart={distance=0f},onDragCancel={distance=0f},
                                onDragEnd={
                                    if(kotlin.math.abs(distance)>=maxOf(touchSlop*3,size.width*.15f))latestMove(if(distance<0)1 else -1)
                                    distance=0f
                                },
                                onHorizontalDrag={change,delta->distance+=delta;change.consume()},
                            )
                        }.pointerInput(bookId,chapterId) {
                            detectTapGestures(onTap={latestTap(it.x/size.width,it.y/size.height)})
                        },highlightText=highlightText?.let{convertChineseVariantText(it,chineseVariant)}) { image, placement ->
                        PageIllustration(image,placement,textColor,
                            onPreview={onPreview(ReaderContentBlock.Image(image.url,image.alt,image.originalUrl),image.alt ?: "正文插图")},
                            onDimensions={value->if(images[image.id]!=value)images=images+(image.id to value)})
                    }
                    }
                }
            }
        }
    }
}

@Composable
private fun PageIllustration(image:ChapterDocumentBlock.Image,placement:Modifier,textColor:Color,onPreview:()->Unit,onDimensions:(ImageDimensions)->Unit) {
    val context=LocalContext.current
    val touchSlop=LocalViewConfiguration.current.touchSlop
    var retry by remember(image.url){mutableIntStateOf(0)}
    Box(placement.longPressOnly(touchSlop,onPreview),contentAlignment=Alignment.Center) {
        key(image.url,retry) {
            SubcomposeAsyncImage(model=ImageRequest.Builder(context).data(image.url).crossfade(false).build(),
                contentDescription=image.alt ?: "正文插图，长按查看原图",contentScale=ContentScale.Fit,modifier=Modifier.fillMaxSize(),
                loading={Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    CircularProgressIndicator();Text("正在加载插图",color=textColor)
                }},
                error={Column(Modifier.fillMaxSize(),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center) {
                    Text("插图加载失败",color=textColor)
                    TextButton(onClick={retry++}){Text("重试插图")}
                }},
                onSuccess={success->success.result.drawable.let{drawable->
                    if(drawable.intrinsicWidth>0&&drawable.intrinsicHeight>0)onDimensions(ImageDimensions(drawable.intrinsicWidth,drawable.intrinsicHeight))
                }})
        }
    }
}
