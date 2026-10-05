import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { ArrowLeft } from 'lucide-react';

type FormErrors = { name?: string; phone?: string; email?: string };

export function StepCustomer() {
  const { state, updateState, setStep } = useSpaBooking();
  const [form, setForm] = useState(state.customer);
  const [errors, setErrors] = useState<FormErrors>({});

  const validate = () => {
    const newErrors: FormErrors = {};
    if (!form.name.trim()) newErrors.name = 'Vui lòng nhập họ và tên';
    const rawPhone = form.phone.trim();
    const normalizedPhone = rawPhone.replace(/[\s.-]/g, '');
    if (!rawPhone) {
      newErrors.phone = 'Vui lòng nhập số điện thoại';
    } else if (!/^0\d{9}$/.test(normalizedPhone)) {
      newErrors.phone = 'Số điện thoại phải gồm đúng 10 chữ số.';
    }

    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Email không hợp lệ';
    }

    setErrors(newErrors);
    return newErrors;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    const newErrors = validate();
    if (Object.keys(newErrors).length > 0) {
      const firstInvalid = (['name', 'phone', 'email'] as const).find((k) => newErrors[k]);
      if (firstInvalid) {
        document.getElementById(`customer-${firstInvalid}`)?.focus();
      }
      return;
    }
    const rawPhone = form.phone.trim();
    const normalizedPhone = rawPhone.replace(/[\s.-]/g, '');
    updateState({ customer: { ...form, phone: normalizedPhone } });
    setStep(5);
  };

  const inputClass = (hasError: boolean) =>
    `w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 focus:ring-stone-800 transition-shadow max-sm:min-h-11 ${
      hasError ? 'border-rose-500' : 'border-stone-300'
    }`;

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => setStep(3)}
          className="mr-3 p-2 -ml-2 max-sm:min-h-11 max-sm:min-w-11 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" aria-hidden="true" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Thông tin của bạn</h2>
      </div>

      <form onSubmit={handleSubmit} noValidate className="max-w-md mx-auto sm:mx-0 space-y-5">
        <div>
          <label htmlFor="customer-name" className="block text-sm font-medium text-stone-700 mb-1.5">
            Họ và tên <span className="text-rose-500" aria-hidden="true">*</span>
          </label>
          <input
            id="customer-name"
            type="text"
            autoComplete="name"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            aria-invalid={errors.name ? true : undefined}
            aria-describedby={errors.name ? 'customer-name-error' : undefined}
            className={inputClass(!!errors.name)}
            placeholder="Ví dụ: Nguyễn Văn A"
          />
          {errors.name && (
            <p id="customer-name-error" className="mt-1.5 text-sm text-rose-500">
              {errors.name}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="customer-phone" className="block text-sm font-medium text-stone-700 mb-1.5">
            Số điện thoại <span className="text-rose-500" aria-hidden="true">*</span>
          </label>
          <input
            id="customer-phone"
            type="tel"
            autoComplete="tel"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            aria-invalid={errors.phone ? true : undefined}
            aria-describedby={errors.phone ? 'customer-phone-error' : undefined}
            className={inputClass(!!errors.phone)}
            placeholder="Ví dụ: 0912345678"
          />
          {errors.phone && (
            <p id="customer-phone-error" className="mt-1.5 text-sm text-rose-500">
              {errors.phone}
            </p>
          )}
        </div>

        <div>
          <label htmlFor="customer-email" className="block text-sm font-medium text-stone-700 mb-1.5">
            Email <span className="text-stone-400 font-normal">(Không bắt buộc)</span>
          </label>
          <input
            id="customer-email"
            type="email"
            autoComplete="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            aria-invalid={errors.email ? true : undefined}
            aria-describedby={errors.email ? 'customer-email-error' : undefined}
            className={inputClass(!!errors.email)}
            placeholder="Ví dụ: email@example.com"
          />
          {errors.email && (
            <p id="customer-email-error" className="mt-1.5 text-sm text-rose-500">
              {errors.email}
            </p>
          )}
        </div>

        <div className="pt-4">
          <button
            type="submit"
            className="w-full sm:w-auto px-8 py-3.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors max-sm:min-h-11"
          >
            Tiếp tục
          </button>
        </div>
      </form>
    </div>
  );
}
