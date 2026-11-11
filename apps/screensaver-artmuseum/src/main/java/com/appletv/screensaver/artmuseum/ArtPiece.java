package com.appletv.screensaver.artmuseum;

public final class ArtPiece {
    public final String id;
    public final String filename;
    public final String title;
    public final String artist;
    public final String year;
    public final String museum;

    public ArtPiece(String id, String filename, String title, String artist, String year, String museum) {
        this.id = id;
        this.filename = filename;
        this.title = title;
        this.artist = artist;
        this.year = year;
        this.museum = museum;
    }
}
