#!/usr/bin/env bash
# End to end walk through of the Sunrise Dental Clinic web service.
BASE=http://localhost:8080
OUT=D:/DevTools/demo
JAR=$OUT/cookies.txt
mkdir -p "$OUT"; rm -f "$JAR"

tok() { grep XSRF-TOKEN "$JAR" | awk '{print $7}'; }
say() { echo; echo "----- $1"; }
py()  { python -c "$1"; }

say "1. Not signed in: the web service refuses"
curl -s -o "$OUT/r.json" -w "HTTP %{http_code}  " "$BASE/api/treatments"; cat "$OUT/r.json"; echo

say "2. Not signed in: a browser page is sent to the login screen"
curl -s -o /dev/null -w "HTTP %{http_code} -> %{redirect_url}\n" "$BASE/appointments"

say "3. Sign in as the receptionist"
curl -s -c "$JAR" "$BASE/login" > /dev/null
curl -s -b "$JAR" -c "$JAR" -X POST "$BASE/login" \
     -d "username=reception&password=Recep@123&_csrf=$(tok)" \
     -o /dev/null -w "HTTP %{http_code} -> %{redirect_url}\n"
# Follow the redirect, as a browser does. That request hands over the new CSRF
# token, because signing in threw the old one away.
curl -s -b "$JAR" -c "$JAR" "$BASE/" -o /dev/null
T=$(tok)
echo "   csrf token now held: ${T:0:8}..."

say "4. The dropdown lists the booking form needs"
curl -s -b "$JAR" -o "$OUT/den.json" "$BASE/api/dentists"
py "import json;[print('  ',d['dentistId'],d['dentistCode'],d['fullName'],'|',d['specialisation']) for d in json.load(open(r'$OUT/den.json'))]"
curl -s -b "$JAR" -o "$OUT/trt.json" "$BASE/api/treatments"
py "import json;[print('  ',t['treatmentId'],t['treatmentCode'],t['name'],'Rs.',t['cost']) for t in json.load(open(r'$OUT/trt.json'))][:3]"

say "5. FR2 register an appointment"
BOOK='{"fullName":"Kamal Silva","address":"No. 42, Galle Road, Colombo 03","contactNumber":"0771234567","email":"kamal@example.lk","dateOfBirth":"1995-04-17","gender":"MALE","dentistId":3,"treatmentId":5,"appointmentDate":"2026-09-15","appointmentTime":"09:00:00","notes":"Pain in lower left molar"}'
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "$BOOK" -o "$OUT/apt.json" -w "HTTP %{http_code}   Location: %header{location}\n" "$BASE/api/appointments"
py "import json;a=json.load(open(r'$OUT/apt.json'));print('   number  :',a['appointmentNo']);print('   patient :',a['patient']['patientCode'],a['patient']['fullName'],'age',a['patient']['age']);print('   dentist :',a['dentistName']);print('   work    :',a['treatmentName'],'Rs.',a['treatmentCost']);print('   when    :',a['appointmentDate'],a['appointmentTime'],'status',a['status'])"
APT=$(py "import json;print(json.load(open(r'$OUT/apt.json'))['appointmentNo'])")

say "6. Validation refuses a bad form (digits in the name, short phone)"
BAD='{"fullName":"Kamal 123","address":"x","contactNumber":"077","dentistId":3,"treatmentId":5,"appointmentDate":"2026-09-15","appointmentTime":"09:30:00"}'
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "$BAD" -o "$OUT/bad.json" -w "HTTP %{http_code}\n" "$BASE/api/appointments"
py "import json;[print('   ',k,':',v) for k,v in json.load(open(r'$OUT/bad.json'))['fieldErrors'].items()]"

say "7. Double booking the same dentist and slot is refused"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "$BOOK" -o "$OUT/dup.json" -w "HTTP %{http_code}   " "$BASE/api/appointments"
py "import json;print(json.load(open(r'$OUT/dup.json'))['message'])"

