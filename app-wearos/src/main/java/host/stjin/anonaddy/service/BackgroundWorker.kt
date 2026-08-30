package host.stjin.anonaddy.service

import android.content.Context
import androidx.wear.tiles.TileService
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import host.stjin.anonaddy.ServiceLocator
import host.stjin.anonaddy.tiles.PinnedAliasesTileService
import host.stjin.anonaddy_shared.network.NetworkResult

/*
This BackgroundWorker is used for obtaining data in the background, this data is then being used to "Watch" aliases and updating the data the widget uses.
 */

class BackgroundWorker(private val ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {

    private fun updateTiles() {
        TileService.getUpdater(ctx)
            .requestUpdate(PinnedAliasesTileService::class.java)
    }

    override suspend fun doWork(): Result {

        val userRepository = ServiceLocator.userRepository
        val aliasRepository = ServiceLocator.aliasRepository

        val userResourceResult = userRepository.cacheUserResourceForWidget()
        val aliasResult = aliasRepository.cacheLastUpdatedAliasesData()
        val pinnedAliasResult = aliasRepository.cachePinnedAliasesData()

        return if (userResourceResult is NetworkResult.Success<Boolean> && userResourceResult.data &&
            aliasResult is NetworkResult.Success<Boolean> && aliasResult.data &&
            pinnedAliasResult is NetworkResult.Success<Boolean> && pinnedAliasResult.data
        ) {
            // Now the data has been updated, we can update the tiles as well
            updateTiles()
            Result.success()
        } else {
            Result.failure()
        }
    }
}
