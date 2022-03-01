package com.insidious.plugin.extension.model.command;

public class GetAllBookmarksResponse
        implements CommandResponse {
    Bookmark[] bookmarks;

    public Bookmark[] getBookmarks() {
        return this.bookmarks;
    }

    public void setBookmarks(Bookmark[] bookmarks) {
        this.bookmarks = bookmarks;
    }

    public static class Bookmark {
        private long bbcount;
        private String name;

        public long getBbcount() {
            return this.bbcount;
        }

        public void setBbcount(long bbcount) {
            this.bbcount = bbcount;
        }

        public String getName() {
            return this.name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }
}


