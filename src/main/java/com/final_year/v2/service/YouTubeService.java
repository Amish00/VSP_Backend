package com.final_year.v2.service;

import com.final_year.v2.dto.YouTubeVideoDTO;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class YouTubeService {

    @Value("${youtube.api.key}")
    private String apiKey;

    private YouTube getYouTubeService() throws Exception {
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        JacksonFactory jsonFactory = JacksonFactory.getDefaultInstance();
        return new YouTube.Builder(httpTransport, jsonFactory, request -> {})
                .setApplicationName("your-app-name")
                .build();
    }

    public List<YouTubeVideoDTO> searchVideos(String query, int maxResults) throws Exception {
        YouTube youtube = getYouTubeService();
        YouTube.Search.List search = youtube.search()
                .list(Collections.singletonList("id,snippet"));
        search.setKey(apiKey);
        search.setQ(query);
        search.setType(Collections.singletonList("video"));
        search.setMaxResults(Long.valueOf(maxResults));  // ✅ Long, not long
        search.setFields("items(id/videoId,snippet(channelId,channelTitle,title,thumbnails/default/url,publishedAt))");

        SearchListResponse response = search.execute();
        List<SearchResult> items = response.getItems();
        if (items == null) return Collections.emptyList();

        return items.stream().map(this::mapSearchResultToDTO).collect(Collectors.toList());
    }

    public List<YouTubeVideoDTO> getTrendingVideos(int maxResults) throws Exception {
        YouTube youtube = getYouTubeService();

        YouTube.Videos.List list = youtube.videos()
                .list(Collections.singletonList("snippet,statistics"));
        list.setKey(apiKey);
        list.setChart("mostPopular");
        list.setMaxResults(Long.valueOf(maxResults));  // ✅ Long
        list.setFields("items(id,snippet(channelId,channelTitle,title,thumbnails/default/url,publishedAt),statistics(viewCount))");

        VideoListResponse response = list.execute();
        if (response.getItems() == null) return Collections.emptyList();

        Set<String> channelIds = response.getItems().stream()
                .map(video -> video.getSnippet().getChannelId())
                .collect(Collectors.toSet());

        Map<String, String> channelThumbnails = fetchChannelThumbnails(channelIds);

        return response.getItems().stream()
                .map(video -> mapVideoToDTO(video, channelThumbnails))
                .collect(Collectors.toList());
    }

    private Map<String, String> fetchChannelThumbnails(Set<String> channelIds) throws Exception {
        if (channelIds.isEmpty()) return Collections.emptyMap();
        YouTube youtube = getYouTubeService();
        YouTube.Channels.List request = youtube.channels()
                .list(Collections.singletonList("snippet"));
        request.setKey(apiKey);
        request.setId(new ArrayList<>(channelIds));
        request.setFields("items(id,snippet(thumbnails/default/url))");

        ChannelListResponse response = request.execute();
        Map<String, String> map = new HashMap<>();
        if (response.getItems() != null) {
            for (Channel channel : response.getItems()) {
                String url = channel.getSnippet().getThumbnails().getDefault().getUrl();
                map.put(channel.getId(), url);
            }
        }
        return map;
    }

    private YouTubeVideoDTO mapSearchResultToDTO(SearchResult item) {
        YouTubeVideoDTO dto = new YouTubeVideoDTO();
        dto.setId(item.getId().getVideoId());
        dto.setTitle(item.getSnippet().getTitle());
        dto.setThumbnailUrl(item.getSnippet().getThumbnails().getDefault().getUrl());
        dto.setChannelTitle(item.getSnippet().getChannelTitle());
        dto.setPublishedAt(item.getSnippet().getPublishedAt().toString());
        dto.setChannelId(item.getSnippet().getChannelId());
        dto.setProfilePictureUrl(null);
        dto.setViewCount(null);
        return dto;
    }

    private YouTubeVideoDTO mapVideoToDTO(Video video, Map<String, String> channelThumbnails) {
        YouTubeVideoDTO dto = new YouTubeVideoDTO();
        dto.setId(video.getId());
        dto.setTitle(video.getSnippet().getTitle());
        dto.setThumbnailUrl(video.getSnippet().getThumbnails().getDefault().getUrl());
        dto.setChannelTitle(video.getSnippet().getChannelTitle());
        dto.setPublishedAt(video.getSnippet().getPublishedAt().toString());
        dto.setChannelId(video.getSnippet().getChannelId());
        dto.setProfilePictureUrl(channelThumbnails.get(video.getSnippet().getChannelId()));

        if (video.getStatistics() != null && video.getStatistics().getViewCount() != null) {
            dto.setViewCount(video.getStatistics().getViewCount().longValue());
        } else {
            dto.setViewCount(null);
        }

        return dto;
    }

    public YouTubeVideoDTO getVideoDetails(String videoId) throws Exception {
        YouTube youtube = getYouTubeService();
        YouTube.Videos.List list = youtube.videos()
                .list(Collections.singletonList("snippet,statistics"));
        list.setKey(apiKey);
        list.setId(Collections.singletonList(videoId));
        list.setFields("items(id,snippet(channelId,channelTitle,title,thumbnails/default/url,publishedAt),statistics(viewCount))");

        VideoListResponse response = list.execute();
        if (response.getItems() == null || response.getItems().isEmpty()) {
            throw new RuntimeException("Video not found");
        }
        Video video = response.getItems().get(0);
        // fetch channel thumbnail
        Set<String> channelIds = Set.of(video.getSnippet().getChannelId());
        Map<String, String> thumbnails = fetchChannelThumbnails(channelIds);
        return mapVideoToDTO(video, thumbnails);
    }

    public List<YouTubeVideoDTO> getRelatedVideos(String videoId, int maxResults) throws Exception {
        YouTubeVideoDTO current = getVideoDetails(videoId);
        String query = current.getTitle();
        return searchVideos(query, maxResults + 1).stream()
                .filter(v -> !v.getId().equals(videoId))
                .limit(maxResults)
                .collect(Collectors.toList());
    }
}