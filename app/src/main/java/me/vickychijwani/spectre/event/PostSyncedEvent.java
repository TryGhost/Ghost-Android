package me.vickychijwani.spectre.event;

import me.vickychijwani.spectre.model.entity.Post;

public class PostSyncedEvent {

    public final Post post;

    public PostSyncedEvent(Post post) {
        this.post = post;
    }

}
