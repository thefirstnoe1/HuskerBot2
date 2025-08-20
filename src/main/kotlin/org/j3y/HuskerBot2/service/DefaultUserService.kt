package org.j3y.HuskerBot2.service

import net.dv8tion.jda.api.JDA
import org.j3y.HuskerBot2.model.User
import org.j3y.HuskerBot2.repository.UserRepo
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service

@Service
class DefaultUserService(
    @Lazy private val api: JDA,
    private val userRepo: UserRepo
) : UserService {

    override fun setUserBlocked(userTag: String, isBlocked: Boolean): User {
        if (!isValidUserTag(userTag)) {
            throw IllegalArgumentException("Invalid user was provided.")
        }

        val user = userRepo.findById(userTag).orElse(User(userId = userTag, isAdmin = false))
        user.isBlocked = isBlocked
        userRepo.save(user)
        return user
    }

    override fun setUserAdmin(userTag: String, isAdmin: Boolean): User {
        if (!isValidUserTag(userTag)) {
            throw IllegalArgumentException("Invalid user was provided.")
        }

        val user = userRepo.findById(userTag).orElse(User(userId = userTag, isBlocked = false))
        user.isAdmin = isAdmin
        userRepo.save(user)
        return user
    }

    override fun isUserBlocked(userTag: String): Boolean {
        val user = userRepo.findById(userTag).orElse(null)
        return user?.isBlocked == true
    }

    override fun isUserAdmin(userTag: String): Boolean {
        val user = userRepo.findById(userTag).orElse(null)
        return user?.isAdmin == true
    }

    fun isValidUserTag(userTag: String): Boolean {
        return try {
            api.getUserByTag(userTag) != null
        } catch (iae: IllegalArgumentException) {
            false
        }
    }
}
