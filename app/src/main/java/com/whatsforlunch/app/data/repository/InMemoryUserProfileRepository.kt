package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Perfil en memoria para tests (patrón de `InMemoryPantryRepository`): emite el
// perfil inicial y `save` lo sustituye completo.
class InMemoryUserProfileRepository(
    initial: UserProfile = UserProfile.empty,
) : UserProfileRepository {
    private val profile = MutableStateFlow(initial)
    private val mutex = Mutex()

    override fun observeProfile(): Flow<UserProfile> = profile.asStateFlow()

    override suspend fun save(profile: UserProfile) {
        mutex.withLock { this.profile.value = profile }
    }
}
