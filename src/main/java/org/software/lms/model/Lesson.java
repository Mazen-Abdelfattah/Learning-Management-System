package org.software.lms.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "lessons")
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



    @Column(nullable = false)
    private String title;

    @Column(length = 1000, nullable = false)
    private String description;

    @ManyToOne
    @JoinColumn(name = "course_id",nullable = false)
    @JsonIgnore
    private Course course;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<LessonResource> lessonResources = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL)
    @JsonIgnore
    private List<LessonAttendance> attendanceRecords = new ArrayList<>();

    @Column(nullable = false)
    private Integer duration;

    @Column(nullable = false)
    private Integer orderIndex;

    @Column(nullable = false)
    private Date createdAt = new Date();

    @Column(nullable = false)
    private Date updatedAt = new Date();

    @PrePersist
    protected void onCreate() {
        createdAt = new Date();
        updatedAt = new Date();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = new Date();
    }

    @ManyToMany
    @JoinTable(
            name = "Lesson_students",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "lesson_id")
    )    private List<User> attendances = new ArrayList<>();

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL)
    private List<LessonResource> resources = new ArrayList<>();


    public void addAttendance(User student) {
        if (this.attendances == null) {
            this.attendances = new ArrayList<>();
        }
        attendances.add(student);
    }

    public void removeAttendance(User student) {
        if (this.attendances != null) {
            attendances.remove(student);
        }
    }

    public void addResource(LessonResource resource) {
        if (this.resources == null) {
            this.resources = new ArrayList<>();
        }
        resources.add(resource);
        resource.setLesson(this);
    }


    public void removeResource(LessonResource resource) {
        if (this.resources != null) {
            this.resources.remove(resource);
            resource.setLesson(null);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }

    public List<LessonResource> getLessonResources() {
        return lessonResources;
    }

    public void setLessonResources(List<LessonResource> lessonResources) {
        this.lessonResources = lessonResources;
    }

    public List<LessonAttendance> getAttendanceRecords() {
        return attendanceRecords;
    }

    public void setAttendanceRecords(List<LessonAttendance> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(Integer orderIndex) {
        this.orderIndex = orderIndex;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<User> getAttendances() {
        return attendances;
    }

    public void setAttendances(List<User> attendances) {
        this.attendances = attendances;
    }

    public List<LessonResource> getResources() {
        return resources;
    }

    public void setResources(List<LessonResource> resources) {
        this.resources = resources;
    }
}