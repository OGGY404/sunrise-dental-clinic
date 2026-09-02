#!/usr/bin/env bash
# Fills the clinic with believable data so the screenshots look like a real day.
BASE=http://localhost:8080
OUT=D:/DevTools/demo
JAR=$OUT/seed-cookies.txt
TODAY=$(date +%Y-%m-%d)
mkdir -p "$OUT"; rm -f "$JAR"

tok() { grep XSRF-TOKEN "$JAR" | awk '{print $7}'; }
jq_() { python -c "import json,sys;d=json.load(open(r'$1'));print(d$2)" 2>/dev/null; }

curl -s -c "$JAR" "$BASE/login" > /dev/null
curl -s -b "$JAR" -c "$JAR" -X POST "$BASE/login" \
     -d "username=reception&password=Recep@123&_csrf=$(tok)" -o /dev/null
curl -s -b "$JAR" -c "$JAR" "$BASE/" -o /dev/null
T=$(tok)

book() { # name address phone dentistId treatmentId time notes
  local body
  body=$(python - "$1" "$2" "$3" "$4" "$5" "$TODAY" "$6" "$7" <<'PY'
import json,sys
n,a,p,d,t,day,time,notes=sys.argv[1:9]
print(json.dumps({"fullName":n,"address":a,"contactNumber":p,
  "dentistId":int(d),"treatmentId":int(t),
  "appointmentDate":day,"appointmentTime":time,"notes":notes}))
PY
)
  curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
       -d "$body" -o "$OUT/b.json" "$BASE/api/appointments"
  jq_ "$OUT/b.json" "['appointmentNo']"
}

complete() { curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -X POST -o /dev/null "$BASE/api/appointments/$1/complete"; }

bill() { # appointmentNo [discount]
  local body="{\"appointmentNo\":\"$1\"}"
  [ -n "$2" ] && body="{\"appointmentNo\":\"$1\",\"discount\":$2}"
  curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
       -d "$body" -o "$OUT/bl.json" "$BASE/api/bills"
  jq_ "$OUT/bl.json" "['billNo']"
}

pay() { curl -s -b "$JAR" -H "X-XSRF-TOKEN: $T" -H "Content-Type: application/json" \
        -d "{\"method\":\"$2\"}" -o /dev/null "$BASE/api/bills/$1/pay"; }

echo "Seeding appointments for $TODAY ..."

A1=$(book "Kamal Silva"      "No. 42, Galle Road, Colombo 03"   "0771234567" 1 3  "09:00" "Filling on the lower left molar")
A2=$(book "Nadeeka Fernando" "No. 7, Temple Road, Nugegoda"     "0715558899" 2 8  "10:00" "Wants advice about braces")
A3=$(book "Kamal Silva"      "No. 42, Galle Road, Colombo 03"   "0771234567" 1 1  "11:00" "Routine six month check-up")
A4=$(book "Ruwan Jayawardena" "No. 118, Main Street, Panadura"  "0723334455" 3 4  "14:00" "Broken tooth, needs extraction")
A5=$(book "Anusha Perera"    "No. 9, Lake Drive, Rajagiriya"    "0761112223" 4 12 "15:30" "Child, first visit, very nervous")
A6=$(book "Saman Kumara"     "No. 55, Station Road, Moratuwa"   "0779998877" 1 2  "16:00" "Scaling and polishing")

echo "  booked: $A1 $A2 $A3 $A4 $A5 $A6"

# Two visits done and paid -> the revenue report has something in it
complete "$A1"; B1=$(bill "$A1" "500.00"); pay "$B1" CASH
complete "$A3"; B2=$(bill "$A3");          pay "$B2" CARD

# Two visits done but NOT paid -> the unpaid chase list has something in it
complete "$A4"; B3=$(bill "$A4")
echo "  billed: $B1 (paid) $B2 (paid) $B3 (unpaid)"

echo
echo "Today's diary:"
curl -s -b "$JAR" -o "$OUT/day.json" "$BASE/api/appointments?date=$TODAY"
python -c "
import json
for a in json.load(open(r'$OUT/day.json')):
    print('   %s  %-18s %-22s %-24s %s' % (a['appointmentTime'][:5], a['appointmentNo'],
          a['patient']['fullName'], a['treatmentName'], a['status']))"
echo
echo "Unpaid bills:"
curl -s -b "$JAR" -o "$OUT/un.json" "$BASE/api/bills/unpaid"
python -c "
import json
for b in json.load(open(r'$OUT/un.json')):
    print('   %s  %-16s Rs. %s' % (b['billNo'], b['patientName'], b['totalAmount']))"
echo
echo "SEED_APPOINTMENT_BOOKED=$A2"
echo "SEED_APPOINTMENT_DONE=$A1"
echo "SEED_BILL_PAID=$B1"
echo "SEED_BILL_UNPAID=$B3"
