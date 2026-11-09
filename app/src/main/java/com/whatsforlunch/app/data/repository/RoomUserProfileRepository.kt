package com.whatsforlunch.app.data.repository

import com.whatsforlunch.app.data.local.ProfileDao
import com.whatsforlunch.app.data.local.ProfileEntity
import com.whatsforlunch.app.data.local.ProfileRestrictionEntity
import com.whatsforlunch.app.domain.model.DietType
import com.whatsforlunch.app.domain.model.ProfileSection
import com.whatsforlunch.app.domain.model.UserProfile
import com.whatsforlunch.app.domain.repository.UserProfileRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Adaptador Room del perfil (`crear-mi-perfil`, D1): la fila única guarda tipo de
// alimentación y las marcas «No tengo ninguna»; las chips se reescriben enteras
// en cada guardado para conservar su orden de alta (RD25). Sin fila aún, el flujo
// emite el perfil vacío (app recién instalada, caso límite D6).
class RoomUserProfileRepository(private val dao: ProfileDao) : UserProfileRepository {
    private val saveMutex = Mutex()

    override fun observeProfile(): Flow<UserProfile> =
        combine(
            dao.observeRow(ProfileEntity.SINGLE_ID),
            dao.observeRestrictions(),
        ) { row, restrictions -> row.toDomain(restrictions) }

    override suspend fun save(profile: UserProfile) = saveMutex.withLock {
        val allergies = profile.allergies.map {
            ProfileRestrictionEntity(section = ProfileSection.ALLERGY.name, value = it)
        }
        val intolerances = profile.intolerances.map {
            ProfileRestrictionEntity(section = ProfileSection.INTOLERANCE.name, value = it)
        }
        dao.replaceProfile(
            row = ProfileEntity(
                id = ProfileEntity.SINGLE_ID,
                dietType = profile.dietType?.name,
                allergiesNone = profile.allergiesNone,
                intolerancesNone = profile.intolerancesNone,
            ),
            // Orden de alta: alergias primero e intolerancias después, cada lista
            // en su orden; el id autoincremental fija el orden de lectura.
            restrictions = allergies + intolerances,
        )
    }
}

private fun ProfileEntity?.toDomain(restrictions: List<ProfileRestrictionEntity>): UserProfile {
    val allergies = restrictions
        .filter { it.section == ProfileSection.ALLERGY.name }
        .map { it.value }
    val intolerances = restrictions
        .filter { it.section == ProfileSection.INTOLERANCE.name }
        .map { it.value }
    if (this == null) {
        return UserProfile(allergies = allergies, intolerances = intolerances)
    }
    return UserProfile(
        allergies = allergies,
        allergiesNone = allergiesNone,
        intolerances = intolerances,
        intolerancesNone = intolerancesNone,
        dietType = dietType?.let { name -> DietType.entries.firstOrNull { it.name == name } },
    )
}
