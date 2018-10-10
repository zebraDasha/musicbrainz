package com.zebra.musicbrainz.services;

import com.zebra.musicbrainz.dtos.ReleaseDto;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface ArtistService {
    public CompletableFuture<List<ReleaseDto>> getAlbumsByArtistId(int id);

    CompletableFuture<Map<String, List<ReleaseDto>>> getAlbumsByArtistName(String name);
}
