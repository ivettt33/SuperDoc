import { Appointment, AppointmentStatus } from "../api";

/**
 * Gets the CSS classes for appointment status badges
 */
export function getAppointmentStatusColor(status: AppointmentStatus | string): string {
  switch (status) {
    case "CONFIRMED":
      return "bg-green-500/20 text-green-400 border-green-500/50";
    case "SCHEDULED":
      return "bg-blue-500/20 text-blue-400 border-blue-500/50";
    case "CANCELLED":
      return "bg-red-500/20 text-red-400 border-red-500/50";
    case "COMPLETED":
      return "bg-gray-500/20 text-gray-400 border-gray-500/50";
    case "PASSED":
      return "bg-yellow-500/20 text-yellow-400 border-yellow-500/50";
    default:
      return "bg-gray-500/20 text-gray-400 border-gray-500/50";
  }
}

/**
 * Gets the priority value for sorting appointments by status
 * Lower number = higher priority (shown first)
 */
function getStatusPriority(status: AppointmentStatus | string): number {
  switch (status) {
    case "SCHEDULED":
    case "CONFIRMED":
      return 0; // Highest priority - show first
    case "PASSED":
      return 1; // Medium priority - show after active appointments
    case "COMPLETED":
      return 2; // Lower priority
    case "CANCELLED":
      return 3; // Lowest priority - show last
    default:
      return 1;
  }
}

/**
 * Sorts appointments: active ones first (by date), then passed, then cancelled/completed last
 */
export function sortAppointments(appointments: Appointment[]): Appointment[] {
  return [...appointments].sort((a, b) => {
    // Get display status for sorting (considers if appointment has passed)
    const displayStatusA = getDisplayStatus(a);
    const displayStatusB = getDisplayStatus(b);
    
    const statusPriorityA = getStatusPriority(displayStatusA);
    const statusPriorityB = getStatusPriority(displayStatusB);

    // First sort by status priority
    if (statusPriorityA !== statusPriorityB) {
      return statusPriorityA - statusPriorityB;
    }

    // If same status priority, sort by date/time (earliest first)
    return new Date(a.appointmentDateTime).getTime() - new Date(b.appointmentDateTime).getTime();
  });
}

/**
 * Formats a datetime string into readable date and time
 */
export function formatAppointmentDateTime(dateTime: string): { date: string; time: string } {
  const date = new Date(dateTime);
  return {
    date: date.toLocaleDateString("en-US", { 
      weekday: "long", 
      year: "numeric", 
      month: "long", 
      day: "numeric" 
    }),
    time: date.toLocaleTimeString("en-US", { 
      hour: "2-digit", 
      minute: "2-digit" 
    })
  };
}

/**
 * Checks if an appointment has already passed (date/time is in the past)
 */
export function isAppointmentPassed(appointment: Appointment): boolean {
  const appointmentDate = new Date(appointment.appointmentDateTime);
  const now = new Date();
  return appointmentDate < now;
}

/**
 * Gets the display status for an appointment
 * Returns "PASSED" if the appointment has passed and is not cancelled/completed
 */
export function getDisplayStatus(appointment: Appointment): AppointmentStatus | "PASSED" {
  if (appointment.status === "CANCELLED" || appointment.status === "COMPLETED") {
    return appointment.status;
  }
  
  if (isAppointmentPassed(appointment)) {
    return "PASSED";
  }
  
  return appointment.status;
}

