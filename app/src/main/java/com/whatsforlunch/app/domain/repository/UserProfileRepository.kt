package com.whatsforlunch.app.domain.repository

import com.whatsforlunch.app.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

// Puerto del perfil del usuario (`crear-mi-perfil`, D1): un único perfil local
// (Room en producción, memoria en tests). `observeProfile` emite siempre un
// perfil (el vacío si aún no hay fila) y `save` persiste el estado completo.
interface UserProfileRepository {
    fun observeProfile(): Flow<UserProfile>

    suspend fun save(profile: UserProfile)
}
