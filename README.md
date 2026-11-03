# Лаб 8: Mock объектууд — Mockito-гоор хамаарлыг орлуулж тестлэх

F.CSA313 — Программ хангамжийн чанарын баталгаа ба тест (2026)

**Оюутан:** Г. Нармандах | **Код:** B232270057

## 1. Орчин

`java -version`:

```
openjdk version "17.0.20.1" 2026-08-18
OpenJDK Runtime Environment Homebrew (build 17.0.20.1+0)
OpenJDK 64-Bit Server VM Homebrew (build 17.0.20.1+0, mixed mode, sharing)
```

`mvn -version`:

```
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: /opt/homebrew/Cellar/maven/3.9.16/libexec
Java version: 17.0.20.1, vendor: Homebrew, runtime: /opt/homebrew/Cellar/openjdk@17/17.0.20.1/libexec/openjdk.jdk/Contents/Home
Default locale: en_MN, platform encoding: UTF-8
OS name: "mac os x", version: "26.6.2", arch: "aarch64", family: "mac"
```

| Зүйл | Утга |
|---|---|
| Хэрэгсэл | JUnit 5.10.2 (Jupiter), Mockito 5.11.0, Maven Surefire 3.2.5 |
| Компайлер | `maven.compiler.release` = 17 |
| Машин | MacBook Air (Apple Silicon, macOS 26.6.2) |

> **Орчны тэмдэглэл.** Энэ машин дээр Homebrew-гаар JDK 17 ба JDK 27 хоёул суусан байсан.
> `java -version` нь 17-г харуулдаг ч Maven анхдагчаар JDK **27**-г сонгож авч байв
> (`mvn -version` → `Java version: 27`). Mockito 5.11.0 нь byte-buddy 1.14.12-оор байт-код
> өөрчилдөг бөгөөд ийм шинэ class file хувилбарыг мэддэггүй тул эрсдэлтэй. Тиймээс
> `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` гэж Maven-ийг JDK 17 руу тогтоож,
> тэр мөрийг `~/.zshrc`-д нэмсэн. Дээрх `mvn -version`-ийн гаралт үүний дараах байдал юм.

## 2. Төслийн бүтэц

```
F.CSA313-lab8/
├── pom.xml                                          JUnit 5 + Mockito 5 + surefire
├── mutate.sh                                        Хэсэг B-г давтагдахуйц ажиллуулах скрипт
├── src/main/java/mn/edu/must/sqat/
│   ├── Student.java                                 record — оюутны өгөгдөл
│   ├── StudentRepository.java                       интерфейс (сан), хэрэгжүүлэлтгүй
│   ├── EmailSender.java                             интерфейс (и-мэйл), хэрэгжүүлэлтгүй
│   └── CourseRegistrationService.java               тестлэгдэх класс
├── src/test/java/mn/edu/must/sqat/
│   └── CourseRegistrationServiceTest.java           A1–A8 тестүүд
├── results/
│   ├── mvn-test.txt                                 ногоон гаралт (BUILD SUCCESS)
│   ├── mvn-test-mutant-1.txt                        идэвхгүй шалгалт комментлосон
│   ├── mvn-test-mutant-2.txt                        saveRegistration комментлосон
│   └── mvn-test-mutant-3.txt                        courseId-ийн null шалгалт хассан
└── .gitignore                                       target/, .idea/, .DS_Store
```

