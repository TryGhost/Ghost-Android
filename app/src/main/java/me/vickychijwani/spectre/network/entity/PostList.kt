package me.vickychijwani.spectre.network.entity

import me.vickychijwani.spectre.model.entity.Post

// dummy wrapper class needed for Retrofit
class PostList(var posts: MutableList<Post>) {

    operator fun contains(id: String): Boolean {
        for (post in posts) {
            if (id == post.id) {
                return true
            }
        }
        return false
    }

    fun remove(idx: Int): Post {
        return posts.removeAt(idx)
    }

}
