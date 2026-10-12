import { describe, it } from 'node:test';
import assert from 'node:assert/strict';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function validateForgotPasswordEmail(raw: string): { valid: boolean; error?: string; normalized?: string } {
  const trimmed = raw.trim();
  if (!trimmed) {
    return { valid: false, error: 'Vui lòng nhập địa chỉ email.' };
  }
  if (!EMAIL_REGEX.test(trimmed)) {
    return { valid: false, error: 'Địa chỉ email không đúng định dạng.' };
  }
  return { valid: true, normalized: trimmed.toLowerCase() };
}

function validateResetPasswordForm(password: string, confirm: string): { valid: boolean; error?: string } {
  if (!password || password.length < 6) {
    return { valid: false, error: 'Mật khẩu mới phải có ít nhất 6 ký tự.' };
  }
  if (password !== confirm) {
    return { valid: false, error: 'Xác nhận mật khẩu không khớp với mật khẩu mới.' };
  }
  return { valid: true };
}

describe('Forgot Password Form Validation', () => {
  it('rejects blank and whitespace-only email', () => {
    assert.equal(validateForgotPasswordEmail('').valid, false);
    assert.equal(validateForgotPasswordEmail('   ').valid, false);
    assert.equal(validateForgotPasswordEmail('').error, 'Vui lòng nhập địa chỉ email.');
  });

  it('rejects invalid email formats', () => {
    assert.equal(validateForgotPasswordEmail('plainaddress').valid, false);
    assert.equal(validateForgotPasswordEmail('@missinguser.com').valid, false);
    assert.equal(validateForgotPasswordEmail('user@.com').valid, false);
    assert.equal(validateForgotPasswordEmail('user@domain').valid, false);
  });

  it('accepts valid email and normalizes', () => {
    const res = validateForgotPasswordEmail('  Owner@Tikey.VN  ');
    assert.equal(res.valid, true);
    assert.equal(res.normalized, 'owner@tikey.vn');
  });
});

describe('Reset Password Form Validation', () => {
  it('rejects passwords shorter than 6 characters', () => {
    assert.equal(validateResetPasswordForm('', '').valid, false);
    assert.equal(validateResetPasswordForm('12345', '12345').valid, false);
    assert.equal(validateResetPasswordForm('12345', '12345').error, 'Mật khẩu mới phải có ít nhất 6 ký tự.');
  });

  it('rejects mismatched password confirmation', () => {
    const res = validateResetPasswordForm('ValidPassword123', 'DifferentPassword456');
    assert.equal(res.valid, false);
    assert.equal(res.error, 'Xác nhận mật khẩu không khớp với mật khẩu mới.');
  });

  it('accepts matching password with 6 or more characters', () => {
    const res = validateResetPasswordForm('NewSecretPassword99', 'NewSecretPassword99');
    assert.equal(res.valid, true);
    assert.equal(res.error, undefined);
  });
});
