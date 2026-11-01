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
> `java -version` нь 17-г харуулдаг ч Maven анхдагчаар JDK **27**-г сонгож авдаг байв
> (`mvn -version` → `Java version: 27`). Mockito 5.11.0 нь byte-buddy 1.14.12-оор
> байт-код өөрчилдөг бөгөөд тийм шинэ class file хувилбарыг мэдэхгүй тул эрсдэлтэй.
> Тиймээс `export JAVA_HOME=$(/usr/libexec/java_home -v 17)` гэж Maven-ийг JDK 17 руу
> тогтоож, тэр мөрийг `~/.zshrc`-д нэмсэн. Дээрх `mvn -version`-ийн гаралт үүний дараах
> байдал юм.

## 2. Төслийн бүтэц

```
F.CSA313-lab8/
├── pom.xml                                          JUnit 5 + Mockito 5 + surefire
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
│   └── mvn-test-mutant-3.txt                        courseId null шалгалт хассан
└── .gitignore                                       target/, .idea/, .DS_Store
```

Ажиллуулах:

```bash
mkdir -p results && mvn test 2>&1 | tee results/mvn-test.txt
```

## 3. Спецификаци (oracle)

`CourseRegistrationService.register(studentId, courseId)`-ийн зөв зан төлөв:

| Нөхцөл | Үр дүн | Хажуугийн нөлөө |
|---|---|---|
| `studentId` эсвэл `courseId` нь `null` | `IllegalArgumentException` | хамааралтай огт харьцахгүй |
| Оюутан олдсонгүй (`findStudent` → `null`) | `"ERROR_NO_STUDENT"` | хадгалахгүй, и-мэйл илгээхгүй |
| Оюутан идэвхгүй (`active() == false`) | `"ERROR_INACTIVE_STUDENT"` | хадгалахгүй, и-мэйл илгээхгүй |
| Хичээл олдсонгүй (`courseExists` → `false`) | `"ERROR_NO_COURSE"` | хадгалахгүй, и-мэйл илгээхгүй |
| Өмнөх холбоо хичээл дутуу | `"ERROR_PREREQUISITES"` | хадгалахгүй, и-мэйл илгээхгүй |
| Бүх нөхцөл хангагдсан | `"OK"` | `saveRegistration` дуудагдана, и-мэйл илгээгдэнэ |
| И-мэйл илгээгч exception шидсэн | `"OK"` (бүртгэл хүчинтэй) | `saveRegistration` аль хэдийн хийгдсэн |

## 4. Хэсэг A: Тестүүд

TODO — тестээ бичсэний дараа бөглөнө.

| # | Тест | Гол хэрэгсэл |
|---|---|---|
| A1 | TODO | `when` + `verify` |
| A2 | TODO | `verify(..., never())` |
| A3 | TODO | `verifyNoInteractions` |
| A4 | TODO | `when` + `never` |
| A5 | TODO | `doThrow(...).when(...)` |
| A6 | TODO | `assertThrows` |
| A7 | TODO | `ArgumentCaptor` |
| A8 | TODO | `@ParameterizedTest` + `@CsvSource` |

```
TODO — results/mvn-test.txt-ийн сүүлийн "Tests run:" мөрийг энд хуулж тавь
```

→ [results/mvn-test.txt](results/mvn-test.txt)

### A8 ба `lenient()`

TODO — 1–2 өгүүлбэр: A8-д яагаад `lenient().when(...)` хэрэгтэй болсон (эсвэл хэрэггүй байсан) бэ.

## 5. Хэсэг B: Мини мутаци

| # | Эвдсэн зүйл | Унасан тестүүд | Гаралт |
|---|---|---|---|
| 1 | `if (!student.active()) ...` комментлосон | TODO | [mutant-1](results/mvn-test-mutant-1.txt) |
| 2 | `repository.saveRegistration(...)` комментлосон | TODO | [mutant-2](results/mvn-test-mutant-2.txt) |
| 3 | `\|\| courseId == null` хассан | TODO | [mutant-3](results/mvn-test-mutant-3.txt) |

TODO — мутант бүрийн дараа кодыг сэргээж, эцэст нь `results/mvn-test.txt`-ийг ногоонооор дахин авсан эсэхээ бич. Мутант 3 эхлээд нэг ч тест унагаагүй бол A6-д юу зассанаа бич.

## 6. Дүгнэлт

TODO — 6–8 өгүүлбэр:

- mock ба гараар бичсэн fake-ийн ялгаа
- юуг mock хийх ЁСГҮЙ вэ
- `verify`-г хэтрүүлбэл ямар эрсдэлтэй вэ

## 7. AI ашиглалтын тэмдэглэл

TODO — AI-г ямар ажилд ашигласнаа бич.
