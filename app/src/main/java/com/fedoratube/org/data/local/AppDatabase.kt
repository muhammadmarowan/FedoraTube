package com.fedoratube.org.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import com.fedoratube.org.data.local.dao.CacheDao
import com.fedoratube.org.data.local.dao.DownloadCollectionDao
import com.fedoratube.org.data.local.dao.DownloadDao
import com.fedoratube.org.data.local.dao.HomeFeedCacheDao
import com.fedoratube.org.data.local.dao.MusicGraphDao
import com.fedoratube.org.data.local.dao.NoteDao
import com.fedoratube.org.data.local.dao.NotificationDao
import com.fedoratube.org.data.local.dao.PlaylistDao
import com.fedoratube.org.data.local.dao.RecognitionHistoryDao
import com.fedoratube.org.data.local.dao.SubscriptionGroupDao
import com.fedoratube.org.data.local.dao.SyncLogDao
import com.fedoratube.org.data.local.dao.SyncPeerDao
import com.fedoratube.org.data.local.dao.VideoDao
import com.fedoratube.org.data.local.dao.WatchHistoryDao
import com.fedoratube.org.data.local.entity.DownloadCollectionEntity
import com.fedoratube.org.data.local.entity.DownloadCollectionItemEntity
import com.fedoratube.org.data.local.entity.DownloadEntity
import com.fedoratube.org.data.local.entity.DownloadItemEntity
import com.fedoratube.org.data.local.entity.HomeFeedCacheEntity
import com.fedoratube.org.data.local.entity.MusicGraphAlbumEntity
import com.fedoratube.org.data.local.entity.MusicGraphArtistEntity
import com.fedoratube.org.data.local.entity.MusicGraphEdgeEntity
import com.fedoratube.org.data.local.entity.MusicGraphPlaylistEntity
import com.fedoratube.org.data.local.entity.MusicGraphTrackEntity
import com.fedoratube.org.data.local.entity.MusicHomeCacheEntity
import com.fedoratube.org.data.local.entity.MusicHomeChipEntity
import com.fedoratube.org.data.local.entity.NoteEntity
import com.fedoratube.org.data.local.entity.NotificationEntity
import com.fedoratube.org.data.local.entity.PlaylistEntity
import com.fedoratube.org.data.local.entity.PlaylistVideoCrossRef
import com.fedoratube.org.data.local.entity.RecognitionHistoryEntity
import com.fedoratube.org.data.local.entity.SubscriptionFeedEntity
import com.fedoratube.org.data.local.entity.SubscriptionGroupEntity
import com.fedoratube.org.data.local.entity.SyncLogEntity
import com.fedoratube.org.data.local.entity.SyncPeerEntity
import com.fedoratube.org.data.local.entity.VideoEntity
import com.fedoratube.org.data.local.entity.WatchHistoryEntity
import com.fedoratube.org.data.local.migrations.MIGRATIONS
import com.fedoratube.org.data.local.migrations.Migration24To25

@Database(
    entities = [
        VideoEntity::class,
        PlaylistEntity::class,
        PlaylistVideoCrossRef::class,
        NotificationEntity::class,
        SubscriptionFeedEntity::class,
        MusicHomeCacheEntity::class,
        MusicHomeChipEntity::class,
        DownloadEntity::class,
        DownloadItemEntity::class,
        WatchHistoryEntity::class,
        HomeFeedCacheEntity::class,
        SubscriptionGroupEntity::class,
        RecognitionHistoryEntity::class,
        SyncLogEntity::class,
        SyncPeerEntity::class,
        MusicGraphTrackEntity::class,
        MusicGraphArtistEntity::class,
        MusicGraphAlbumEntity::class,
        MusicGraphPlaylistEntity::class,
        MusicGraphEdgeEntity::class,
        NoteEntity::class,
        DownloadCollectionEntity::class,
        DownloadCollectionItemEntity::class,
    ],
    autoMigrations = [
        AutoMigration(from = 24, to = 25, spec = Migration24To25::class),
        AutoMigration(from = 25, to = 26),
        AutoMigration(from = 26, to = 27),
        AutoMigration(from = 27, to = 28),
        AutoMigration(from = 28, to = 29),
        AutoMigration(from = 29, to = 30),
        AutoMigration(from = 30, to = 31),
        AutoMigration(from = 31, to = 32),
        AutoMigration(from = 32, to = 33),
    ],
    version = 33,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    abstract fun playlistDao(): PlaylistDao

    abstract fun notificationDao(): NotificationDao

    abstract fun noteDao(): NoteDao

    abstract fun cacheDao(): CacheDao

    abstract fun downloadDao(): DownloadDao

    abstract fun downloadCollectionDao(): DownloadCollectionDao

    abstract fun watchHistoryDao(): WatchHistoryDao

    abstract fun homeFeedCacheDao(): HomeFeedCacheDao

    abstract fun subscriptionGroupDao(): SubscriptionGroupDao

    abstract fun recognitionHistoryDao(): RecognitionHistoryDao

    abstract fun syncLogDao(): SyncLogDao

    abstract fun syncPeerDao(): SyncPeerDao

    abstract fun musicGraphDao(): MusicGraphDao

    companion object {
        @Volatile
        @Suppress("ktlint:standard:property-naming")
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: android.content.Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                val instance =
                    databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "flow_database",
                    ).addMigrations(*MIGRATIONS)
                        .fallbackToDestructiveMigration(false)
                        .build()
                INSTANCE = instance
                instance
            }
    }
}
