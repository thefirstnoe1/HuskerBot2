package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "t_user")
class User(
    @Id
    var userId: String = "",
    var isAdmin: Boolean = false,
    var isBlocked: Boolean = false
)
