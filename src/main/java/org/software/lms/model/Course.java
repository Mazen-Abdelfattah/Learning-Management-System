    package org.software.lms.model;
    
    import com.fasterxml.jackson.annotation.JsonIgnore;
    import com.fasterxml.jackson.annotation.JsonManagedReference;
    import jakarta.persistence.*;
    import lombok.*;
    
    import java.util.*;
    
    @Entity
    @Table(name = "courses")
    @NoArgsConstructor
    @AllArgsConstructor
    public class Course {
        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;
    
        @Column(nullable = false)
        private String title;
    
        @Column(length = 1000, nullable = false)
        private String description;
    
        @Column(nullable = false)
        private Integer duration;
    
        @Column(nullable = false , updatable = false)
        private Date createdAt = new Date();
    
        @Column(nullable = false)
        private Date updatedAt = new Date(); ;

        @JsonIgnore
        @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
        private List<Quiz> quizzes = new ArrayList<>();

        // In Course.java, add:
        @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
        private List<Assignment> assignments = new ArrayList<>();

        @JsonIgnore
        @OneToMany(mappedBy = "course", cascade = CascadeType.ALL)
        private List<Question> questionBank = new ArrayList<>();

        // Add helper methods
        public void addAssignment(Assignment assignment) {
            if (this.assignments == null) {
                this.assignments = new ArrayList<>();
            }
            this.assignments.add(assignment);
            assignment.setCourse(this);
        }


        public List<Assignment> getAssignments() {
            return assignments;
        }

        public void setAssignments(List<Assignment> assignments) {
            this.assignments = assignments;
        }
    
    
        @ManyToMany
        @JoinTable(
                name = "course_instructor",
                joinColumns = @JoinColumn(name = "course_id"),
                inverseJoinColumns = @JoinColumn(name = "instructor_id")
        )
        private List<User> instructors = new ArrayList<>();
    
        @OneToMany(mappedBy = "course", cascade = CascadeType.ALL )
        private List<Lesson> lessons = new ArrayList<>();
    
        @ManyToMany
        @JoinTable(
                name = "course_students",
                joinColumns = @JoinColumn(name = "course_id"),
                inverseJoinColumns = @JoinColumn(name = "student_id")
        )
            private List<User> studentEnrolledCourses = new ArrayList<>();
    
        @PreUpdate
        protected void onUpdate() {
            updatedAt = new Date();
        }
        public void addInstructor(User instructor) {
            if (this.instructors == null) {
                this.instructors = new ArrayList<>();
            }
            this.instructors.add(instructor);
        }
    
        public void addLesson(Lesson lesson) {
            if (this.lessons == null) {
                this.lessons = new ArrayList<>();
            }
            this.lessons.add(lesson);
        }
    
        public void addEnrolledStudent(User student) {
            if (this.studentEnrolledCourses == null) {
                this.studentEnrolledCourses = new ArrayList<>();
            }
            this.studentEnrolledCourses.add(student);
        }
    
        public String getFormattedDuration() {
            int hours = this.duration / 60;
            int minutes = this.duration % 60;
            return String.format("%d hours %d minutes", hours, minutes);
        }
    
        public boolean hasLessons() {
            return !this.lessons.isEmpty();
        }
    
        public void updateTimestamp() {
            this.updatedAt = new Date();
        }
    
        public void removeInstructor(User instructor) {
            if (this.instructors != null) {
                this.instructors.remove(instructor);
            }
        }
    
        public void removeLesson(Lesson lesson) {
            if (this.lessons != null) {
                this.lessons.remove(lesson);
            }
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
        public Integer getDuration() {
            return duration;
        }
        public void setDuration(Integer duration) {
            this.duration = duration;
        }
        public List<User> getInstructors() {
            return instructors;
        }
        public void setInstructors(List<User> instructors) {
            if (this.instructors == null) {
                this.instructors = new ArrayList<>();
            }
            this.instructors = instructors;
        }
        public List<Lesson> getLessons() {
            return lessons;
        }
        public void setLessons(List<Lesson> lessons) {
            if (this.lessons == null) {
                this.lessons = new ArrayList<>();
            }
            this.lessons = lessons;
        }
        public List<User> getStudentEnrolledCourses() {
            return studentEnrolledCourses;
        }
        public void setStudentEnrolledCourses(List<User> studentEnrolledCourses) {
            if (this.studentEnrolledCourses == null) {
                this.studentEnrolledCourses =  new ArrayList<>();;
            }
            this.studentEnrolledCourses = studentEnrolledCourses;
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
        public Long getId() {
            return id;
        }
        public void setId(Long id) {
            this.id = id;
        }
        public List<Question> getQuestionBank() {
            if (this.questionBank == null) {
                this.questionBank = new ArrayList<>();
            }
            return questionBank;
        }

        public void setQuestionBank(List<Question> questionBank) {
            this.questionBank = questionBank;
        }

        public void addQuestionToBank(Question question) {
            if (this.questionBank == null) {
                this.questionBank = new ArrayList<>();
            }
            this.questionBank.add(question);
            question.setCourse(this);
        }

        public void removeQuestionFromBank(Question question) {
            if (this.questionBank != null) {
                this.questionBank.remove(question);
                question.setCourse(null);
            }
        }

        public List<Quiz> getQuizzes() {
            return quizzes;
        }

        public void setQuizzes(List<Quiz> quizzes) {
            if (this.quizzes == null) {
                this.quizzes = new ArrayList<>();
            }
            this.quizzes = quizzes;
        }

    }