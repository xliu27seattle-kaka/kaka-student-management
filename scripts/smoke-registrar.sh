#!/usr/bin/env bash
# End-to-end check of the registrar flow against the running stack (docker compose up):
# catalogue with prerequisites, a semester through its whole life, rule-checked enrollment,
# grading, transcript, notifications and graduation status.
# Every run uses new course codes, a new semester and a new student, so it can be repeated.
#
# Usage: scripts/smoke-registrar.sh
set -uo pipefail

API="http://localhost:${BACKEND_PORT:-8080}"
STAMP="$(date +%s)"
N=$((RANDOM % 900 + 100))
YEAR=$((3000 + RANDOM % 7000))
LETTERS=$(LC_ALL=C tr -dc 'A-Z' </dev/urandom | head -c 2)
A="Z${LETTERS}${N}"
B="Y${LETTERS}${N}"
STUDENT_EMAIL="smoke.registrar.${STAMP}@uni.example"
JSON='Content-Type: application/json'

passed=0
failed=0

check() {
  if [[ "$3" == *"$2"* ]]; then
    echo "PASS  $1"
    passed=$((passed + 1))
  else
    echo "FAIL  $1"
    echo "      expected to contain: $2"
    echo "      got: ${3:0:300}"
    failed=$((failed + 1))
  fi
}

call() {
  local method="$1" path="$2" token="${3:-}" body="${4:-}"
  local args=(-s -w '\n%{http_code}' -X "$method" "$API$path" -H "$JSON")
  [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
  [[ -n "$body" ]] && args+=(-d "$body")
  local response
  response="$(curl "${args[@]}")"
  echo "${response##*$'\n'} ${response%$'\n'*}"
}

field() { sed -n "s/.*\"$1\":\"\\{0,1\\}\\([^\",}]*\\).*/\\1/p" <<<"$2" | head -1; }
first() { grep -o "\"$1\":\"\\{0,1\\}[^\",}]*" <<<"$2" | head -1 | sed "s/\"$1\":\"\\{0,1\\}//"; }
login() { call POST /api/v1/auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}"; }

echo "Checking $API with courses $A, $B and semester ${YEAR}SP"

r="$(login admin@kaka.example KakaDemo2026)"
ADMIN="$(field accessToken "$r")"
r="$(login amara.okafor@faculty.example KakaDemo2026)"
PROF="$(field accessToken "$r")"
r="$(call GET '/api/v1/admin/professors?q=okafor' "$ADMIN")"
PROF_ID="$(first id "$r")"
check "found the demo professor" '200 ' "$r"

call POST /api/v1/auth/register "" \
  "{\"email\":\"$STUDENT_EMAIL\",\"firstName\":\"Rita\",\"lastName\":\"Registrar\",\"password\":\"Passw0rd1\"}" >/dev/null
r="$(call GET "/api/v1/admin/students?q=$STUDENT_EMAIL" "$ADMIN")"
STUDENT_ID="$(first id "$r")"
call POST "/api/v1/admin/students/$STUDENT_ID/approve" "$ADMIN" >/dev/null
r="$(login "$STUDENT_EMAIL" Passw0rd1)"
STUDENT="$(field accessToken "$r")"
check "new student is approved and signs in" '200 ' "$r"

# ---- catalogue ----
r="$(call POST /api/v1/admin/courses "$ADMIN" "{\"code\":\"$A\",\"title\":\"Smoke Basics\",\"department\":\"ZQ\",\"credits\":3,\"active\":true,\"prerequisiteIds\":[]}")"
check "course $A is created" '201 ' "$r"
A_ID="$(first id "$r")"
r="$(call POST /api/v1/admin/courses "$ADMIN" "{\"code\":\"$B\",\"title\":\"Smoke Advanced\",\"department\":\"ZQ\",\"credits\":3,\"active\":true,\"prerequisiteIds\":[$A_ID]}")"
check "course $B requires $A" "\"code\":\"$A\"" "$r"
B_ID="$(first id "$r")"
r="$(call PUT "/api/v1/admin/courses/$A_ID" "$ADMIN" "{\"code\":\"$A\",\"title\":\"Smoke Basics\",\"department\":\"ZQ\",\"credits\":3,\"active\":true,\"prerequisiteIds\":[$B_ID]}")"
check "a prerequisite cycle is refused with its path" "$A → $B → $A" "$r"
check "the cycle answer is 422" '422 ' "$r"
check "a student cannot change the catalogue" '403 ' \
  "$(call POST /api/v1/admin/courses "$STUDENT" '{}')"

# ---- semester and sections ----
r="$(call POST /api/v1/admin/semesters "$ADMIN" "{\"code\":\"${YEAR}SP\",\"name\":\"Smoke Spring\",\"startDate\":\"${YEAR}-01-10\",\"endDate\":\"${YEAR}-05-10\"}")"
check "semester starts as a draft" '"status":"DRAFT"' "$r"
SEM="$(first id "$r")"
r="$(call POST "/api/v1/admin/semesters/$SEM/sections" "$ADMIN" "{\"courseId\":$A_ID,\"professorId\":$PROF_ID,\"room\":\"Hall 1\",\"meetings\":[{\"day\":\"MONDAY\",\"start\":\"10:00\",\"end\":\"11:15\"}]}")"
check "section $A-A is created with 20 seats" '"capacity":20' "$r"
SEC_A="$(first id "$r")"
# (assigned first: bash 3.2 mangles escaped quotes inside a nested command substitution)
r="$(call POST "/api/v1/admin/semesters/$SEM/sections" "$ADMIN" "{\"courseId\":$B_ID,\"professorId\":$PROF_ID,\"meetings\":[{\"day\":\"MONDAY\",\"start\":\"11:20\",\"end\":\"12:00\"}]}")"
check "a professor clash under 10 minutes is refused" 'PROFESSOR_SCHEDULE_CONFLICT' "$r"
r="$(call POST "/api/v1/admin/semesters/$SEM/sections" "$ADMIN" "{\"courseId\":$B_ID,\"professorId\":$PROF_ID,\"meetings\":[{\"day\":\"MONDAY\",\"start\":\"11:25\",\"end\":\"12:00\"}]}")"
check "exactly 10 minutes later is fine" '201 ' "$r"
check "a draft cannot jump to started" 'SEMESTER_TRANSITION_NOT_ALLOWED' \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"STARTED"}')"
check "the semester opens for enrollment" '"status":"READY"' \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"READY"}')"

