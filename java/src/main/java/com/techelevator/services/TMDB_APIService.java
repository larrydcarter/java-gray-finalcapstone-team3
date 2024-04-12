package com.techelevator.services;

import com.techelevator.dao.MovieDao;
import com.techelevator.model.*;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Component
public class TMDB_APIService {
    //properties
    private final String API_BASE_URL = "https://api.themoviedb.org/3";
    private final String SEARCH = "/search/movie?query=";
    private final String DISCOVER = "/discover/movie?include_adult=false&include_video=false&language=en-US&page=";
    private RestTemplate restTemplate = new RestTemplate();
    private final String BEARER_TOKEN = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJmZGU3MzhlZTFmNjUzNGQ3MDFlYjBlZDcwYjBhMDdmNCIsInN1YiI6IjY1ZGJlMjMxZWQyYWMyMDE4NzQwZGQyNyIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.SrPF0blfo7MklOWQqkeSN8WnfNLQgyUS8r0TtSOdAC4";
    MovieDao movieDao;

    //constructors

    //methods
    public MovieApiResponse getMoviesByTitle(String searchTerm, int page) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<MovieApiResponse> entity = new HttpEntity<>(headers);
        MovieApiResponse movieApiResponse;
        String formattedSearchTerm = searchTerm.replace(" ", "%20");

        try {

            ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + SEARCH + formattedSearchTerm + "&include_adult=false&page=" + page, HttpMethod.GET, entity, MovieApiResponse.class);
            movieApiResponse = response.getBody();

        } catch (RestClientException e) {

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

        } if (movieApiResponse == null) {

            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No movies found for the given search term.");

        } return movieApiResponse;

    }

    public MovieApiResponse queryForRecommended4u(MovieApiResponse recommended,List<Integer> genres, double vote_average, double vote_count, int neededLayers, int currentLayer) {

        int page =1;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<MovieApiResponse> entity = new HttpEntity<>(headers);
        String genresAsApiInput = "";
        genresAsApiInput = createGenresAsApiInput(genresAsApiInput, genres);
        MovieApiResponse layeredResults = new MovieApiResponse();
        MovieApiResponse apiQueryResults = new MovieApiResponse();
        double voteAverageAdjustmentRatePerLayer = 0.95;
        double voteCountAdjustmentRatePerLayer = 0.9;
        Integer savedGenre;

        if (currentLayer < neededLayers) {

            currentLayer++;
            vote_average = vote_average * voteAverageAdjustmentRatePerLayer;
            vote_count = vote_count * voteCountAdjustmentRatePerLayer;

            for (int i = 0; i < genres.size(); i++) {

                savedGenre = genres.get(i);
                genres.remove(i);
                genresAsApiInput = "";
                genresAsApiInput = createGenresAsApiInput(genresAsApiInput, genres);

                if (currentLayer == neededLayers) {

                    try {

                        while (page == 1 || page <= apiQueryResults.getTotal_pages()) {

                            ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count + "&with_genres=" + genresAsApiInput, HttpMethod.GET, entity, MovieApiResponse.class);
                            apiQueryResults = response.getBody();
                            //apiQueryResults = movieDao.throwOutBadMovies((apiQueryResults));
                            page++;

                            for (Movie movie : apiQueryResults.getResults()) {

                                layeredResults.getResults().add(movie);

                                if (layeredResults.getResults().size() >= 50) {

                                    break;

                                }

                            }
                            if (layeredResults.getResults().size() >= 50) {

                                break;

                            }

                        }

                        for (Movie movie : layeredResults.getResults()) {

                            if (!recommended.getResults().contains(movie)) {

                                recommended.getResults().add(movie);

                                if (recommended.getResults().size() >= 50) {

                                    break;

                                }

                            }

                        }

                    } catch (RestClientException e) {

                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

                    }

                } else {

                    recommended = queryForRecommended4u(recommended, genres, vote_average, vote_count, neededLayers, currentLayer);

                } genres.add(i, savedGenre);

            }

        } else {

            try {

                ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count + "&with_genres=" + genresAsApiInput, HttpMethod.GET, entity, MovieApiResponse.class);
                recommended = response.getBody();

            } catch (RestClientException e) {

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

            } while (recommended.getResults().size() < 50) {

                neededLayers++;
                recommended = queryForRecommended4u(recommended, genres, vote_average, vote_count, neededLayers, currentLayer);

            }

        } return recommended;

    }

    public Movie getMovieById(int id) {
        for (MovieApiResponse movieApiResponse) {
            if (id == movies.getId()) {

                return movie;
            }
        }
        return null;
    }

    private String createGenresAsApiInput(String genresAsApiInput, List<Integer> genres) {

        if (genres.size() > 0) {

            for (Integer genre : genres) {

                if (genresAsApiInput == "") {

                    genresAsApiInput += genre;

                } else {

                    genresAsApiInput += "%2C" + genre;

                }

            }

        } return genresAsApiInput;

    }

    public MovieApiResponse queryForGenreRecommendations(MovieApiResponse recommended, Integer genre, double vote_average, double vote_count, int neededLayers, int currentLayer) {

        int page = 1;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<MovieApiResponse> entity = new HttpEntity<>(headers);
        MovieApiResponse layeredResults = new MovieApiResponse();
        MovieApiResponse apiQueryResults = new MovieApiResponse();
        double voteAverageAdjustmentRatePerLayer = 0.95;
        double voteCountAdjustmentRatePerLayer = 0.9;

        if (currentLayer < neededLayers) {

            currentLayer++;

            vote_average = vote_average * voteAverageAdjustmentRatePerLayer;
            vote_count = vote_count * voteCountAdjustmentRatePerLayer;

            if (currentLayer == neededLayers) {

                try {

                    while (page == 1 || page <= apiQueryResults.getTotal_pages()) {

                        ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count + "&with_genres=" + genre, HttpMethod.GET, entity, MovieApiResponse.class);
                        apiQueryResults = response.getBody();
                        //apiQueryResults = movieDao.throwOutBadMovies(apiQueryResults);
                        page++;

                        for (Movie movie : apiQueryResults.getResults()) {

                            layeredResults.getResults().add(movie);

                            if (layeredResults.getResults().size() >= 50) {

                                break;

                            }

                        } if (layeredResults.getResults().size() >= 50) {

                            break;

                        }

                    }

                    for (Movie movie : layeredResults.getResults()) {

                        if (!recommended.getResults().contains(movie)) {

                            recommended.getResults().add(movie);

                            if (recommended.getResults().size() >= 50) {

                                break;

                            }

                        }

                    }

                } catch (RestClientException e) {

                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

                }

            } else {

                recommended = queryForGenreRecommendations(recommended, genre, vote_average, vote_count, neededLayers, currentLayer);

            }

        } else {

            try {

                ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count + "&with_genres=" + genre, HttpMethod.GET, entity, MovieApiResponse.class);
                recommended = response.getBody();

            } catch (RestClientException e) {

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

            } while (recommended.getResults().size() < 20) {

                neededLayers++;
                recommended = queryForGenreRecommendations(recommended, genre, vote_average, vote_count, neededLayers, currentLayer);

            }

        } return recommended;

    }

    public MovieApiResponse queryForPopular() {

        int page = 1;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<MovieApiResponse> entity = new HttpEntity<>(headers);
        MovieApiResponse movieApiResponse;


        try {

            ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc", HttpMethod.GET, entity, MovieApiResponse.class);
            movieApiResponse = response.getBody();

        } catch (RestClientException e) {

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

        } return movieApiResponse;

    }

    public MovieApiResponse queryForAllTimeGreats(MovieApiResponse recommended ,double vote_average, double vote_count, int neededLayers, int currentLayer) {

        int page = 1;
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<MovieApiResponse> entity = new HttpEntity<>(headers);
        MovieApiResponse layeredResults;
        double voteAverageAdjustmentRatePerLayer = 0.95;
        double voteCountAdjustmentRatePerLayer = 0.9;

        if (currentLayer < neededLayers) {

            currentLayer++;

            vote_average = vote_average * voteAverageAdjustmentRatePerLayer;
            vote_count = vote_count * voteCountAdjustmentRatePerLayer;

            if (currentLayer == neededLayers) {

                try {

                    ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count, HttpMethod.GET, entity, MovieApiResponse.class);
                    layeredResults = response.getBody();

                    for (Movie movie : layeredResults.getResults()) {

                        if (!recommended.getResults().contains(movie)) {

                            recommended.getResults().add(movie);

                            if (recommended.getResults().size() >= 20) {

                                break;

                            }

                        }

                    }

                } catch (RestClientException e) {

                    throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

                }

            } else {

                recommended = queryForAllTimeGreats(recommended, vote_average, vote_count, neededLayers, currentLayer);

            }

        } else {

            try {

                ResponseEntity<MovieApiResponse> response = restTemplate.exchange(API_BASE_URL + DISCOVER + page + "&sort_by=popularity.desc&vote_average.gte=" + vote_average + "&vote_count.gte=" + vote_count, HttpMethod.GET, entity, MovieApiResponse.class);
                recommended = response.getBody();

            } catch (RestClientException e) {

                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

            } while (recommended.getResults().size() < 20) {

                neededLayers++;
                recommended = queryForAllTimeGreats(recommended, vote_average, vote_count, neededLayers, currentLayer);

            }

        } return recommended;
