package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.EmojiUsage
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface EmojiUsageRepo : JpaRepository<EmojiUsage, Long> {
    fun findByEmojiName(emojiName: String): EmojiUsage?
    fun findAllByOrderByCountDesc(): List<EmojiUsage>
}
