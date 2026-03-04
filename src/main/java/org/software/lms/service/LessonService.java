package org.software.lms.service;

import jakarta.transaction.Transactional;
import org.software.lms.dto.LessonDTO;
import org.software.lms.dto.LessonResponseDto;
import org.software.lms.exception.ResourceNotFoundException;
import org.software.lms.model.Course;
import org.software.lms.model.Lesson;
import org.software.lms.model.LessonResource;
import org.software.lms.model.ResourceType;
import org.software.lms.repository.CourseRepository;
import org.software.lms.repository.LessonRepository;
import org.software.lms.repository.LessonResourceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class LessonService {
    @Autowired
    private LessonRepository lessonRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private LessonResourceRepository lessonResourceRepository;

    @Value("${file.upload.directory}")
    private String uploadDirectory;

    public Lesson createLesson(Long courseId, LessonDTO lessonDTO) {
        Course course = courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course not found with id: " + courseId));

        Lesson lesson = new Lesson();
        lesson.setTitle(lessonDTO.getTitle());
        lesson.setDescription(lessonDTO.getDescription());
        lesson.setDuration(lessonDTO.getDuration());
        lesson.setOrderIndex(lessonDTO.getOrderIndex());
        lesson.setCourse(course);
        lesson.setCreatedAt(new Date());
        lesson.setUpdatedAt(new Date());

        return lessonRepository.save(lesson);


    }

    @Cacheable(value = "lessons", key = "#courseId")
    public List<LessonResponseDto> getLessonsByCourse(Long courseId) {
        if (!courseRepository.existsById(courseId)) {
            throw new ResourceNotFoundException("Course not found with id: " + courseId);
        }

        return lessonRepository.findByCourse_IdOrderByOrderIndexAsc(courseId)
                .stream()
                .map(lesson -> new LessonResponseDto(
                        lesson.getId(),
                        lesson.getTitle(),
                        lesson.getDescription(),
                        lesson.getDuration(),
                        lesson.getOrderIndex(),
                        courseId,
                        lesson.getCreatedAt(),
                        lesson.getUpdatedAt()
                ))
                .collect(Collectors.toList());

//        return lessonRepository.findByCourse_IdOrderByOrderIndexAsc(courseId);
    }

    public Lesson getLessonById(Long courseId, Long lessonId) {
        return lessonRepository.findByIdAndCourse_Id(lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Lesson not found with id: " + lessonId + " in course: " + courseId));
    }

    @CacheEvict(value = "lessons", key = "#courseId")
    public Lesson updateLesson(Long courseId, Long lessonId, LessonDTO lessonDTO) {
        Lesson lesson = getLessonById(courseId, lessonId);

        lesson.setTitle(lessonDTO.getTitle());
        lesson.setDescription(lessonDTO.getDescription());
        lesson.setDuration(lessonDTO.getDuration());
        lesson.setOrderIndex(lessonDTO.getOrderIndex());
        lesson.setUpdatedAt(new Date());

        return lessonRepository.save(lesson);
    }

    @CacheEvict(value = "lessons", key = "#courseId")
    public void deleteLesson(Long courseId, Long lessonId) {
        Lesson lesson = getLessonById(courseId, lessonId);
        // Delete associated resources from filesystem
        for (LessonResource resource : lesson.getLessonResources()) {
            deleteResourceFile(resource.getFileUrl());
        }
        lessonRepository.delete(lesson);
    }

    // Resource management methods
    public LessonResource uploadResource(Long courseId, Long lessonId, MultipartFile file) throws IOException {
        Lesson lesson = getLessonById(courseId, lessonId);

        String fileName = processAndSaveFile(file);
        ResourceType resourceType = determineResourceType(file.getOriginalFilename());

        LessonResource resource = new LessonResource();
        resource.setLesson(lesson);
        resource.setFileName(fileName);
        resource.setFileUrl(uploadDirectory + "/" + fileName);
        resource.setType(resourceType);
        resource.setFileSize(file.getSize());
        resource.setUploadedAt(new Date());

        return lessonResourceRepository.save(resource);
    }
    public List<LessonResource> getLessonResources(Long courseId, Long lessonId) {
        Lesson lesson = getLessonById(courseId, lessonId);
        return lessonResourceRepository.findByLesson_Id(lessonId);
    }

    public Resource viewResource(Long courseId, Long lessonId, Long resourceId) throws IOException {
        LessonResource lessonResource = lessonResourceRepository.findByIdAndLesson_IdAndLesson_Course_Id(
                        resourceId, lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        Path filePath = Paths.get(lessonResource.getFileUrl());
        if (!Files.exists(filePath)) {
            throw new ResourceNotFoundException("File not found: " + lessonResource.getFileName());
        }

        return new FileSystemResource(filePath.toFile());
    }


    private String processAndSaveFile(MultipartFile file) throws IOException {
        Path uploadPath = Paths.get(uploadDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileName = UUID.randomUUID().toString() + "_" + originalFilename;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    private ResourceType determineResourceType(String fileName) {
        if (fileName == null) {
            return ResourceType.OTHER;
        }
        String extension = fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
        return switch (extension) {
            case "pdf" -> ResourceType.PDF;
            case "mp4", "avi", "mov" -> ResourceType.VIDEO;
            case "mp3", "wav" -> ResourceType.AUDIO;
            case "jpg", "jpeg", "png", "gif" -> ResourceType.IMAGE;
            case "doc", "docx", "txt" -> ResourceType.DOCUMENT;
            default -> ResourceType.OTHER;
        };
    }
    private void deleteResourceFile(String fileUrl) {
        try {
            Path filePath = Paths.get(fileUrl);
            Files.deleteIfExists(filePath);
        } catch (IOException e) {
        }
    }

    public void deleteResource(Long courseId, Long lessonId, Long resourceId) {
        LessonResource resource = lessonResourceRepository.findByIdAndLesson_IdAndLesson_Course_Id(
                        resourceId, lessonId, courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found"));

        deleteResourceFile(resource.getFileUrl());
        lessonResourceRepository.delete(resource);
    }
}