package com.example.muzzio.domain.usecase

import com.example.muzzio.data.repository.TrackRepository
import javax.inject.Inject

class GetTracksUseCase @Inject constructor(
    private val repo: TrackRepository
) {
    operator fun invoke() = repo.getDeviceTracks()
}
