package org.j3y.HuskerBot2.repository

import org.j3y.HuskerBot2.model.ImageEntity
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface ImageRepo : JpaRepository<ImageEntity, String>
