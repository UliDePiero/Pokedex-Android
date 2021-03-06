package com.github.utn.frba.mobile.dextracker.db.storage

import android.content.Context
import com.github.utn.frba.mobile.dextracker.data.Favourite
import com.github.utn.frba.mobile.dextracker.data.Subscription
import com.github.utn.frba.mobile.dextracker.db.db
import com.github.utn.frba.mobile.dextracker.db.table.FavouritesTable
import com.github.utn.frba.mobile.dextracker.db.table.PokedexTable
import com.github.utn.frba.mobile.dextracker.db.table.SessionTable
import com.github.utn.frba.mobile.dextracker.db.table.SubscriptionTable
import com.github.utn.frba.mobile.dextracker.extensions.mapToSet
import com.github.utn.frba.mobile.dextracker.model.PokedexRef
import com.github.utn.frba.mobile.dextracker.model.Session

class SessionStorage(context: Context) {
    private val sessionDao = db(context).sessionDao()
    private val pokedexDao = db(context).pokedexDao()
    private val favouritesDao = db(context).favouritesDao()
    private val subscriptionsDao = db(context).subscriptionsDao()

    suspend fun get(): Session? = sessionDao.storedSession()?.let { session ->
        val pokedex = pokedexDao.findAll(session.userId)
        val favourites = favouritesDao.favourites(session.userId)
        val subscriptions = subscriptionsDao.subscriptions(session.userId)

        Session(
            userId = session.userId,
            dexToken = session.dexToken,
            pokedex = pokedex.map {
                PokedexRef(
                    id = it.id,
                    caught = it.caught,
                    total = it.total,
                    game = it.game,
                    name = it.dexName,
                )
            },
            favourites = favourites.map {
                Favourite(
                    species = it.species,
                    dexId = it.dexId,
                    gen = it.gen,
                )
            },
            subscriptions = subscriptions.mapToSet {
                Subscription(
                    subscriptionId = it.id,
                    dexId = it.subscribedDexId,
                    userId = it.subscribedDexUserId,
                )
            }
        )
    }

    suspend fun store(session: Session) {
        favouritesDao.drop()
        pokedexDao.drop()
        sessionDao.drop()
        subscriptionsDao.drop()

        sessionDao.save(
            SessionTable(
                dexToken = session.dexToken,
                userId = session.userId,
            )
        )
        pokedexDao.saveAll(session.pokedex.map {
            PokedexTable(
                id = it.id,
                caught = it.caught,
                total = it.total,
                game = it.game,
                userOwnerId = session.userId,
            )
        })
        favouritesDao.saveAll(session.favourites.map {
            FavouritesTable(
                gen = it.gen,
                dexId = it.dexId,
                species = it.species,
                ownerUserId = session.userId,
            )
        })
        subscriptionsDao.saveAll(session.subscriptions.map {
            SubscriptionTable(
                subscribedDexId = it.dexId,
                subscribedDexUserId = it.userId,
                id = it.subscriptionId,
                ownerUserId = session.userId,
            )
        })
    }
}