Ажиллуулах:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
mkdir -p results && mvn test 2>&1 | tee results/mvn-test.txt
```

Хэсэг A ба B-г бүхэлд нь давтах:

```bash
./mutate.sh
```

## 3. Спецификаци (oracle)

`CourseRegistrationService.register(studentId, courseId)`-ийн зөв зан төлөв:

| Нөхцөл | Үр дүн | Хажуугийн нөлөө |
|---|---|---|
| `studentId` эсвэл `courseId` нь `null` | `IllegalArgumentException` | хамаарлуудтай огт харьцахгүй |
| Оюутан олдсонгүй (`findStudent` → `null`) | `"ERROR_NO_STUDENT"` | хадгалахгүй, и-мэйл илгээхгүй |
| Оюутан идэвхгүй (`active() == false`) | `"ERROR_INACTIVE_STUDENT"` | хадгалахгүй, и-мэйл илгээхгүй |
| Хичээл олдсонгүй (`courseExists` → `false`) | `"ERROR_NO_COURSE"` | хадгалахгүй, и-мэйл илгээхгүй |
| Өмнөх холбоо хичээл дутуу | `"ERROR_PREREQUISITES"` | хадгалахгүй, и-мэйл илгээхгүй |
| Бүх нөхцөл хангагдсан | `"OK"` | `saveRegistration` дуудагдаж, и-мэйл илгээгдэнэ |
| И-мэйл илгээгч exception шидсэн | `"OK"` (бүртгэл хүчинтэй) | `saveRegistration` аль хэдийн хийгдсэн |

## 4. Хэсэг A: Тестүүд

| # | Тестийн метод | Юуг шалгадаг | Гол хэрэгсэл |
|---|---|---|---|
| A1 | `register_activeStudentAllConditionsMet_returnsOkSavesAndSendsEmail` | Бүх нөхцөл хангагдсан үед `"OK"`, `saveRegistration` дуудагдсан, и-мэйл оюутны хаяг руу илгээгдсэн | `when` + `verify` |
| A2 | `register_studentNotFound_returnsErrorNoStudentAndSavesNothing` | `findStudent` → `null` үед `"ERROR_NO_STUDENT"`, хадгалалт ба и-мэйл хийгдээгүй | `verify(..., never())` |
| A3 | `register_inactiveStudent_returnsErrorInactiveAndNeverTouchesEmailSender` | Идэвхгүй оюутанд `"ERROR_INACTIVE_STUDENT"`, и-мэйл илгээгчтэй **огт** харьцаагүй | `verifyNoInteractions` |
| A4 | `register_missingPrerequisites_returnsErrorPrerequisitesAndSavesNothing` | CS301 нь CS101+CS201 шаардахад оюутан зөвхөн CS101 үзсэн → `"ERROR_PREREQUISITES"` | `when` + `never` |
| A5 | `register_emailSenderThrows_stillReturnsOkAndKeepsRegistration` | И-мэйл илгээгч `RuntimeException` шидсэн ч `"OK"`, хадгалалт хэвээр | `doThrow(...).when(...)` |
| A6 | `register_nullArguments_throwsIllegalArgumentExceptionAndTouchesNoDependency` | `(null, "CS301")`, `("S001", null)`, `(null, null)` гурвуулаад exception; хамаарал дуудагдаагүй | `assertThrows` |
| A7 | `register_successfulRegistration_emailBodyContainsCourseId` | Илгээгдсэн и-мэйлийн хүлээн авагч, гарчиг, бие — бие дотор хичээлийн код орсон эсэх | `ArgumentCaptor` |
| A8 | `register_invalidScenarios_returnsMatchingErrorCode` | Гурван алдааны анги нэг тестээр: `ERROR_NO_STUDENT`, `ERROR_INACTIVE_STUDENT`, `ERROR_NO_COURSE` | `@ParameterizedTest` + `@CsvSource` |

Surefire нь `@CsvSource`-ийн мөр бүрийг тусад нь тоолдог тул:

**7 энгийн тест + A8-ийн 3 мөр = Tests run: 10**

```
[INFO] Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

→ [results/mvn-test.txt](results/mvn-test.txt)

### A8 ба `lenient()`

A8-ийн гурван мөр `register()`-ийн **өөр өөр салаа** руу орно: `S404` нь `findStudent`-аас цааш явахгүй, `S002` нь `active()` дээр зогсоно, зөвхөн `S003` л `courseExists`-т хүрнэ. Тиймээс мөр бүр ажиллахад тестийн дотор бэлдсэн дөрвөн stub-аас ердөө нэг нь ашиглагдаж, үлдсэн нь хөндөгдөхгүй үлддэг — `MockitoExtension`-ий **strict stubs** горим үүнийг `UnnecessaryStubbingException` гэж алдаа болгон мэдээлнэ. `lenient().when(...)` нь тухайн stub ашиглагдахгүй байж болохыг Mockito-д зөвшөөрүүлж, параметржүүлсэн тест ажиллах боломжийг олгоно.

## 5. Хэсэг B: Мини мутаци

Мутаци бүрийг [`mutate.sh`](mutate.sh) скриптээр хийсэн — эх кодыг өөрчилж, `mvn test` ажиллуулж, гаралтыг хадгалаад, кодыг **нэн даруй сэргээдэг**. Эцэст нь `diff`-ээр эх кодтой ижил эсэхийг баталгаажуулдаг.

| # | Эвдсэн зүйл | Үр дүн | Унасан тестүүд | Гаралт |
|---|---|---|---|---|
| 1 | `if (!student.active()) return "ERROR_INACTIVE_STUDENT";` комментлосон | `Tests run: 10, Failures: 2` · BUILD FAILURE | **A3** (`...inactiveStudent...`) ба **A8**-ийн `S002` мөр — хоёул `expected: <ERROR_INACTIVE_STUDENT> but was: <ERROR_NO_COURSE>` | [mutant-1](results/mvn-test-mutant-1.txt) |
| 2 | `repository.saveRegistration(studentId, courseId);` комментлосон | `Tests run: 10, Failures: 2` · BUILD FAILURE | **A1** ба **A5** — хоёул `Wanted but not invoked: repository.saveRegistration("S001", "CS301")` | [mutant-2](results/mvn-test-mutant-2.txt) |
| 3 | `if (studentId == null \|\| courseId == null)` → `if (studentId == null)` | `Tests run: 10, Failures: 1` · BUILD FAILURE | **A6** — `courseId null үед exception шидэх ёстой ==> Expected java.lang.IllegalArgumentException to be thrown, but nothing was thrown` | [mutant-3](results/mvn-test-mutant-3.txt) |

