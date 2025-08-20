package org.j3y.HuskerBot2.service

import org.j3y.HuskerBot2.model.User

interface UserService {
    fun setUserBlocked(userTag: String, isBlocked: Boolean): User
    fun setUserAdmin(userTag: String, isAdmin: Boolean): User

    fun isUserBlocked(userTag: String): Boolean
    fun isUserAdmin(userTag: String): Boolean
}
