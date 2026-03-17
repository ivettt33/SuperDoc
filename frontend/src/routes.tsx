import React from "react";
import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider, useAuth } from "./AuthContext";
import LoginPage from "./screens/LoginPage";
import RegisterPage from "./screens/RegisterPage";
import Dashboard from "./screens/Dashboard";
import ForgotPasswordPage from "./screens/ForgotPasswordPage";
import ResetPasswordPage from "./screens/ResetPasswordPage";
import DoctorOnboarding from "./pages/onboarding/DoctorOnboarding";
import PatientOnboarding from "./pages/onboarding/PatientOnboarding";
import ProfileDashboard from "./pages/ProfileDashboard";
import EditProfile from "./pages/EditProfile";
import AppointmentsList from "./pages/AppointmentsList";
import BookAppointment from "./pages/BookAppointment";
import PrescriptionsList from "./pages/PrescriptionsList";
import CreatePrescription from "./pages/CreatePrescription";
import EditPrescription from "./pages/EditPrescription";
import UsersList from "./pages/UsersList";
import Layout from "./components/Layout";
import RoleRoute from "./components/RoleRoute";

function PrivateRoute({ children }: { children: React.ReactElement }) {
  const { isAuthenticated, user } = useAuth();
  
  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  // If user data is not loaded yet but we have a token, allow access (user data will load)
  // This handles page refresh scenarios
  if (!user) {
    return children;
  }

  // Check if user needs onboarding (only redirect if not already on onboarding page)
  if (user.onboardingRequired) {
    const currentPath = window.location.pathname;
    if (user.role === "DOCTOR" && !currentPath.includes("/onboarding/doctor")) {
      return <Navigate to="/onboarding/doctor" replace />;
    }
    if (user.role === "PATIENT" && !currentPath.includes("/onboarding/patient")) {
      return <Navigate to="/onboarding/patient" replace />;
    }
  }

  return children;
}

function RoutesWithAuth() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/register" element={<RegisterPage />} />
      <Route path="/forgot-password" element={<ForgotPasswordPage />} />
      <Route path="/reset-password" element={<ResetPasswordPage />} />
      <Route path="/onboarding/doctor" element={<PrivateRoute><RoleRoute allowedRoles={["DOCTOR"]}><DoctorOnboarding /></RoleRoute></PrivateRoute>} />
      <Route path="/onboarding/patient" element={<PrivateRoute><RoleRoute allowedRoles={["PATIENT"]}><PatientOnboarding /></RoleRoute></PrivateRoute>} />
      <Route path="/profile" element={<PrivateRoute><Layout currentPage="account"><ProfileDashboard /></Layout></PrivateRoute>} />
      <Route path="/profile/edit" element={<PrivateRoute><Layout currentPage="account"><EditProfile /></Layout></PrivateRoute>} />
      <Route path="/appointments" element={<PrivateRoute><Layout currentPage="appointments"><AppointmentsList /></Layout></PrivateRoute>} />
      <Route path="/appointments/book" element={<PrivateRoute><RoleRoute allowedRoles={["PATIENT"]}><Layout currentPage="appointments"><BookAppointment /></Layout></RoleRoute></PrivateRoute>} />
      <Route path="/prescriptions" element={<PrivateRoute><Layout currentPage="prescriptions"><PrescriptionsList /></Layout></PrivateRoute>} />
      <Route path="/prescriptions/create" element={<PrivateRoute><RoleRoute allowedRoles={["DOCTOR"]}><Layout currentPage="prescriptions"><CreatePrescription /></Layout></RoleRoute></PrivateRoute>} />
      <Route path="/prescriptions/:id/edit" element={<PrivateRoute><RoleRoute allowedRoles={["DOCTOR"]}><Layout currentPage="prescriptions"><EditPrescription /></Layout></RoleRoute></PrivateRoute>} />
      <Route path="/users" element={<PrivateRoute><RoleRoute allowedRoles={["DOCTOR"]}><Layout currentPage="users"><UsersList /></Layout></RoleRoute></PrivateRoute>} />
      <Route path="/" element={<PrivateRoute><Layout currentPage="dashboard"><Dashboard /></Layout></PrivateRoute>} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}

export default function AppRoutes() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <RoutesWithAuth />
      </BrowserRouter>
    </AuthProvider>
  );
}


