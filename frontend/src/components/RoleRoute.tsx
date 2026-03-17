import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../AuthContext";

interface RoleRouteProps {
  children: React.ReactElement;
  allowedRoles: ("DOCTOR" | "PATIENT")[];
}

/**
 * Role-based route protection component
 * Only allows access if user has one of the allowed roles
 * 
 * Example usage:
 * <Route path="/doctors-only" element={
 *   <RoleRoute allowedRoles={["DOCTOR"]}>
 *     <DoctorOnlyPage />
 *   </RoleRoute>
 * } />
 */
export default function RoleRoute({ children, allowedRoles }: RoleRouteProps) {
  const { isAuthenticated, user } = useAuth();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  if (!allowedRoles.includes(user.role)) {
    // Redirect based on role
    if (user.role === "DOCTOR") {
      return <Navigate to="/" replace />;
    }
    return <Navigate to="/" replace />;
  }

  return children;
}

