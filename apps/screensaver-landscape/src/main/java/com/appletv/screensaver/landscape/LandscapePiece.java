package com.appletv.screensaver.landscape;

public final class LandscapePiece {
    public final String id;
    public final String filename;
    public final String titleVi;
    public final String titleEn;
    public final String artist;
    public final String year;
    public final String museum;
    public final float aspectRatio;

    public LandscapePiece(String id, String filename, String titleVi, String titleEn,
                          String artist, String year, String museum, float aspectRatio) {
        this.id = id;
        this.filename = filename;
        this.titleVi = titleVi;
        this.titleEn = titleEn;
        this.artist = artist;
        this.year = year;
        this.museum = museum;
        this.aspectRatio = aspectRatio;
    }
}
