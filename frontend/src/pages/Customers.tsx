import { useState, useEffect, useCallback } from 'react';
import AppShell from '../components/AppShell';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';
import { Button } from '../components/ui/Button';
import { getCustomers, deleteCustomer } from '../lib/api/customers';
import type { Customer } from '../types/customer';
import { CustomerForm } from './CustomerForm';
import { AlertCircle, Users, Trash2 } from 'lucide-react';
import { useAuth } from '../app/auth/useAuth';

const Customers = () => {
  const { user } = useAuth();
  const isOwner = user?.roles?.includes('ROLE_OWNER');

  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Form state
  const [isFormOpen, setIsFormOpen] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState<Customer | undefined>(undefined);
  const [deletingId, setDeletingId] = useState<number | null>(null);

  const fetchCustomers = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const data = await getCustomers();
      setCustomers(data);
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      setError(errorObj.response?.data?.message || 'Failed to load customers.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    fetchCustomers();
  }, [fetchCustomers]);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && deletingId !== null) {
        setDeletingId(null);
      }
    };
    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [deletingId]);

  const handleOpenForm = (customer?: Customer) => {
    setEditingCustomer(customer);
    setIsFormOpen(true);
  };

  const handleCloseForm = () => {
    setIsFormOpen(false);
    setEditingCustomer(undefined);
  };

  const handleFormSuccess = () => {
    handleCloseForm();
    fetchCustomers();
  };

  const handleDeleteClick = (id: number) => {
    if (!isOwner) return;
    setDeletingId(id);
  };

  const confirmDelete = async () => {
    if (!deletingId) return;
    try {
      await deleteCustomer(deletingId);
      setDeletingId(null);
      fetchCustomers();
    } catch (err: unknown) {
      const errorObj = err as { response?: { data?: { message?: string } } };
      alert(errorObj.response?.data?.message || 'Failed to delete customer. They may have related bookings.');
      setDeletingId(null);
    }
  };

  if (isFormOpen) {
    return (
      <AppShell title="Customers">
        <PageHeader title="Customer Management" />
        <div className="max-w-2xl mx-auto">
          <CustomerForm 
            customer={editingCustomer}
            onSuccess={handleFormSuccess}
            onCancel={handleCloseForm}
          />
        </div>
      </AppShell>
    );
  }

  return (
    <AppShell title="Customers">
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-6 gap-4">
        <PageHeader title="Customer Management" description="Manage your spa clients." />
        <Button onClick={() => handleOpenForm()}>New Customer</Button>
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
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">Unable to load customers</h3>
          <p className="text-[var(--color-neutral-500)] mb-6 text-center max-w-md">{error}</p>
          <Button onClick={fetchCustomers}>Try Again</Button>
        </div>
      )}

      {!isLoading && !error && customers.length === 0 && (
        <div className="flex flex-col items-center justify-center p-12 bg-white rounded-xl border border-[var(--color-neutral-200)] text-center">
          <Users className="text-[var(--color-neutral-400)] mb-4" size={40} />
          <h3 className="text-lg font-semibold text-[var(--color-neutral-900)] mb-2">No customers yet</h3>
          <p className="text-[var(--color-neutral-500)] mb-6">
            {isOwner ? "Add your first customer." : "No customers are currently available."}
          </p>
          <Button onClick={() => handleOpenForm()}>New Customer</Button>
        </div>
      )}

      {!isLoading && !error && customers.length > 0 && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {customers.map((customer) => (
            <Card key={customer.id}>
              <CardContent className="p-4 sm:p-6 flex flex-col h-full relative group">
                <div className="flex justify-between items-start mb-2">
                  <h4 className="font-semibold text-lg text-[var(--color-neutral-900)]">{customer.name}</h4>
                  {customer.isActive !== undefined && (
                    <span className={`px-2 py-0.5 rounded-full text-xs font-medium border ${customer.isActive ? 'bg-green-50 text-green-700 border-green-200' : 'bg-gray-50 text-gray-600 border-gray-200'}`}>
                      {customer.isActive ? 'Active' : 'Inactive'}
                    </span>
                  )}
                </div>
                
                <div className="text-sm text-[var(--color-neutral-600)] space-y-1 mb-4 flex-grow">
                  {customer.email && (
                    <p>
                      <strong>Email:</strong> {customer.email}
                    </p>
                  )}
                  {customer.phone && (
                    <p>
                      <strong>Phone:</strong> {customer.phone}
                    </p>
                  )}
                  {customer.lastVisit && (
                    <p>
                      <strong>Last Visit:</strong>{' '}
                      {new Date(customer.lastVisit).toLocaleString('vi-VN', { dateStyle: 'medium', timeStyle: 'short' })}
                    </p>
                  )}
                </div>
                
                <div className="mt-auto flex items-center justify-end pt-4 border-t border-[var(--color-neutral-100)]">
                  <div className="flex gap-2 opacity-100 md:opacity-0 md:group-hover:opacity-100 transition-opacity">
                    <Button variant="secondary" size="sm" onClick={() => handleOpenForm(customer)}>
                      Edit
                    </Button>
                    {isOwner && (
                      <Button variant="danger" size="sm" onClick={() => handleDeleteClick(customer.id)} aria-label="Delete customer">
                        <Trash2 size={16} />
                      </Button>
                    )}
                  </div>
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
            <h3 id="delete-dialog-title" className="text-lg font-bold text-[var(--color-neutral-900)] mb-2">Delete Customer?</h3>
            <p className="text-sm text-[var(--color-neutral-500)] mb-6">
              Are you sure you want to delete this customer? This action cannot be undone and may fail if they are associated with existing bookings.
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

export default Customers;
