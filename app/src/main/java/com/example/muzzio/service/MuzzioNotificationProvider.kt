package com.example.muzzio.service

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.example.muzzio.R
import com.google.common.collect.ImmutableList

/**
 * Обычное уведомление Media3, но с нашими чёткими иконками
 * предыдущий / пауза-играть / следующий.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class MuzzioNotificationProvider(context: Context) : DefaultMediaNotificationProvider(context) {

    override fun getMediaButtons(
        session: MediaSession,
        playerCommands: Player.Commands,
        customLayout: ImmutableList<CommandButton>,
        showPauseButton: Boolean
    ): ImmutableList<CommandButton> {
        val original = super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
        val result = ImmutableList.builder<CommandButton>()

        for (button in original) {
            val icon = if (button.sessionCommand != null) {
                null // пользовательские кнопки не трогаем
            } else {
                when (button.playerCommand) {
                    Player.COMMAND_SEEK_TO_PREVIOUS,
                    Player.COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM -> R.drawable.ic_notif_prev

                    Player.COMMAND_PLAY_PAUSE ->
                        if (showPauseButton) R.drawable.ic_notif_pause else R.drawable.ic_notif_play

                    Player.COMMAND_SEEK_TO_NEXT,
                    Player.COMMAND_SEEK_TO_NEXT_MEDIA_ITEM -> R.drawable.ic_notif_next

                    else -> null
                }
            }

            if (icon == null) {
                result.add(button)
            } else {
                result.add(
                    CommandButton.Builder()
                        .setPlayerCommand(button.playerCommand)
                        .setIconResId(icon)
                        .setDisplayName(button.displayName)
                        .setExtras(button.extras)
                        .setEnabled(button.isEnabled)
                        .build()
                )
            }
        }
        return result.build()
    }
}
