import AppShell from '../components/AppShell';
import { useAuth } from '../app/auth/useAuth';

const Dashboard = () => {
  const { user } = useAuth();

  return (
    <AppShell title="Dashboard">
      <div className="placeholder-page">
        <p className="placeholder-welcome">
          Welcome back, <strong>{user?.username ?? 'User'}</strong>!
        </p>
        <p className="placeholder-hint">
          Dashboard metrics will appear here in a future week.
        </p>
      </div>
    </AppShell>
  );
};

export default Dashboard;
