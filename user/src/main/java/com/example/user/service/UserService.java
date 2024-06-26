package com.example.user.service;

import org.springframework.beans.factory.annotation.Value;
import com.example.user.entity.User;
import com.example.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    private RestTemplate restTemplate;
    @Value("${song.service.url}")
    private String songServiceUrl;
    @Value("${playlist.service.url}")
    private String playlistServiceUrl;
    @Value("${artist.service.url}")
    private String artistServiceUrl;

    public ResponseEntity<String> getAllUsers() {
        List<User> users = userRepository.findAll();
        return new ResponseEntity<>(convertUsersToJson(users), HttpStatus.OK);
    }

    public ResponseEntity<String> getUserById(String userId) {
        Optional<User> userOptional = userRepository.findById(Long.parseLong(userId));
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return new ResponseEntity<>(convertUserToJson(user), HttpStatus.OK);
        } else {
            return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
        }
    }

    private String convertUserToJson(User user) {
        return "{ \"id\":" + user.getId() + ", \"username\":\"" + user.getUsername() + "\", \"email\":\"" + user.getEmail() + "\", \"role\":\"" + user.getRole() + "\" }";
    }

    private String convertUsersToJson(List<User> users) {
        StringBuilder sb = new StringBuilder("[");
        for (User user : users) {
            sb.append(convertUserToJson(user)).append(",");
        }
        sb.setLength(sb.length() - 1);
        sb.append("]");
        return sb.toString();
    }


    public ResponseEntity<String> createUser(@RequestBody String userDetails) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            User user = mapper.readValue(userDetails, User.class);
            User savedUser = userRepository.save(user);
            return new ResponseEntity<>(convertUserToJson(savedUser), HttpStatus.CREATED);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error parsing user data", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> updateUser(@RequestParam String userId, @RequestBody String updatedUserDetails) {
        try {
            Long longUserId = Long.parseLong(userId);
            Optional<User> userOptional = userRepository.findById(longUserId);

            if (userOptional.isPresent()) {
                ObjectMapper mapper = new ObjectMapper();
                User updatedUser = mapper.readValue(updatedUserDetails, User.class);

                userOptional.get().setUsername(updatedUser.getUsername());
                userOptional.get().setEmail(updatedUser.getEmail());

                User savedUser = userRepository.save(userOptional.get());
                return new ResponseEntity<>(convertUserToJson(savedUser), HttpStatus.OK);
            } else {
                return new ResponseEntity<>("User not found", HttpStatus.NOT_FOUND);
            }
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error parsing user data", HttpStatus.BAD_REQUEST);
        } catch (NumberFormatException e) {
            return new ResponseEntity<>("Invalid user ID format", HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<Void> deleteUser(@RequestParam String userId) {
        try {
            Long longUserId = Long.parseLong(userId);
            userRepository.deleteById(longUserId);
            return new ResponseEntity<>(HttpStatus.NO_CONTENT);
        } catch (NumberFormatException e) {
            return new ResponseEntity<>(HttpStatus.BAD_REQUEST);
        }
    }

    public ResponseEntity<String> getUserPlaylists(String userId) {
        String url = playlistServiceUrl + "/users/" + userId + "/playlists";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response;
            } else {
                return new ResponseEntity<>("Error retrieving user playlists: " + response.getStatusCodeValue(), response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Playlist Service", HttpStatus.INTERNAL_SERVER_ERROR);

        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error retrieving user playlists", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> addUserPlaylist(String userId, String playlistId) {
        String url = playlistServiceUrl + "/users/" + userId + "/playlists/" + playlistId;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url,null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return new ResponseEntity<>("Playlist added to user's playlists", HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("Error adding playlist: " + response.getStatusCodeValue(), response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Playlist Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error adding playlist to user's playlists", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void removeUserPlaylist(String userId, String playlistId) {
        restTemplate.delete(songServiceUrl + "/users/" + userId + "/playlists/" + playlistId);
    }

    public ResponseEntity<String> getUserSongs(String userId) {
        String url = songServiceUrl + "/users/" + userId + "/songs";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response;
            } else {
                return new ResponseEntity<>("Error retrieving user songs: " + response.getStatusCodeValue(), response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Song Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error retrieving user songs", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> addUserSong(String userId, String songId) {
        String url = songServiceUrl + "/users/" + userId + "/songs/" + songId;

        ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

        if (response.getStatusCode() == HttpStatus.CREATED) {
            return new ResponseEntity<>("Song added to user's playlist", HttpStatus.CREATED);
        } else {
            return new ResponseEntity<>("Error adding song: " + response.getStatusCodeValue(), response.getStatusCode());
        }
    }

    public void removeUserSong(String userId, String songId) {
        restTemplate.delete(songServiceUrl + "/users/" + userId + "/songs/" + songId);
    }

    public ResponseEntity<String> getUserArtists(String userId) {
        String url = artistServiceUrl + "/users/" + userId + "/artists";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response;
            } else {
                return new ResponseEntity<>("Error retrieving user artists: " + response.getStatusCodeValue(), response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Artist Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error retrieving user artists", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public ResponseEntity<String> addUserArtist(String userId, String artistId) {
        String url = artistServiceUrl + "/users/" + userId + "/artists/" + artistId;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return new ResponseEntity<>("Artist added to user's profile", HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("Error adding artist: " + response.getStatusCodeValue(), response.getStatusCode());
            }
        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Artist Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error adding artist to user's profile", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void removeUserArtist(String userId, String artistId) {
        String url = artistServiceUrl + "/users/" + userId + "/artists/" + artistId;

        try {
            restTemplate.delete(url);

        } catch (RestClientResponseException e) {
            throw new RuntimeException("Error communicating with Artist Service", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error removing artist from user", e);
        }
    }

    public ResponseEntity<String> addSongToPlaylist(@RequestParam String userId, @RequestParam String playlistId, @RequestParam String songId) {
        String url = playlistServiceUrl + "/users/" + userId + "/playlists/" + playlistId + "/songs/" + songId;

        try {

            ResponseEntity<String> response = restTemplate.postForEntity(url, null, String.class);

            if (response.getStatusCode() == HttpStatus.CREATED) {
                return new ResponseEntity<>("Song added to playlist", HttpStatus.CREATED);
            } else {
                return new ResponseEntity<>("Error adding song: " + response.getStatusCodeValue(), response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Playlist Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error adding song to playlist", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public void removeSongFromPlaylist(String userId, String playlistId, String songId) {
        String url = playlistServiceUrl + "/users/" + userId + "/playlists/" + playlistId + "/songs/" + songId;

        try {
            restTemplate.delete(url);
        } catch (RestClientResponseException e) {
            throw new RuntimeException("Error communicating with Playlist Service", e);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error removing song from playlist", e);
        }
    }

    public ResponseEntity<String> getPlaylistSongs(String userId, String playlistId) {
        String url = playlistServiceUrl + "/users/" + userId + "/playlists/" + playlistId + "/songs";

        try {
            ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                return response;
            } else {
                return new ResponseEntity<>("Error retrieving playlist songs: " + response.getStatusCodeValue(), response.getStatusCode());
            }

        } catch (RestClientResponseException e) {
            return new ResponseEntity<>("Error communicating with Playlist Service", HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error retrieving playlist songs", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}

