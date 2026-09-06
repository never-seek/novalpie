package com.novalpie.nativeapp.feature.library

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.novalpie.nativeapp.data.NovalPieApi
import com.novalpie.nativeapp.model.*
import com.novalpie.nativeapp.ui.*
import kotlinx.coroutines.*

internal data class LibraryQuery(
    val tab:FavoritesContentTab=FavoritesContentTab.Favorites,
    val page:Int=1,
    val groupId:Long?=null,
    val keyword:String="",
    val sortField:String="created_at",
    val sortOrder:String="desc",
)
internal interface LibraryRepository {
    suspend fun page(query:LibraryQuery):FavoritePage
    suspend fun groups():List<FavoriteGroup>
    suspend fun user():UserProfile
}
internal class WebsiteLibraryRepository(private val api:NovalPieApi):LibraryRepository {
    override suspend fun page(query:LibraryQuery)=if(query.tab==FavoritesContentTab.History)api.readingHistoryPage(query.page,20)else
        api.favoritePage(query.page,20,query.groupId,query.keyword,query.sortField,query.sortOrder)
    override suspend fun groups()=api.favoriteGroups()
    override suspend fun user()=api.currentUser()
}

/** Owns shelf requests/append state. Root callbacks only reconcile shared reading/profile stores. */
internal class LibraryContentViewModel(
    private val repository:LibraryRepository,
    scope:CoroutineScope?=null,
    private val reconcile:(List<FavoriteEntry>)->List<FavoriteEntry> = {it},
    private val onUser:(UserProfile)->Unit={},
    private val onLoaded:()->Unit={},
):ViewModel() {
    private val work=scope ?: viewModelScope
    var state by mutableStateOf(HomeState())
        private set
    private var query:LibraryQuery?=null
    private var generation=0L
    private var requests:Job?=null
    fun present(transform:(HomeState)->HomeState){state=transform(state)}

    fun load(requested:LibraryQuery,options:FavoritesUiOptions=state.options,tokenProfile:UserProfile?=null,message:String?=null,retain:Boolean=false) {
        val firstLoad=query==null
        val next=requested.copy(page=if(firstLoad)1 else requested.page.coerceAtLeast(1))
        val same=query?.copy(page=1)==next.copy(page=1)
        val retained=if(retain&&same)(state.favoriteEntries as? LoadResult.Success)?.value else null
        val favoriteTotal=if(same||next.tab==FavoritesContentTab.History)state.favoriteTotal else null
        val serial=++generation
        requests?.cancel()
        query=next
        state=HomeState(user=tokenProfile?.let{LoadResult.Success(it)} ?: LoadResult.Loading,groups=LoadResult.Loading,
            favorites=retained?.let{LoadResult.Success(it.map(FavoriteEntry::book))} ?: if(next.tab==FavoritesContentTab.Favorites)LoadResult.Loading else LoadResult.Idle,
            favoriteEntries=retained?.let{LoadResult.Success(it)} ?: if(next.tab==FavoritesContentTab.Favorites)LoadResult.Loading else LoadResult.Idle,
            favoriteTotal=favoriteTotal,history=if(next.tab==FavoritesContentTab.History)LoadResult.Loading else LoadResult.Idle,
            favoritesPage=next.page,options=options.copy(currentPage=next.page),selectedFavoriteGroupId=next.groupId,actionMessage=message)
        requests=work.launch {
            supervisorScope {
                launch {
                    try {
                        var response=repository.page(if(same&&next.page>1)next.copy(page=1)else next)
                        val refreshed=response.items.toMutableList()
                        if(same&&next.page>1)for(number in 2..next.page) {
                            if(!response.hasMore())break
                            response=repository.page(next.copy(page=number))
                            refreshed.addAll(response.items)
                        }
                        if(serial!=generation)return@launch
                        val entries=reconcile(refreshed.associateBy{it.book.id}.values.toList())
                        state=if(next.tab==FavoritesContentTab.Favorites)state.copy(favorites=LoadResult.Success(entries.map(FavoriteEntry::book)),
                            favoriteEntries=LoadResult.Success(entries),favoriteTotal=response.total ?: favoriteTotal,favoritesPage=response.page,favoritesCanLoadMore=response.hasMore())
                        else state.copy(history=LoadResult.Success(entries),historyTotal=response.total,favoritesPage=response.page,favoritesCanLoadMore=response.hasMore())
                        onLoaded()
                    }catch(cancelled:CancellationException){throw cancelled}
                    catch(failure:Exception){if(serial==generation)state=if(next.tab==FavoritesContentTab.Favorites)state.copy(
                        favorites=LoadResult.Error(apiFailureMessage("收藏",failure)),favoriteEntries=LoadResult.Error(apiFailureMessage("收藏",failure)))
                        else state.copy(history=LoadResult.Error(apiFailureMessage("阅读历史",failure)))}
                }
                launch {
                    try{val user=repository.user();if(serial==generation){state=state.copy(user=LoadResult.Success(user));onUser(user)}}
                    catch(cancelled:CancellationException){throw cancelled}
                    catch(failure:Exception){if(serial==generation)state=state.copy(user=tokenProfile?.let{LoadResult.Success(it)} ?: LoadResult.Error(apiFailureMessage("账号",failure)))}
                }
                launch {
                    try{val groups=repository.groups();if(serial==generation)state=state.copy(groups=LoadResult.Success(groups))}
                    catch(cancelled:CancellationException){throw cancelled}
                    catch(failure:Exception){if(serial==generation)state=state.copy(groups=LoadResult.Error(apiFailureMessage("收藏分组",failure)))}
                }
            }
        }
    }
    fun loadMore() {
        val current=query ?: return
        if(!state.favoritesCanLoadMore||state.favoritesLoadingMore)return
        val serial=generation
        val target=current.copy(page=state.favoritesPage+1)
        state=state.copy(favoritesLoadingMore=true,favoritesLoadMoreError=null)
        work.launch {
            try {
                val response=repository.page(target)
                if(serial!=generation)return@launch
                val previous=if(current.tab==FavoritesContentTab.Favorites)(state.favoriteEntries as? LoadResult.Success)?.value.orEmpty()
                    else (state.history as? LoadResult.Success)?.value.orEmpty()
                val entries=reconcile((previous+response.items).associateBy{it.book.id}.values.toList())
                state=if(current.tab==FavoritesContentTab.Favorites)state.copy(favorites=LoadResult.Success(entries.map(FavoriteEntry::book)),favoriteEntries=LoadResult.Success(entries),favoriteTotal=response.total ?: state.favoriteTotal)
                    else state.copy(history=LoadResult.Success(entries),historyTotal=response.total ?: state.historyTotal)
                state=state.copy(favoritesPage=response.page,options=state.options.copy(currentPage=response.page),favoritesCanLoadMore=response.hasMore(),favoritesLoadingMore=false,favoritesLoadMoreError=null)
                onLoaded()
            }catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(serial==generation)state=state.copy(favoritesLoadingMore=false,favoritesLoadMoreError=apiFailureMessage("加载下一页",failure))}
        }
    }
    fun mutation(successMessage:String,action:suspend()->Unit,onCompleted:()->Unit) {
        if(state.actionLoading)return
        val serial=generation
        state=state.copy(actionLoading=true,actionMessage=null)
        work.launch {
            try {
                action()
                if(serial!=generation)return@launch
                state=state.copy(actionLoading=false,selectionMode=false,selectedBookIds=emptySet(),actionMessage=successMessage)
                onCompleted()
            }catch(cancelled:CancellationException){throw cancelled}
            catch(failure:Exception){if(serial==generation)state=state.copy(actionLoading=false,actionMessage=apiFailureMessage("收藏操作",failure))}
        }
    }
    fun environmentChanged(){generation++;requests?.cancel();query=null;state=HomeState(options=state.options.copy(currentPage=1))}
    fun close(){generation++;work.cancel()}
    private fun FavoritePage.hasMore()=totalPages?.let{page<it} ?: total?.let{page.toLong()*pageSize<it} ?: (items.size>=pageSize)
}
