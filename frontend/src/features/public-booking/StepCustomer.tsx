import { useState } from 'react';
import { useSpaBooking } from './SpaBookingContext';
import { ArrowLeft } from 'lucide-react';

export function StepCustomer() {
  const { state, updateState, setStep } = useSpaBooking();
  const [form, setForm] = useState(state.customer);
  const [errors, setErrors] = useState<{ name?: string; phone?: string; email?: string }>({});

  const validate = () => {
    const newErrors: typeof errors = {};
    if (!form.name.trim()) newErrors.name = 'Vui lòng nhập họ và tên';
    if (!form.phone.trim()) {
      newErrors.phone = 'Vui lòng nhập số điện thoại';
    } else if (!/^[0-9]{9,11}$/.test(form.phone.trim())) {
      newErrors.phone = 'Số điện thoại không hợp lệ';
    }
    
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
      newErrors.email = 'Email không hợp lệ';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (validate()) {
      updateState({ customer: form });
      setStep(5);
    }
  };

  return (
    <div>
      <div className="flex items-center mb-6">
        <button
          onClick={() => setStep(3)}
          className="mr-3 p-2 -ml-2 rounded-full text-stone-400 hover:text-stone-700 hover:bg-stone-100 transition-colors"
          aria-label="Quay lại"
        >
          <ArrowLeft className="w-5 h-5" />
        </button>
        <h2 className="text-xl sm:text-2xl font-medium text-stone-900">Thông tin của bạn</h2>
      </div>

      <form onSubmit={handleSubmit} className="max-w-md mx-auto sm:mx-0 space-y-5">
        <div>
          <label htmlFor="customerName" className="block text-sm font-medium text-stone-700 mb-1.5">
            Họ và tên <span className="text-rose-500">*</span>
          </label>
          <input
            id="customerName"
            type="text"
            value={form.name}
            onChange={(e) => setForm({ ...form, name: e.target.value })}
            className={`w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 focus:ring-stone-800 transition-shadow ${
              errors.name ? 'border-rose-500' : 'border-stone-300'
            }`}
            placeholder="Ví dụ: Nguyễn Văn A"
          />
          {errors.name && <p className="mt-1.5 text-sm text-rose-500">{errors.name}</p>}
        </div>

        <div>
          <label htmlFor="customerPhone" className="block text-sm font-medium text-stone-700 mb-1.5">
            Số điện thoại <span className="text-rose-500">*</span>
          </label>
          <input
            id="customerPhone"
            type="tel"
            value={form.phone}
            onChange={(e) => setForm({ ...form, phone: e.target.value })}
            className={`w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 focus:ring-stone-800 transition-shadow ${
              errors.phone ? 'border-rose-500' : 'border-stone-300'
            }`}
            placeholder="Ví dụ: 0912345678"
          />
          {errors.phone && <p className="mt-1.5 text-sm text-rose-500">{errors.phone}</p>}
        </div>

        <div>
          <label htmlFor="customerEmail" className="block text-sm font-medium text-stone-700 mb-1.5">
            Email <span className="text-stone-400 font-normal">(Không bắt buộc)</span>
          </label>
          <input
            id="customerEmail"
            type="email"
            value={form.email}
            onChange={(e) => setForm({ ...form, email: e.target.value })}
            className={`w-full px-4 py-3 rounded-xl border focus:outline-none focus:ring-2 focus:ring-stone-800 transition-shadow ${
              errors.email ? 'border-rose-500' : 'border-stone-300'
            }`}
            placeholder="Ví dụ: email@example.com"
          />
          {errors.email && <p className="mt-1.5 text-sm text-rose-500">{errors.email}</p>}
        </div>

        <div className="pt-4">
          <button
            type="submit"
            className="w-full sm:w-auto px-8 py-3.5 bg-stone-900 text-white font-medium rounded-xl hover:bg-stone-800 transition-colors"
          >
            Tiếp tục
          </button>
        </div>
      </form>
    </div>
  );
}
