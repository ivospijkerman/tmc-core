package fi.helsinki.cs.tmc.core.communication;

import fi.helsinki.cs.tmc.core.configuration.TmcSettings;
import fi.helsinki.cs.tmc.core.domain.Course;
import fi.helsinki.cs.tmc.core.domain.Exercise;
import fi.helsinki.cs.tmc.core.domain.Review;
import fi.helsinki.cs.tmc.core.domain.submission.SubmissionResult;
import fi.helsinki.cs.tmc.core.exceptions.TmcCoreException;

import com.google.common.base.Optional;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A Utility class for handling JSONs downloaded from the TMC-server.
 */
// TODO: all 'keys', use objects Course, Exercise etc.
public class TmcApi {

    private UrlCommunicator urlCommunicator;
    private TmcSettings settings;
    private UrlHelper helper;

    public TmcApi(TmcSettings settings) {
        this.settings = settings;
        this.helper = new UrlHelper(settings);
        this.urlCommunicator = new UrlCommunicator(settings);
    }

    public TmcApi(UrlCommunicator urlCommunicator, TmcSettings settings) {
        this.urlCommunicator = urlCommunicator;
        this.settings = settings;
        this.helper = new UrlHelper(settings);
    }

    /**
     * Get list of all the courses on the server specified by ServerData.
     *
     * @param serverAddress address of the tmc server
     * @return List of Course-objects
     */
    // TODO: extract gson + courses array to list?
    public List<Course> getCourses(String serverAddress) throws IOException {
        String coursesAddress = helper.allCoursesAddress(serverAddress);
        JsonObject jsonObject = getJsonFrom(URI.create(coursesAddress));
        Gson mapper = new Gson();
        Course[] courses = mapper.fromJson(jsonObject.getAsJsonArray("courses"), Course[].class);
        return Arrays.asList(courses);
    }

    /**
     * Get list of all the courses on the server specified by ServerData.
     *
     * @return List of Course-objects
     */
    public List<Course> getCourses() throws IOException {
        return getCourses(settings.getServerAddress());
    }

    /**
     * Get JSON-data from url.
     *
     * @param url url from which the object data is fetched
     * @return JSON-object
     */
    // TODO: WAT - private
    public JsonObject getJsonFrom(URI url) throws IOException {
        HttpResult httpResult = urlCommunicator.makeGetRequestWithAuthentication(url);
        if (httpResult == null) {
            // TODO: log + exception?
            return null;
        }
        String data = httpResult.getData();
        return new JsonParser().parse(data).getAsJsonObject();
    }

    /**
     * Get String from url.
     *
     * @param url url from which the data is fetched
     * @return JSON-object
     */
    // TODO:  can't we use get Json? -- or fix that - private?
    public String getRawTextFrom(URI url) throws IOException {
        HttpResult httpResult = urlCommunicator.makeGetRequestWithAuthentication(url);
        if (httpResult == null) {
            return null;
        }
        return httpResult.getData();
    }

    /**
     * Maps the JSON as review-objects.
     *
     * @param reviewUrl which is found from course-object
     * @return List of reviews
     */
    // TODO: extract GSON + review[] -> List<>
    public List<Review> getReviews(URI reviewUrl) throws IOException, URISyntaxException {
        JsonObject jsonObject = getJsonFrom(helper.withParams(reviewUrl));
        Gson mapper = new Gson();
        Review[] reviews = mapper.fromJson(jsonObject.getAsJsonArray("reviews"), Review[].class);
        return Arrays.asList(reviews);
    }

    /**
     * Get all exercise names of a course specified by courseUrl.
     *
     * @param courseUrl url of the course we are interested in
     * @return String of all exercise names separated by newlines
     */
    // TODO: return List<> or rm
    public String getExerciseNames(URI courseUrl) throws IOException {
        List<Exercise> exercises = getExercises(courseUrl);
        StringBuilder asString = new StringBuilder();
        for (Exercise exercise : exercises) {
            asString.append(exercise.getName());
            asString.append("\n");
        }
        return asString.toString();
    }

    /**
     * Reads courses from string.
     */
    // TODO: deserialize json data to course objects + private + extract gson :)
    public List<Course> getCoursesFromString(String jsonString) {
        JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        Gson mapper = new Gson();
        Course[] courses = mapper.fromJson(jsonObject.getAsJsonArray("courses"), Course[].class);
        return Arrays.asList(courses);
    }

