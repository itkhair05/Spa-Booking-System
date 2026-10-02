import { useAuth } from '../app/auth/useAuth';

const Dashboard = () => {
  const { user, logout } = useAuth();

  const handleLogout = () => {
    logout();
  };

  return (
    <div className="p-6 min-h-screen bg-gray-50">
      <header className="flex justify-between items-center mb-8">
        <h1 className="text-3xl font-bold">Dashboard</h1>
        <button
          onClick={handleLogout}
          className="px-4 py-2 bg-red-600 text-white rounded"
        >
          Logout
        </button>
      </header>
      <section>
        <p className="text-lg">
          Welcome, {user?.username ?? 'User'}! You are authenticated as{' '}
          {user?.roles?.join(', ') ?? ''}.
        </p>
        {/* Future dashboard widgets will go here */}
      </section>
    </div>
  );
};

export default Dashboard;