say "8. FR3 look the appointment up by its number"
curl -s -b "$JAR" -o "$OUT/one.json" -w "HTTP %{http_code}\n" "$BASE/api/appointments/$APT"
py "import json;a=json.load(open(r'$OUT/one.json'));print('   ',a['appointmentNo'],'|',a['patient']['fullName'],a['patient']['contactNumber'],'|',a['dentistName'],'|',a['treatmentName'])"

say "9. Billing is refused before the visit has happened"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "{\"appointmentNo\":\"$APT\"}" -o "$OUT/early.json" -w "HTTP %{http_code}   " "$BASE/api/bills"
py "import json;print(json.load(open(r'$OUT/early.json'))['message'])"

say "10. Mark the visit as completed"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -X POST -o "$OUT/done.json" -w "HTTP %{http_code}   " "$BASE/api/appointments/$APT/complete"
py "import json;print('status now',json.load(open(r'$OUT/done.json'))['status'])"

say "11. FR4 produce the bill (Surgical rule + Rs.500 agreed discount)"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "{\"appointmentNo\":\"$APT\",\"discount\":500.00}" -o "$OUT/bill.json" -w "HTTP %{http_code}\n" "$BASE/api/bills"
py "import json;b=json.load(open(r'$OUT/bill.json'));print('   bill no      :',b['billNo']);print('   patient      :',b['patientName']);print('   treatment    : Rs.',b['treatmentCost']);print('   consultation : Rs.',b['consultationFee']);print('   discount     : Rs.',b['discount']);print('   TOTAL        : Rs.',b['totalAmount'],'(',b['paymentStatus'],')')"
BILL=$(py "import json;print(json.load(open(r'$OUT/bill.json'))['billNo'])")

say "12. One visit, one bill: a second bill is refused"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d "{\"appointmentNo\":\"$APT\"}" -o "$OUT/twice.json" -w "HTTP %{http_code}   " "$BASE/api/bills"
py "import json;print(json.load(open(r'$OUT/twice.json'))['message'])"

say "13. Record the payment"
curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
     -d '{"method":"CASH"}' -o "$OUT/paid.json" -w "HTTP %{http_code}   " "$BASE/api/bills/$BILL/pay"
py "import json;b=json.load(open(r'$OUT/paid.json'));print(b['billNo'],b['paymentStatus'],'by',b['paymentMethod'],'at',b['paidAt'])"

say "14. FR7 the treatment history for this patient"
PAT=$(py "import json;print(json.load(open(r'$OUT/apt.json'))['patient']['patientCode'])")
curl -s -b "$JAR" -o "$OUT/hist.json" "$BASE/api/patients/$PAT/history"
py "import json;[print('  ',a['appointmentDate'],a['appointmentTime'],a['treatmentName'],a['status']) for a in json.load(open(r'$OUT/hist.json'))]"

say "15. The day schedule report"
curl -s -b "$JAR" -o "$OUT/day.json" "$BASE/api/appointments?date=2026-09-15"
py "import json;[print('  ',a['appointmentTime'],a['appointmentNo'],a['patient']['fullName'],'->',a['dentistName']) for a in json.load(open(r'$OUT/day.json'))]"

say "16. A receptionist is refused the admin only area"
curl -s -b "$JAR" -o "$OUT/f.json" -w "HTTP %{http_code}   " "$BASE/api/admin/settings"
py "import json;print(json.load(open(r'$OUT/f.json'))['message'])"

say "17. A write with no CSRF token is refused, even though signed in"
curl -s -b "$JAR" -H "Content-Type: application/json" -d "$BOOK" \
     -o /dev/null -w "HTTP %{http_code}\n" "$BASE/api/appointments"

say "18. Sign out"
curl -s -b "$JAR" -c "$JAR" -X POST "$BASE/logout" -d "_csrf=$T" -o /dev/null -w "HTTP %{http_code} -> %{redirect_url}\n"
curl -s -o /dev/null -b "$JAR" -w "   web service after sign out: HTTP %{http_code}\n" "$BASE/api/treatments"
