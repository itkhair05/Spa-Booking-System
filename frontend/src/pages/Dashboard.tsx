import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';
import { PageHeader } from '../components/ui/PageHeader';
import { Card, CardContent } from '../components/ui/Card';

const Dashboard = () => {
  const { user } = useAuth();

  return (
    <AppShell title="Dashboard">
      <PageHeader 
        title="Overview" 
        description={`Welcome back, ${user?.username ?? 'User'}. Here is what's happening at your spa today.`}
      />
      
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        <Card>
          <CardContent className="flex flex-col items-center justify-center h-32 text-center text-[var(--color-neutral-500)]">
            <span className="text-2xl mb-2">📈</span>
            <p className="text-sm">Metrics coming soon</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex flex-col items-center justify-center h-32 text-center text-[var(--color-neutral-500)]">
            <span className="text-2xl mb-2">📅</span>
            <p className="text-sm">Upcoming bookings coming soon</p>
          </CardContent>
        </Card>
        <Card>
          <CardContent className="flex flex-col items-center justify-center h-32 text-center text-[var(--color-neutral-500)]">
            <span className="text-2xl mb-2">💰</span>
            <p className="text-sm">Revenue stats coming soon</p>
          </CardContent>
        </Card>
      </div>
    </AppShell>
  );
};

export default Dashboard;
