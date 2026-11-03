package mn.edu.must.sqat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * CourseRegistrationService-ийн нэгжийн тестүүд.
 * Хоёр хамаарал (сан, и-мэйл илгээгч) Mockito-гийн mock-оор орлуулагдсан тул
 * өгөгдлийн сан, SMTP сервергүйгээр зөвхөн бизнес логикийг тусгаарлан шалгана.
 */
@ExtendWith(MockitoExtension.class)
class CourseRegistrationServiceTest {

    @Mock StudentRepository repository;
    @Mock EmailSender emailSender;
    CourseRegistrationService service;

    @BeforeEach
    void setUp() {
        service = new CourseRegistrationService(repository, emailSender);
    }

    // ---------------------------------------------------------------
    // A1 — амжилттай бүртгэл: when + verify
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A1: Идэвхтэй оюутан бүх нөхцөл хангасан үед OK буцаж, хадгалагдаж, и-мэйл илгээгдэнэ")
    void register_activeStudentAllConditionsMet_returnsOkSavesAndSendsEmail() {
        // Arrange
        Student student = new Student("S001", "nara@must.edu.mn", true, List.of("CS101", "CS201"));
        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101", "CS201"));

        // Act
        String result = service.register("S001", "CS301");

        // Assert
        assertEquals("OK", result);
        verify(repository).saveRegistration("S001", "CS301");
        verify(emailSender).send(eq("nara@must.edu.mn"), anyString(), anyString());
    }

