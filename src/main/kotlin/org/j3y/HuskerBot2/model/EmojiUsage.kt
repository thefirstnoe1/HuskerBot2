package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(name = "t_emoji_usage", indexes = [Index(name = "emoji_usage_emoji_name_idx", columnList = "emojiName")])
class EmojiUsage(
    @Id var emojiId: Long = 0,
    var emojiName: String = "",
    var count: Int = 0,
    var imageUrl: String = ""
)