# ---- enrollment ----
r="$(call GET "/api/v1/student/semesters/$SEM/offerings" "$STUDENT")"
check "offerings say why $B is not available" "Requires $A" "$r"
r="$(call POST /api/v1/student/enrollments "$STUDENT" "{\"sectionId\":$SEC_A}")"
check "student enrolls in $A-A" '"status":"ENROLLED"' "$r"
r="$(call POST /api/v1/student/enrollments "$STUDENT" "{\"sectionId\":$SEC_A}")"
check "enrolling twice is refused" 'Already enrolled in this section' "$r"
check "going back to draft is refused while someone is enrolled" 'SEMESTER_HAS_ENROLLMENTS' \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"DRAFT"}')"
check "the semester starts" '"status":"STARTED"' \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"STARTED"}')"
check "it cannot end while a grade is missing" "$A-A" \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"ENDED"}')"

# ---- teaching, grading, notifications ----
r="$(call GET "/api/v1/professor/sections/$SEC_A" "$PROF")"
check "the professor sees the class list" "$STUDENT_EMAIL" "$r"
ENR="$(first enrollmentId "$r")"
r="$(call PUT "/api/v1/professor/sections/$SEC_A/grades" "$PROF" "{\"grades\":[{\"enrollmentId\":$ENR,\"score\":91}]}")"
check "a score of 91 is an A-" '"letterGrade":"A-"' "$r"
r="$(call POST "/api/v1/professor/sections/$SEC_A/notifications" "$PROF" '{"subject":"Smoke quiz","body":"The quiz moves to Friday."}')"
check "the professor notifies the section" '"recipientCount":1' "$r"
check "the student has an unread notification" '"unread":' \
  "$(call GET /api/v1/notifications/unread-count "$STUDENT")"
check "the student sees it in the inbox" 'Smoke quiz' "$(call GET /api/v1/notifications "$STUDENT")"
check "the semester ends" '"status":"ENDED"' \
  "$(call POST "/api/v1/admin/semesters/$SEM/transition" "$ADMIN" '{"target":"ENDED"}')"

# ---- transcript and graduation ----
r="$(call GET /api/v1/student/transcript "$STUDENT")"
check "the transcript has a 3.70 GPA" '"cumulativeGpa":3.70' "$r"
check "the transcript counts 3 earned credits" '"earnedCredits":3' "$r"
check "graduation shows what is missing" '27 more credits needed' \
  "$(call GET /api/v1/student/graduation "$STUDENT")"
check "an administrator reads the same transcript" '"cumulativeGpa":3.70' \
  "$(call GET "/api/v1/admin/students/$STUDENT_ID/transcript" "$ADMIN")"
check "a course with sections cannot be deleted" 'COURSE_IN_USE' \
  "$(call DELETE "/api/v1/admin/courses/$A_ID" "$ADMIN")"
check "a student with history cannot be deleted" 'HAS_HISTORY' \
  "$(call DELETE "/api/v1/admin/students/$STUDENT_ID" "$ADMIN")"
check "the dashboard answers" '"activeCourses":' "$(call GET /api/v1/admin/dashboard "$ADMIN")"

echo
echo "$passed passed, $failed failed"
[[ $failed -eq 0 ]]
