package org.j3y.HuskerBot2.commands.images

import org.j3y.HuskerBot2.commands.SlashCommand
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component

@Component
class Image : SlashCommand() {
    @Autowired lateinit var addImage: AddImage
    @Autowired lateinit var deleteImage: DeleteImage
    @Autowired lateinit var showImage: ShowImage
    @Autowired lateinit var listImages: ListImages

    override fun getCommandKey(): String = "image"
    override fun getDescription(): String = "Image commands"
    override fun getSubcommands(): List<SlashCommand> = listOf(
        addImage, deleteImage, showImage, listImages
    )
}
