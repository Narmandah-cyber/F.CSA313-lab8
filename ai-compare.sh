#!/bin/bash
# Нэмэлт даалгавар (Сонголт 2): AI-гийн үүсгэсэн тестийн классыг Хэсэг B-гийн
# гурван мутантын эсрэг ажиллуулж, эвдрэлийг барьж чадах эсэхийг ХЭМЖИНЭ.
#
# Төслийг /tmp-д хуулж, тэнд ЗӨВХӨН AI-гийн тестийг үлдээж ажиллуулна —
# үндсэн репогийн src/test болон results/mvn-test*.txt огт хөндөгдөхгүй.
#   ./ai-compare.sh

cd "$(dirname "$0")" || exit 1
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

REPO="$(pwd)"
WORK=/tmp/lab8-ai
OUT="$REPO/results/ai-tests-vs-mutants.txt"

rm -rf "$WORK"
mkdir -p "$WORK"
cp pom.xml "$WORK"/
cp -R src "$WORK"/
rm -f "$WORK"/src/test/java/mn/edu/must/sqat/CourseRegistrationServiceTest.java
cp ai-test/AiCourseRegistrationServiceTest.java "$WORK"/src/test/java/mn/edu/must/sqat/

cd "$WORK" || exit 1
SRC=src/main/java/mn/edu/must/sqat/CourseRegistrationService.java
BAK=/tmp/lab8-ai-orig.java
cp "$SRC" "$BAK"

{
  echo "AI (Google Gemini)-гийн тестийн класс Хэсэг B-гийн мутантуудын эсрэг"
  echo "Үүсгэсэн: $(date '+%Y-%m-%d %H:%M')"
  echo "Тестийн класс: ai-test/AiCourseRegistrationServiceTest.java (13 тест)"
  echo ""
} > "$OUT"

run() {
  mvn test > /tmp/lab8-ai-last.txt 2>&1
  {
    echo "=================== $1 ==================="
    grep -o 'Tests run: [0-9]*, Failures: [0-9]*, Errors: [0-9]*, Skipped: [0-9]*' /tmp/lab8-ai-last.txt | tail -1
    grep -o 'BUILD SUCCESS\|BUILD FAILURE' /tmp/lab8-ai-last.txt | tail -1
    grep 'AiCourseRegistrationServiceTest\.register_' /tmp/lab8-ai-last.txt \
      | grep -v '^\[INFO\]' | sed 's/^\[ERROR\]  */  унав: /' | sort -u
    echo ""
  } >> "$OUT"
  printf "%-18s %s\n" "$1" "$(grep -o 'Tests run: [0-9]*, Failures: [0-9]*, Errors: [0-9]*, Skipped: [0-9]*' /tmp/lab8-ai-last.txt | tail -1)"
}

run "0-эвдрэлгүй"

perl -pi -e 's{^(\s*)if \(!student\.active\(\)\) return "ERROR_INACTIVE_STUDENT";}{$1// if (!student.active()) return "ERROR_INACTIVE_STUDENT";}' "$SRC"
run "1-active-хассан"
cp "$BAK" "$SRC"

perl -pi -e 's{^(\s*)repository\.saveRegistration\(studentId, courseId\);}{$1// repository.saveRegistration(studentId, courseId);}' "$SRC"
run "2-save-хассан"
cp "$BAK" "$SRC"

perl -pi -e 's{if \(studentId == null \|\| courseId == null\)}{if (studentId == null)}' "$SRC"
run "3-courseId-null-хассан"
cp "$BAK" "$SRC"

echo ""
echo "Гаралт: results/ai-tests-vs-mutants.txt"
cat "$OUT"
