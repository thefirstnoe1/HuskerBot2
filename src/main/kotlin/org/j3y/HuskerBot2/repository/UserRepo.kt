package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepo : JpaRepository<User, String>
