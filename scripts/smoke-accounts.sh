#!/usr/bin/env bash
# End-to-end check of the account flows against the running stack (docker compose up).
# Uses fresh test addresses on every run, so it can be repeated. Needs only bash and curl.
#
# Usage: scripts/smoke-accounts.sh
set -uo pipefail

API="http://localhost:${BACKEND_PORT:-8080}"
INBOX="http://localhost:${MAILBOX_UI_PORT:-8025}"
STAMP="$(date +%s)"
STUDENT_EMAIL="smoke.student.${STAMP}@uni.example"
PROFESSOR_EMAIL="smoke.prof.${STAMP}@faculty.example"
JSON='Content-Type: application/json'

passed=0
failed=0

# check <name> <expected substring> <actual>
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

# call <method> <path> [token] [json body]  ->  "<http status> <response body>"
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

login() { call POST /api/v1/auth/login "" "{\"email\":\"$1\",\"password\":\"$2\"}"; }

echo "Checking $API"

check "backend is up" '"status":"UP"' "$(curl -s "$API/actuator/health")"

admin_login="$(login 'admin@kaka.example' 'KakaDemo2026')"
check "administrator signs in" '200 ' "$admin_login"
ADMIN_TOKEN="$(field accessToken "$admin_login")"

check "no token is refused with an error body" '401 {"status":401,"code":"UNAUTHENTICATED"' \
  "$(call GET /api/v1/auth/me)"

registered="$(call POST /api/v1/auth/register "" \
  "{\"email\":\"  ${STUDENT_EMAIL} \",\"firstName\":\"Smoke\",\"lastName\":\"Student\",\"password\":\"Passw0rd1\"}")"
check "student registers and is pending" '"status":"PENDING"' "$registered"
check "registration answers 201" '201 ' "$registered"

check "pending student cannot sign in" '403 {"status":403,"code":"ACCOUNT_PENDING"' \
  "$(login "$STUDENT_EMAIL" 'Passw0rd1')"

upper_email="$(printf '%s' "$STUDENT_EMAIL" | tr '[:lower:]' '[:upper:]')"
duplicate="$(call POST /api/v1/auth/register "" \
  "{\"email\":\"${upper_email}\",\"firstName\":\"Smoke\",\"lastName\":\"Student\",\"password\":\"Passw0rd1\"}")"
check "same email in capitals is refused" '"code":"EMAIL_TAKEN"' "$duplicate"

found="$(call GET "/api/v1/admin/students?status=PENDING&q=smoke.student.${STAMP}" "$ADMIN_TOKEN")"
STUDENT_ID="$(field id "$found")"
check "administrator finds the pending student" '"totalItems":1' "$found"

check "administrator approves" '"status":"ACTIVE"' \
  "$(call POST "/api/v1/admin/students/${STUDENT_ID}/approve" "$ADMIN_TOKEN")"
check "approving twice is a conflict" '409 {"status":409,"code":"INVALID_STATUS_TRANSITION"' \
  "$(call POST "/api/v1/admin/students/${STUDENT_ID}/approve" "$ADMIN_TOKEN")"

student_login="$(login "$STUDENT_EMAIL" 'Passw0rd1')"
check "approved student signs in" '"role":"STUDENT"' "$student_login"
STUDENT_TOKEN="$(field accessToken "$student_login")"

check "student is kept out of administrator pages" '403 {"status":403,"code":"ACCESS_DENIED"' \
  "$(call GET /api/v1/admin/students "$STUDENT_TOKEN")"

created="$(call POST /api/v1/admin/professors "$ADMIN_TOKEN" \
  "{\"email\":\"${PROFESSOR_EMAIL}\",\"firstName\":\"Smoke\",\"lastName\":\"Professor\",\"profile\":{\"title\":\"Lecturer\",\"bio\":\"Teaches testing.\",\"expertise\":[\"Testing\",\"Databases\"],\"experience\":[{\"roleTitle\":\"Engineer\",\"organization\":\"Northwind Data\",\"period\":\"2015 to 2020\"}],\"awards\":[{\"title\":\"Teaching Award\",\"year\":2023}]}}")"
check "administrator creates a professor" '201 ' "$created"
PROFESSOR_ID="$(field id "$created")"

profile="$(call GET "/api/v1/professors/${PROFESSOR_ID}" "$STUDENT_TOKEN")"
check "student reads the professor profile" '"expertise":["Testing","Databases"]' "$profile"
if [[ "$profile" == *"$PROFESSOR_EMAIL"* ]]; then
  check "profile page hides the email address" 'no email' 'email found in profile'
else
  check "profile page hides the email address" 'ok' 'ok'
fi

check "student changes their password" '204 ' \
  "$(call PUT /api/v1/auth/password "$STUDENT_TOKEN" '{"currentPassword":"Passw0rd1","newPassword":"NewPassw0rd2"}')"
check "old password no longer works" '401 {"status":401,"code":"BAD_CREDENTIALS"' \
  "$(login "$STUDENT_EMAIL" 'Passw0rd1')"
check "new password works" '200 ' "$(login "$STUDENT_EMAIL" 'NewPassw0rd2')"

check "administrator deactivates the student" '"status":"INACTIVE"' \
  "$(call POST "/api/v1/admin/students/${STUDENT_ID}/deactivate" "$ADMIN_TOKEN")"
check "a deactivated student's token stops working at once" '"code":"SESSION_REVOKED"' \
  "$(call GET /api/v1/auth/me "$STUDENT_TOKEN")"

# Emails are sent in the background; give them a few seconds to arrive.
student_mail=""
professor_mail=""
for _ in 1 2 3 4 5 6 7 8 9 10; do
  student_mail="$(curl -s "$INBOX/api/v1/search?query=to:${STUDENT_EMAIL}")"
  professor_mail="$(curl -s "$INBOX/api/v1/search?query=to:${PROFESSOR_EMAIL}")"
  if [[ "$student_mail" == *"Your account is approved"* && "$professor_mail" == *"Your Kaka Student Management account"* ]]; then
    break
  fi
  sleep 2
done
check "inbox: registration received" 'We received your registration' "$student_mail"
check "inbox: account approved" 'Your account is approved' "$student_mail"
check "inbox: professor account created" 'Your Kaka Student Management account' "$professor_mail"

echo
echo "$passed passed, $failed failed"
[[ "$failed" -eq 0 ]]
