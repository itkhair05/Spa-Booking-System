import { useEffect, lazy, Suspense, type ReactNode } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import { AuthProvider } from './app/auth/AuthContext';
import { useAuth } from './app/auth/useAuth';
import PublicBooking from './pages/PublicBooking';

const LoginPage = lazy(() => import('./pages/LoginPage'));
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Bookings = lazy(() => import('./pages/Bookings'));
const Customers = lazy(() => import('./pages/Customers'));
const Services = lazy(() => import('./pages/Services'));
const Staff = lazy(() => import('./pages/Staff'));
const FeedbackPage = lazy(() => import('./pages/Feedback'));
const Articles = lazy(() => import('./pages/Articles'));
const ReviewsPage = lazy(() => import('./pages/Reviews'));
const Settings = lazy(() => import('./pages/Settings'));
const BookingResultPage = lazy(() => import('./pages/BookingResultPage'));

/**
 * Ensures direct loads, reloads, and page navigation start at the top of the page
 * unless a specific hash anchor is targeted. Disables browser automatic scroll restoration
 * to avoid jumping down to lazily measured sections.
 */
function ScrollToTop() {
  const { pathname, hash } = useLocation();

  useEffect(() => {
    if ('scrollRestoration' in window.history) {
      window.history.scrollRestoration = 'manual';
    }
  }, []);

  useEffect(() => {
    if (hash) {
      const targetId = hash.replace('#', '');
      const element = document.getElementById(targetId);
      if (element) {
        element.scrollIntoView({ behavior: 'smooth' });
        return;
      }
    }
    window.scrollTo({ top: 0, left: 0, behavior: 'instant' as ScrollBehavior });
  }, [pathname, hash]);

  return null;
}

/**
 * Guard for authenticated-only routes.
 * Redirects to /login, preserving the intended destination as a query param.
 */
const ProtectedRoute = ({ children }: { children: ReactNode }) => {
  const { user, isLoading } = useAuth();
  const location = useLocation();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <span className="text-slate-500 text-sm">Đang tải...</span>
      </div>
    );
  }

  if (!user) {
    const redirect = location.pathname !== '/' ? `?redirect=${encodeURIComponent(location.pathname)}` : '';
    return <Navigate to={`/login${redirect}`} replace />;
  }

  return <>{children}</>;
};

/**
 * Guard for public-only routes (e.g. /login).
 * Redirects authenticated users to /dashboard.
 */
const PublicRoute = ({ children }: { children: ReactNode }) => {
  const { user, isLoading } = useAuth();

  if (isLoading) {
    return (
      <div className="min-h-screen flex items-center justify-center">
        <span className="text-slate-500 text-sm">Đang tải...</span>
      </div>
    );
  }

  return user ? <Navigate to="/dashboard" replace /> : <>{children}</>;
};

function PageFallback() {
  return (
    <div className="min-h-screen bg-stone-50 flex items-center justify-center font-sans" aria-busy="true">
      <div className="w-8 h-8 rounded-full border-2 border-stone-300 border-t-[#465d4c] animate-spin" />
      <span className="sr-only">Đang tải nội dung...</span>
    </div>
  );
}

function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <ScrollToTop />
        <Suspense fallback={<PageFallback />}>
          <Routes>
            {/* Public */}
            <Route
              path="/login"
              element={
                <PublicRoute>
                  <LoginPage />
                </PublicRoute>
              }
            />

            <Route
              path="/spas/:slug"
              element={<PublicBooking />}
            />

            {/* Legacy slug redirect */}
            <Route path="/spas/demo-spa" element={<Navigate to="/spas/tikey-spa" replace />} />

            {/* Protected */}
            <Route
              path="/dashboard"
              element={
                <ProtectedRoute>
                  <Dashboard />
                </ProtectedRoute>
              }
            />
            <Route
              path="/bookings"
              element={
                <ProtectedRoute>
                  <Bookings />
                </ProtectedRoute>
              }
            />
            <Route
              path="/customers"
              element={
                <ProtectedRoute>
                  <Customers />
                </ProtectedRoute>
              }
            />
            <Route
              path="/services"
              element={
                <ProtectedRoute>
                  <Services />
                </ProtectedRoute>
              }
            />
            <Route
              path="/staff"
              element={
                <ProtectedRoute>
                  <Staff />
                </ProtectedRoute>
              }
            />
            <Route
              path="/articles"
              element={
                <ProtectedRoute>
                  <Articles />
                </ProtectedRoute>
              }
            />
            <Route
              path="/reviews"
              element={
                <ProtectedRoute>
                  <ReviewsPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/feedback"
              element={
                <ProtectedRoute>
                  <FeedbackPage />
                </ProtectedRoute>
              }
            />
            <Route
              path="/settings"
              element={
                <ProtectedRoute>
                  <Settings />
                </ProtectedRoute>
              }
            />

            {/* Public landing on root, tra-cuu, and dat-lich callback/ket-qua */}
            <Route path="/" element={<PublicBooking />} />
            <Route path="/tra-cuu" element={<PublicBooking />} />
            <Route path="/dat-lich/callback" element={<BookingResultPage />} />
            <Route path="/dat-lich/ket-qua" element={<BookingResultPage />} />

            {/* Catch-all */}
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </Suspense>
      </BrowserRouter>
    </AuthProvider>
  );
}

export default App;
