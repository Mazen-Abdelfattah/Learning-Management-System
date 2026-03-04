package org.software.lms.controller;

import jakarta.validation.Valid;
import org.software.lms.dto.LessonDTO;
import org.software.lms.dto.LessonResponseDto;
import org.software.lms.dto.UserDto;
import org.software.lms.exception.ResourceNotFoundException;
import org.software.lms.model.Lesson;
import org.software.lms.model.ResourceType;
import org.software.lms.model.User;
import org.software.lms.repository.CourseRepository;
import org.software.lms.repository.LessonResourceRepository;
import org.software.lms.service.CourseService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.software.lms.model.LessonResource;
import org.software.lms.service.LessonService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/courses/{courseId}/lessons")
public class LessonController {
    @Autowired
    private LessonService lessonService;

    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Autowired
    private NotificationController notificationController;

    @Autowired
    private CourseService courseService;


    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Lesson> createLesson(
            @PathVariable Long courseId,
            @Valid @RequestBody LessonDTO lessonDTO) {
        Lesson createdLesson = lessonService.createLesson(courseId, lessonDTO);
        List<UserDto> enrolledStudents = courseService.findStudentEnrolledInCourse(courseId);
        for (UserDto stud : enrolledStudents) {
            Long StudId = stud.getId();
            String title = "New Lesson";
            String message = "new Lesson has been Added to the course.";
            notificationController.createNotification(StudId, courseId, title, message);
        }
        return ResponseEntity.ok(createdLesson);

    }
    @GetMapping
    public ResponseEntity<List<LessonResponseDto>> getLessonsByCourse(@PathVariable Long courseId) {
        List<LessonResponseDto> lessons = lessonService.getLessonsByCourse(courseId);
        return ResponseEntity.ok(lessons);
    }

    @GetMapping("/{lessonId}")
    public ResponseEntity<Lesson> getLessonById(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {
        Lesson lesson = lessonService.getLessonById(courseId, lessonId);
        return ResponseEntity.ok(lesson);
    }

    @PutMapping("/{lessonId}")
    public ResponseEntity<Lesson> updateLesson(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @Valid @RequestBody LessonDTO lessonDTO) {
        Lesson updatedLesson = lessonService.updateLesson(courseId, lessonId, lessonDTO);
        return ResponseEntity.ok(updatedLesson);
    }

    @DeleteMapping("/{lessonId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteLesson(
            @PathVariable("courseId") Long courseId,
            @PathVariable("lessonId") Long lessonId) {
        lessonService.deleteLesson(courseId, lessonId);
        return ResponseEntity.noContent().build();
    }

    // Resource management endpoints
    @PostMapping("/{lessonId}/resources")
    public ResponseEntity<LessonResource> uploadResource(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @RequestParam("file") MultipartFile file) throws IOException {
        LessonResource resource = lessonService.uploadResource(courseId, lessonId, file);
        return ResponseEntity.ok(resource);
    }

    @GetMapping("/{lessonId}/resources")
    public ResponseEntity<List<LessonResource>> getLessonResources(
            @PathVariable Long courseId,
            @PathVariable Long lessonId) {
        List<LessonResource> resources = lessonService.getLessonResources(courseId, lessonId);
        return ResponseEntity.ok(resources);
    }


    @GetMapping("/{lessonId}/resources/{resourceId}/view")
    public ResponseEntity<Resource> viewResource(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable Long resourceId) throws IOException {

        LessonResource lessonResource = lessonResourceRepository.findByIdAndLesson_IdAndLesson_Course_Id(
                        resourceId, lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Resource resource = lessonService.viewResource(courseId, lessonId, resourceId);

        // Determine media type based on resource type
        MediaType mediaType = getMediaTypeForResource(lessonResource.getType());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + lessonResource.getFileName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }

    private MediaType getMediaTypeForResource(ResourceType type) {
        return switch (type) {
            case PDF -> MediaType.APPLICATION_PDF;
            case IMAGE -> MediaType.IMAGE_JPEG; // You might want to be more specific based on the actual image type
            case VIDEO -> MediaType.parseMediaType("video/mp4"); // Adjust based on video type
            case AUDIO -> MediaType.parseMediaType("audio/mpeg"); // Adjust based on audio type
            case DOCUMENT -> MediaType.parseMediaType("application/msword"); // Or application/vnd.openxmlformats-officedocument.wordprocessingml.document for .docx
            default -> MediaType.APPLICATION_OCTET_STREAM;
        };
    }
    @DeleteMapping("/{lessonId}/resources/{resourceId}")
    public ResponseEntity<Void> deleteResource(
            @PathVariable Long courseId,
            @PathVariable Long lessonId,
            @PathVariable Long resourceId) {
        lessonService.deleteResource(courseId, lessonId, resourceId);
        return ResponseEntity.noContent().build();
    }
}
