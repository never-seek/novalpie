package com.novalpie.nativeapp.data

import com.novalpie.nativeapp.model.MessageQuery
import com.novalpie.nativeapp.model.MessageSettings
import com.novalpie.nativeapp.model.BookEditRequest
import com.novalpie.nativeapp.model.ForumCreateRequest
import com.novalpie.nativeapp.model.ForumPollDraft
import com.novalpie.nativeapp.model.PoliticalExamAnswers
import com.novalpie.nativeapp.model.UserProfile
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.ServerSocket
import java.io.ByteArrayInputStream

@RunWith(RobolectricTestRunner::class)
class NovalPieApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: NovalPieApi

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NovalPieApi(baseUrl = server.url("/").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun chaptersNormalizeWebsiteFieldAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "chapters": [
                          {
                            "chapter_id": 1001,
                            "chapter_name": "Prologue",
                            "display_order": 7,
                            "words": 3210,
                            "created_at": "2026-07-01T12:00:00Z"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val chapters = api.chapters(354491)

        assertEquals(1, chapters.size)
        assertEquals(1001L, chapters.single().id)
        assertEquals("Prologue", chapters.single().title)
        assertEquals(7, chapters.single().number)
        assertEquals(3210L, chapters.single().wordCount)
        assertEquals("2026-07-01T12:00:00Z", chapters.single().updatedAt)
        assertEquals("/api/novels/354491/chapters", server.takeRequest().path)
    }

    @Test
    fun chaptersAreSortedByWebsiteDisplayOrder() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "chapters": [
                          { "chapter_id": 3003, "chapter_name": "Third", "display_order": 3 },
                          { "chapter_id": 3001, "chapter_name": "First", "display_order": 1 },
                          { "chapter_id": 3002, "chapter_name": "Second", "display_order": 2 }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val chapters = api.chapters(354491)

        assertEquals(listOf(3001L, 3002L, 3003L), chapters.map { it.id })
        assertEquals(listOf(1, 2, 3), chapters.map { it.number })
    }

    @Test
    fun bookDetailUnwrapsNestedNovelAndNormalizesWebsiteFieldAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "novel": {
                          "novel_id": 354491,
                          "novel_title": "Native Book",
                          "true_name": "네이티브 북",
                          "platform": "upload",
                          "author": { "name": "Author Name" },
                            "cover_path": "/covers/native-book.jpg",
                            "photo_true_url": "/covers/native-book-original.jpg",
                          "synopsis": "A native detail payload",
                          "words": 88000,
                          "favorite_count": 2345,
                          "site_read_count": 120000,
                          "source_read_count": 980000,
                          "source_favorite_count": 45000,
                          "status": "连载中",
                          "created_at": "2026-07-02T08:30:00Z",
                          "tags": [
                            { "name": "Fantasy" },
                            { "title": "Translated" },
                            "Featured"
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val book = api.bookDetail(354491)

        assertEquals(354491L, book.id)
        assertEquals("Native Book", book.title)
        assertEquals("네이티브 북", book.originalTitle)
        assertEquals("upload", book.platform)
        assertEquals("Author Name", book.author)
        assertEquals("${server.url("/").toString().trimEnd('/')}/covers/native-book.jpg", book.coverUrl)
        assertEquals("${server.url("/").toString().trimEnd('/')}/covers/native-book-original.jpg", book.fullCoverUrl)
        assertEquals("A native detail payload", book.description)
        assertEquals(88000L, book.wordCount)
        assertEquals(2345L, book.favoriteCount)
        assertEquals(120000L, book.siteReadCount)
        assertEquals(980000L, book.sourceReadCount)
        assertEquals(45000L, book.sourceFavoriteCount)
        assertEquals("连载中", book.status)
        assertEquals("2026-07-02T08:30:00Z", book.updatedAt)
        assertEquals(listOf("Fantasy", "Translated", "Featured"), book.tags)
        assertEquals("/api/novels/354491/detail", server.takeRequest().path)
    }

    @Test
    fun bookCoverPhotoUsesWebsiteOriginalPhotoEndpoint() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "photo_url": "/covers/native-book-preview.jpg",
                        "photo_true_url": "/covers/native-book-original.jpg"
                      }
                    }
                    """.trimIndent()
                )
        )

        val photoUrl = api.bookCoverPhoto(354491)

        assertEquals(
            "${server.url("/").toString().trimEnd('/')}/covers/native-book-original.jpg",
            photoUrl
        )
        val request = server.takeRequest()
        assertEquals("/api/novels/354491/photo", request.requestUrl?.encodedPath)
        assertEquals("novel", request.requestUrl?.queryParameter("favorite_type"))
    }

    @Test
    fun editorAiRegexUsesWebsiteOpenAiCompatibleContract() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"regex\":\"^Chapter\\\\s+\\\\d+\"}"
                          }
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val regex = api.generateEditorRegex(
            endpoint = server.url("/").toString().trimEnd('/'),
            apiKey = "test-editor-key",
            model = "test-model",
            chapterTitles = listOf("Chapter 1 Start", "Chapter 2 Continue")
        )

        assertEquals("^Chapter\\s+\\d+", regex)
        val request = server.takeRequest()
        assertEquals("/v1/chat/completions", request.requestUrl?.encodedPath)
        assertEquals("Bearer test-editor-key", request.getHeader("authorization"))
        val body = JSONObject(request.body.readUtf8())
        assertEquals("test-model", body.getString("model"))
        assertEquals(0.3, body.getDouble("temperature"), 0.0)
        assertEquals("json_object", body.getJSONObject("response_format").getString("type"))
        assertTrue(body.getJSONArray("messages").getJSONObject(1).getString("content").contains("Chapter 2 Continue"))
    }

    @Test
    fun politicalExamSessionNormalizesAllWebsiteQuestionTypes() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "session_id": "session-123",
                      "remaining_time": 1200,
                      "exam": {
                        "single_choice": [
                          { "question": "Single question", "options": ["A1", "A2"] }
                        ],
                        "multiple_choice": [
                          { "question": "Multiple question", "options": ["B1", "B2", "B3"] }
                        ],
                        "true_false": [
                          { "question": "True or false" }
                        ],
                        "fill_blank": [
                          { "question": "Fill ____" }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val session = api.startPoliticalExam()

        assertEquals("session-123", session.sessionId)
        assertEquals(1200, session.remainingTimeSeconds)
        assertEquals("Single question", session.paper.singleChoice.single().question)
        assertEquals(listOf("A1", "A2"), session.paper.singleChoice.single().options)
        assertEquals("Multiple question", session.paper.multipleChoice.single().question)
        assertEquals("True or false", session.paper.trueFalse.single().question)
        assertEquals("Fill ____", session.paper.fillBlank.single().question)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/political-exams/sessions", request.path)
    }

    @Test
    fun politicalExamSubmitUsesWebsiteAnswersShapeAndNormalizesResult() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "score": 88,
                        "total": 100,
                        "passed": true,
                        "token": "replacement.jwt.token",
                        "details": {
                          "single_choice": [
                            {
                              "correct": true,
                              "question": "Single question",
                              "user_answer": 1,
                              "correct_answer": 1
                            }
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val result = api.submitPoliticalExam(
            sessionId = "session-123",
            answers = PoliticalExamAnswers(
                singleChoice = listOf(1, null),
                multipleChoice = listOf(listOf(0, 2)),
                trueFalse = listOf(true, false),
                fillBlank = listOf("answer")
            )
        )

        assertEquals(88, result.score)
        assertEquals(100, result.total)
        assertTrue(result.passed)
        assertEquals("replacement.jwt.token", result.token)
        assertTrue(result.details.getValue("single_choice").single().correct)
        val request = server.takeRequest()
        assertEquals("/api/political-exams/sessions/submit", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("session-123", body.getString("session_id"))
        val answers = body.getJSONObject("answers")
        assertEquals(1, answers.getJSONArray("single_choice").getInt(0))
        assertTrue(answers.getJSONArray("single_choice").isNull(1))
        assertEquals(2, answers.getJSONArray("multiple_choice").getJSONArray(0).getInt(1))
        assertEquals(false, answers.getJSONArray("true_false").getBoolean(1))
        assertEquals("answer", answers.getJSONArray("fill_blank").getString(0))
    }

    @Test
    fun currentUserNormalizesWebsiteProfileFields() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "id": 100000,
                      "username": "Profile User",
                      "role": "admin",
                      "point": 3210,
                      "created_at": "2026-01-02T03:04:05Z",
                      "avatar": "/avatars/user.png",
                      "avatar_frame": "/frames/frame.png",
                      "bio": "Native profile",
                      "email": "profile@example.test",
                      "is_banned": false,
                      "is_adult": true,
                      "deleted": false,
                      "show_checkin": true,
                      "auto_checkin": false,
                      "badges": ["founder", {"name":"translator"}],
                      "stats": {"novels": 4, "comments": 29, "followers": 8}
                    }
                    """.trimIndent()
                )
        )

        val user = api.currentUser()

        assertEquals(100000L, user.id)
        assertEquals("Profile User", user.name)
        assertEquals(3210L, user.points)
        assertEquals("${server.url("/").toString().trimEnd('/')}/avatars/user.png", user.avatarUrl)
        assertEquals("Native profile", user.bio)
        assertEquals("profile@example.test", user.email)
        assertTrue(user.isAdult == true)
        assertEquals(listOf("founder", "translator"), user.badges)
        assertEquals(29L, user.stats["comments"])
    }

    @Test
    fun updateCurrentUserAndCheckinSettingsUseWebsitePatchBodies() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("{\"success\":true}"))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("{\"success\":true}"))

        api.updateCurrentUser(
            UserProfile(
                id = 100000,
                name = "Updated Name",
                bio = "Updated bio",
                showCheckin = false,
                autoCheckin = true
            )
        )
        api.updateCurrentUserCheckinSettings(showCheckin = false, autoCheckin = true)

        val profileRequest = server.takeRequest()
        assertEquals("PATCH", profileRequest.method)
        assertEquals("/api/users/me", profileRequest.path)
        val profileBody = JSONObject(profileRequest.body.readUtf8())
        assertEquals("Updated Name", profileBody.getString("username"))
        assertEquals("Updated bio", profileBody.getString("bio"))
        assertEquals(false, profileBody.getBoolean("show_checkin"))
        assertEquals(true, profileBody.getBoolean("auto_checkin"))

        val settingsRequest = server.takeRequest()
        assertEquals("PATCH", settingsRequest.method)
        assertEquals("/api/users/me/checkins/settings", settingsRequest.path)
    }

    @Test
    fun currentUserCheckinStatsAndCheckinUseWebsiteEndpoints() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """
                {"stats":{"total_days":20,"total_points":240,"max_streak":7,"current_streak":3}}
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """
                {"success":true,"message":"checked in","points":12}
                """.trimIndent()
            )
        )

        val stats = api.currentUserCheckinStats()
        val action = api.checkinCurrentUser()

        assertEquals(20, stats.totalDays)
        assertEquals(240L, stats.totalPoints)
        assertEquals(7, stats.maxStreak)
        assertEquals(3, stats.currentStreak)
        assertTrue(action.success)
        assertEquals(12L, action.points)
        assertEquals("/api/users/me/checkins/stats", server.takeRequest().path)
        val checkinRequest = server.takeRequest()
        assertEquals("POST", checkinRequest.method)
        assertEquals("/api/users/me/checkins", checkinRequest.path)
    }

    @Test
    fun publicUserProfileAndActivityUseWebsiteEndpointsAndNormalizeNestedTargets() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"id":42,"username":"Public User","bio":"Reader","point":88}"""
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """
                {"activities":[
                  {"id":9,"type":"post_comment","created_at":"2026-07-10T01:02:03Z",
                   "comment":{"id":77,"content":"Useful reply"},
                   "post":{"id":1422,"title":"Site update"}}
                ]}
                """.trimIndent()
            )
        )

        val user = api.userProfile(42)
        val activities = api.userActivities(userId = 42, type = "post_comment", page = 2, limit = 30)

        assertEquals("Public User", user.name)
        assertEquals("post_comment", activities.single().type)
        assertEquals("Site update", activities.single().title)
        assertEquals("Useful reply", activities.single().content)
        assertEquals(1422L, activities.single().postId)
        assertEquals(77L, activities.single().commentId)
        assertEquals("/api/users/42", server.takeRequest().path)
        assertEquals(
            "/api/users/42/activities?type=post_comment&page=2&limit=30",
            server.takeRequest().path
        )
    }

    @Test
    fun publicUserNovelsAndCheckinDataUseWebsiteEndpoints() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"novels":[{"id":354491,"title":"Large EPUB","author":"Uploader","cover":"/covers/large.jpg"}]}"""
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"records":{"2026-07-09":{"points":10},"2026-07-10":{"points":12}}}"""
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"checkin_settings":{"show_checkin":true,"auto_checkin":false}}"""
            )
        )

        val novels = api.userNovels(userId = 42)
        val records = api.userCheckinRecords(42, "2026-01-01", "2026-12-31")
        val settings = api.userCheckinSettings(42)

        assertEquals(354491L, novels.single().id)
        assertEquals(2, records.size)
        assertEquals(12L, records.last().points)
        assertTrue(settings.showCheckin)
        assertFalse(settings.autoCheckin)
        assertEquals("/api/users/42/novels", server.takeRequest().path)
        assertEquals(
            "/api/users/42/checkins?start_date=2026-01-01&end_date=2026-12-31",
            server.takeRequest().path
        )
        assertEquals("/api/users/42/checkins/settings?user_id=42", server.takeRequest().path)
    }

    @Test
    fun adultVerificationUsesWebsiteBirthYearBody() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("{\"success\":true}"))

        val action = api.verifyCurrentUserAdult(1995)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/users/me/verifies/adult", request.path)
        assertEquals(1995, JSONObject(request.body.readUtf8()).getInt("birth_year"))
        assertTrue(action.success)
    }

    @Test
    fun forumPostNormalizesAuthorIdForNativeProfileNavigation() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"posts":[{"id":1422,"title":"Profile route","author":{"id":100000,"username":"Noah"}}]}"""
            )
        )

        val post = api.forumPosts().single()

        assertEquals(100000L, post.authorId)
        assertEquals("Noah", post.authorName)
    }

    @Test
    fun avatarUploadUsesWebsiteMultipartFieldAndEndpoint() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("{\"success\":true}"))
        val bytes = "native-avatar".toByteArray()

        val action = api.uploadCurrentUserAvatar(
            UploadFileSource(
                fileName = "avatar.png",
                sizeBytes = bytes.size.toLong(),
                contentType = "image/png",
                openStream = { ByteArrayInputStream(bytes) }
            )
        )

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/users/me/avatar", request.path)
        assertTrue(request.getHeader("content-type").orEmpty().startsWith("multipart/form-data"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("name=\"avatar\""))
        assertTrue(body.contains("filename=\"avatar.png\""))
        assertTrue(action.success)
    }

    @Test
    fun managedBookInfoPermissionsAndSaveUseWebsiteContracts() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """
                {"success":true,"data":{
                  "id":354491,"title":"中文名","true_name":"Original","author_name":"Author",
                  "description":"Description","source":"novelpia","source_url":"https://source.example/book/1",
                  "language":"ko","spans":"19 已完结","is_adult":1,
                  "photo_url":"https://img.example/original.jpg","tags":["奇幻","论坛"]
                }}
                """.trimIndent()
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"success":true,"permissions":{"title":true,"true_name":false,"author_name":true,"description":true,"source":false,"source_url":true,"language":true,"is_adult":true,"photo_url":true,"spans":true,"tags":true}}"""
            )
        )
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json").setBody(
                """{"success":true,"message":"saved","failed_fields":["source_url"]}"""
            )
        )

        val info = api.managedBookInfo(354491)
        val permissions = api.managedBookPermissions(354491)
        val result = api.updateManagedBook(
            354491,
            BookEditRequest(
                title = "新中文名",
                titleTranslation = "New Original",
                authorName = "New Author",
                description = "New description",
                source = "novelpia",
                sourceUrl = "https://source.example/book/2",
                language = "ko",
                status = "已完结",
                isAdult = true,
                photoUrl = "https://img.example/new.jpg",
                tags = listOf("奇幻", "完成")
            )
        )

        assertEquals(354491L, info.id)
        assertEquals("Original", info.titleTranslation)
        assertEquals("Author", info.authorName)
        assertEquals("已完结", info.status)
        assertTrue(info.isAdult)
        assertEquals(listOf("奇幻", "论坛"), info.tags)
        assertTrue(permissions.title)
        assertFalse(permissions.titleTranslation)
        assertTrue(permissions.photoUrl)
        assertTrue(result.success)
        assertEquals(listOf("source_url"), result.failedFields)

        assertEquals("/api/novels/354491/detail", server.takeRequest().requestUrl?.encodedPath)
        assertEquals("/api/users/me/novels/354491/permissions/check", server.takeRequest().requestUrl?.encodedPath)
        val save = server.takeRequest()
        assertEquals("PATCH", save.method)
        assertEquals("/api/users/me/novels/354491", save.requestUrl?.encodedPath)
        val body = JSONObject(save.body.readUtf8())
        assertEquals("新中文名", body.getString("title"))
        assertEquals("New Original", body.getString("title_translation"))
        assertEquals("New Author", body.getString("author_name"))
        assertEquals("New description", body.getString("description"))
        assertEquals("19 已完结", body.getString("spans"))
        assertEquals(1, body.getInt("is_adult"))
        assertEquals("https://img.example/new.jpg", body.getString("photo_url"))
        assertEquals(listOf("奇幻", "完成"), body.getJSONArray("tags").let { array ->
            (0 until array.length()).map(array::getString)
        })
    }

    @Test
    fun managedBookCoverUploadKeepsOriginalFileAndUsesPut() = runBlocking {
        server.enqueue(
            MockResponse().setHeader("content-type", "application/json")
                .setBody("""{"success":true,"data":{"photo_url":"https://img.example/full.jpg"}}""")
        )
        val bytes = "full-resolution-cover".toByteArray()

        val url = api.uploadManagedBookCover(
            354491,
            UploadFileSource(
                fileName = "cover.png",
                sizeBytes = bytes.size.toLong(),
                contentType = "image/png",
                openStream = { ByteArrayInputStream(bytes) }
            )
        )

        assertEquals("https://img.example/full.jpg", url)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/novels/354491/photo", request.requestUrl?.encodedPath)
        assertTrue(request.getHeader("content-type").orEmpty().startsWith("multipart/form-data"))
        val body = request.body.readUtf8()
        assertTrue(body.contains("name=\"cover\""))
        assertTrue(body.contains("filename=\"cover.png\""))
        assertTrue(body.contains("full-resolution-cover"))
    }

    @Test
    fun bookDetailNormalizesCompletedBooleanStatusAlias() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "novel": {
                          "id": 354491,
                          "title": "Completed Book",
                          "is_completed": true
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val book = api.bookDetail(354491)

        assertEquals("已完结", book.status)
    }

    @Test
    fun bookDetailCombinesCategoryGenreAndStringTagAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "novel": {
                          "id": 354491,
                          "title": "Tagged Book",
                          "category": "Fantasy",
                          "genre": "Adventure",
                          "tags": "Native, Commercial, Fantasy"
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val book = api.bookDetail(354491)

        assertEquals(listOf("Fantasy", "Adventure", "Native", "Commercial"), book.tags)
    }

    @Test
    fun searchNormalizesResultArrayAliasesAndSendsQueryParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "results": [
                          {
                            "novel_id": 456,
                            "novel_title": "Search Book",
                            "true_name": "서치 북",
                            "platform": "novelPia",
                            "author": { "display_name": "Search Author" },
                            "cover_path": "/covers/search-book.jpg",
                            "favoriteCount": 99,
                            "siteReadCount": 1200,
                            "sourceReadCount": 4500,
                            "sourceFavoriteCount": 321,
                            "tags": [{ "label": "Searchable" }]
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val books = api.search(
            keyword = "native search",
            page = 2,
            limit = 5,
            sortBy = "favorite_count",
            sortOrder = "asc",
            scope = "tags",
            matchType = "ai",
            adultFilter = "adult_only",
            source = "novelPia",
            minWordCount = 100000,
            maxWordCount = 500000
        )

        assertEquals(1, books.size)
        assertEquals(456L, books.single().id)
        assertEquals("Search Book", books.single().title)
        assertEquals("서치 북", books.single().originalTitle)
        assertEquals("novelPia", books.single().platform)
        assertEquals("Search Author", books.single().author)
        assertEquals("${server.url("/").toString().trimEnd('/')}/covers/search-book.jpg", books.single().coverUrl)
        assertEquals(99L, books.single().favoriteCount)
        assertEquals(1200L, books.single().siteReadCount)
        assertEquals(4500L, books.single().sourceReadCount)
        assertEquals(321L, books.single().sourceFavoriteCount)
        assertEquals(listOf("Searchable"), books.single().tags)

        val request = server.takeRequest()
        assertEquals("/api/search", request.requestUrl?.encodedPath)
        assertEquals("native search", request.requestUrl?.queryParameter("q"))
        assertEquals(null, request.requestUrl?.queryParameter("keyword"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("5", request.requestUrl?.queryParameter("limit"))
        assertEquals("favorite_count", request.requestUrl?.queryParameter("sort_by"))
        assertEquals("asc", request.requestUrl?.queryParameter("sort_order"))
        assertEquals("tags", request.requestUrl?.queryParameter("scope"))
        assertEquals("ai", request.requestUrl?.queryParameter("match_type"))
        assertEquals("adult_only", request.requestUrl?.queryParameter("adult_filter"))
        assertEquals("novelPia", request.requestUrl?.queryParameter("source"))
        assertEquals("100000", request.requestUrl?.queryParameter("min_word_count"))
        assertEquals("500000", request.requestUrl?.queryParameter("max_word_count"))
        assertTrue(request.getHeader("user-agent").orEmpty().contains("NovalPieNative"))
    }

    @Test
    fun searchNormalizesRelativeCoverPathsWithoutLeadingSlash() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "results": [
                          {
                            "novel_id": 789,
                            "novel_title": "Relative Cover Book",
                            "cover_path": "imagebox/cover/relative-book.jpg"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val books = api.search(keyword = "relative cover")

        assertEquals(
            "${server.url("/").toString().trimEnd('/')}/imagebox/cover/relative-book.jpg",
            books.single().coverUrl
        )
    }

    @Test
    fun searchNormalizesWebsitePhotoUrlAndAuthorName() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "results": [
                        {
                          "id": 6,
                          "title": "Website Search Book",
                          "author_name": "Website Author",
                          "photo_url": "https://images.novelpia.com/imagebox/cover/site-book.file"
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val books = api.search(keyword = "aa")

        assertEquals(1, books.size)
        assertEquals("Website Author", books.single().author)
        assertEquals("https://images.novelpia.com/imagebox/cover/site-book.file", books.single().coverUrl)
    }

    @Test
    fun searchNormalizesFullCoverAliasesAndExpandedTagAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "results": [
                        {
                          "id": 7,
                          "title": "Alias Search Book",
                          "photo_url": "imagebox/cover/alias-thumb.file",
                          "cover_original_path": "imagebox/cover/alias-original.file",
                          "category": "规则",
                          "spans": "R19 完结",
                          "categories": [{ "tag_name": "日常" }, { "value": "现代" }, { "tag": { "name": "治愈" } }],
                          "novel_tags": "电视剧,言情",
                          "tag_list": [{ "text": "心理" }],
                          "tag_relations": [{ "tag": { "name": "冒险" } }]
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val books = api.search(keyword = "alias")
        val book = books.single()
        val base = server.url("/").toString().trimEnd('/')

        assertEquals("$base/imagebox/cover/alias-thumb.file", book.coverUrl)
        assertEquals("$base/imagebox/cover/alias-original.file", book.fullCoverUrl)
        assertEquals(listOf("规则", "R19", "完结", "日常", "现代", "治愈", "电视剧", "言情", "心理", "冒险"), book.tags)
    }

    @Test
    fun searchDropsBareImageHostCoverUrlsButKeepsWebsiteTags() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "results": [
                        {
                          "id": 104786,
                          "title": "Bare Host Cover",
                          "photo_url": "https://images.novelpia.com",
                          "spans": "免费 独家",
                          "tags": ["恋爱喜剧", "日常", "电视剧"]
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val book = api.search(keyword = "bare cover").single()

        assertNull(book.coverUrl)
        assertEquals(listOf("免费", "独家", "恋爱喜剧", "日常", "电视剧"), book.tags)
    }

    @Test
    fun searchFallsBackToProxySelectorWhenDirectConnectionFails() = runBlocking {
        val proxyServer = MockWebServer()
        proxyServer.start()
        try {
            val closedDirectPort = ServerSocket(0).use { it.localPort }
            proxyServer.enqueue(
                MockResponse()
                    .setHeader("content-type", "application/json")
                    .setBody(
                        """
                        {
                          "results": [
                            {
                              "id": 12,
                              "title": "Fallback Proxy Book",
                              "tags": [{ "name": "ProxyTag" }]
                            }
                          ]
                        }
                        """.trimIndent()
                    )
            )

            val fallbackProxy = Proxy(
                Proxy.Type.HTTP,
                InetSocketAddress(proxyServer.hostName, proxyServer.port)
            )
            val fallbackApi = NovalPieApi(
                baseUrl = "http://127.0.0.1:$closedDirectPort",
                proxySelectorProvider = { FixedProxySelector(listOf(Proxy.NO_PROXY, fallbackProxy)) }
            )

            val books = fallbackApi.search(keyword = "proxy fallback")

            assertEquals(1, books.size)
            assertEquals("Fallback Proxy Book", books.single().title)
            assertEquals(listOf("ProxyTag"), books.single().tags)
            assertTrue(proxyServer.takeRequest().requestLine.contains("http://127.0.0.1:$closedDirectPort/api/search"))
        } finally {
            proxyServer.shutdown()
        }
    }

    @Test
    fun tagsNormalizeWebsiteTagAliasesAndSendSortLimitParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "data": [
                        { "id": 10, "tag_name": "异世界", "count": 88 },
                        { "name": "完结", "book_count": 21 }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val tags = api.tags(sort = "count", limit = 12)

        assertEquals(2, tags.size)
        assertEquals(10L, tags[0].id)
        assertEquals("异世界", tags[0].name)
        assertEquals(88, tags[0].count)
        assertEquals(null, tags[1].id)
        assertEquals("完结", tags[1].name)
        assertEquals(21, tags[1].count)

        val request = server.takeRequest()
        assertEquals("/api/tags", request.requestUrl?.encodedPath)
        assertEquals("count", request.requestUrl?.queryParameter("sort"))
        assertEquals("12", request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun messagesNormalizeCurrentWebsiteFieldsAndSendPagination() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "list": [
                        {
                          "id": 501,
                          "message_type": 8,
                          "message_title": "Private message",
                          "message_content": "Message preview",
                          "username": "Sender",
                          "created_at": "2026-07-10T01:00:00Z",
                          "is_read": 0,
                          "is_starred": 1,
                          "priority": 2,
                          "action_url": "/user/42",
                          "action_text": "Open"
                        }
                      ],
                      "pagination": { "page": 2, "total_pages": 4, "total": 61 }
                    }
                    """.trimIndent()
                )
        )

        val messages = api.messages(page = 2, pageSize = 20)

        assertEquals(1, messages.size)
        val message = messages.single()
        assertEquals(501L, message.id)
        assertEquals(8, message.type)
        assertEquals("Private message", message.title)
        assertEquals("Message preview", message.content)
        assertEquals("Sender", message.username)
        assertEquals("2026-07-10T01:00:00Z", message.createdAt)
        assertFalse(message.isRead)
        assertTrue(message.isStarred)
        assertEquals(2, message.priority)
        assertEquals("/user/42", message.actionUrl)
        assertEquals("Open", message.actionText)

        val request = server.takeRequest()
        assertEquals("/api/messages", request.requestUrl?.encodedPath)
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("page_size"))
    }

    @Test
    fun messagePageSendsWebsiteFiltersAndNormalizesPaginationAndMetadata() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "list": [
                        {
                          "id": 701,
                          "message_type": 4,
                          "message_title": "Novel update",
                          "message_content": "A new chapter is ready",
                          "username": "System",
                          "created_at": "2026-07-10T02:00:00Z",
                          "read_at": "2026-07-10T02:01:00Z",
                          "is_read": 1,
                          "is_starred": 0,
                          "priority": 2,
                          "user_id": 20,
                          "execute_user_id": 10,
                          "avatar": "/avatar/system.png",
                          "avatar_frame": "/frame/blue.png",
                          "extra_data": { "book_id": 354491, "chapter_id": "9001" }
                        }
                      ],
                      "pagination": {
                        "page": 2,
                        "page_size": 20,
                        "total": 61,
                        "total_pages": 4
                      }
                    }
                    """.trimIndent()
                )
        )

        val page = api.messagePage(
            query = MessageQuery(
                keyword = "更新",
                messageType = 4,
                isRead = false,
                priority = 2
            ),
            page = 2,
            pageSize = 20
        )

        assertEquals(1, page.items.size)
        assertEquals(2, page.pagination.page)
        assertEquals(20, page.pagination.pageSize)
        assertEquals(61, page.pagination.total)
        assertEquals(4, page.pagination.totalPages)
        val message = page.items.single()
        assertEquals(701L, message.id)
        assertEquals("2026-07-10T02:01:00Z", message.readAt)
        assertEquals(20L, message.userId)
        assertEquals(10L, message.executeUserId)
        assertEquals("/avatar/system.png", message.avatarUrl)
        assertEquals("/frame/blue.png", message.avatarFrameUrl)
        assertEquals("354491", message.extraData["book_id"])
        assertEquals("9001", message.extraData["chapter_id"])

        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/messages", request.requestUrl?.encodedPath)
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("20", request.requestUrl?.queryParameter("page_size"))
        assertEquals("4", request.requestUrl?.queryParameter("message_type"))
        assertEquals("false", request.requestUrl?.queryParameter("is_read"))
        assertEquals("2", request.requestUrl?.queryParameter("priority"))
        assertEquals("更新", request.requestUrl?.queryParameter("keyword"))
    }

    @Test
    fun messageDetailNormalizesCurrentWebsiteResponse() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "data": {
                        "message": {
                          "id": 77,
                          "message_type": 2,
                          "message_title": "Reply",
                          "message_content": "Full detail body",
                          "is_read": 1,
                          "is_starred": 1,
                          "action_url": "/forum/1422",
                          "action_text": "Open reply"
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val detail = api.messageDetail(77)

        assertEquals(77L, detail.id)
        assertEquals(2, detail.type)
        assertEquals("Reply", detail.title)
        assertEquals("Full detail body", detail.content)
        assertTrue(detail.isRead)
        assertTrue(detail.isStarred)
        assertEquals("/forum/1422", detail.actionUrl)
        assertEquals("Open reply", detail.actionText)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/messages/77", request.path)
    }

    @Test
    fun messageStatsNormalizeCurrentWebsiteCounters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "total_count": 61,
                      "unread_count": 7,
                      "read_count": 54,
                      "starred_count": 3,
                      "important_count": 2,
                      "recent_7days_count": 9,
                      "unread_by_type": { "8": 4, "9": 2 }
                    }
                    """.trimIndent()
                )
        )

        val stats = api.messageStats()

        assertEquals(61, stats.totalCount)
        assertEquals(7, stats.unreadCount)
        assertEquals(54, stats.readCount)
        assertEquals(3, stats.starredCount)
        assertEquals(2, stats.importantCount)
        assertEquals(9, stats.recentSevenDaysCount)
        assertEquals(4, stats.unreadByType[8])
        assertEquals(2, stats.unreadByType[9])
        assertEquals("/api/messages/stats", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun messageMutationsUseWebsiteMethodsAndPayloads() = runBlocking {
        repeat(6) {
            server.enqueue(
                MockResponse()
                    .setHeader("content-type", "application/json")
                    .setBody("""{"success":true,"message":"ok"}""")
            )
        }

        assertTrue(api.markMessageRead(71).success)
        assertTrue(api.markMessagesRead(listOf(71, 72)).success)
        assertTrue(api.markAllMessagesRead().success)
        assertTrue(api.starMessage(71, starred = true).success)
        assertTrue(api.deleteMessage(71).success)
        assertTrue(api.deleteMessages(listOf(71, 72)).success)

        val singleRead = server.takeRequest()
        assertEquals("POST", singleRead.method)
        assertEquals("/api/messages/71/read", singleRead.requestUrl?.encodedPath)
        assertEquals(71L, JSONObject(singleRead.body.readUtf8()).getLong("id"))

        val selectedRead = server.takeRequest()
        assertEquals("POST", selectedRead.method)
        assertEquals("/api/messages/read", selectedRead.requestUrl?.encodedPath)
        assertEquals(listOf(71L, 72L), JSONObject(selectedRead.body.readUtf8()).getJSONArray("ids").let { ids ->
            (0 until ids.length()).map(ids::getLong)
        })

        val allRead = server.takeRequest()
        assertEquals("POST", allRead.method)
        assertEquals("/api/messages/read", allRead.requestUrl?.encodedPath)
        assertTrue(JSONObject(allRead.body.readUtf8()).getBoolean("all"))

        val star = server.takeRequest()
        assertEquals("POST", star.method)
        assertEquals("/api/messages/71/star", star.requestUrl?.encodedPath)
        assertEquals(1, JSONObject(star.body.readUtf8()).getInt("starred"))

        val singleDelete = server.takeRequest()
        assertEquals("DELETE", singleDelete.method)
        assertEquals("/api/messages/71", singleDelete.requestUrl?.encodedPath)
        JSONObject(singleDelete.body.readUtf8()).also { body ->
            assertEquals(71L, body.getLong("id"))
            assertFalse(body.getBoolean("permanent"))
        }

        val selectedDelete = server.takeRequest()
        assertEquals("DELETE", selectedDelete.method)
        assertEquals("/api/messages", selectedDelete.requestUrl?.encodedPath)
        assertEquals(listOf(71L, 72L), JSONObject(selectedDelete.body.readUtf8()).getJSONArray("ids").let { ids ->
            (0 until ids.length()).map(ids::getLong)
        })
    }

    @Test
    fun messageSettingsUseWebsiteFieldsAndPutPayload() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "enable_notifications": true,
                        "enable_email": false,
                        "enable_browser_push": true,
                        "notification_types": [1, 4, 8],
                        "quiet_hours_start": "23:00",
                        "quiet_hours_end": "07:30",
                        "auto_read_after_days": 30
                      }
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""{"success":true,"message":"saved"}""")
        )

        val settings = api.messageSettings()
        assertTrue(settings.enableNotifications)
        assertFalse(settings.enableEmail)
        assertTrue(settings.enableBrowserPush)
        assertEquals(setOf(1, 4, 8), settings.notificationTypes)
        assertEquals("23:00", settings.quietHoursStart)
        assertEquals("07:30", settings.quietHoursEnd)
        assertEquals(30, settings.autoReadAfterDays)

        val result = api.updateMessageSettings(
            settings.copy(
                enableEmail = true,
                notificationTypes = setOf(2, 8),
                autoReadAfterDays = null
            )
        )
        assertTrue(result.success)

        val get = server.takeRequest()
        assertEquals("GET", get.method)
        assertEquals("/api/messages/settings", get.requestUrl?.encodedPath)

        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/messages/settings", put.requestUrl?.encodedPath)
        JSONObject(put.body.readUtf8()).also { body ->
            assertTrue(body.getBoolean("enable_notifications"))
            assertTrue(body.getBoolean("enable_email"))
            assertTrue(body.getBoolean("enable_browser_push"))
            assertEquals(setOf(2, 8), body.getJSONArray("notification_types").let { types ->
                (0 until types.length()).map(types::getInt).toSet()
            })
            assertEquals("23:00", body.getString("quiet_hours_start"))
            assertEquals("07:30", body.getString("quiet_hours_end"))
            assertFalse(body.has("auto_read_after_days"))
        }
        Unit
    }

    @Test
    fun messageConversationUsesWebsiteQueryAndNormalizesMessages() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "list": [
                        {
                          "id": 901,
                          "message_content": "Hello",
                          "created_at": "2026-07-10T03:00:00Z",
                          "user_id": 20,
                          "execute_user_id": 10
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val messages = api.messageConversation(targetUserId = 20, page = 2, pageSize = 100)

        assertEquals(1, messages.size)
        assertEquals(901L, messages.single().id)
        assertEquals("Hello", messages.single().content)
        assertEquals("2026-07-10T03:00:00Z", messages.single().createdAt)
        assertEquals(20L, messages.single().userId)
        assertEquals(10L, messages.single().executeUserId)
        val request = server.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/messages/conversations", request.requestUrl?.encodedPath)
        assertEquals("20", request.requestUrl?.queryParameter("target_user_id"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("100", request.requestUrl?.queryParameter("page_size"))
    }

    @Test
    fun sendDirectMessageUsesCurrentWebsitePayload() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""{"success":true,"message":"sent"}""")
        )

        val result = api.sendDirectMessage(
            currentUserId = 10,
            targetUserId = 20,
            currentUserName = "Alice",
            content = "Hello from Android"
        )

        assertTrue(result.success)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/messages", request.requestUrl?.encodedPath)
        JSONObject(request.body.readUtf8()).also { body ->
            assertEquals(20L, body.getLong("user_id"))
            assertEquals(10L, body.getLong("execute_user_id"))
            assertEquals(8, body.getInt("message_type"))
            assertEquals("来自 Alice 的私信", body.getString("message_title"))
            assertEquals("Hello from Android", body.getString("message_content"))
        }
        Unit
    }

    @Test
    fun chapterContentNormalizesWebsiteBodyAliasesAndSendsReaderParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "session_id": "reader-session-plain",
                      "session_key": "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                      "expires": 1783472002
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "chapter": {
                          "chapter_name": "Reader Chapter",
                          "body_html": "",
                          "bodyHtml": "<p>Line one</p><p>Line two</p>"
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val content = api.chapterContent(9001)

        assertEquals("Reader Chapter", content.title)
        assertEquals("<p>Line one</p><p>Line two</p>", content.content)
        assertEquals("api", content.source)

        val sessionRequest = server.takeRequest()
        assertEquals("/api/reader/session-key", sessionRequest.requestUrl?.encodedPath)

        val request = server.takeRequest()
        assertEquals("/api/chapters/9001/content", request.requestUrl?.encodedPath)
        assertEquals("reader-session-plain", request.requestUrl?.queryParameter("session"))
        assertEquals("india", request.requestUrl?.queryParameter("replace_mode"))
        assertEquals("1", request.requestUrl?.queryParameter("show_images"))
    }

    @Test
    fun chapterContentNormalizesWebsiteIllustrationPlaceholders() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "session_id": "reader-session-illustrations",
                      "session_key": "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "data": {
                        "chapter": {
                          "chapter_name": "Illustrated Chapter",
                          "content": "<p>Before</p>[[img:2]]",
                          "illustrations": [
                            {"id": 12, "index": 2, "src": "/uploads/chapters/two.webp"}
                          ]
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val content = api.chapterContent(9003)

        assertEquals("Illustrated Chapter", content.title)
        assertEquals("<p>Before</p>[[img:2]]", content.content)
        assertEquals(1, content.illustrations.size)
        assertEquals(12L, content.illustrations.single().id)
        assertEquals(2, content.illustrations.single().index)
        assertEquals(server.url("/uploads/chapters/two.webp").toString(), content.illustrations.single().src)
    }

    @Test
    fun chapterContentRequestsSignedReaderSessionAndSendsSessionParameter() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "session_id": "reader-session-1",
                      "session_key": "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                      "expires": 1783472002
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "title": "Reader Chapter",
                      "content": "<p>Line one</p>"
                    }
                    """.trimIndent()
                )
        )

        val content = api.chapterContent(9001)

        assertEquals("<p>Line one</p>", content.content)

        val sessionRequest = server.takeRequest()
        assertEquals("/api/reader/session-key", sessionRequest.requestUrl?.encodedPath)
        assertEquals("GET", sessionRequest.method)
        assertTrue(sessionRequest.getHeader("X-Client-Signature").orEmpty().isNotBlank())
        assertTrue(sessionRequest.getHeader("X-Client-Timestamp").orEmpty().isNotBlank())
        assertTrue(sessionRequest.getHeader("X-Client-Nonce").orEmpty().isNotBlank())

        val contentRequest = server.takeRequest()
        assertEquals("/api/chapters/9001/content", contentRequest.requestUrl?.encodedPath)
        assertEquals("reader-session-1", contentRequest.requestUrl?.queryParameter("session"))
        assertEquals("india", contentRequest.requestUrl?.queryParameter("replace_mode"))
        assertEquals("1", contentRequest.requestUrl?.queryParameter("show_images"))
    }

    @Test
    fun chapterContentDecryptsWebsiteEncryptedPayloadWithReaderSessionKey() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "session_id": "reader-session-2",
                      "session_key": "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
                      "expires": 1783472002
                    }
                    """.trimIndent()
                )
        )
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "title": "Encrypted Reader Chapter",
                      "content": "Oxt8DM+9dnnEkTfRA/GixFl5MMWB",
                      "iv": "YWJjZGVmZ2hpamtsbW5vcA==",
                      "tag": "7Eo2Pe7NNaSedHR2BVxk1g==",
                      "encrypted": true
                    }
                    """.trimIndent()
                )
        )

        val content = api.chapterContent(9002)

        assertEquals("Encrypted Reader Chapter", content.title)
        assertEquals("Decrypted reader body", content.content)
        assertEquals("api", content.source)
    }

    @Test
    fun favoritesNormalizesFavoriteArrayAliasesAndSendsBookshelfParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "favorites": [
                          {
                            "novel_id": 777,
                            "novel_title": "Bookshelf Book",
                            "author": { "name": "Bookshelf Author" },
                            "cover_path": "/covers/bookshelf-book.jpg",
                            "favorites": 88,
                            "read_count": 7777,
                            "tags": [{ "name": "Saved" }]
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val books = api.favorites(page = 3, limit = 8)

        assertEquals(1, books.size)
        assertEquals(777L, books.single().id)
        assertEquals("Bookshelf Book", books.single().title)
        assertEquals("Bookshelf Author", books.single().author)
        assertEquals("${server.url("/").toString().trimEnd('/')}/covers/bookshelf-book.jpg", books.single().coverUrl)
        assertEquals(88L, books.single().favoriteCount)
        assertEquals(7777L, books.single().siteReadCount)
        assertEquals(listOf("Saved"), books.single().tags)

        val request = server.takeRequest()
        assertEquals("/api/favorites", request.requestUrl?.encodedPath)
        assertEquals("3", request.requestUrl?.queryParameter("page"))
        assertEquals("8", request.requestUrl?.queryParameter("limit"))
        assertEquals("updated_at", request.requestUrl?.queryParameter("sort_field"))
        assertEquals("desc", request.requestUrl?.queryParameter("sort_order"))
        assertEquals("novel", request.requestUrl?.queryParameter("type"))
    }

    @Test
    fun favoritesPreferWebsiteObjectIdAndReadLiveBookshelfCounters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "favorites": [
                        {
                          "id": 382566,
                          "object_id": 1673,
                          "object_name": "天下第一的青梅竹马",
                          "favorite_type": "novelPia",
                          "novel_title": "天下第一的青梅竹马",
                          "photo_url": "https://images.novelpia.com/imagebox/cover/live.file",
                          "novel_type": "武侠",
                          "author_name": "우비람",
                          "novel_read": 46973811,
                          "novel_like": 919,
                          "spans": "15 PLUS 独家 连载中",
                          "updated_at": "2026-07-11 01:10:08"
                        }
                      ]
                    }
                    """.trimIndent()
                )
        )

        val book = api.favorites(page = 1, limit = 1).single()

        assertEquals(1673L, book.id)
        assertEquals("天下第一的青梅竹马", book.title)
        assertEquals("novelPia", book.platform)
        assertEquals(46973811L, book.siteReadCount)
        assertEquals(919L, book.favoriteCount)
        assertEquals(listOf("武侠", "15", "PLUS", "独家", "连载中"), book.tags)
    }

    @Test
    fun favoritesCanRequestSpecificFavoriteGroup() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""{"data":{"favorites":[]}}""")
        )

        api.favorites(page = 2, limit = 10, groupId = 88)

        val request = server.takeRequest()
        assertEquals("/api/favorites", request.requestUrl?.encodedPath)
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("10", request.requestUrl?.queryParameter("limit"))
        assertEquals("88", request.requestUrl?.queryParameter("group_id"))
        assertEquals("novel", request.requestUrl?.queryParameter("type"))
    }

    @Test
    fun forumPostsNormalizeWebsiteAliasesAndSendReadonlyParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "posts": [
                          {
                            "post_id": 91,
                            "subject": "Native forum topic",
                            "type": "book_review",
                            "author": { "nickname": "Forum User" },
                            "novel": { "title": "Linked Novel" },
                            "reply_count": 7,
                            "like_count": 81,
                            "reaction_count": 12,
                            "award_points": 7,
                            "view_count": 7305,
                            "last_active_at": "2026-07-07T10:00:00Z",
                            "content": "<p>Readable excerpt</p>",
                            "tags": "hot,review",
                            "is_pinned": true,
                            "is_featured": true
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val posts = api.forumPosts(page = 2, limit = 6)

        assertEquals(1, posts.size)
        val post = posts.single()
        assertEquals(91L, post.id)
        assertEquals("书评", post.category)
        assertEquals("Native forum topic", post.title)
        assertEquals("Forum User", post.authorName)
        assertEquals("Linked Novel", post.bookTitle)
        assertEquals(7, post.replyCount)
        assertEquals(81, post.likeCount)
        assertEquals(12, post.reactionCount)
        assertEquals(7, post.awardPoints)
        assertEquals(7305, post.viewCount)
        assertEquals("2026-07-07T10:00:00Z", post.lastActiveLabel)
        assertEquals("Readable excerpt", post.excerpt)
        assertEquals(listOf("书评", "hot", "review"), post.tags)
        assertTrue(post.pinned)
        assertTrue(post.featured)

        val request = server.takeRequest()
        assertEquals("/api/posts", request.requestUrl?.encodedPath)
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("6", request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun createForumPostUsesWebsitePayloadAndReturnsCreatedPostId() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""{"post":{"id":92},"message":"created"}""")
        )

        val result = api.createForumPost(
            ForumCreateRequest(
                type = "discussion",
                title = "Native topic",
                content = "Body with **Markdown**",
                tags = listOf("android", "reader"),
                poll = ForumPollDraft(
                    question = "Choose one",
                    options = listOf("A", "B", "C"),
                    allowMultiple = true,
                    maxChoices = 2,
                    endsAt = "2026-07-20T08:00:00.000Z"
                )
            )
        )

        assertTrue(result.success)
        assertEquals("created", result.message)
        assertEquals(92L, result.postId)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/posts", request.requestUrl?.encodedPath)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("discussion", body.getString("type"))
        assertEquals("Native topic", body.getString("title"))
        assertEquals("Body with **Markdown**", body.getString("content"))
        assertEquals(listOf("android", "reader"), body.getJSONArray("tags").let { array ->
            (0 until array.length()).map(array::getString)
        })
        val poll = body.getJSONObject("poll")
        assertEquals("Choose one", poll.getString("question"))
        assertEquals(listOf("A", "B", "C"), poll.getJSONArray("options").let { array ->
            (0 until array.length()).map(array::getString)
        })
        assertTrue(poll.getBoolean("allowMultiple"))
        assertEquals(2, poll.getInt("maxChoices"))
        assertEquals("2026-07-20T08:00:00.000Z", poll.getString("endsAt"))
    }

    @Test
    fun forumPostDetailNormalizesWebsiteAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "post": {
                          "post_id": 91,
                          "subject": "Native forum topic",
                          "type": "discussion",
                          "body_html": "<p>Line one</p><p>https://example.test/very/long/path?x=1</p>",
                          "author": { "nickname": "Forum User" },
                          "novel": { "title": "Linked Novel" },
                          "reply_count": 7,
                          "like_count": 12,
                          "dislike_count": 2,
                          "reaction_count": 5,
                          "award_points": 30,
                          "created_at": "2026-07-07T10:00:00Z",
                          "tags": [{ "name": "讨论" }, "长链接"],
                          "is_pinned": true,
                          "featured": true
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val detail = api.forumPostDetail(91)

        assertEquals(91L, detail.post.id)
        assertEquals("讨论", detail.post.category)
        assertEquals("Native forum topic", detail.post.title)
        assertEquals("Forum User", detail.post.authorName)
        assertEquals("Linked Novel", detail.post.bookTitle)
        assertEquals(7, detail.post.replyCount)
        assertEquals("<p>Line one</p><p>https://example.test/very/long/path?x=1</p>", detail.content)
        assertEquals(12, detail.likeCount)
        assertEquals(2, detail.dislikeCount)
        assertEquals(5, detail.reactionCount)
        assertEquals(30, detail.awardPoints)
        assertEquals(listOf("讨论", "长链接"), detail.post.tags)
        assertTrue(detail.post.pinned)
        assertTrue(detail.post.featured)
        assertEquals("/api/posts/91", server.takeRequest().requestUrl?.encodedPath)
    }

    @Test
    fun forumPostCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "comments": [
                          {
                            "comment_id": 501,
                            "post_id": 91,
                            "parent_comment_id": 400,
                            "content_html": "<p>Reply body</p>",
                            "user": { "display_name": "Comment User" },
                            "reply_to_name": "Original User",
                            "like_count": 3,
                            "dislike_count": 1,
                            "reaction_count": 2,
                            "award_points": 10,
                            "created_at": "2026-07-07T11:00:00Z"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val comments = api.forumPostComments(postId = 91, page = 3, limit = 8)

        assertEquals(1, comments.size)
        val comment = comments.single()
        assertEquals(501L, comment.id)
        assertEquals(91L, comment.postId)
        assertEquals(400L, comment.parentCommentId)
        assertEquals("Comment User", comment.authorName)
        assertEquals("Original User", comment.replyToName)
        assertEquals("<p>Reply body</p>", comment.content)
        assertEquals(3, comment.likeCount)
        assertEquals(1, comment.dislikeCount)
        assertEquals(2, comment.reactionCount)
        assertEquals(10, comment.awardPoints)
        assertEquals("2026-07-07T11:00:00Z", comment.createdAt)

        val request = server.takeRequest()
        assertEquals("/api/posts/91/comments", request.requestUrl?.encodedPath)
        assertEquals("3", request.requestUrl?.queryParameter("page"))
        assertEquals("8", request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun chapterCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "comments": [
                          {
                            "comment_id": 701,
                            "book_id": 354491,
                            "chapter_id": 9901,
                            "parent_comment_id": 700,
                            "content_html": "<p>章节评论正文</p>",
                            "user": { "display_name": "章节读者" },
                            "reply_to_name": "楼主",
                            "like_count": 8,
                            "dislike_count": 1,
                            "reaction_count": 3,
                            "award_points": 20,
                            "created_at": "2026-07-08T02:50:00Z"
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val comments = api.chapterComments(bookId = 354491, chapterId = 9901, page = 2, limit = 6)

        assertEquals(1, comments.size)
        val comment = comments.single()
        assertEquals(701L, comment.id)
        assertEquals(354491L, comment.bookId)
        assertEquals(9901L, comment.chapterId)
        assertEquals(700L, comment.parentCommentId)
        assertEquals("章节读者", comment.authorName)
        assertEquals("楼主", comment.replyToName)
        assertEquals("<p>章节评论正文</p>", comment.content)
        assertEquals(8, comment.likeCount)
        assertEquals(1, comment.dislikeCount)
        assertEquals(3, comment.reactionCount)
        assertEquals(20, comment.awardPoints)
        assertEquals("2026-07-08T02:50:00Z", comment.createdAt)

        val request = server.takeRequest()
        assertEquals("/api/comments", request.requestUrl?.encodedPath)
        assertEquals("chapter", request.requestUrl?.queryParameter("type"))
        assertEquals("354491", request.requestUrl?.queryParameter("book_id"))
        assertEquals("9901", request.requestUrl?.queryParameter("chapter_id"))
        assertEquals("2", request.requestUrl?.queryParameter("page"))
        assertEquals("6", request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun bookCommentsNormalizeWebsiteAliasesAndSendReadonlyParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "success": true,
                      "comments": [
                        {
                          "id": 45570,
                          "type": "book",
                          "book_id": 354491,
                          "content": "book comment body",
                          "authorId": 102208,
                          "authorName": "Book Reader",
                          "likeCount": 4,
                          "helpfulCount": 2,
                          "notHelpfulCount": 1,
                          "funnyCount": 3,
                          "awardCount": 5,
                          "replyCount": 6,
                          "replies": [
                            {
                              "id": 45571,
                              "content": "reply body",
                              "authorName": "Responder",
                              "replyToName": "Book Reader",
                              "likeCount": 2,
                              "createdAt": "2026-06-28 01:00:00"
                            }
                          ],
                          "createdAt": "2026-06-28 00:58:13"
                        }
                      ],
                      "pagination": { "page": 1, "limit": 5, "total": 1, "pages": 1 }
                    }
                    """.trimIndent()
                )
        )

        val comments = api.bookComments(bookId = 354491, page = 1, limit = 5)

        assertEquals(2, comments.size)
        val comment = comments.first()
        assertEquals(45570L, comment.id)
        assertEquals(354491L, comment.bookId)
        assertEquals(null, comment.chapterId)
        assertEquals("Book Reader", comment.authorName)
        assertEquals("book comment body", comment.content)
        assertEquals(4, comment.likeCount)
        assertEquals(1, comment.dislikeCount)
        assertEquals(3, comment.reactionCount)
        assertEquals(5, comment.awardPoints)
        assertEquals("2026-06-28 00:58:13", comment.createdAt)
        val reply = comments.last()
        assertEquals(45571L, reply.id)
        assertEquals(354491L, reply.bookId)
        assertEquals(45570L, reply.parentCommentId)
        assertEquals("Responder", reply.authorName)
        assertEquals("Book Reader", reply.replyToName)
        assertEquals("reply body", reply.content)
        assertEquals(2, reply.likeCount)

        val request = server.takeRequest()
        assertEquals("/api/comments", request.requestUrl?.encodedPath)
        assertEquals("book", request.requestUrl?.queryParameter("type"))
        assertEquals("354491", request.requestUrl?.queryParameter("book_id"))
        assertEquals("1", request.requestUrl?.queryParameter("page"))
        assertEquals("5", request.requestUrl?.queryParameter("limit"))
    }

    @Test
    fun createBookAndChapterCommentsUseWebsiteMutationBodies() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"book ok"}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"chapter ok"}"""))

        val bookResult = api.createBookComment(bookId = 354491, content = "书籍评论")
        val chapterResult = api.createChapterComment(bookId = 354491, chapterId = 9901, content = "章节评论")

        assertTrue(bookResult.success)
        assertEquals("book ok", bookResult.message)
        assertTrue(chapterResult.success)
        assertEquals("chapter ok", chapterResult.message)

        val bookRequest = server.takeRequest()
        assertEquals("POST", bookRequest.method)
        assertEquals("/api/comments", bookRequest.requestUrl?.encodedPath)
        val bookBody = bookRequest.body.readUtf8()
        assertTrue(bookBody.contains("\"type\":\"book\""))
        assertTrue(bookBody.contains("\"book_id\":354491"))
        assertTrue(bookBody.contains("\"content\":\"书籍评论\""))

        val chapterRequest = server.takeRequest()
        assertEquals("POST", chapterRequest.method)
        assertEquals("/api/comments", chapterRequest.requestUrl?.encodedPath)
        val chapterBody = chapterRequest.body.readUtf8()
        assertTrue(chapterBody.contains("\"type\":\"chapter\""))
        assertTrue(chapterBody.contains("\"book_id\":354491"))
        assertTrue(chapterBody.contains("\"chapter_id\":9901"))
        assertTrue(chapterBody.contains("\"content\":\"章节评论\""))
    }

    @Test
    fun commentReplyLikeAndReactionUseWebsiteMutationEndpoints() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"reply ok"}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"reacted"}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"reply reacted"}"""))

        val reply = api.createCommentReply(commentId = 45570, content = "回复内容", replyToName = "beruuz")
        val like = api.toggleCommentLike(commentId = 45570)
        val reaction = api.reactToComment(commentId = 45570, reactionType = "award", awardPoints = 10)
        val replyReaction = api.reactToCommentReply(parentCommentId = 45570, replyId = 45571, reactionType = "emoji:heart")

        assertTrue(reply.success)
        assertEquals("reply ok", reply.message)
        assertTrue(like.success)
        assertTrue(reaction.success)
        assertEquals("reacted", reaction.message)
        assertTrue(replyReaction.success)
        assertEquals("reply reacted", replyReaction.message)

        val replyRequest = server.takeRequest()
        assertEquals("POST", replyRequest.method)
        assertEquals("/api/comments/45570/replies", replyRequest.requestUrl?.encodedPath)
        val replyBody = replyRequest.body.readUtf8()
        assertTrue(replyBody.contains("\"content\":\"回复内容\""))
        assertTrue(replyBody.contains("\"reply_to_name\":\"beruuz\""))

        val likeRequest = server.takeRequest()
        assertEquals("POST", likeRequest.method)
        assertEquals("/api/comments/45570/likes", likeRequest.requestUrl?.encodedPath)

        val reactionRequest = server.takeRequest()
        assertEquals("POST", reactionRequest.method)
        assertEquals("/api/comments/45570/reactions", reactionRequest.requestUrl?.encodedPath)
        val reactionBody = reactionRequest.body.readUtf8()
        assertTrue(reactionBody.contains("\"reaction_type\":\"award\""))
        assertTrue(reactionBody.contains("\"award_points\":10"))

        val replyReactionRequest = server.takeRequest()
        assertEquals("POST", replyReactionRequest.method)
        assertEquals("/api/comments/45570/replies/45571/reactions", replyReactionRequest.requestUrl?.encodedPath)
        val replyReactionBody = replyReactionRequest.body.readUtf8()
        assertTrue(replyReactionBody.contains("\"reaction_type\":\"emoji:heart\""))
    }

    @Test
    fun createForumCommentPostsContentAndReplyMetadata() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody("""{"success":true,"message":"ok"}""")
        )

        val result = api.createForumComment(
            postId = 91,
            content = "回复正文",
            parentCommentId = 501,
            replyToName = "Comment User"
        )

        assertTrue(result.success)
        assertEquals("ok", result.message)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/posts/91/comments", request.requestUrl?.encodedPath)
        val body = request.body.readUtf8()
        assertTrue(body.contains("\"content\":\"回复正文\""))
        assertTrue(body.contains("\"comment_id\":501"))
        assertTrue(body.contains("\"reply_to_name\":\"Comment User\""))
    }

    @Test
    fun forumPostLikeAndReactionUseWebsiteMutationEndpoints() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"awarded"}"""))

        assertTrue(api.toggleForumPostLike(91).success)
        assertTrue(api.reactToForumPost(91, reactionType = "emoji:heart", awardPoints = 20).success)

        val like = server.takeRequest()
        assertEquals("POST", like.method)
        assertEquals("/api/posts/91/likes", like.requestUrl?.encodedPath)

        val reaction = server.takeRequest()
        assertEquals("POST", reaction.method)
        assertEquals("/api/posts/91/reactions", reaction.requestUrl?.encodedPath)
        val body = reaction.body.readUtf8()
        assertTrue(body.contains("\"reaction_type\":\"emoji:heart\""))
        assertTrue(body.contains("\"award_points\":20"))
    }

    @Test
    fun forumCommentLikeAndReactionUseWebsiteMutationEndpoints() = runBlocking {
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true}"""))
        server.enqueue(MockResponse().setHeader("content-type", "application/json").setBody("""{"success":true,"message":"comment reacted"}"""))

        assertTrue(api.toggleForumCommentLike(501).success)
        assertTrue(api.reactToForumComment(501, reactionType = "down", awardPoints = 5).success)

        val like = server.takeRequest()
        assertEquals("POST", like.method)
        assertEquals("/api/comments/501/likes", like.requestUrl?.encodedPath)

        val reaction = server.takeRequest()
        assertEquals("POST", reaction.method)
        assertEquals("/api/comments/501/reactions", reaction.requestUrl?.encodedPath)
        val body = reaction.body.readUtf8()
        assertTrue(body.contains("\"reaction_type\":\"down\""))
        assertTrue(body.contains("\"award_points\":5"))
    }

    @Test
    fun currentUserNormalizesWebsiteProfileAliases() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "profile": {
                          "uid": 42,
                          "nickname": "Native Admin",
                          "user_role": "admin"
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val user = api.currentUser()

        assertEquals(42L, user.id)
        assertEquals("Native Admin", user.name)
        assertEquals("admin", user.role)
        assertEquals("/api/users/me", server.takeRequest().path)
    }

    @Test
    fun favoriteStatusNormalizesWebsiteStatusAliasesAndSendsParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "isFavorite": true,
                        "status_text": "added",
                        "favorite_group": {
                          "id": 9
                        }
                      }
                    }
                    """.trimIndent()
                )
        )

        val status = api.favoriteStatus(354491)

        assertTrue(status.isFavorited)
        assertEquals(9L, status.groupId)
        assertEquals("added", status.rawState)

        val request = server.takeRequest()
        assertEquals("/api/favorites/status", request.requestUrl?.encodedPath)
        assertEquals("354491", request.requestUrl?.queryParameter("object_id"))
        assertEquals("novel", request.requestUrl?.queryParameter("type"))
    }

    @Test
    fun favoriteGroupsNormalizesWebsiteGroupAliasesAndSendsPreviewParameters() = runBlocking {
        server.enqueue(
            MockResponse()
                .setHeader("content-type", "application/json")
                .setBody(
                    """
                    {
                      "data": {
                        "favorite_groups": [
                          {
                            "group_id": 12,
                            "group_name": "Reading Now",
                            "book_count": 4
                          }
                        ]
                      }
                    }
                    """.trimIndent()
                )
        )

        val groups = api.favoriteGroups()

        assertEquals(1, groups.size)
        assertEquals(12L, groups.single().id)
        assertEquals("Reading Now", groups.single().name)
        assertEquals(4, groups.single().count)

        val request = server.takeRequest()
        assertEquals("/api/favorites/groups", request.requestUrl?.encodedPath)
        assertEquals("6", request.requestUrl?.queryParameter("preview_limit"))
        assertEquals("true", request.requestUrl?.queryParameter("with_preview"))
    }
}
