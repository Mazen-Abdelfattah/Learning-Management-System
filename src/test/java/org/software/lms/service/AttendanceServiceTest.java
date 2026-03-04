package org.software.lms.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.software.lms.model.*;
import org.software.lms.repository.*;
import org.software.lms.service.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.util.concurrent.TimeUnit;
import static org.mockito.Mockito.never;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService attendanceService;

    @Mock
    private LessonRepository lessonRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private LessonAttendanceRepository attendanceRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private org.springframework.data.redis.core.ValueOperations<String, String> valueOperations;


    private Lesson mockLesson;
    private User mockStudent;
    private LessonAttendance mockAttendance;

    @BeforeEach
    void setUp() {
        mockLesson = new Lesson();
        mockLesson.setId(1L);

        mockStudent = new User();
        mockStudent.setId(1L);

        mockAttendance = new LessonAttendance();
        mockAttendance.setLesson(mockLesson);
        mockAttendance.setStudent(mockStudent);
        mockAttendance.setAttendanceDate(new Date());
        mockAttendance.setOtpUsed("123456");
        mockAttendance.setPresent(true);
    }

    @Test
    void testGenerateOTP() {
        when(lessonRepository.findByIdAndCourse_Id(1L, 1L)).thenReturn(Optional.of(mockLesson));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        String otp = attendanceService.generateOTP(1L, 1L);

        assertNotNull(otp);
        assertEquals(6, otp.length());
        verify(lessonRepository, times(1)).findByIdAndCourse_Id(1L, 1L);
        verify(lessonRepository, never()).save(any()); // no longer saves to DB
        verify(valueOperations, times(1)).set(anyString(), anyString(), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    void testMarkAttendance() {
        when(lessonRepository.findByIdAndCourse_Id(1L, 1L)).thenReturn(Optional.of(mockLesson));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");
        when(attendanceRepository.findByLessonAndStudent(mockLesson, mockStudent)).thenReturn(Optional.empty());
        when(attendanceRepository.save(any(LessonAttendance.class))).thenReturn(mockAttendance);

        LessonAttendance attendance = attendanceService.markAttendance(1L, 1L, 1L, "123456");

        assertNotNull(attendance);
        assertTrue(attendance.getPresent());
        assertEquals(mockLesson, attendance.getLesson());
        assertEquals(mockStudent, attendance.getStudent());
        verify(attendanceRepository, times(1)).save(any(LessonAttendance.class));
    }

    @Test
    void testMarkAttendanceWithInvalidOTP() {
        when(lessonRepository.findByIdAndCourse_Id(1L, 1L)).thenReturn(Optional.of(mockLesson));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");

        Exception exception = assertThrows(IllegalStateException.class, () ->
                attendanceService.markAttendance(1L, 1L, 1L, "654321"));

        assertEquals("Invalid OTP", exception.getMessage());
    }

    @Test
    void testMarkAttendanceAlreadyMarked() {
        when(lessonRepository.findByIdAndCourse_Id(1L, 1L)).thenReturn(Optional.of(mockLesson));
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn("123456");
        when(attendanceRepository.findByLessonAndStudent(mockLesson, mockStudent))
                .thenReturn(Optional.of(mockAttendance));

        Exception exception = assertThrows(IllegalStateException.class, () ->
                attendanceService.markAttendance(1L, 1L, 1L, "123456"));

        assertEquals("Attendance already marked for this student", exception.getMessage());
    }

    @Test
    void testGetLessonAttendance() {
        when(lessonRepository.findByIdAndCourse_Id(1L, 1L)).thenReturn(Optional.of(mockLesson));
        when(attendanceRepository.findByLesson(mockLesson)).thenReturn(List.of(mockAttendance));

        List<LessonAttendance> attendances = attendanceService.getLessonAttendance(1L, 1L);

        assertNotNull(attendances);
        assertFalse(attendances.isEmpty());
        assertEquals(1, attendances.size());
        assertEquals(mockAttendance, attendances.get(0));
    }

    @Test
    void testGetStudentAttendance() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockStudent));
        when(attendanceRepository.findByStudent(mockStudent)).thenReturn(List.of(mockAttendance));

        List<LessonAttendance> attendances = attendanceService.getStudentAttendance(1L, 1L, 1L);

        assertNotNull(attendances);
        assertFalse(attendances.isEmpty());
        assertEquals(1, attendances.size());
        assertEquals(mockAttendance, attendances.get(0));
    }

    @Test
    void testHasAttendance() {
        when(attendanceRepository.existsByLessonIdAndStudentIdAndPresent(1L, 1L, true)).thenReturn(true);

        boolean hasAttendance = attendanceService.hasAttendance(1L, 1L, 1L);

        assertTrue(hasAttendance);
        verify(attendanceRepository, times(1)).existsByLessonIdAndStudentIdAndPresent(1L, 1L, true);
    }
}
