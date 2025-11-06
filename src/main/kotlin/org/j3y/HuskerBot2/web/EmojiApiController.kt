package org.j3y.HuskerBot2.web

import org.j3y.HuskerBot2.model.EmojiUsage
import org.j3y.HuskerBot2.repository.EmojiUsageRepo
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/emoji")
class EmojiApiController(
    private val emojiUsageRepo: EmojiUsageRepo
) {
    data class EmojiUsageDto(
        val emojiId: Long,
        val emojiName: String,
        val imageUrl: String,
        val count: Int
    )

    @GetMapping("/usage")
    fun usage(): List<EmojiUsageDto> =
        emojiUsageRepo.findAllByOrderByCountDesc().map { it.toDto() }

    private fun EmojiUsage.toDto() = EmojiUsageDto(
        emojiId = this.emojiId,
        emojiName = this.emojiName,
        imageUrl = this.imageUrl,
        count = this.count
    )
}
