package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "t_message")
class Messages(
    @Id
    var messageKey: String = "",
    var messageId: Long = 0
)
