/*
 * Captures every screenshot the CIS6003 report needs, from the running
 * application, using the copy of Chrome already installed on this machine.
 *
 *   node capture.mjs
 *
 * The application must be running on localhost:8080 and MySQL must be up.
 */
import puppeteer from 'puppeteer-core';
import { mkdirSync } from 'fs';

const CHROME = 'C:/Program Files/Google/Chrome/Application/chrome.exe';
const BASE = 'http://localhost:8080';
const OUT = 'D:/ICBT/Advance programming/02_SunriseDentalClinic_Resit/SunriseDentalClinic/docs/report/screenshots';
const REPO = 'https://github.com/OGGY404/sunrise-dental-clinic';

const BOOKED = 'APT-20260902-0002';   // still BOOKED, so the action buttons show
const DONE = 'APT-20260902-0001';     // COMPLETED and billed
const PAID_BILL = 'BIL-20260902-0002';
const PATIENT = 'PAT-000001';         // Kamal Silva, several visits

mkdirSync(OUT, { recursive: true });

const shot = async (page, name, opts = {}) => {
  await new Promise(r => setTimeout(r, 350));       // let fonts settle
  await page.screenshot({ path: `${OUT}/${name}.png`, fullPage: true, ...opts });
  console.log('  captured', name);
};

const signIn = async (context, username, password) => {
  const page = await context.newPage();
  await page.setViewport({ width: 1440, height: 900, deviceScaleFactor: 2 });
  await page.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await page.type('#username', username);
  await page.type('#password', password);
  await Promise.all([
    page.waitForNavigation({ waitUntil: 'networkidle2' }),
    page.click('form[action*="/login"] button[type=submit]'),
  ]);
  return page;
};

const browser = await puppeteer.launch({
  executablePath: CHROME,
  headless: 'new',
  args: ['--hide-scrollbars', '--force-device-scale-factor=2'],
});

try {
  // ---------------------------------------------------------------- signed out
  console.log('signed out:');
  const anon = await browser.createBrowserContext();
  const p0 = await anon.newPage();
  await p0.setViewport({ width: 1440, height: 900, deviceScaleFactor: 2 });
  await p0.goto(`${BASE}/login`, { waitUntil: 'networkidle2' });
  await shot(p0, '01-login');
  await anon.close();

  // ------------------------------------------------------------- receptionist
  console.log('as the receptionist:');
  const rcx = await browser.createBrowserContext();
  const p = await signIn(rcx, 'reception', 'Recep@123');

  await shot(p, '02-dashboard-menu');

  await p.goto(`${BASE}/appointments/new`, { waitUntil: 'networkidle2' });
  await shot(p, '03-booking-form');

  // Fill it in badly on purpose, submit, and capture the rejected form.
  await p.type('#fullName', 'Kamal 123');
  await p.type('#address', 'No. 42, Galle Road');
  await p.type('#contactNumber', '077');
  await p.select('#treatmentId', '3');
  // NOTE: the selector must be scoped to the booking form. The navigation bar
  // contains a "Sign out" form whose button is the FIRST submit button on the
  // page, so a bare button[type=submit] signs the receptionist out instead.
  await Promise.all([
    p.waitForNavigation({ waitUntil: 'networkidle2' }),
    p.click('main form[action*="/appointments/new"] button[type=submit]'),
  ]);
  await shot(p, '04-booking-form-rejected');

  await p.goto(`${BASE}/appointments/${BOOKED}`, { waitUntil: 'networkidle2' });
  await shot(p, '05-appointment-details-booked');

  await p.goto(`${BASE}/appointments/${DONE}`, { waitUntil: 'networkidle2' });
  await shot(p, '06-appointment-details-completed');

  await p.goto(`${BASE}/appointments/search`, { waitUntil: 'networkidle2' });
  await shot(p, '07-find-appointment');

  await p.goto(`${BASE}/appointments/schedule`, { waitUntil: 'networkidle2' });
  await shot(p, '08-day-schedule');

  await p.goto(`${BASE}/patients?name=Kamal`, { waitUntil: 'networkidle2' });
  await shot(p, '09-patient-search');

  await p.goto(`${BASE}/patients/${PATIENT}`, { waitUntil: 'networkidle2' });
  await shot(p, '10-patient-treatment-history');

  await p.goto(`${BASE}/bills/${PAID_BILL}`, { waitUntil: 'networkidle2' });
  await shot(p, '11-bill-receipt');

  // Exactly what the printer would produce: the print stylesheet removes the
  // navigation, the buttons and the page background.
  await p.emulateMediaType('print');
  await shot(p, '12-bill-print-preview');
  await p.emulateMediaType('screen');

  await p.goto(`${BASE}/bills/unpaid`, { waitUntil: 'networkidle2' });
  await shot(p, '13-unpaid-bills');

  await p.goto(`${BASE}/appointments/reminders`, { waitUntil: 'networkidle2' });
  await shot(p, '27-appointment-reminders');

  await p.goto(`${BASE}/help`, { waitUntil: 'networkidle2' });
  await shot(p, '14-help-section');

  await p.goto(`${BASE}/appointments/APT-DOES-NOT-EXIST`, { waitUntil: 'networkidle2' });
  await shot(p, '15-error-page');

  await p.goto(`${BASE}/reports`, { waitUntil: 'networkidle2' });
  await shot(p, '16-receptionist-refused-reports');

  await rcx.close();

  // -------------------------------------------------------------- administrator
  console.log('as the administrator:');
  const acx = await browser.createBrowserContext();
  const a = await signIn(acx, 'admin', 'Admin@123');

  await a.goto(`${BASE}/reports`, { waitUntil: 'networkidle2' });
  await shot(a, '17-reports-menu');

  await a.goto(`${BASE}/reports/revenue`, { waitUntil: 'networkidle2' });
  await shot(a, '18-revenue-by-treatment');

  await a.goto(`${BASE}/reports/workload`, { waitUntil: 'networkidle2' });
  await shot(a, '19-dentist-workload');

  await acx.close();

  // ------------------------------------------------------------------ evidence
  console.log('evidence pages:');
  const ecx = await browser.createBrowserContext();
  const e = await ecx.newPage();
  await e.setViewport({ width: 1440, height: 1000, deviceScaleFactor: 2 });

  await e.goto('file:///D:/ICBT/Advance%20programming/02_SunriseDentalClinic_Resit/SunriseDentalClinic/target/site/jacoco/index.html',
               { waitUntil: 'networkidle2' });
  await shot(e, '20-jacoco-coverage');

  await e.goto(`${REPO}/actions`, { waitUntil: 'networkidle2' });
  await shot(e, '21-github-actions', { fullPage: false });

  await e.goto(`${REPO}/releases`, { waitUntil: 'networkidle2' });
  await shot(e, '22-github-release', { fullPage: false });

  await e.goto(`${REPO}/commits/develop`, { waitUntil: 'networkidle2' });
  await shot(e, '23-github-commit-history', { fullPage: false });

  await ecx.close();
} finally {
  await browser.close();
}

console.log('\nAll screenshots written to');
console.log(OUT);
