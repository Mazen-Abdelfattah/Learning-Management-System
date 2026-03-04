package org.software.lms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.software.lms.dto.UserDto;
import org.software.lms.exception.ResourceNotFoundException;
import org.software.lms.model.Course;
import org.software.lms.model.Lesson;
import org.software.lms.model.Role;
import org.software.lms.model.User;
import org.software.lms.repository.CourseRepository;
import org.software.lms.repository.LessonRepository;
import org.software.lms.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CourseServiceTest {

    @Mock
    private CourseRepository courseRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private CourseService courseService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Manually inject repositories
        ReflectionTestUtils.setField(courseService, "userRepository", userRepository);
        ReflectionTestUtils.setField(courseService, "lessonRepository", lessonRepository);
        ReflectionTestUtils.setField(courseService, "courseRepository", courseRepository);

        // Setup Security Context
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void testCreateCourse() {
        // Prepare test data
        String instructorEmail = "instructor@test.com";
        User instructor = new User();
        instructor.setEmail(instructorEmail);
        instructor.setRole(Role.INSTRUCTOR);

        Course course = new Course();
        course.setTitle("Test Course");

        when(authentication.getName()).thenReturn(instructorEmail);
        when(userRepository.findByEmail(instructorEmail)).thenReturn(Optional.of(instructor));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        // Test course creation
        Course result = courseService.createCourse(course);

        // Verify results
        assertNotNull(result);
        assertEquals("Test Course", result.getTitle());
        verify(courseRepository, times(1)).save(any(Course.class));
        assertTrue(result.getInstructors().contains(instructor));
    }

    @Test
    void testCreateCourse_InstructorNotFound() {
        String instructorEmail = "nonexistent@test.com";
        Course course = new Course();

        when(authentication.getName()).thenReturn(instructorEmail);
        when(userRepository.findByEmail(instructorEmail)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            courseService.createCourse(course);
        });
    }

    @Test
    void testGetAllCourses() {
        List<Course> courses = Arrays.asList(new Course(), new Course());
        when(courseRepository.findAll()).thenReturn(courses);

        List<Course> result = courseService.getAllCourses();

        assertEquals(2, result.size());
        verify(courseRepository, times(1)).findAll();
    }

    @Test
    void testGetCourseById() {
        Long courseId = 1L;
        Course course = new Course();
        course.setId(courseId);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        Optional<Course> result = courseService.getCourseById(courseId);

        assertTrue(result.isPresent());
        assertEquals(courseId, result.get().getId());
    }

    @Test
    void testAddInstructorsToCourse() {
        Long courseId = 1L;
        Course course = new Course();
        User instructor = new User();
        instructor.setRole(Role.INSTRUCTOR);
        List<Long> instructorIds = Collections.singletonList(1L);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findAllById(instructorIds)).thenReturn(Collections.singletonList(instructor));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course result = courseService.addInstructorsToCourse(courseId, instructorIds);

        assertNotNull(result);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testAddStudentsToCourse() {
        Long courseId = 1L;
        Course course = new Course();
        User student = new User();
        student.setRole(Role.STUDENT);
        List<Long> studentIds = Collections.singletonList(1L);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findAllById(studentIds)).thenReturn(Collections.singletonList(student));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course result = courseService.addStudentsToCourse(courseId, studentIds);

        assertNotNull(result);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testAddLessonsToCourse() {
        Long courseId = 1L;
        Course course = new Course();
        course.setLessons(new ArrayList<>());
        Lesson lesson = new Lesson();
        List<Long> lessonIds = Collections.singletonList(1L);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(lessonRepository.findAllById(lessonIds)).thenReturn(Collections.singletonList(lesson));
        when(courseRepository.save(any(Course.class))).thenReturn(course);

        Course result = courseService.addLessonsToCourse(courseId, lessonIds);

        assertNotNull(result);
        verify(courseRepository, times(1)).save(any(Course.class));
    }

    @Test
    void testDeleteInstructorFromCourse() {
        Long courseId = 1L;
        Long instructorId = 1L;
        Course course = new Course();
        course.setInstructors(new ArrayList<>());
        User instructor = new User();
        course.getInstructors().add(instructor);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(instructorId)).thenReturn(Optional.of(instructor));

        courseService.deleteInstructorFromCourse(courseId, instructorId);

        verify(courseRepository, times(1)).save(course);
    }

    @Test
    void testDeleteStudentFromCourse() {
        Long courseId = 1L;
        Long studentId = 1L;
        Course course = new Course();
        course.setStudentEnrolledCourses(new ArrayList<>());
        User student = new User();
        course.getStudentEnrolledCourses().add(student);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));
        when(userRepository.findById(studentId)).thenReturn(Optional.of(student));

        courseService.deleteStudentFromCourse(courseId, studentId);

        verify(courseRepository, times(1)).save(course);
    }

    @Test
    void testFindStudentEnrolledInCourse() {
        Long courseId = 1L;
        Course course = new Course();
        List<User> enrolledStudents = Arrays.asList(new User(), new User());
        course.setStudentEnrolledCourses(enrolledStudents);

        when(courseRepository.findById(courseId)).thenReturn(Optional.of(course));

        List<UserDto> result = courseService.findStudentEnrolledInCourse(courseId);

        assertEquals(2, result.size());
    }

    @Test
    void testFindCoursesByTitle() {
        String title = "Test Course";
        List<Course> courses = Arrays.asList(new Course(), new Course());

        when(courseRepository.findByTitle(title)).thenReturn(courses);

        List<Course> result = courseService.findCoursesByTitle(title);

        assertEquals(2, result.size());
        verify(courseRepository, times(1)).findByTitle(title);
    }

    @Test
    void testFindCoursesByInstructorId() {
        Long instructorId = 1L;
        List<Course> courses = Arrays.asList(new Course(), new Course());

        when(courseRepository.findByInstructors_Id(instructorId)).thenReturn(courses);

        List<Course> result = courseService.findCoursesByInstructorId(instructorId);

        assertEquals(2, result.size());
        verify(courseRepository, times(1)).findByInstructors_Id(instructorId);
    }
}