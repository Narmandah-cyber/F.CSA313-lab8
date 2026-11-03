package mn.edu.must.sqat;

// AI туслах (Google Gemini)-ийн үүсгэсэн тестийн класс — ЗӨВХӨН харьцуулалтын
// туршилтад ашигласан. Үндсэн build-д ороогүй (src/test-д байрлуулаагүй).
// Агуулгыг нь өөрчлөөгүй, AI-гийн буцаасан хэвээр хадгалсан.

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AiCourseRegistrationServiceTest {

    @Mock
    private StudentRepository repository;

    @Mock
    private EmailSender emailSender;

    @InjectMocks
    private CourseRegistrationService service;

    private final String studentId = "S001";
    private final String courseId = "CS101";

    private Student activeStudent() {
        return new Student(
                studentId,
                "student@must.edu.mn",
                true,
                Arrays.asList("CS100", "MATH101")
        );
    }

    // A1: Идэвхтэй, шаардлага хангасан оюутан бүртгүүлэхэд амжилттай байна.
    @Test
    void register_shouldReturnOK_whenStudentMeetsAllRequirements() {
        when(repository.findStudent(studentId)).thenReturn(activeStudent());
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Collections.singletonList("CS100"));

        String result = service.register(studentId, courseId);

        assertEquals("OK", result);

        verify(repository).findStudent(studentId);
        verify(repository).courseExists(courseId);
        verify(repository).prerequisites(courseId);
        verify(repository).saveRegistration(studentId, courseId);

        verify(emailSender).send(
                "student@must.edu.mn",
                "Бүртгэл баталгаажлаа",
                "CS101 хичээлд амжилттай бүртгэгдлээ."
        );
    }

    // A2: Оюутан олдохгүй бол алдаа буцаана.
    @Test
    void register_shouldReturnErrorNoStudent_whenStudentDoesNotExist() {
        when(repository.findStudent(studentId)).thenReturn(null);

        String result = service.register(studentId, courseId);

        assertEquals("ERROR_NO_STUDENT", result);

        verify(repository).findStudent(studentId);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A3: Идэвхгүй оюутныг бүртгэхгүй.
    @Test
    void register_shouldReturnErrorInactiveStudent_whenStudentIsInactive() {
        Student inactiveStudent = new Student(
                studentId,
                "student@must.edu.mn",
                false,
                Collections.emptyList()
        );

        when(repository.findStudent(studentId)).thenReturn(inactiveStudent);

        String result = service.register(studentId, courseId);

        assertEquals("ERROR_INACTIVE_STUDENT", result);

        verify(repository).findStudent(studentId);
        verify(repository, never()).courseExists(anyString());
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A4: Хичээл байхгүй бол алдаа буцаана.
    @Test
    void register_shouldReturnErrorNoCourse_whenCourseDoesNotExist() {
        when(repository.findStudent(studentId)).thenReturn(activeStudent());
        when(repository.courseExists(courseId)).thenReturn(false);

        String result = service.register(studentId, courseId);

        assertEquals("ERROR_NO_COURSE", result);

        verify(repository).findStudent(studentId);
        verify(repository).courseExists(courseId);
        verify(repository, never()).prerequisites(anyString());
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A5: Урьдчилсан нөхцөл хангагдаагүй бол бүртгэхгүй.
    @Test
    void register_shouldReturnErrorPrerequisites_whenPrerequisitesAreMissing() {
        Student student = new Student(
                studentId,
                "student@must.edu.mn",
                true,
                Collections.singletonList("MATH101")
        );

        when(repository.findStudent(studentId)).thenReturn(student);
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Arrays.asList("CS100", "MATH101"));

        String result = service.register(studentId, courseId);

        assertEquals("ERROR_PREREQUISITES", result);

        verify(repository).prerequisites(courseId);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // A6: Бүх урьдчилсан нөхцөл хангасан бол бүртгэнэ.
    @Test
    void register_shouldReturnOK_whenAllPrerequisitesAreCompleted() {
        Student student = new Student(
                studentId,
                "student@must.edu.mn",
                true,
                Arrays.asList("CS100", "MATH101", "PHY101")
        );

        when(repository.findStudent(studentId)).thenReturn(student);
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Arrays.asList("CS100", "MATH101"));

        String result = service.register(studentId, courseId);

        assertEquals("OK", result);

        verify(repository).saveRegistration(studentId, courseId);
        verify(emailSender).send(
                "student@must.edu.mn",
                "Бүртгэл баталгаажлаа",
                "CS101 хичээлд амжилттай бүртгэгдлээ."
        );
    }

    // A7: Урьдчилсан нөхцөл байхгүй хичээлд бүртгүүлж болно.
    @Test
    void register_shouldReturnOK_whenCourseHasNoPrerequisites() {
        when(repository.findStudent(studentId)).thenReturn(activeStudent());
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Collections.emptyList());

        String result = service.register(studentId, courseId);

        assertEquals("OK", result);

        verify(repository).saveRegistration(studentId, courseId);
        verify(emailSender).send(
                "student@must.edu.mn",
                "Бүртгэл баталгаажлаа",
                "CS101 хичээлд амжилттай бүртгэгдлээ."
        );
    }

    // A8: studentId null бол IllegalArgumentException шиднэ.
    @Test
    void register_shouldThrowException_whenStudentIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(null, courseId)
        );

        assertEquals(
                "studentId, courseId хоёул шаардлагатай",
                exception.getMessage()
        );

        verifyNoInteractions(repository, emailSender);
    }

    // A9: courseId null бол IllegalArgumentException шиднэ.
    @Test
    void register_shouldThrowException_whenCourseIdIsNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.register(studentId, null)
        );

        assertEquals(
                "studentId, courseId хоёул шаардлагатай",
                exception.getMessage()
        );

        verifyNoInteractions(repository, emailSender);
    }

    // A10: Хоёр параметр null бол IllegalArgumentException шиднэ.
    @Test
    void register_shouldThrowException_whenBothParametersAreNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> service.register(null, null)
        );

        verifyNoInteractions(repository, emailSender);
    }

    // A11: Email илгээхэд RuntimeException гарсан ч бүртгэл хүчинтэй байна.
    @Test
    void register_shouldReturnOK_whenEmailSendingFails() {
        when(repository.findStudent(studentId)).thenReturn(activeStudent());
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Email sending failed"))
                .when(emailSender)
                .send(anyString(), anyString(), anyString());

        String result = service.register(studentId, courseId);

        assertEquals("OK", result);

        verify(repository).saveRegistration(studentId, courseId);
        verify(emailSender).send(
                "student@must.edu.mn",
                "Бүртгэл баталгаажлаа",
                "CS101 хичээлд амжилттай бүртгэгдлээ."
        );
    }

    // A12: Бүртгэл хадгалагдаагүй бол Email илгээхгүй.
    @Test
    void register_shouldNotSendEmail_whenRegistrationSavingFails() {
        when(repository.findStudent(studentId)).thenReturn(activeStudent());
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId))
                .thenReturn(Collections.emptyList());

        doThrow(new RuntimeException("Database error"))
                .when(repository)
                .saveRegistration(studentId, courseId);

        assertThrows(
                RuntimeException.class,
                () -> service.register(studentId, courseId)
        );

        verify(repository).saveRegistration(studentId, courseId);
        verifyNoInteractions(emailSender);
    }

    // A13: Урьдчилсан нөхцөлийн жагсаалт өөрчлөгдсөн ч Repository-ийн жагсаалт өөрчлөгдөхгүй.
    @Test
    void register_shouldNotModifyRepositoryPrerequisitesList() {
        List<String> prerequisites = new ArrayList<>(
                Arrays.asList("CS100", "MATH101")
        );

        Student student = new Student(
                studentId,
                "student@must.edu.mn",
                true,
                Collections.singletonList("CS100")
        );

        when(repository.findStudent(studentId)).thenReturn(student);
        when(repository.courseExists(courseId)).thenReturn(true);
        when(repository.prerequisites(courseId)).thenReturn(prerequisites);

        String result = service.register(studentId, courseId);

        assertEquals("ERROR_PREREQUISITES", result);
        assertEquals(Arrays.asList("CS100", "MATH101"), prerequisites);

        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
