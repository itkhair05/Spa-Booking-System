import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getServices, deleteService } from '../lib/api/services';
import type { Service } from '../types/service';
import { ServiceForm } from './ServiceForm';
import { AlertCircle, Scissors, Trash2 } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const formatVND = (amount: number) => {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
};

const Services = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [services, setServices] = useState<Service[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingService, setEditingService] = useState<Service | undefined>(undefined);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchServices = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getServices();
      setServices(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Failed to load services.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchServices();
  }, [fetchServices]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && deletingId !== null) {
        setDeletingId(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [deletingId]);

  const handleOpenForm = (service?: Service) => {
    if (!isOwner) return;
    setEditingService(service);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingService(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchServices();
  };

  const handleDeleteClick = (id: number) => {
    if (!isOwner) return;
    setDeletingId(id);
  };

  const confirmDelete = async () => {
    if (!deletingId) return;
    try {
      await deleteService(deletingId);
      setDeletingId(null);
      fetchServices();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      alert(errorObj.response?.data?.message || 'Failed to delete service. It may be in use.');
      setDeletingId(null);
    }
  };

  if (isFormOpen && isOwner) {
    return (
      <AppShell title="Services">
        <PageHeader title="Service Management" />
        <div className="max-w-2xl mx-auto">
          <ServiceForm 
            service={editingService}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Services">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader title="Service Management" description="Manage the spa treatments and services you offer." />
        {isOwner && (
          <Button onClick={() => handleOpenForm()}>New Service</Button>
        )}
      </div>

      {isLoading && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 animate-pulse" aria-busy="true">
          {[1, 2, 3].map((i) => (
            <Card key={i}>
              <CardContent className="h-32 bg-[var(--color-neutral-100)] rounded-xl">
                <div />
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {error && !isLoading && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)]" role="alert">
          <AlertCircle className="text-[var(--color-error)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Unable to load services</h3>
          <p className="text-[var(--color-neutral-500)] mb-6 text-center max-w-md">{error}</p>
          <Button onClick={fetchServices}>Try Again</Button>
        </div>
      )}

      {!isLoading && !error && services.length === 0 && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)] text-center">
          <Scissors className="text-[var(--color-neutral-400)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">No services yet</h3>
          <p className="text-[var(--color-neutral-500)] mb-6">
            {isOwner ? "Add your first service to get started." : "No services are currently offered."}
          </p>
          {isOwner && (
            <Button onClick={() => handleOpenForm()}>New Service</Button>
          )}
        </div>
      )}

      {!isLoading && !error && services.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {services.map((service) => (
            <Card key={service.id}>
              <CardContent className="p-4 sm:p-6 flex flex-col h-full relative group">
                <div className="flex justify-between items-start mb-2">
                  <h4 className="font-semibold text-lg text-[var(--color-neutral-900)]">{service.name}</h4>
                  <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${service.isActive ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>
                    {service.isActive ? 'Active' : 'Inactive'}
                  </span>
                </div>
                
                {service.description && (
                  <p className="text-sm text-[var(--color-neutral-500)] mb-4 flex-grow line-clamp-2">
                    {service.description}
                  </p>
                )}
                
                <div className="mt-auto flex items-center justify-between pt-4 border-t border-[var(--color-neutral-100)]">
                  <div className="text-sm">
                    <span className="font-bold text-[var(--color-brand-600)]">{formatVND(service.price)}</span>
                    <span className="text-[var(--color-neutral-400)] mx-1">•</span>
                    <span className="text-[var(--color-neutral-600)]">{service.durationMinutes} min</span>
                  </div>
                  
                  {isOwner && (
                    <div className="flex gap-2 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                      <Button variant="secondary" size="sm" onClick={() => handleOpenForm(service)}>
                        Edit
                      </Button>
                      <Button variant="danger" size="sm" onClick={() => handleDeleteClick(service.id)} aria-label="Delete service">
                        <Trash2 size={16} />
                      </Button>
                    </div>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      {/* Delete Confirmation UI */}
      {deletingId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/50" role="dialog" aria-modal="true" aria-labelledby="delete-dialog-title">
          <div className="bg-white rounded-xl shadow-xl p-6 max-w-sm w-full animate-in fade-in zoom-in duration-200">
            <h3 id="delete-dialog-title" className="text-lg font-bold text-[var(--color-neutral-900)] mb-2">Delete Service?</h3>
            <p className="text-sm text-[var(--color-neutral-500)] mb-6">
              Are you sure you want to delete this service? This action cannot be undone and may fail if the service is actively used in bookings.
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="secondary" onClick={() => setDeletingId(null)}>Cancel</Button>
              <Button variant="danger" onClick={confirmDelete}>Delete</Button>
            </div>
          </div>
        </div>
      )}
    </AppShell>
  );
};

export default Services;
