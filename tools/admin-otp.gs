/**
 * Zujaj admin one-time codes (Google Apps Script web app, runs on the owner's Google account).
 *
 * 1. The app sends the admin password + email -> a 6-digit code is emailed to the admin.
 * 2. The app sends the code back -> if right, the script returns the secret Firebase admin key.
 * The Firebase admin key is generated here and never leaves this script except to a
 * verified code, so knowing the password and email alone is not enough to get in.
 *
 * Deploy: Deploy -> New deployment -> Web app -> Execute as: Me, Who has access: Anyone.
 */
const ADMIN_EMAIL = 'hegazi.creator@gmail.com';
const GATE_HASH = '0db2cde54e86a0e70d3095763b58310744658fc5643843638f4e9f3ac74848a3';
const CODE_MINUTES = 10;
const MAX_TRIES = 5;

function sha256_(s) {
  return Utilities.computeDigest(Utilities.DigestAlgorithm.SHA_256, s, Utilities.Charset.UTF_8)
    .map(function (b) { return ('0' + ((b + 256) % 256).toString(16)).slice(-2); }).join('');
}

function secret_() {
  const p = PropertiesService.getScriptProperties();
  let s = p.getProperty('FB_KEY');
  if (!s) { s = Utilities.getUuid() + '-' + Utilities.getUuid(); p.setProperty('FB_KEY', s); }
  return s;
}

function out_(o) {
  return ContentService.createTextOutput(JSON.stringify(o)).setMimeType(ContentService.MimeType.JSON);
}

function doGet() { return out_({ ok: true, service: 'zujaj-admin-otp' }); }

function doPost(e) {
  const lock = LockService.getScriptLock();
  lock.waitLock(10000);
  try {
    const b = JSON.parse((e && e.postData && e.postData.contents) || '{}');
    const p = PropertiesService.getScriptProperties();
    const now = Date.now();
    // lock out for 15 minutes after 10 bad passwords
    const blockedUntil = +(p.getProperty('BLOCK') || 0);
    if (now < blockedUntil) return out_({ ok: false, err: 'blocked' });
    if (sha256_(String(b.pass || '')) !== GATE_HASH || String(b.email || '').trim().toLowerCase() !== ADMIN_EMAIL) {
      const bad = +(p.getProperty('BAD') || 0) + 1;
      p.setProperty('BAD', String(bad));
      if (bad >= 10) { p.setProperty('BLOCK', String(now + 15 * 60000)); p.setProperty('BAD', '0'); }
      Utilities.sleep(1500);
      return out_({ ok: false, err: 'denied' });
    }
    p.setProperty('BAD', '0');
    if (b.action === 'send') {
      const last = +(p.getProperty('LAST') || 0);
      if (now - last < 45000) return out_({ ok: false, err: 'wait' });
      const code = String(Math.floor(100000 + Math.random() * 900000));
      p.setProperties({ CODE: sha256_(code), EXP: String(now + CODE_MINUTES * 60000), TRIES: '0', LAST: String(now) });
      MailApp.sendEmail({
        to: ADMIN_EMAIL,
        subject: 'Zujaj admin code: ' + code,
        htmlBody: '<div style="font-family:sans-serif;text-align:center;padding:24px">' +
          '<h2>Zujaj control panel</h2><p>Your sign-in code:</p>' +
          '<div style="font-size:34px;letter-spacing:8px;font-weight:bold">' + code + '</div>' +
          '<p style="color:#888">Valid for ' + CODE_MINUTES + ' minutes. If this wasn\'t you, change the admin password.</p></div>'
      });
      return out_({ ok: true });
    }
    if (b.action === 'verify') {
      const tries = +(p.getProperty('TRIES') || 0);
      const h = p.getProperty('CODE');
      if (!h || now > +(p.getProperty('EXP') || 0) || tries >= MAX_TRIES) {
        p.deleteProperty('CODE');
        return out_({ ok: false, err: 'expired' });
      }
      if (sha256_(String(b.code || '').trim()) !== h) {
        p.setProperty('TRIES', String(tries + 1));
        return out_({ ok: false, err: 'wrong', left: MAX_TRIES - tries - 1 });
      }
      p.deleteProperty('CODE');
      return out_({ ok: true, key: secret_() });
    }
    return out_({ ok: false, err: 'action' });
  } finally {
    lock.releaseLock();
  }
}
