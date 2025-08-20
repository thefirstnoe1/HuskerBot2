package org.j3y.HuskerBot2.model

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "t_image")
class ImageEntity(
    @Id
    var imageName: String = "",
    var imageUrl: String = "",
    var uploadingUser: String = ""
)