<<<<<<< HEAD:java/src/main/java/com/techelevator/services/TMDB_APIService.java

    }

    public int queryForCertificationById(int movie_id) {

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + BEARER_TOKEN);
        HttpEntity<ReleaseDatesApiResponse> entity = new HttpEntity<>(headers);
        ReleaseDatesApiResponse releaseDatesApiResponse;

        try {

            ResponseEntity<ReleaseDatesApiResponse> response = restTemplate.exchange(API_BASE_URL + "/movie/" + movie_id + "/release_dates", HttpMethod.GET, entity, ReleaseDatesApiResponse.class);
            releaseDatesApiResponse = response.getBody();

        }  catch (RestClientException e) {

            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error retrieving movie from API using search term.", e);

        }
        for (CountryReleaseDates country : releaseDatesApiResponse.getResults()) {

            if(country.getIso_3166_1().equalsIgnoreCase("US")) {

                for (ReleaseDates releaseDate : country.getRelease_dates()) {

                    if (releaseDate.getType() != null && (releaseDate.getType() <= 5)) {

                        return releaseDate.getType();

                    }

                }

            }

        } return 0;
=======
>>>>>>> 6e6c8149c836380e2e2e6ba1e837dba6117da87a:java/src/main/java/com/techelevator/services/TMDBService.java

    }

}