**Дүгнэлт.** Гурван мутант гурвуулаа баригдсан нь тестүүд үнэхээр ажил хийж байгааг харууллаа. Анзаарах зүйл нь мутант бүрийг хэдэн тест барьсан харьцаа: мутант 1-ийг хоёр тест барьсан бөгөөд тэдгээрийн нэг нь A8-ийн параметржүүлсэн мөр байсан — өөрөөр хэлбэл параметржүүлсэн тест нь зөвхөн код богиносгодог төдийгүй бодит нэмэлт хамгаалалт өгч байна. Мутант 2-ыг A1 ба A5 хоёулаа барьсан нь онцлог: A5 бол «и-мэйл унасан» хувилбар тул гадуураа и-мэйлийн тухай мэт боловч дотроо хадгалалт хийгдсэнийг шалгадаг учраас энэ эвдрэлийг мэдэрсэн. Мутант 3 нь даалгаврын сануулсан цоорхойн яг жишээ: хэрэв A6-г зөвхөн `studentId`-ийн null-ээр бичсэн бол нэг ч тест унахгүй байж, тест бүрэн мэт харагдах байсан. Тиймээс A6-г эхнээс нь гурван хослолоор (`(null, courseId)`, `(studentId, null)`, `(null, null)`) бичсэн бөгөөд энэ нь мутант 3-ыг шууд барив — засвар хийх шаардлага гарсангүй. Мутаци бүрийн дараа код сэргээгдэж, эцсийн `results/mvn-test.txt` нь `Tests run: 10, Failures: 0` ба `BUILD SUCCESS` хэвээр байна.

## 6. Дүгнэлт

Mock ба гараар бичсэн fake хоёрын гол ялгаа нь зорилгодоо байна: fake бол ажиллах чадвартай хялбаршуулсан хэрэгжүүлэлт (жишээ нь `HashMap` дээр суурилсан хуурамч сан) бөгөөд төлөв хадгалдаг, харин mock бол зан төлөв урьдчилан тохируулагддаг, **харилцан үйлдлийг нь шалгаж болдог** орлуулагч юм. Энэ лабораторид `verify(repository).saveRegistration(...)` гэж шалгах боломж нь яг mock-ийн давуу тал байсан — fake ашигласан бол «хадгалагдсан эсэх»-ийг түүний дотоод төлөвөөс нь уншиж шалгах байсан бөгөөд энэ нь тестийг хэрэгжүүлэлтээс илүү хамааралтай болгодог. Mock хийх ёсгүй зүйлс ч бий: өөрийн эзэмшдэггүй гуравдагч этгээдийн нарийн API (mock нь бодит API өөрчлөгдсөнийг хэзээ ч мэдэхгүй), `String`, `List` зэрэг энгийн утгын объектууд (жинхэнийг нь шууд ашиглах нь хялбар бөгөөд үнэн), мөн тестлэж байгаа класс өөрөө. Ерөнхий дүрэм нь — удаан, тогтворгүй, эсвэл гадаад ертөнцөд хүрдэг хамаарлыг л орлуул, бусдыг нь жинхэнээр нь үлдээ. `verify`-г хэтрүүлбэл тест нь үр дүнг биш **хэрэгжүүлэлтийн алхмуудыг** цээжилж эхэлдэг: кодын логик өөрчлөгдөөгүй атал дотоод дуудалтын дараалал өөрчлөгдөхөд тест унаж, рефакторинг хийх бүрд тестээ засах ачаа үүснэ. Тиймээс энэ лабораторид `verify`-г зөвхөн спецификацид тодорхой заасан хажуугийн нөлөөнд (хадгалалт хийгдсэн эсэх, и-мэйл илгээгдсэн эсэх) хэрэглэж, `findStudent`, `courseExists` зэрэг дотоод унших дуудалтуудыг верификаци хийгээгүй — тэдгээр нь `register()`-ийн буцах утгаар аль хэдийн шалгагдаж байгаа. Хамгийн чухал сургамж нь Хэсэг B-ээс гарлаа: ногоон тест бол чанартай тест гэсэн үг биш, тестийн чанарыг зөвхөн санаатай оруулсан эвдрэл илчилдэг.

## 7. AI ашиглалтын тэмдэглэл

Энэ лабораторид AI туслахыг тестийн классын төсөл боловсруулах, мутацийн скрипт (`mutate.sh`) бичих, тайланг хэлбэржүүлэхэд ашигласан. Бүх build, тест, мутацийн туршилтыг өөрийн MacBook дээр (JDK 17.0.20.1, Maven 3.9.16) ажиллуулж, `results/` доторх дөрвөн гаралтын файлыг өөрөө үүсгэсэн; README-д бичигдсэн тоо, унасан тестийн нэр бүр тэдгээр файлаас шууд авагдсан. JDK 17/27-ийн зөрүүг өөрөө оношилж засав.