    /**
     * Reads one course from string.
     */
    // TODO: deserialize json data to course objects + private + extract gson :)
    public Course getCourseFromString(String jsonString) {
        JsonObject jsonObject = new JsonParser().parse(jsonString).getAsJsonObject();
        Gson mapper = new Gson();
        return mapper.fromJson(jsonObject.get("course"), Course.class);
    }

    /**
     * Get information about course specified by the course ID.
     *
     * @return an course Object (parsed from JSON)
     */
    // TODO: make non optional? move out anyways -- courseDB
    public Optional<Course> getCourse(int courseId) throws IOException, URISyntaxException {
        List<Course> allCourses = getCourses();
        for (Course course : allCourses) {
            if (course.getId() == courseId) {
                return getCourse(helper.getCourseUrl(course));
            }
        }
        return Optional.absent();
    }

    /**
     * Get information about course specified by the URL path to course JSON.
     *
     * @param courseUrl URL path to course JSON
     * @return an Course object (parsed from JSON)
     */
    // TODO: make non optional - extract GSON - preconditions -
    public Optional<Course> getCourse(URI courseUrl) throws IOException {
        JsonObject courseJson = getJsonFrom(courseUrl);
        if (courseJson == null) {
            return Optional.absent();
        }
        Gson mapper = new Gson();
        Course course = mapper.fromJson(courseJson.getAsJsonObject("course"), Course.class);

        if (course == null) {
            // TODO: pois/precond?
            return Optional.fromNullable(course);
        }

        for (Exercise e : course.getExercises()) {
            e.setCourseName(course.getName());
        }

        return Optional.of(course);
    }

    // TODO: rm - unused
    private boolean courseExists(int courseId) throws IOException, TmcCoreException {
        List<Course> allCourses = getCourses();
        for (Course course : allCourses) {
            if (course.getId() == courseId) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all exercises of a course specified by Course.
     *
     * @param course Course that we are interested in
     * @return List of all exercises as Exercise-objects
     */
    // TODO: WAT - rm
    public List<Exercise> getExercisesFromServer(Course course)
            throws IOException, URISyntaxException {
        return getExercises(course.getId());
    }

    /**
     * Get all exercises of a course specified by Course id.
     *
     * @param id id of the course we are interested in
     * @return List of a all exercises as Exercise-objects
     */
    // TODO: rename ID
    public List<Exercise> getExercises(int id) throws IOException, URISyntaxException {
        Optional<Course> courseOptional = getCourse(id);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            for (Exercise exercise : course.getExercises()) {
                exercise.setCourseName(course.getName());
            }
            return course.getExercises();
        }
        return new ArrayList<>();
    }

    /**
     * Get all exercises of a course specified by courseUrl.
     *
     * @param courseUrl url of the course we are interested in
     * @return List of all exercises as Exercise-objects. If no course is found, empty list will be
     *     returned.
     */
    // TODO: same as above. refactor.
    public List<Exercise> getExercises(URI courseUrl) throws IOException {
        Optional<Course> courseOptional = getCourse(courseUrl);
        if (courseOptional.isPresent()) {
            Course course = courseOptional.get();
            for (Exercise exercise : course.getExercises()) {
                exercise.setCourseName(course.getName());
            }
            return course.getExercises();
        }
        return new ArrayList<>();
    }

    /**
     * Parses JSON in url to create a SubmissionResult object.
     *
     * @param url to make request to
     * @return A SubmissionResult object which contains data of submission.
     */
    // TODO: gson. ya know
    public SubmissionResult getSubmissionResult(URI url) throws IOException {
        JsonObject submission = getJsonFrom(url);
        Gson mapper = new Gson();
        return mapper.fromJson(submission, SubmissionResult.class);
    }

    /**
     * Parses the submission result URL from a HttpResult with JSON.
     *
     * @param result HTTPResult containing JSON with submission url.
     * @return url where submission results are located.
     */
    // TODO:  parse result, private?
    public URI getSubmissionUrl(HttpResult result) {
        return getPropertyFromResult(result, "submission_url");
    }

    /**
     * Parses the submission result paste URL from a HttpResult with JSON.
     *
     * @param result HTTPResult containing JSON with paste url.
     * @return url where paste is located.
     */
    // TODO:  parse result, private?
    public URI getPasteUrl(HttpResult result) {
        return getPropertyFromResult(result, "paste_url");
    }

    // TODO:  extract json parser?
    private URI getPropertyFromResult(HttpResult result, String property) {
        JsonElement jelement = new JsonParser().parse(result.getData());
        JsonObject jobject = jelement.getAsJsonObject();
        return URI.create(jobject.get(property).getAsString());
    }
}
