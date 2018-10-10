package com.zebra.musicbrainz.services;

import com.zebra.musicbrainz.dtos.ReleaseDto;
import fm.last.musicbrainz.data.dao.ArtistDao;
import fm.last.musicbrainz.data.dao.ReleaseDao;
import fm.last.musicbrainz.data.model.Artist;
import fm.last.musicbrainz.data.model.Release;
import fm.last.musicbrainz.data.model.ReleaseGroupPrimaryType;
import fm.last.musicbrainz.data.model.ReleaseStatus;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ArtistServiceImpl implements ArtistService {

    private final ArtistDao artistDao;
    private final ReleaseDao releaseDao;

    @Autowired
    public ArtistServiceImpl(ArtistDao artistDao, ReleaseDao releaseDao) {
        this.artistDao = artistDao;
        this.releaseDao = releaseDao;
    }

    @Override
    public CompletableFuture<List<ReleaseDto>> getAlbumsByArtistId(int id) {
        return CompletableFuture.supplyAsync(() -> {
                    Artist artist = artistDao.getById(id);
                    List<Release> releases = releaseDao.getByArtist(artist);
                    List<Release> albums = releases.stream()
                            .filter(x -> x.getReleaseGroup().getType().equals(ReleaseGroupPrimaryType.ALBUM)
                                    && (x.getStatus().equals(ReleaseStatus.OFFICIAL)))
                            .collect(Collectors.toList());
                    ModelMapper modelMapper = new ModelMapper();
                    return albums.stream().map(album -> modelMapper.map(album, ReleaseDto.class)).collect(Collectors.toList());
                }
        );
    }

    @Override
    public CompletableFuture<Map<String, List<ReleaseDto>>> getAlbumsByArtistName(String name) {
        Map<String, List<ReleaseDto>> result = new HashMap<>();
        return CompletableFuture.supplyAsync(() -> {
            List<Artist> artists = artistDao.getByName(name);
            ModelMapper modelMapper = new ModelMapper();
            artists.forEach(artist -> {
                        List<Release> releases = releaseDao.getByArtist(artist);
                        List<Release> albums = releases.stream()
                                .filter(x -> x.getReleaseGroup().getType().equals(ReleaseGroupPrimaryType.ALBUM)
                                        && (x.getStatus().equals(ReleaseStatus.OFFICIAL)))
                                .collect(Collectors.toList());
                        List<ReleaseDto> releaseDtos = albums.stream().map(album ->
                                modelMapper.map(album, ReleaseDto.class)
                        ).collect(Collectors.toList());
                        result.put(String.valueOf(artist.getId()), releaseDtos);
                    }
            );
            return result;
        });
    }

}