    // ---------------------------------------------------------------
    // A2 — оюутан олдсонгүй: verify(..., never())
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A2: Оюутан олдоогүй үед ERROR_NO_STUDENT буцаж, хадгалахгүй, и-мэйл илгээхгүй")
    void register_studentNotFound_returnsErrorNoStudentAndSavesNothing() {
        // Arrange
        when(repository.findStudent("S404")).thenReturn(null);

        // Act
        String result = service.register("S404", "CS301");

        // Assert
        assertEquals("ERROR_NO_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verify(emailSender, never()).send(anyString(), anyString(), anyString());
    }

    // ---------------------------------------------------------------
    // A3 — идэвхгүй оюутан: verifyNoInteractions
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A3: Идэвхгүй оюутны хувьд ERROR_INACTIVE_STUDENT буцаж, и-мэйлтэй огт харьцахгүй")
    void register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmailSender() {
        // Arrange
        Student student = new Student("S002", "bat@must.edu.mn", false, List.of("CS101"));
        when(repository.findStudent("S002")).thenReturn(student);

        // Act
        String result = service.register("S002", "CS301");

        // Assert
        assertEquals("ERROR_INACTIVE_STUDENT", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---------------------------------------------------------------
    // A4 — өмнөх холбоо хичээл дутуу: when + never
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A4: Өмнөх холбоо хичээл дутуу үед ERROR_PREREQUISITES буцаж, хадгалахгүй")
    void register_missingPrerequisites_returnsErrorPrerequisitesAndSavesNothing() {
        // Arrange — CS301 нь CS101, CS201 хоёрыг шаарддаг, оюутан зөвхөн CS101-ийг үзсэн
        Student student = new Student("S003", "dorj@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S003")).thenReturn(student);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101", "CS201"));

        // Act
        String result = service.register("S003", "CS301");

        // Assert
        assertEquals("ERROR_PREREQUISITES", result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }

    // ---------------------------------------------------------------
    // A5 — и-мэйл унасан ч бүртгэл хүчинтэй: doThrow
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A5: И-мэйл илгээгч алдаа шидсэн ч бүртгэл OK хэвээр, хадгалалт хийгдсэн байна")
    void register_emailSenderThrows_stillReturnsOkAndKeepsRegistration() {
        // Arrange
        Student student = new Student("S001", "nara@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101"));
        doThrow(new RuntimeException("SMTP сервер холбогдсонгүй"))
                .when(emailSender).send(anyString(), anyString(), anyString());

        // Act
        String result = service.register("S001", "CS301");

        // Assert — мэдэгдэл унасан нь бүртгэлийг унагаах ёсгүй
        assertEquals("OK", result);
        verify(repository).saveRegistration("S001", "CS301");
        verify(emailSender).send(anyString(), anyString(), anyString());
    }

    // ---------------------------------------------------------------
    // A6 — null оролт: assertThrows (хоёр аргумент тус бүрд)
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A6: null оролтод IllegalArgumentException шидэж, хамаарлуудтай огт харьцахгүй")
    void register_nullArguments_throwsIllegalArgumentExceptionAndTouchesNoDependency() {
        // Act + Assert — аргумент тус бүрийн null-ийг тусад нь шалгана
        assertThrows(IllegalArgumentException.class, () -> service.register(null, "CS301"),
                "studentId null үед exception шидэх ёстой");
        assertThrows(IllegalArgumentException.class, () -> service.register("S001", null),
                "courseId null үед exception шидэх ёстой");
        assertThrows(IllegalArgumentException.class, () -> service.register(null, null),
                "хоёул null үед exception шидэх ёстой");

        // Шалгалт оролтын түвшинд таслагдсан тул хамаарлууд огт дуудагдахгүй
        verifyNoInteractions(repository);
        verifyNoInteractions(emailSender);
    }

    // ---------------------------------------------------------------
    // A7 — и-мэйлийн биеийг барих: ArgumentCaptor
    // ---------------------------------------------------------------
    @Test
    @DisplayName("A7: Илгээгдсэн и-мэйлийн биед хичээлийн код орсон байна (ArgumentCaptor)")
    void register_successfulRegistration_emailBodyContainsCourseId() {
        // Arrange
        Student student = new Student("S001", "nara@must.edu.mn", true, List.of("CS101"));
        when(repository.findStudent("S001")).thenReturn(student);
        when(repository.courseExists("CS301")).thenReturn(true);
        when(repository.prerequisites("CS301")).thenReturn(List.of("CS101"));

        // Act
        service.register("S001", "CS301");

        // Assert — илгээгдсэн гурван аргументыг барьж авч шалгана
        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> subjectCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailSender).send(toCaptor.capture(), subjectCaptor.capture(), bodyCaptor.capture());

        assertEquals("nara@must.edu.mn", toCaptor.getValue());
        assertFalse(subjectCaptor.getValue().isBlank(), "гарчиг хоосон байж болохгүй");
        assertTrue(bodyCaptor.getValue().contains("CS301"),
                "и-мэйлийн биед хичээлийн код орсон байх ёстой, гэвч: " + bodyCaptor.getValue());
    }

    // ---------------------------------------------------------------
    // A8 — гурван алдааны анги нэг параметржүүлсэн тестээр
    // ---------------------------------------------------------------
    @ParameterizedTest(name = "A8: {0} → {1}")
    @CsvSource({
            "S404, ERROR_NO_STUDENT",
            "S002, ERROR_INACTIVE_STUDENT",
            "S003, ERROR_NO_COURSE"
    })
    @DisplayName("A8: Алдааны гурван анги тохирох кодоо буцаана")
    void register_invalidScenarios_returnsMatchingErrorCode(String studentId, String expected) {
        // Arrange — мөр бүр эдгээрийн ЗӨВХӨН нэгийг ашигладаг тул lenient() шаардлагатай
        lenient().when(repository.findStudent("S404")).thenReturn(null);
        lenient().when(repository.findStudent("S002"))
                .thenReturn(new Student("S002", "bat@must.edu.mn", false, List.of("CS101")));
        lenient().when(repository.findStudent("S003"))
                .thenReturn(new Student("S003", "dorj@must.edu.mn", true, List.of("CS101")));
        lenient().when(repository.courseExists("CS999")).thenReturn(false);

        // Act
        String result = service.register(studentId, "CS999");

        // Assert
        assertEquals(expected, result);
        verify(repository, never()).saveRegistration(anyString(), anyString());
        verifyNoInteractions(emailSender);
    }
}
