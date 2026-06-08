package com.final_year.v2.controller;

import com.final_year.v2.dto.YouTubeVideoDTO;
import com.final_year.v2.service.YouTubeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/youtube")
public class YouTubeController {

    private final YouTubeService youTubeService;

    public YouTubeController(YouTubeService youTubeService) {
        this.youTubeService = youTubeService;
    }

    @GetMapping("/search")
    public List<YouTubeVideoDTO> search(@RequestParam String q, @RequestParam(defaultValue = "20") int max) throws Exception {
        return youTubeService.searchVideos(q, max);
    }

    @GetMapping("/trending")
    public List<YouTubeVideoDTO> trending(@RequestParam(defaultValue = "24") int max) throws Exception {
        return youTubeService.getTrendingVideos(max);
    }

    @GetMapping("/video/{videoId}")
    public YouTubeVideoDTO getVideo(@PathVariable String videoId) throws Exception {
        return youTubeService.getVideoDetails(videoId);
    }

    @GetMapping("/suggestions")
    public List<YouTubeVideoDTO> getSuggestions(@RequestParam String videoId, @RequestParam(defaultValue = "10") int max) throws Exception {
        return youTubeService.getRelatedVideos(videoId, max);
    }
}