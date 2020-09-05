package nhdphuong.com.manga.data.repository

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import nhdphuong.com.manga.Logger
import nhdphuong.com.manga.data.MasterDataSource
import nhdphuong.com.manga.data.entity.LatestAppVersion
import nhdphuong.com.manga.data.entity.book.tags.Artist
import nhdphuong.com.manga.data.entity.book.tags.Tag
import nhdphuong.com.manga.data.entity.book.tags.Character
import nhdphuong.com.manga.data.entity.book.tags.Group
import nhdphuong.com.manga.data.entity.book.tags.Parody
import nhdphuong.com.manga.scope.Local
import nhdphuong.com.manga.scope.Remote
import nhdphuong.com.manga.scope.corountine.IO
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MasterDataRepository @Inject constructor(
    @Remote private val mMasterRemoteDataSource: MasterDataSource.Remote,
    @Local private val mMasterLocalDataSource: MasterDataSource.Local,
    @IO private val io: CoroutineScope
) {
    companion object {
        private const val TAG = "TagRepository"
        private const val TOTAL_TAG_TYPES = 8
    }

    @Volatile
    private var isDownloading: Boolean = false

    fun fetchAllTagLists(onComplete: (Boolean) -> Unit) {
        Logger.d(TAG, "fetchAllTagLists, is downloading started=$isDownloading")
        if (isDownloading) {
            return
        }
        isDownloading = true
        val tagCount = AtomicInteger(0)
        val successCount = AtomicInteger(0)
        val handleResponse: (Boolean) -> Unit = { success ->
            if (success) {
                successCount.incrementAndGet()
            }
            if (tagCount.incrementAndGet() >= TOTAL_TAG_TYPES) {
                onComplete(successCount.get() >= TOTAL_TAG_TYPES)
                isDownloading = false
            }
        }

        io.launch {
            launch {
                mMasterRemoteDataSource.fetchArtistsList(onSuccess = { artists ->
                    Logger.d(TAG, "artists=${artists?.size ?: 0}")
                    handleResponse(artists != null)
                    artists?.let {
                        mMasterLocalDataSource.insertArtistsList(artists)
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchCategoriesList(onSuccess = { categories ->
                    Logger.d(TAG, "categories=${categories?.size ?: 0}")
                    handleResponse(categories != null)
                    categories?.let {
                        mMasterLocalDataSource.insertCategoriesList(categories)
                        return@fetchCategoriesList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchCharactersList(onSuccess = { characters ->
                    Logger.d(TAG, "characters=${characters?.size ?: 0}")
                    handleResponse(characters != null)
                    characters?.let {
                        mMasterLocalDataSource.insertCharactersList(characters)
                        return@fetchCharactersList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchGroupsList(onSuccess = { groups ->
                    Logger.d(TAG, "groups=${groups?.size ?: 0}")
                    handleResponse(groups != null)
                    groups?.let {
                        mMasterLocalDataSource.insertGroupsList(groups)
                        return@fetchGroupsList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchLanguagesList(onSuccess = { languages ->
                    Logger.d(TAG, "languages=${languages?.size ?: 0}")
                    handleResponse(languages != null)
                    languages?.let {
                        mMasterLocalDataSource.insertLanguagesList(languages)
                        return@fetchLanguagesList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchParodiesList(onSuccess = { parodies ->
                    Logger.d(TAG, "parodies=${parodies?.size ?: 0}")
                    handleResponse(parodies != null)
                    parodies?.let {
                        mMasterLocalDataSource.insertParodiesList(parodies)
                        return@fetchParodiesList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchTagsList(onSuccess = { tags ->
                    Logger.d(TAG, "tags=${tags?.size ?: 0}")
                    handleResponse(tags != null)
                    tags?.let {
                        mMasterLocalDataSource.insertTagsList(tags)
                        return@fetchTagsList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
            launch {
                mMasterRemoteDataSource.fetchUnknownTypesList(onSuccess = { unknownTypes ->
                    Logger.d(TAG, "unknownTypes=${unknownTypes?.size ?: 0}")
                    handleResponse(unknownTypes != null)
                    unknownTypes?.let {
                        mMasterLocalDataSource.insertUnknownTypesList(unknownTypes)
                        return@fetchUnknownTypesList
                    }
                }, onError = {
                    handleResponse(false)
                })
            }
        }
    }

    suspend fun getTagCount(): Int = mMasterLocalDataSource.getTagCount()

    suspend fun getTagsCountByPrefix(firstChar: Char): Int {
        return mMasterLocalDataSource.getTagCountByPrefix("$firstChar%")
    }

    suspend fun getTagCountBySpecialCharactersPrefix(): Int {
        return mMasterLocalDataSource.getTagCountBySpecialCharactersPrefix()
    }

    suspend fun getTagsByPrefixAscending(prefixChar: Char, limit: Int, offset: Int): List<Tag> {
        return mMasterLocalDataSource.getTagsByPrefixAscending(
            "$prefixChar%", limit, offset
        )
    }

    suspend fun getTagsBySpecialCharactersPrefixAscending(limit: Int, offset: Int): List<Tag> {
        return mMasterLocalDataSource.getTagsBySpecialCharactersPrefixAscending(limit, offset)
    }

    @Suppress("unused")
    suspend fun getTagsByPopularityAscending(limit: Int, offset: Int): List<Tag> {
        return mMasterLocalDataSource.getTagsByPopularityAscending(limit, offset)
    }

    suspend fun getTagsByPopularityDescending(limit: Int, offset: Int): List<Tag> {
        return mMasterLocalDataSource.getTagsByPopularityDescending(limit, offset)
    }

    suspend fun getArtistsCount(): Int = mMasterLocalDataSource.getArtistsCount()

    suspend fun getArtistsCountByPrefix(firstChar: Char): Int {
        return mMasterLocalDataSource.getArtistsCountByPrefix("$firstChar%")
    }

    suspend fun getArtistsCountBySpecialCharactersPrefix(): Int {
        return mMasterLocalDataSource.getArtistsCountBySpecialCharactersPrefix()
    }

    suspend fun getArtistsByPrefixAscending(
        prefixChar: Char,
        limit: Int,
        offset: Int
    ): List<Artist> {
        return mMasterLocalDataSource.getArtistsByPrefixAscending(
            "$prefixChar%", limit, offset
        )
    }

    suspend fun getArtistsBySpecialCharactersPrefixAscending(
        limit: Int,
        offset: Int
    ): List<Artist> {
        return mMasterLocalDataSource.getArtistsBySpecialCharactersPrefixAscending(limit, offset)
    }

    @Suppress("unused")
    suspend fun getArtistsByPopularityAscending(
        limit: Int,
        offset: Int
    ): List<Artist> = mMasterLocalDataSource.getArtistsByPopularityAscending(limit, offset)

    suspend fun getArtistsByPopularityDescending(
        limit: Int,
        offset: Int
    ): List<Artist> = mMasterLocalDataSource.getArtistsByPopularityDescending(limit, offset)

    suspend fun getCharactersCount(): Int {
        return mMasterLocalDataSource.getCharactersCount()
    }

    suspend fun getCharactersCountByPrefix(firstChar: Char): Int {
        return mMasterLocalDataSource.getCharactersCountByPrefix("$firstChar%")
    }

    suspend fun getCharactersCountBySpecialCharactersPrefix(): Int {
        return mMasterLocalDataSource.getCharactersCountBySpecialCharactersPrefix()
    }

    suspend fun getCharactersByPrefixAscending(
        prefixChar: Char,
        limit: Int,
        offset: Int
    ): List<Character> {
        return mMasterLocalDataSource.getCharactersByPrefixAscending(
            "$prefixChar%", limit, offset
        )
    }

    suspend fun getCharactersBySpecialCharactersPrefixAscending(
        limit: Int,
        offset: Int
    ): List<Character> {
        return mMasterLocalDataSource.getCharactersBySpecialCharactersPrefixAscending(limit, offset)
    }

    @Suppress("unused")
    suspend fun getCharactersByPopularityAscending(
        limit: Int,
        offset: Int
    ): List<Character> = mMasterLocalDataSource.getCharactersByPopularityAscending(limit, offset)

    suspend fun getCharactersByPopularityDescending(
        limit: Int,
        offset: Int
    ): List<Character> = mMasterLocalDataSource.getCharactersByPopularityDescending(limit, offset)

    suspend fun getGroupsCount(): Int = mMasterLocalDataSource.getGroupsCount()

    suspend fun getGroupsCountByPrefix(firstChar: Char): Int {
        return mMasterLocalDataSource.getGroupsCountByPrefix("$firstChar%")
    }

    suspend fun getGroupsCountBySpecialCharactersPrefix(): Int {
        return mMasterLocalDataSource.getGroupsCountBySpecialCharactersPrefix()
    }

    suspend fun getGroupsByPrefixAscending(
        prefixChar: Char,
        limit: Int,
        offset: Int
    ): List<Group> {
        return mMasterLocalDataSource.getGroupsByPrefixAscending(
            "$prefixChar%", limit, offset
        )
    }

    suspend fun getGroupsBySpecialCharactersPrefixAscending(
        limit: Int,
        offset: Int
    ): List<Group> =
        mMasterLocalDataSource.getGroupsBySpecialCharactersPrefixAscending(limit, offset)

    @Suppress("unused")
    suspend fun getGroupsByPopularityAscending(
        limit: Int,
        offset: Int
    ): List<Group> = mMasterLocalDataSource.getGroupsByPopularityAscending(limit, offset)

    suspend fun getGroupsByPopularityDescending(
        limit: Int,
        offset: Int
    ): List<Group> = mMasterLocalDataSource.getGroupsByPopularityDescending(limit, offset)

    suspend fun getParodiesCount(): Int = mMasterLocalDataSource.getParodiesCount()

    suspend fun getParodiesCountByPrefix(firstChar: Char): Int {
        return mMasterLocalDataSource.getParodiesCountByPrefix("$firstChar%")
    }

    suspend fun getParodiesCountBySpecialCharactersPrefix(): Int {
        return mMasterLocalDataSource.getParodiesCountBySpecialCharactersPrefix()
    }

    suspend fun getParodiesByPrefixAscending(
        prefixChar: Char,
        limit: Int,
        offset: Int
    ): List<Parody> {
        return mMasterLocalDataSource.getParodiesByPrefixAscending(
            "$prefixChar%", limit, offset
        )
    }

    suspend fun getParodiesBySpecialCharactersPrefixAscending(
        limit: Int,
        offset: Int
    ): List<Parody> {
        return mMasterLocalDataSource.getParodiesBySpecialCharactersPrefixAscending(limit, offset)
    }

    @Suppress("unused")
    suspend fun getParodiesByPopularityAscending(
        limit: Int,
        offset: Int
    ): List<Parody> = mMasterLocalDataSource.getParodiesByPopularityAscending(limit, offset)

    suspend fun getParodiesByPopularityDescending(
        limit: Int,
        offset: Int
    ): List<Parody> = mMasterLocalDataSource.getParodiesByPopularityDescending(limit, offset)


    fun getTagDataVersion(onSuccess: (Long) -> Unit, onError: () -> Unit) {
        io.launch {
            mMasterRemoteDataSource.fetchTagDataVersion(onSuccess, onError)
        }
    }

    fun getAppVersion(onSuccess: (LatestAppVersion) -> Unit, onError: (error: Throwable) -> Unit) {
        io.launch {
            mMasterRemoteDataSource.fetchAppVersion(onSuccess, onError)
        }
    }
}
