package com.zebra.musicbrainz.controllers;

import com.zebra.musicbrainz.dtos.AlbumInfo;
import com.zebra.musicbrainz.dtos.ReleaseDto;
import com.zebra.musicbrainz.services.ArtistService;
import com.zebra.musicbrainz.services.MusicBrainzClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class GetDataController {
    private final ArtistService artistService;
    private final MusicBrainzClient musicBrainzClient;

    public GetDataController(ArtistService artistService, MusicBrainzClient musicBrainzClient) {
        this.artistService = artistService;
        this.musicBrainzClient = musicBrainzClient;
    }

    @GetMapping(value = "/api/albums/artist/{artistId}")
    public CompletableFuture<List<ReleaseDto>> getAllAlbumsByArtist(@PathVariable int artistId) {
        return artistService.getAlbumsByArtistId(artistId);
    }

    @GetMapping(value = "/api/albums/artist/{artistName}")
    public CompletableFuture<Map<String, List<ReleaseDto>>> getAllAlbumsByArtist(@PathVariable String artistName) {
        return artistService.getAlbumsByArtistName(artistName);
    }


    @GetMapping(value = "/api/albums/artist/artistName={artistName}&albumName={albumName}")
    public CompletableFuture<List<AlbumInfo>> getAlbumInfo(@PathVariable String artistName, @PathVariable String albumName) {
        return CompletableFuture.supplyAsync(() -> MusicBrainzClient.getAlbum(artistName, albumName));
    }
}
