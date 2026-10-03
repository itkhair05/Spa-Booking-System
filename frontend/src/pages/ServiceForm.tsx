import { useState } from 'react';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { Input } from '../components/ui/Input';
import { createService, updateService } from '../lib/api/services';
import type { Service, CreateServiceRequest, UpdateServiceRequest } from '../types/service';

interface ServiceFormProps {
  service?: Service; // if undefined, it's create mode
  onSuccess: () => void;
  onCancel: () => void;
}

export const ServiceForm = ({ service, onSuccess, onCancel }: ServiceFormProps) => {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [name, setName] = useState<string>(service ? service.name : '');
  const [description, setDescription] = useState<string>(service ? (service.description || '') : '');
  const [durationMinutes, setDurationMinutes] = useState<string>(service ? String(service.durationMinutes) : '60');
  const [price, setPrice] = useState<string>(service ? String(service.price) : '0');
  const [isActive, setIsActive] = useState<boolean>(service ? service.isActive : true);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name || !durationMinutes || !price) {
      setError('Name, duration, and price are required.');
      return;
    }

    const duration = parseInt(durationMinutes, 10);
    const priceVal = parseFloat(price);

    if (isNaN(duration) || duration < 1) {
      setError('Duration must be at least 1 minute.');
      return;
    }
    
    if (isNaN(priceVal) || priceVal < 0) {
      setError('Price cannot be negative.');
      return;
    }

    setIsSubmitting(true);
    setError(null);
    try {
      if (service) {
        const data: UpdateServiceRequest = {
          name,
          description: description || undefined,
          durationMinutes: duration,
          price: priceVal,
          isActive,
        };
        await updateService(service.id, data);
      } else {
        const data: CreateServiceRequest = {
          name,
          description: description || undefined,
          durationMinutes: duration,
          price: priceVal,
          isActive,
        };
        await createService(data);
      }
      onSuccess();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'An error occurred while saving the service.');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <Card>
      <CardContent className="p-6">
        <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-4">
          {service ? 'Edit Service' : 'New Service'}
        </h3>
        
        {error && (
          <div className="mb-4 p-3 rounded-lg bg-[var(--color-error-bg)] border border-[var(--color-error-border)] text-[var(--color-error)] text-sm" role="alert">
            {error}
          </div>
        )}

        <form onSubmit={handleSubmit} className="flex flex-col gap-4">
          <Input 
            type="text" 
            label="Name" 
            value={name}
            onChange={(e) => setName(e.target.value)}
            required
            placeholder="e.g. Classic Massage"
          />
          <Input 
            type="text" 
            label="Description" 
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Optional description"
          />
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <Input 
              type="number" 
              label="Duration (minutes)" 
              value={durationMinutes}
              onChange={(e) => setDurationMinutes(e.target.value)}
              required
              min="1"
            />
            <Input 
              type="number" 
              label="Price (VND)" 
              value={price}
              onChange={(e) => setPrice(e.target.value)}
              required
              min="0"
              step="1000"
            />
          </div>
          
          <label className="flex items-center gap-2 text-sm font-semibold text-[var(--color-neutral-700)] mt-2 cursor-pointer">
            <input 
              type="checkbox" 
              checked={isActive}
              onChange={(e) => setIsActive(e.target.checked)}
              className="w-4 h-4 rounded border-[var(--color-neutral-300)] text-[var(--color-brand-600)] focus:ring-[var(--color-brand-500)]"
            />
            Active Service
          </label>

          <div className="flex justify-end gap-3 mt-4">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isSubmitting}>
              Cancel
            </Button>
            <Button type="submit" disabled={isSubmitting}>
              {isSubmitting ? 'Saving...' : (service ? 'Save Changes' : 'Create Service')}
            </Button>
          </div>
        </form>
      </CardContent>
    </Card>
  );
};
