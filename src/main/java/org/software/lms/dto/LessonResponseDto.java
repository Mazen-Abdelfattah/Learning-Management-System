package org.software.lms.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Date;


@AllArgsConstructor
@NoArgsConstructor
public class LessonResponseDto {
    private Long id;
    private String title;
    private String description;
    private Integer duration;
    private Integer orderIndex;
    private Long courseId;
    private Date createdAt;
    private Date updatedAt;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public Integer getDuration() {
        return duration;
    }

    public Integer getOrderIndex() {
        return orderIndex;
    }

    public Long getCourseId() {
        return courseId;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }
}