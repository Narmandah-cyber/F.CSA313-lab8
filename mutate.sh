#!/bin/bash
# Хэсэг A ба Хэсэг B-г дараалан ажиллуулна.
# Мутаци бүрийн дараа эх кодыг ЗААВАЛ сэргээж, эцэст нь ногоон гаралтыг дахин авна.
#   ./mutate.sh

cd "$(dirname "$0")" || exit 1
export JAVA_HOME=$(/usr/libexec/java_home -v 17)

SRC=src/main/java/mn/edu/must/sqat/CourseRegistrationService.java
BAK=/tmp/CourseRegistrationService.java.orig

mkdir -p results
cp "$SRC" "$BAK"

run() {
  echo ""
  echo "=================== $1 ==================="
  mvn test 2>&1 | tee "results/$1" | tail -8
}

check() {
  if grep -q "$1" "$SRC"; then
    echo "   мутаци хэрэгжсэн"
  else
    echo "   !!! МУТАЦИ ХЭРЭГЖСЭНГҮЙ — гараар шалга: $1"
  fi
}

# --- 0. Суурь: бүх тест ногоон ----------------------------------------
run mvn-test.txt

# --- 1. Идэвхгүй оюутны шалгалтыг комментлох --------------------------
perl -pi -e 's{^(\s*)if \(!student\.active\(\)\) return "ERROR_INACTIVE_STUDENT";}{$1// if (!student.active()) return "ERROR_INACTIVE_STUDENT";}' "$SRC"
check '// if (!student.active())'
run mvn-test-mutant-1.txt
cp "$BAK" "$SRC"

# --- 2. saveRegistration дуудалтыг комментлох -------------------------
perl -pi -e 's{^(\s*)repository\.saveRegistration\(studentId, courseId\);}{$1// repository.saveRegistration(studentId, courseId);}' "$SRC"
check '// repository.saveRegistration'
run mvn-test-mutant-2.txt
cp "$BAK" "$SRC"

# --- 3. courseId-ийн null шалгалтыг хасах -----------------------------
perl -pi -e 's{if \(studentId == null \|\| courseId == null\)}{if (studentId == null)}' "$SRC"
check 'if (studentId == null) {'
run mvn-test-mutant-3.txt
cp "$BAK" "$SRC"

# --- 4. Эх код сэргээгдсэн, ногоон гаралтыг дахин авах ----------------
run mvn-test.txt

echo ""
echo "=================== ДҮН ==================="
for f in results/mvn-test.txt results/mvn-test-mutant-1.txt results/mvn-test-mutant-2.txt results/mvn-test-mutant-3.txt; do
  printf "%-32s %s | %s\n" "$(basename $f)" \
    "$(grep -o 'Tests run: [0-9]*, Failures: [0-9]*, Errors: [0-9]*, Skipped: [0-9]*' $f | tail -1)" \
    "$(grep -o 'BUILD SUCCESS\|BUILD FAILURE' $f | tail -1)"
done
echo ""
echo "Эх код сэргээгдсэн эсэх:"
diff -q "$BAK" "$SRC" && echo "   эх хувилбартай ижил"
