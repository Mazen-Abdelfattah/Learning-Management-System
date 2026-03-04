package org.software.lms.controller;

import org.software.lms.dto.CourseDto;
import org.software.lms.dto.UserDto;
import org.software.lms.model.Course;
import org.software.lms.model.User;
import org.software.lms.repository.UserRepository;
import org.software.lms.service.CourseService;
import org.software.lms.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseController {

    private final CourseService courseService;

    private final NotificationController notifControl;

    @Autowired
    public CourseController(CourseService courseService, NotificationController notifControl) {
        this.courseService = courseService;
        this.notifControl = notifControl;
    }

    @Autowired
    private UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public Course createCourse(@RequestBody Course course) {
        return courseService.createCourse(course);
    }

    @GetMapping
    public List<Course> getAllCourses() {
        return courseService.getAllCourses();
    }

    @GetMapping("/{id}")
    public Course getCourseById(@PathVariable Long id) {
        return courseService.getCourseById(id)
                .orElseThrow(() -> new RuntimeException("Course not found with id: " + id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public Course updateCourse(@PathVariable Long CourseId, @RequestBody Course updatedCourse) {
        Course course = courseService.updateCourse(CourseId, updatedCourse);
        List<UserDto> enrolledStudents = findStudentEnrolledInCourse(CourseId);
        for (UserDto stud : enrolledStudents) {
            Long StudId = stud.getId();
            String title = "Course Updated";
            String message = "Course Information has been updated";
            notifControl.createNotification(StudId, CourseId, title, message);
        }
        return course;
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public void deleteCourse(@PathVariable Long id) {
        List<UserDto> enrolledStudents = findStudentEnrolledInCourse(id);
        for (UserDto stud : enrolledStudents) {
            Long StudId = stud.getId();
            String title = "Course Deletion";
            String message = "Course has been deleted";
            notifControl.createNotification(StudId, id, title, message);
        }
        courseService.deleteCourse(id);
    }

    @GetMapping("/search/by-title/{title}")
    public List<Course> findCoursesByTitle(@PathVariable String title) {
        return courseService.findCoursesByTitle(title);
    }
    @GetMapping("/search/by-instructor/{instructorId}")
    public List<Course> findCoursesByInstructorId(@PathVariable Long instructorId) {
        return courseService.findCoursesByInstructorId(instructorId);
    }
    @GetMapping("/search/by-created-date/{createdAt}")
    public List<Course> findCoursesByCreatedAtAfter(@PathVariable java.util.Date createdAt) {
        return courseService.findCoursesByCreatedAtAfter(createdAt);
    }
    @PostMapping("/{id}/instructors")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Course> addInstructorsToCourse(@PathVariable Long id, @RequestBody List<Long> instructorIds) {
        Course course = courseService.addInstructorsToCourse(id, instructorIds); // Use 'id' here
        List<UserDto> enrolledStudents = findStudentEnrolledInCourse(id); // Use 'id' here
        if(enrolledStudents!=null) {
            for (UserDto stud : enrolledStudents) {
                Long StudId = stud.getId();
                String title = "New Instructor";
                String message = "A new Instructor has been added to the course";
                notifControl.createNotification(StudId, id, title, message); // Use 'id' here
            }
        }
        return ResponseEntity.ok(course);
    }


    @PostMapping("/{id}/students")
    public ResponseEntity<Course> addStudentsToCourse(@PathVariable Long id, @RequestBody List<Long> studentIds) {
        Course course = courseService.addStudentsToCourse(id, studentIds);
        Long courseId = course.getId();

        // Notify students of their enrollment
        String studentMessage = "Enrollment successful";
        for (Long studentId: studentIds) {
            notifControl.createNotification(studentId, courseId, "Enrollment Confirmation", studentMessage);
        }

        // Get all instructors for the course and notify them
        List<User> instructors = course.getInstructors();
        if (instructors != null && !instructors.isEmpty()) {
            // Get enrolled student names for the message
            List<User> enrolledStudents = userRepository.findAllById(studentIds);
            StringBuilder studentNames = new StringBuilder();
            for (int i = 0; i < enrolledStudents.size(); i++) {
                User student = enrolledStudents.get(i);
                studentNames.append(student.getFirstName())
                        .append(" ")
                        .append(student.getLastName());
                if (i < enrolledStudents.size() - 1) {
                    studentNames.append(", ");
                }
            }

            String instructorMessage = "New student" +
                    (studentIds.size() > 1 ? "s" : "") +
                    " enrolled: " + studentNames.toString();

            // Create notification for each instructor
            for (User instructor : instructors) {
                notifControl.createNotification(
                        instructor.getId(),
                        courseId,
                        "New Student Enrollment",
                        instructorMessage
                );
            }
        }

        return ResponseEntity.ok(course);
    }


    @DeleteMapping("/{id}/instructors/{instructorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteInstructorFromCourse(@PathVariable Long id, @PathVariable Long instructorId) {
        courseService.deleteInstructorFromCourse(id, instructorId);
        return ResponseEntity.noContent().build();
    }
    @DeleteMapping("/{id}/students/{studentId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public ResponseEntity<Void> deleteStudentFromCourse(@PathVariable Long id, @PathVariable Long studentId) {
        courseService.deleteStudentFromCourse(id, studentId);
        notifControl.createNotification(studentId, id, "Removal", "You have been removed");
        return ResponseEntity.noContent().build();
    }


    @GetMapping("/{id}/studentEnrolled")
    @PreAuthorize("hasAnyRole('ADMIN', 'INSTRUCTOR')")
    public List<UserDto> findStudentEnrolledInCourse(@PathVariable Long id) {
        return courseService.findStudentEnrolledInCourse(id);
    }
}
